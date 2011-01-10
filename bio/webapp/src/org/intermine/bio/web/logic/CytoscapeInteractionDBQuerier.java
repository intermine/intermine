package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;

/**
 * This class has the logics to query the database for interaction information.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeInteractionDBQuerier
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeInteractionDBQuerier.class);

    //========== for normal gene-gene interaction ==========
    /**
     * Find genes that interact with the hub gene.
     *
     * @param genePID hub gene
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a set of genes
     */
    public Set<String> findInteractingGenes(String genePID, Model model,
            PathQueryExecutor executor) {

        PathQuery q = new PathQuery(model);

        Set<String> interactingGeneSet = new HashSet<String>();

        q.addView("Gene.interactions.interactingGenes.primaryIdentifier");
        q.addOrderBy("Gene.interactions.interactingGenes.primaryIdentifier", OrderDirection.ASC);
        q.addConstraint(Constraints.lookup("Gene", genePID, ""));

        ExportResultsIterator results = executor.execute(q);

        while (results.hasNext()) {
            List<ResultElement> row = results.next();

            // parse returned data
            String interactingGene = (String) row.get(0).getField();
            interactingGeneSet.add(interactingGene);
        }

        return interactingGeneSet;
    }

    /**
     * Query interactions among a list of genes.
     *
     * @param keys a list of genes
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return raw query results
     */
    public ExportResultsIterator queryInteractions(Set<String> keys, Model model,
            PathQueryExecutor executor) {

        PathQuery q = new PathQuery(model);

        if (keys == null || keys.size() < 1) {
            return null;
        }

        String bag = StringUtil.join(keys, ",");

        q.addViews("Gene.primaryIdentifier",
                "Gene.symbol",
                "Gene.interactions.interactionType",
                "Gene.interactions.interactingGenes.primaryIdentifier",
                "Gene.interactions.interactingGenes.symbol",
                "Gene.interactions.dataSets.dataSource.name",
                "Gene.interactions.shortName",
                "Gene.id", //object store id
                "Gene.interactions.interactingGenes.id");

        q.addOrderBy("Gene.primaryIdentifier", OrderDirection.ASC);
        q.addConstraint(Constraints.lookup("Gene", bag, ""), "B");
        q.addConstraint(Constraints.lookup("Gene.interactions.interactingGenes", bag, ""), "A");
        q.setConstraintLogic("B and A");

        ExportResultsIterator results = executor.execute(q);

        return results;
    }

    /**
     * Query interactions to extend current network.
     *
     * @param genePID the gene to be extended
     * @param keys a list of genes
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return raw query results
     */
    public ExportResultsIterator extendNetwork(String genePID,
            Set<String> keys, Model model, PathQueryExecutor executor) {

        Set<String> interactingGeneSet = findInteractingGenes(genePID, model, executor);
        keys.addAll(interactingGeneSet);
        ExportResultsIterator results = queryInteractions(keys, model, executor);

        return results;
    }

    // Separated modMine specific logics from bio, moved to RegulatoryNetworkDBUtil in modMine
}
