package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.xml.ProfileBinding;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.Emailer;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.user.DeletionTokens.TokenExpired;

/** @author Alex Kalderimis **/
public class DeregistrationService extends WebService
{

    private DeletionTokens tokens;

    /** @param im The InterMine state object. **/
    public DeregistrationService(InterMineAPI im) {
        super(im);
        this.tokens = DeletionTokens.getInstance();
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated() || getPermission().isRO()) {
            throw new ServiceForbiddenException("Access denied");
        }
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.XML;
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "data.xml");
        return new StreamedOutput(out, new UserDataFormatter(), separator);
    }

    @Override
    protected void execute() {
        String uuid = getRequiredParameter("deregistrationToken");
        DeletionToken token;
        Profile profile = getPermission().getProfile();
        try {
            UUID key = UUID.fromString(uuid);
            token = tokens.retrieveToken(key);
        } catch (IllegalArgumentException  e) {
            throw new BadRequestException(uuid + " is not a deletion token.");
        } catch (TokenExpired e) {
            throw new BadRequestException("token expired.");
        }
        if (!profile.equals(token.getProfile())) {
            throw new ServiceForbiddenException("Access denied");
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer;
        try {
            writer = factory.createXMLStreamWriter(os);
        } catch (XMLStreamException e) {
            throw new ServiceException("Could not export personal data", e);
        }
        try {
            ProfileBinding.marshal(
                    profile,
                    im.getUserProfile(),
                    writer,
                    PathQuery.USERPROFILE_VERSION,
                    im.getClassKeys()
            );
        } catch (Exception e) {
            throw new ServiceException("Error exporting userprofile", e);
        }
        String userData = os.toString();
        MailAction action = new GoodbyeAction(profile.getUsername(), userData);
        if (!InterMineContext.queueMessage(action)) {
            throw new ServiceException(
                    "Cannot send your archive at this time, try again later.", 503);
        }
        try {
            im.getProfileManager().deleteProfile(profile);
        } catch (ObjectStoreException e) {
            throw new ServiceException("Could not delete your profile.", e);
        }
        output.addResultItem(Arrays.asList(userData));
    }

    private class GoodbyeAction implements MailAction
    {

        private final String to, xml;

        GoodbyeAction(String to, String xml) {
            this.to = to;
            this.xml = xml;
        }

        @Override
        public void act(Emailer emailer) throws Exception {
            emailer.sendFareWell(to, xml);
        }
    }
}
