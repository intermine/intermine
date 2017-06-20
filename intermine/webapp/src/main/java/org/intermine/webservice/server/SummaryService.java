package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
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
import org.intermine.webservice.server.core.JSONService;

/**
 * Serve up the paths used to summarise each class.
 * @author Alexis Kalderimis
 *
 */
public class SummaryService extends JSONService
{

    private static final Logger LOG = Logger.getLogger(SummaryService.class);

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
        Boolean refsAllowed = !Boolean.valueOf(getOptionalParameter("norefs", "false"));
        WebConfig webConfig = InterMineContext.getWebConfig();

        Map<String, Object> summaryFieldsForCd = getMapping(refsAllowed, webConfig);

        addResultItem(summaryFieldsForCd, false);
    }

    private Map<String, Object> getMapping(Boolean refsAllowed, WebConfig webConfig) {
        Map<String, Object> summaryFieldsForCd = new HashMap<String, Object>();
        Model m = im.getModel();
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
        return summaryFieldsForCd;
    }

    @Override
    protected String getResultsKey() {
        return "classes";
    }

    @Override
    protected String getDefaultFileName() {
        return "summary_fields.json";
    }

}

