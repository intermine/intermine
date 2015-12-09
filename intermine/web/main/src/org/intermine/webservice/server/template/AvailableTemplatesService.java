package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateHelper;
import org.intermine.api.template.TemplateManager;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.NotAcceptableException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;

/**
 * Fetch the names of public template queries for use with the Templates web service.
 * @author Richard Smith
 */
public class AvailableTemplatesService extends WebService
{

    private static final String FILE_BASE_NAME = "templates";

    /**
     * Constructor.
     * @param im The InterMineAPI for this webservice
     */
    public AvailableTemplatesService(InterMineAPI im) {
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

        TemplateManager templateManager = im.getTemplateManager();
        Map<String, ApiTemplate> templates;
        boolean includeBroken = Boolean.parseBoolean(request.getParameter("includeBroken"));
        if (isAuthenticated()) {
            Profile profile = getPermission().getProfile();
            templates = (includeBroken)
                            ? templateManager.getUserAndGlobalTemplates(profile)
                            : templateManager.getWorkingTemplates(profile);

        } else {
            templates = (includeBroken)
                            ? templateManager.getGlobalTemplates()
                            : templateManager.getWorkingTemplates();
        }

        switch (getFormat()) {
            case XML:
                output.addResultItem(Arrays.asList(TemplateHelper.apiTemplateMapToXml(templates,
                        PathQuery.USERPROFILE_VERSION)));
                break;
            case JSON:
                Map<String, Object> attributes = new HashMap<String, Object>();
                if (formatIsJSONP()) {
                    attributes.put(JSONFormatter.KEY_CALLBACK, getCallback());
                }
                attributes.put(JSONFormatter.KEY_INTRO, "\"templates\":");
                output.setHeaderAttributes(attributes);
                output.addResultItem(Arrays.asList(
                        TemplateHelper.apiTemplateMapToJson(im, templates)));
                break;
            case TEXT:
                Set<String> templateNames = new TreeSet<String>(templates.keySet());
                for (String templateName : templateNames) {
                    output.addResultItem(Arrays.asList(templateName));
                }
            case HTML:
                throw new ServiceException("Not implemented: " + Format.HTML);
            default:
                throw new NotAcceptableException();
        }
    }
}
