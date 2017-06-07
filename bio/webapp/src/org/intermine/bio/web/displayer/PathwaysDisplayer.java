package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

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

    private Map<Integer, Map<InterMineObject, Integer>> cache =
        new ConcurrentHashMap<Integer, Map<InterMineObject, Integer>>();

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
        Map<InterMineObject, Integer> pathways = getPathways(gene);
        if (pathways.isEmpty()) {
            request.setAttribute("noPathwayResults", "No pathways found");
        } else {
            SortedMap<InterMineObject, Integer> sortedPathways =
                new TreeMap<InterMineObject, Integer>(new ValueComparator(pathways));
            sortedPathways.putAll(pathways);
            request.setAttribute("pathways", sortedPathways);
        }
    }

    private Map<InterMineObject, Integer> getPathways(InterMineObject gene) {
        if (!cache.containsKey(gene.getId())) {
            try {
                Map<InterMineObject, Integer> pathways = new HashMap<InterMineObject, Integer>();
                Collection col = (Collection) gene.getFieldValue("pathways");
                for (Object item : col) {
                    InterMineObject pathway = (InterMineObject) item;
                    if (pathway != null) {
                        Collection genes = (Collection) pathway.getFieldValue("genes");
                        pathways.put(pathway, genes.size());
                    }
                }
                cache.put(gene.getId(), pathways);
            } catch (IllegalAccessException e) {
                // oops
            }
        }
        return cache.get(gene.getId());
    }
}

/**
 * sorting pathway gene counts descending
 *
 * @author Julie
 */
class ValueComparator implements Comparator
{
    Map base;

    /**
     * @param base map
     */
    public ValueComparator(Map base) {
        this.base = base;
    }

    @Override
    public int compare(Object a, Object b) {
        // sort descending
        int i = ((Integer) base.get(a)).compareTo((Integer) base.get(b));

        // gene counts are the same, sort by name ascending
        if (i == 0) {
            InterMineObject aObject = (InterMineObject) a;
            InterMineObject bObject = (InterMineObject) b;

            try {
                String aName = (String) aObject.getFieldValue("name");
                String bName = (String) bObject.getFieldValue("name");
                return aName.compareTo(bName);
            } catch (IllegalAccessException e) {
                // bad pathway, return zero
            }
        }
        return i;
    }
}

