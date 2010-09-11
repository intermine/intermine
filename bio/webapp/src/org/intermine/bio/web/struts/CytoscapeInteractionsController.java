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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
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
    private static final Logger LOG = Logger.getLogger(CytoscapeInteractionsController.class);
    private Model model = null;
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
        model = im.getModel();

        // Get object from request
        InterMineObject object = (InterMineObject) request
                .getAttribute("object");

        // Network data in flat format
        String theNetwork = new String();
        // A set of interaction strings, each string looks like "A\\tInt\\B"
        Set<CytoscapeNetworkData> interactionSet = new HashSet<CytoscapeNetworkData>();

        // Whether the object is a Gene or Protein
        if (object instanceof Protein) {
            Protein protein = (Protein) object;
            // In most cases, there is only 1 gene in the collect, but rare
            // cases with more than 1
            Set<Gene> genes = protein.getGenes();

            for (Gene gene : genes) {
                // Add the interaction network for this gene to the whole
                // network
                interactionSet = createNetwork(gene, im, interactionSet);
                if (interactionSet.isEmpty()) { return null; }

                theNetwork = makeSIFString(interactionSet);

            }
        } else if (object instanceof Gene) {
            Gene gene = (Gene) object;
            interactionSet = createNetwork(gene, im, interactionSet);
            if (interactionSet.isEmpty()) { return null; }

            theNetwork = makeSIFString(interactionSet);
        }

        String extNetworkData = makeExtDataString(interactionSet);

        request.setAttribute("extNetworkData", extNetworkData);
        request.setAttribute("networkdata", theNetwork);

        return null;

    }

    /**
     * Create SIF format data for Cytoscape Web
     * @param gene PrimaryIdentifier
     * @param im InterMineAPI
     * @param interactionSet A set of CytoscapeNetworkData
     * @return the network as a string in SIF format
     */
    @SuppressWarnings("unchecked")
    private Set<CytoscapeNetworkData> createNetwork(Gene gene, InterMineAPI im,
            Set<CytoscapeNetworkData> interactionSet) {

        // A list of genes including the hub and its interacting genes
        Set<String> keySet = new HashSet<String>();
        keySet.add(gene.getPrimaryIdentifier());

        // get all the genes that interact with the hub gene
        Set<String> interactingGeneSet = findInteractingGenes(gene.getPrimaryIdentifier(), im);
        if (interactingGeneSet == null) {
            return Collections.EMPTY_SET;
        }

        keySet.addAll(interactingGeneSet);
        List<String> keyList = new ArrayList<String>(keySet);

        // Query database
        interactionSet = queryInteractions(keyList, im, interactionSet);
        if (interactionSet == null) {
            return Collections.EMPTY_SET;
        }

        return interactionSet;
    }

    /**
     * Find genes that interact with the hub gene
     * @param gene hub gene
     * @return a list of genes
     */
    private Set<String> findInteractingGenes(String genePID, InterMineAPI im) {
        Set<String> interactingGeneSet = new HashSet<String>();

        // IQL
        Query q = new Query();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcInteractingGene = new QueryClass(Gene.class);
        QueryClass qcInteraction = null;

        // Test if Interaction class in the core model
        try {
            qcInteraction =
                new QueryClass(Class.forName(model.getPackageName() + ".Interaction"));
        } catch (ClassNotFoundException e) {
            return null;
        }

        // result columns
        QueryField qfInteractingGenePID = new QueryField(qcInteractingGene, "primaryIdentifiers");

        q.setDistinct(true);

        q.addToSelect(qfInteractingGenePID);

        q.addFrom(qcGene);
        q.addFrom(qcInteraction);
        q.addFrom(qcInteractingGene);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryField qfGenePID = new QueryField(qcGene, "primaryIdentifier");
        SimpleConstraint sc = new SimpleConstraint(qfGenePID, ConstraintOp.EQUALS,
                new QueryValue(genePID));
        cs.addConstraint(sc);

        // gene.interactions
        QueryCollectionReference cr1 = new QueryCollectionReference(qcGene,
                "interactions");
        cs.addConstraint(new ContainsConstraint(cr1, ConstraintOp.CONTAINS,
                qcInteraction));

        // gene.interations.genePID
        QueryCollectionReference cr2 = new QueryCollectionReference(
                qcInteraction, "interactingGenes");
        cs.addConstraint(new ContainsConstraint(cr2, ConstraintOp.CONTAINS,
                qcInteractingGene));

        q.setConstraint(cs);

        Results results = im.getObjectStore().execute(q);

        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();

            String  aInteractingGene = (String) row.get(0);
            interactingGeneSet.add(aInteractingGene);
        }

        return interactingGeneSet;
    }

    /**
     * Query database by IQL
     * @param keys
     * @param im
     * @return query results
     */
    private Set<CytoscapeNetworkData> queryInteractions(List<String> keys,
            InterMineAPI im, Set<CytoscapeNetworkData> interactionSet) {

        // IQL
        Query q = new Query();

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcInteraction = null;
        QueryClass qcInteractingGene = new QueryClass(Gene.class);
        QueryClass qcInteractionDataSet = new QueryClass(DataSet.class);
        QueryClass qcInteractionDataSource = new QueryClass(DataSource.class);

        // Test if Interaction class in the core model?
        try {

            qcInteraction =
                new QueryClass(Class.forName(model.getPackageName() + ".Interaction"));
        } catch (ClassNotFoundException e) {
            return null;
        }

        // result columns
        QueryField qfGenePID = new QueryField(qcGene, "primaryIdentifier");
        QueryField qfGeneSymbol = new QueryField(qcGene, "symbol");
        QueryField qfInteractionType = new QueryField(qcInteraction,
                "interactionType");
        QueryField qfInteractingGenePID = new QueryField(qcInteractingGene,
                "primaryIdentifier");
        QueryField qfInteractingGeneSymbol = new QueryField(qcInteractingGene,
            "symbol");
        QueryField qfInteractionShortName = new QueryField(qcInteraction,
            "shortName");
        QueryField qfDataSourceName = new QueryField(qcInteractionDataSource,
            "name");

        q.setDistinct(true);

        q.addToSelect(qfGenePID);
        q.addToSelect(qfGeneSymbol);
        q.addToSelect(qfInteractionType);
        q.addToSelect(qfInteractingGenePID);
        q.addToSelect(qfInteractingGeneSymbol);
        q.addToSelect(qfDataSourceName);
        q.addToSelect(qfInteractionShortName);

        q.addFrom(qcGene);
        q.addFrom(qcInteraction);
        q.addFrom(qcInteractingGene);
        q.addFrom(qcInteractionDataSet);
        q.addFrom(qcInteractionDataSource);


        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // Reference
        // Interaction.dataSets = dataSet
        QueryCollectionReference dataset = new QueryCollectionReference(qcInteraction,
            "dataSets");
        ContainsConstraint ccDataset = new ContainsConstraint(dataset,
                ConstraintOp.CONTAINS, qcInteractionDataSet);
        cs.addConstraint(ccDataset);

        // Interaction.dataSets.dataSource = dataSource
        QueryObjectReference datasource = new QueryObjectReference(qcInteractionDataSet,
            "dataSource");
        ContainsConstraint ccDatasource = new ContainsConstraint(datasource,
                ConstraintOp.CONTAINS, qcInteractionDataSource);
        cs.addConstraint(ccDatasource);

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

        // Handle results
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();

            // String genePID = (String) row.get(0);
            String geneSymbol = (String) row.get(1);
            String interactionType = (String) row.get(2);
            // String interactingGenePID = (String) row.get(3);
            String interactingGeneSymbol = (String) row.get(4);
            String dataSourceName = (String) row.get(5);
            String interactionShortName = (String) row.get(6);

//            LOG.info("Interaction Results: " + geneSymbol + "-"
//                    + interactionType + "-" + interactingGeneSymbol);

            interactionSet = addToInteractionSet(geneSymbol, interactionType,
                    interactingGeneSymbol, dataSourceName, interactionShortName, interactionSet);
        }

        return interactionSet;
    }

    /**
     * Add a new interaction to a set of interactions, remove duplication
     * @param genePID Gene PrimaryIdentifier
     * @param interactionType Physical/Genetic
     * @param interactingGenePID Gene PrimaryIdentifier
     * @param dataSourceName
     * @param interactionShortName
     * @param interactionSet A set of interactions
     * @return A set of SIF records
     */
    private Set<CytoscapeNetworkData> addToInteractionSet(String geneSymbol, String interactionType,
         String interactingGeneSymbol,
                    String dataSourceName, String interactionShortName,
                    Set<CytoscapeNetworkData> interactionSet) {

        CytoscapeNetworkData cytodata = new CytoscapeNetworkData();
        LinkedHashSet<String> dataSources = new LinkedHashSet<String>();
        LinkedHashSet<String> interactionShortNames = new LinkedHashSet<String>();

        if (interactionSet.isEmpty()) {
            cytodata.setInteractionString(geneSymbol + "\\t" + interactionType + "\\t"
                    + interactingGeneSymbol);
            dataSources.add(dataSourceName);
            cytodata.setDataSources(dataSources);
            interactionShortNames.add(interactionShortName);
            cytodata.setInteractionShortNames(interactionShortNames);
            interactionSet.add(cytodata);
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

            // Get a list of interactionString from interactionSet
            LinkedHashSet<String> intcStrSet = new LinkedHashSet<String>();
            for (CytoscapeNetworkData networkdata : interactionSet) {
                intcStrSet.add(networkdata.getInteractionString());
            }
            if (!(intcStrSet.contains(interactingString) || intcStrSet
                .contains(interactingStringDup))) {
                cytodata.setInteractionString(interactingString);
                dataSources.add(dataSourceName);
                cytodata.setDataSources(dataSources);
                interactionShortNames.add(interactionShortName);
                cytodata.setInteractionShortNames(interactionShortNames);
                interactionSet.add(cytodata);
            } else {
                // Pull out the CytoscapeNetworkData which contains the current interactionString
                for (CytoscapeNetworkData networkdata : interactionSet) {
                    if (networkdata.getInteractionString().equals(
                            interactingString)
                            || networkdata.getInteractionString().equals(
                                    interactingStringDup)) {
                        networkdata.getDataSources().add(dataSourceName);
                        networkdata.getInteractionShortNames().add(interactionShortName);
                    }
                }
            }
        }
        return interactionSet;
    }

    /**
     * Convert Set to String in SIF format
     * @param interactionSet
     * @return the network in SIF format as a string or text
     */
    private String makeSIFString(Set<CytoscapeNetworkData> interactionSet) {

        StringBuffer theNetwork = new StringBuffer();

        // Build a line of network data, the data will be used in javascript in jsp,
        // js can only take "\n" in a string instead of real new line, so use "\\n" here
        for (CytoscapeNetworkData interactionString : interactionSet) {
            theNetwork.append(interactionString.getInteractionString());
            theNetwork.append("\\n");
        }
        return theNetwork.toString();

    }

    /**
     * Convert Set to String with extra information about interaction data sources and name
     * @param interactionSet
     * @return the network data with extra info
     */
    private String makeExtDataString(Set<CytoscapeNetworkData> interactionSet) {

        StringBuffer extNetworkData = new StringBuffer();

        // Build a line of network data, the data will be used in javascript in jsp,
        // js can only take "\n" in a string instead of real new line, so use "\\n" here
        for (CytoscapeNetworkData interactionData : interactionSet) {
            String newInteractionString = interactionData
                    .getInteractionString().replace("\\t", "-");
            extNetworkData.append(newInteractionString);

            extNetworkData.append(";");

            StringBuffer datasources = new StringBuffer();
            for (String datasource : interactionData.getDataSources()) {
                datasources.append(datasource);
                datasources.append(",");
            }
            extNetworkData.append(datasources.toString().substring(0,
                    datasources.toString().lastIndexOf(",")));

            extNetworkData.append(";");

            // Add the first short name to the network data string
            for (String intcName : interactionData.getInteractionShortNames()) {
                extNetworkData.append(intcName);
                break;
            }

            extNetworkData.append("\\n");
        }
        return extNetworkData.toString();

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
