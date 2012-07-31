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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;

/**
 * This class has the logics to query the database for interaction information.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeNetworkDBQueryRunner
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(CytoscapeNetworkDBQueryRunner.class);

    /**
     * Find all genes that interact with each other.
     *
     * @param featureType Gene or Protein
     * @param startingFeatureSet a set starting genes or proteins
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return a set of genes
     */
    public Set<Integer> getInteractingGenes(String featureType, Set<Integer> startingFeatureSet,
            Model model, PathQueryExecutor executor) {

        Set<Integer> fullInteractingGeneSet = new LinkedHashSet<Integer>();
        Set<Integer> startingGeneSet = new LinkedHashSet<Integer>();

        //=== Get starting genes ===
        if ("Gene".equals(featureType)) {
            startingGeneSet.addAll(startingFeatureSet);
        } else if ("Protein".equals(featureType)) {

            PathQuery q = new PathQuery(model);

            q.addView("Protein.genes.id");
            q.addConstraint(Constraints.inIds(featureType, startingFeatureSet));

            ExportResultsIterator results = executor.execute(q);

            while (results.hasNext()) {
                List<ResultElement> row = results.next();

                Integer interactingGene = (Integer) row.get(0).getField();
                startingGeneSet.add(interactingGene);
            }
        }

        //=== Get genes interacting with starting genes ===

        PathQuery q = new PathQuery(model);

        Set<Integer> interactingGeneSet = new HashSet<Integer>();

        q.addView("Gene.interactions.interactingGenes.id");
        q.addConstraint(Constraints.inIds("Gene", startingGeneSet));

        ExportResultsIterator results = executor.execute(q);

        while (results.hasNext()) {
            List<ResultElement> row = results.next();

            Integer interactingGene = (Integer) row.get(0).getField();
            interactingGeneSet.add(interactingGene);
        }

        // TODO could create a bag with a temp name, and use it in the next query

        fullInteractingGeneSet.addAll(startingGeneSet);
        fullInteractingGeneSet.addAll(interactingGeneSet);

        return fullInteractingGeneSet;
    }

    /**
     * Query interactions among a list of genes.
     *
     * @param keys a list of genes
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return raw query results
     */
    public ExportResultsIterator getInteractions(Set<Integer> keys, Model model,
            PathQueryExecutor executor) {

        PathQuery q = new PathQuery(model);

        if (keys == null || keys.size() < 1) {
            return null;
        }

        q.addViews("Gene.primaryIdentifier",
                "Gene.symbol",
                "Gene.interactions.interactionType",
                "Gene.interactions.interactingGenes.primaryIdentifier",
                "Gene.interactions.interactingGenes.symbol",
                "Gene.interactions.dataSets.dataSource.name",
                "Gene.interactions.shortName",
                "Gene.id",
                "Gene.interactions.interactingGenes.id");

        q.addConstraint(Constraints.inIds("Gene", keys), "B");
        q.addConstraint(Constraints.inIds("Gene.interactions.interactingGenes", keys), "A");
        q.setConstraintLogic("B and A");

        ExportResultsIterator results = executor.execute(q);

        return results;
    }

    /**
     * Query interactions to extend current network.
     *
     * @param geneId the internal object id of the gene to extend network for
     * @param keys a list of genes
     * @param model the Model
     * @param executor the PathQueryExecutor
     * @return raw query results
     */
    public ExportResultsIterator extendNetwork(String geneId,
            Set<Integer> keys, Model model, PathQueryExecutor executor) {

        Set<Integer> startingGeneSet = new HashSet<Integer>(Integer.valueOf(geneId));

        Set<Integer> fullInteractingGeneSet = getInteractingGenes("Gene",
                startingGeneSet, model, executor);
        ExportResultsIterator results = getInteractions(fullInteractingGeneSet, model, executor);

        return results;
    }
}
