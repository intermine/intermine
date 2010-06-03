package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.AttributeLinkDisplayerController;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Interaction;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Set up interaction network data for the cytoscapeInteractionsDisplayer.jsp
 *
 * @author Julie Sullivan
 * @author Fengyuan Hu
 *
 */
public class CytoscapeInteractionsController extends TilesAction
{
    protected static final Logger LOG = Logger
            .getLogger(AttributeLinkDisplayerController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        // Get InterMineAPI
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        // Get object from request
        InterMineObject object = (InterMineObject) request
                .getAttribute("object");

        // Network data in flat format
        String theNetwork = new String();
        // A set of interaction strings, each string looks like "A\\tInt\\B"
        Set<String> interactionSet = new HashSet<String>();

        // Whether the object is a Gene or Protein
        if (object instanceof Protein) {
            Protein protein = (Protein) object;
            // In most cases, there is only 1 gene in the collect, but rare
            // cases with more than 1
            Set<Gene> genes = protein.getGenes();

            for (Gene gene : genes) {
                // Add the interaction network for this gene to the whole
                // network
                theNetwork = createNetwork(gene, im, interactionSet);

            }
        } else if (object instanceof Gene) {
            Gene gene = (Gene) object;
            theNetwork = createNetwork(gene, im, interactionSet);
        }

        request.setAttribute("networkdata", theNetwork);

        return null;

    }

    /**
     * Create SIF format data for Cytoscape Web
     * @param gene
     * @param im
     * @param interactionSet
     * @return the network as a string in SIF format
     */
    private String createNetwork(Gene gene, InterMineAPI im,
            Set<String> interactionSet) {

        // A list of genes including the hub and its interacting genes
        List<String> keys = new ArrayList<String>();
        keys.add(gene.getPrimaryIdentifier());

        Set<Interaction> interactions = gene.getInteractions();

        for (Interaction aInteraction : interactions) {
            Set<Gene> interactingGenes = aInteraction.getInteractingGenes();

            for (Gene aInteractingGene : interactingGenes) {
            keys.add(aInteractingGene.getPrimaryIdentifier()); }
        }

        // Query database
        Results results = dbQuery(keys, im);
        // Handle results
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();

            // String genePID = (String) row.get(0);
            String geneSymbol = (String) row.get(1);
            String interactionType = (String) row.get(2);
            // String interactingGenePID = (String) row.get(3);
            String interactingGeneSymbol = (String) row.get(4);

            interactionSet = addToInteractionSet(geneSymbol, interactionType,
                    interactingGeneSymbol, interactionSet);
        }

        return makeSIFString(interactionSet);
    }

    /**
     * Query database by IQL
     * @param keys
     * @param im
     * @return query results
     */
    private Results dbQuery(List<String> keys, InterMineAPI im) {

        // IQL
        Query q = new Query();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcInteraction = new QueryClass(Interaction.class);
        QueryClass qcInteractingGene = new QueryClass(Gene.class);

        // result columns
        QueryField qfGenePID = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfGeneSymbol = new QueryField(qcGene, "symbol");
        QueryField qfInteractionType = new QueryField(qcInteraction,
                "interactionType");
        QueryField qfInteractingGenePID = new QueryField(qcInteractingGene,
                "primaryIdentifier");
        QueryField qfInteractingGeneSymbol = new QueryField(qcInteractingGene,
        "symbol");

        q.setDistinct(true);

        q.addToSelect(qfGenePID);
        q.addToSelect(qfGeneSymbol);
        q.addToSelect(qfInteractionType);
        q.addToSelect(qfInteractingGenePID);
        q.addToSelect(qfInteractingGeneSymbol);

        q.addFrom(qcGene);
        q.addFrom(qcInteraction);
        q.addFrom(qcInteractingGene);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // gene.primaryidentifier in a list
        cs.addConstraint(new BagConstraint(qfGenePID, ConstraintOp.IN, keys));
        cs.addConstraint(new BagConstraint(qfInteractingGenePID, ConstraintOp.IN, keys));

        // gene.interactions
        QueryCollectionReference c2 = new QueryCollectionReference(qcGene,
                "interactions");
        cs.addConstraint(new ContainsConstraint(c2, ConstraintOp.CONTAINS,
                qcInteraction));

        // gene.interations.genePID
        QueryCollectionReference c3 = new QueryCollectionReference(
                qcInteraction, "interactingGenes");
        cs.addConstraint(new ContainsConstraint(c3, ConstraintOp.CONTAINS,
                qcInteractingGene));

        q.setConstraint(cs);

        Results results = im.getObjectStore().execute(q);

        return results;
    }

    /**
     * Add a new interaction to a set of interactions, remove duplication
     * @param genePID
     * @param interactionType
     * @param interactingGenePID
     * @param interactionSet
     * @return A set of SIF records
     */
    private Set<String> addToInteractionSet(String geneSymbol, String interactionType,
            String interactingGeneSymbol, Set<String> interactionSet) {

        if (interactionSet.isEmpty()) {
            interactionSet.add(geneSymbol + "\\t" + interactionType + "\\t"
                    + interactingGeneSymbol);
        }
        else {
            // You can't add to the HashSet while iterating. You have to either
            // keep track of where you want to add, and then do that after
            // you're done iterating, or else create a new HashSet on the fly,
            // copying from the old one and filling in the holes as they occur.
            // Thrown - java.util.ConcurrentModificationException

            String interactingString = geneSymbol + "\\t" + interactionType
            + "\\t" + interactingGeneSymbol;
            String interactingStringDup = interactingGeneSymbol + "\\t" + interactionType
            + "\\t" + geneSymbol;

            if (!(interactionSet.contains(interactingString) || interactionSet
                    .contains(interactingStringDup))) {
                    interactionSet.add(interactingString);
            }
        }
        return interactionSet;
    }

    /**
     * Convert Set to String in SIF format
     * @param interactionSet
     * @return the network in SIF format as a string or text
     */
    private String makeSIFString(Set<String> interactionSet) {

        StringBuffer theNetwork = new StringBuffer();

        // Build a line of network data, the data will be used in javascript in jsp,
        // js can only take "\n" in a string instead of real new line, so use "\\n" here
        for (String interactionString : interactionSet) {
            theNetwork.append(interactionString);
            theNetwork.append("\\n");
        }
        return theNetwork.toString();

    }

    /**
     * Convert Set to String in XGMML format
     * @param interactionSet
     * @return
     */
    @SuppressWarnings("unused")
    private String makeXGMMLString(Set<String> interactionSet) {
        return null;

    }
}
