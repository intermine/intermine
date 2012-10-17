package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.BadTemplateException;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * A service to enable templates to be uploaded programmatically.
 * @author Alexis Kalderimis
 *
 */
public class TemplateUploadService extends WebService
{

    /** The key for the templates parameter **/
    public static final String TEMPLATES_PARAMETER = "xml";
    /** The key for the version parameter **/
    public static final String VERSION_PARAMETER = "version";

    /** Usage information to help users **/
    public static final String USAGE =
          "\nTemplate Upload Service:\n"
        + "==========================\n"
        + "Parameters:\n"
        + TEMPLATES_PARAMETER + ": XML representation of template(s)\n"
        + VERSION_PARAMETER + ": (optional) XML version number\n"
        + "NOTE: all template upload requests must be authenticated.\n";

    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public TemplateUploadService(InterMineAPI im) {
        super(im);
    }
    
    @Override
    protected void validateState() {
        if (!getPermission().isRW()) {
            throw new ServiceForbiddenException("This request does not have RW permission.");
        }
    }

    @Override
    protected void execute() throws Exception {
        if (!isAuthenticated()) {
            throw new ServiceException("Not authenticated" + USAGE);
        }
        String templatesXML = request.getParameter(TEMPLATES_PARAMETER);
        if (templatesXML == null || "".equals(templatesXML)) {
            throw new ServiceException("No template XML data." + USAGE);
        }
        Profile profile = getPermission().getProfile();

        int version = getVersion(request);
        Reader r = new StringReader(templatesXML);

        Map<String, TemplateQuery> templates;
        try {
            templates = TemplateQueryBinding.unmarshalTemplates(r, version);
        } catch (RuntimeException e) {
            throw new ServiceException("Error parsing templates", e);
        }

        for (Entry<String, TemplateQuery> pair: templates.entrySet()) {
            String name = pair.getKey();
            TemplateQuery templ = pair.getValue();
            if (!templ.isValid()) {
                throw new BadRequestException("Query contains errors:"
                    + formatMessage(templ.verifyQuery()));
            }
            try {
                profile.saveTemplate(name, new ApiTemplate(templ));
                this.output.addResultItem(Arrays.asList(name, "Success"));
            } catch (BadTemplateException bte) {
                throw new BadRequestException("The template has invalid name or empty title.");
            } catch (RuntimeException e) {
                throw new ServiceException("Failed to save template: " + name, e);
            }
        }
    }

    private String formatMessage(List<String> msgs) {
        StringBuilder sb = new StringBuilder();
        for (String msg : msgs) {
            sb.append(msg);
            if (!msg.endsWith(".")) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private int getVersion(HttpServletRequest request) {
        String versionString = request.getParameter(VERSION_PARAMETER);
        if (versionString == null || "".equals(versionString)) {
            return TemplateQuery.USERPROFILE_VERSION;
        }
        try {
            int version = Integer.parseInt(versionString);
            return version;
        } catch (NumberFormatException e) {
            throw new ServiceException("Version provided in request (" + versionString
                    + ") can not be parsed to an integer");
        }
    }

}
