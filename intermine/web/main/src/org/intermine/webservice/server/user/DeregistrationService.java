package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.xml.ProfileBinding;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
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

public class DeregistrationService extends WebService {

    private DeletionTokens tokens;

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
            ServiceException e = new ServiceException("Cannot send your archive at this time, try again later.");
            e.setHttpErrorCode(503);
            throw e;
        }
        try {
            im.getProfileManager().deleteProfile(profile);
        } catch (ObjectStoreException e) {
            throw new ServiceException("Could not delete your profile.", e);
        }
        output.addResultItem(Arrays.asList(userData));
    }

    private class GoodbyeAction implements MailAction {

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
