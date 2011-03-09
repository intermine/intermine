package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * Fetch the names of public template queries for use with the Templates web service.
 * @author Richard Smith
 */
public class AvailableTemplatesService extends WebService
{

    private static final String DEFAULT_CALLBACK = "analyseTemplates";

    private static final String FILE_BASE_NAME= "templates";

    private static final String XML = "xml";

    /**
     * Constructor.
     * @param im The InterMineAPI for this webservice
     */
    public AvailableTemplatesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String pathFromUrl = request.getPathInfo();
        pathFromUrl = StringUtil.trimSlashes(pathFromUrl);

        TemplateManager templateManager = im.getTemplateManager();
        Map<String, TemplateQuery> templates = templateManager.getGlobalTemplates();

        if (pathFromUrl != null && XML.equalsIgnoreCase(pathFromUrl)) {
            ResponseUtil.setXMLHeader(response, FILE_BASE_NAME + "." + XML);
            output.addResultItem(Arrays.asList(TemplateHelper.templateMapToXml(templates,
                    PathQuery.USERPROFILE_VERSION)));
        } else if (formatIsJSON()) {
            ResponseUtil.setJSONHeader(response,  FILE_BASE_NAME + ".json");
            Map<String, String> attributes = new HashMap<String, String>();
            if (formatIsJSONP()) {
                String callback = getCallback();
                if (callback == null || "".equals(callback)) {
                    callback = DEFAULT_CALLBACK;
                }
                attributes.put(JSONFormatter.KEY_CALLBACK, callback);
            }
            output.setHeaderAttributes(attributes);
            output.addResultItem(Arrays.asList(TemplateHelper.templateMapToJson(templates)));
        } else {
            ResponseUtil.setPlainTextHeader(response, FILE_BASE_NAME + ".txt");
            Set<String> templateNames = new TreeSet<String>(templates.keySet());
            for (String templateName : templateNames) {
                output.addResultItem(Arrays.asList(templateName));
            }
        }
    }
}
