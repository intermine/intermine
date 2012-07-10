package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONObject;

/**
 * Serve up the paths used to summarise each class.
 * @author Alexis Kalderimis
 *
 */
public class SummaryService extends WebService
{

	private final static Logger LOG = Logger.getLogger(SummaryService.class);

    /**
     * Constructor
     * @param im InterMine settings
     */
    public SummaryService(InterMineAPI im) {
        super(im);
    }

    /**
     * @see org.intermine.webservice.server.WebService#execute(
     * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * @throws Exception if anything goes wrong
     */
    @Override
    protected void execute() throws Exception {

        Boolean refsAllowed = !Boolean.valueOf(request.getParameter("norefs"));
        Map<String, List<String>> summaryFieldsForCd = new HashMap<String, List<String>>();
        WebConfig webConfig = InterMineContext.getWebConfig();
        Model m = im.getModel();
        output.setHeaderAttributes(getHeaderAttributes());
        for (ClassDescriptor cd: m.getClassDescriptors()) {
            List<String> summaryFields = new ArrayList<String>();
            if (!"org.intermine.model.InterMineObject".equals(cd.getName())) {
                for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cd)) {
                	try {
			            Path p = new Path(m, cd.getUnqualifiedName() + "." + fc.getFieldExpr());
			            if (p.endIsAttribute() && (!p.containsReferences() || refsAllowed)
			                    && fc.getShowInSummary()) {
			                summaryFields.add(p.getNoConstraintsString());
			            }
                	} catch (PathException e) {
                		LOG.warn("Web config contains a bad path!", e);
                	}
                }
                summaryFieldsForCd.put(cd.getUnqualifiedName(), summaryFields);
            }
        }

        JSONObject jo = new JSONObject(summaryFieldsForCd);
        output.addResultItem(Collections.singletonList(jo.toString()));
    }

    /**
     * Get attributes for header.
     * @return a map from attribute name to value
     */
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"classes\":");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        }
        return attributes;
    }

    @Override
    protected String getDefaultFileName() {
        return "summary_fields.json";
    }

    @Override
    public int getFormat() {
        if (hasCallback()) {
            return JSONP_FORMAT;
        } else {
            return JSON_FORMAT;
        }
    }
}

