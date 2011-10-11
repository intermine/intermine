package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Display pathway for this gene, plus the count of the other genes on this pathway
 *
 * @author Julie Sullivan
 */
public class PathwaysDisplayer extends ReportDisplayer
{

    private Map<InterMineObject, Integer> pathways = new HashMap<InterMineObject, Integer>();
    private Map<InterMineObject, Map<InterMineObject, Integer>> cache
        = new HashMap<InterMineObject, Map<InterMineObject, Integer>>();

    /**
     * Construct with config and the InterMineAPI.
     *
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public PathwaysDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        InterMineObject gene = reportObject.getObject();
        if (cache.get(gene) != null) {
            pathways = cache.get(gene);
        } else {
            try {
                Collection col = (Collection) gene.getFieldValue("pathways");
                for (Object item : col) {
                    InterMineObject pathway = (InterMineObject) item;
                    if (pathway != null) {
                        Collection genes = (Collection) pathway.getFieldValue("genes");
                        pathways.put(pathway, genes.size());
                    }
                }
                cache.put(gene, pathways);
            } catch (IllegalAccessException e) {
                // oops
            }
        }
        if (pathways.isEmpty()) {
            request.setAttribute("noPathwayResults", "No pathways found");
        } else {
            request.setAttribute("pathways", pathways);
        }
    }
}
