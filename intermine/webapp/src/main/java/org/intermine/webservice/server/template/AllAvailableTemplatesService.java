package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateHelper;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;

import org.intermine.webservice.server.exceptions.NotAcceptableException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.UnauthorizedException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;

import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * Fetch the names of all the templates created by any user
 * @author Daniela Butano
 */
public class AllAvailableTemplatesService extends WebService
{

    private static final String FILE_BASE_NAME = "templates";

    /**
     * Constructor.
     * @param im The InterMineAPI for this webservice
     */
    public AllAvailableTemplatesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, FILE_BASE_NAME + ".xml");
        return new StreamedOutput(out, new PlainFormatter(), separator);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.XML;
    }

    @Override
    protected boolean canServe(Format format) {
        return Format.BASIC_FORMATS.contains(format);
    }

    @Override
    protected void execute() throws Exception {
        if (isAuthenticated()) {
            Profile authenticatedProfile = getPermission().getProfile();
            if (!authenticatedProfile.isSuperuser()) {
                throw new UnauthorizedException("The request must be authenticated"
                        + " by a super user");
            }
            switch (getFormat()) {
                case XML:
                    output.addResultItem(Arrays.asList(TemplateHelper.allTemplatesMapToXml(im)));
                    break;
                case JSON:
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put(JSONFormatter.KEY_INTRO, "\"templates\":");
                    output.setHeaderAttributes(attributes);
                    output.addResultItem(Arrays.asList(TemplateHelper.allTemplatesMapToJson(im)));
                    break;
                case TEXT:
                    throw new ServiceException("Not implemented: " + Format.TEXT);
                case HTML:
                    throw new ServiceException("Not implemented: " + Format.HTML);
                default:
                    throw new NotAcceptableException();
            }
        } else {
            throw new UnauthorizedException("The request must be authenticated by a super user");
        }
    }
}
