package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;

/**
 * Utility methods for Cytoscape Web and interaction data.
 *
 * @author Fengyuan Hu
 */
public final class CytoscapeNetworkUtil
{
    protected static final Logger LOG = Logger.getLogger(CytoscapeNetworkUtil.class);

    private static Map<String, Set<String>> interactionInfoMap = null;

    private CytoscapeNetworkUtil() {
        super();
    }

    /**
     * Get the interaction information including the organisms and data sources.
     *
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a map in which the key is organism name and value is a list of data sources
     */
    public static synchronized Map<String, Set<String>> getInteractionInfo(
            Model model, PathQueryExecutor executor) {
        // Check Interaction class in the model
        if (!model.getClassNames().contains(model.getPackageName() + ".Interaction")) {
            return null;
        } else if (interactionInfoMap == null) {
            queryInteractionInfo(model, executor);
        }

        return interactionInfoMap;
    }

    /**
     * Query the general interaction information.
     *
     * @param model the Model
     * @param executor the PathQueryExecutor
     */
    private static void queryInteractionInfo(Model model, PathQueryExecutor executor) {
        interactionInfoMap = new LinkedHashMap<String, Set<String>>();

        PathQuery query = new PathQuery(model);

        query.addViews("Interaction.gene1.organism.name",
                "Interaction.details.dataSets.dataSource.name");

        query.addOrderBy("Interaction.gene1.organism.name", OrderDirection.ASC);

        ExportResultsIterator result = executor.execute(query);

        while (result.hasNext()) {
            List<ResultElement> row = result.next();

            String orgName = (String) row.get(0).getField();
            String dataSource = (String) row.get(1).getField();

            if (interactionInfoMap.size() < 1) {
                Set<String> dataSourceSet = new LinkedHashSet<String>();
                dataSourceSet.add(dataSource);
                interactionInfoMap.put(orgName, dataSourceSet);
            } else {
                if (interactionInfoMap.containsKey(orgName)) {
                    interactionInfoMap.get(orgName).add(dataSource);
                } else {
                    Set<String> dataSourceSet = new LinkedHashSet<String>();
                    dataSourceSet.add(dataSource);
                    interactionInfoMap.put(orgName, dataSourceSet);
                }
            }
        }
    }
}
