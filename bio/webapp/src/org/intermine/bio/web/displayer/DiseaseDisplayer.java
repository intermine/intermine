package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.Gene;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * For ratmine, display all disease ontology objects for orthologues
 *
 * @author Julie Sullivan
 */
public class DiseaseDisplayer extends ReportDisplayer
{
    private static final String RAT = "R. norvegicus";
    protected static final Logger LOG = Logger.getLogger(DiseaseDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     *
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public DiseaseDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Gene gene = (Gene) reportObject.getObject();
        Set<String> orthologues = getLocalHomologues(gene);
        if (orthologues != null && !orthologues.isEmpty()) {
            request.setAttribute("ratGenes", StringUtil.join(orthologues, ","));
        }
    }

    private PathQuery getQuery(Gene gene) {
        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.homologues.homologue.primaryIdentifier",
                "Gene.homologues.homologue.secondaryIdentifier");
        q.addConstraint(Constraints.eq("Gene.primaryIdentifier", gene.getPrimaryIdentifier()));
        q.addConstraint(Constraints.eq("Gene.homologues.homologue.organism.shortName", RAT));
        return q;
    }

    private Set<String> getLocalHomologues(Gene gene) {
        Set<String> orthologues = new HashSet<String>();
        ProfileManager profileManager = im.getProfileManager();
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        try {
            PathQuery q = getQuery(gene);
        } catch (Exception e) {
            return Collections.emptySet();
        }
        if (!q.isValid()) {
            return Collections.emptySet();
        }
        ExportResultsIterator it = executor.execute(q);
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String identifier = (String) row.get(0).getField();
            String secondaryIdentifier = (String) row.get(1).getField();
            if (!StringUtils.isEmpty(identifier)) {
                orthologues.add(identifier);
            } else if (!StringUtils.isEmpty(secondaryIdentifier)) {
                orthologues.add(secondaryIdentifier);
            }
        }
        return orthologues;
    }
}
