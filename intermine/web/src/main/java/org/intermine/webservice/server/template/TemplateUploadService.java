package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.BadTemplateException;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * A service to enable templates to be uploaded.
 * @author Alexis Kalderimis
 *
 */
public class TemplateUploadService extends WebService
{

    /** The key for the templates parameter **/
    public static final String TEMPLATES_PARAMETER = "xml";
    /** The key for the version parameter **/
    public static final String VERSION_PARAMETER = "version";

    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public TemplateUploadService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated()) {
            throw new ServiceForbiddenException("This request is not authenticated.");
        }
        if (!getPermission().isRW()) {
            throw new ServiceForbiddenException("This request does not have RW permission.");
        }
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
            case TEXT:
            case JSON:
            case HTML:
            case XML:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void execute() throws Exception {

        String templatesXML = "";
        String contentType = StringUtils.defaultString(request.getContentType(), "");
        if (contentType.contains("application/x-www-form-urlencoded")
                || "GET".equalsIgnoreCase(request.getMethod())) {
            templatesXML = getRequiredParameter(TEMPLATES_PARAMETER);
        } else {
            templatesXML = IOUtils.toString(request.getInputStream());
        }
        Profile profile = getPermission().getProfile();

        int version = getIntParameter(VERSION_PARAMETER, TemplateQuery.USERPROFILE_VERSION);
        Reader r = new StringReader(templatesXML);

        Map<String, TemplateQuery> templates;
        try {
            templates = TemplateQueryBinding.unmarshalTemplates(r, version);
        } catch (Exception e) {
            throw new BadRequestException("Could not parse templates: " + e.getMessage(), e);
        }
        for (TemplateQuery t: templates.values()) {
            if (!t.isValid()) {
                String message = String.format("Template %s contains errors: %s",
                    StringUtils.defaultIfBlank(t.getName(), "NO-NAME"),
                    formatMessage(t.verifyQuery()));
                throw new BadRequestException(message);
            }
        }

        for (Entry<String, TemplateQuery> pair: templates.entrySet()) {
            String name = pair.getKey();
            try {
                profile.saveTemplate(name, new ApiTemplate(pair.getValue()));
            } catch (BadTemplateException bte) {
                throw new BadRequestException("The template has an invalid name.");
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


}
