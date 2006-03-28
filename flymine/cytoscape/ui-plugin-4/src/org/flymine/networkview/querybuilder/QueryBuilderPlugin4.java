package org.flymine.networkview.querybuilder;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneLayout;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinInteraction;
import org.flymine.model.genomic.ProteinInteractionExperiment;
import org.flymine.model.genomic.ProteinInteractor;
import org.flymine.model.genomic.Publication;
import org.flymine.networkview.network.FlyEdge;
import org.flymine.networkview.network.FlyNetwork;
import org.flymine.networkview.network.FlyNode;
import org.flymine.networkview.network.FlyValueWrapper;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;


/**
 * just another user interface
 * this one uses a NetworkHandler to get information about 
 * the current network in cytoscape
 * @author Florian Reisinger
 */
public class QueryBuilderPlugin4 implements QueryBuilderPlugin
{
    private static final Logger LOG = Logger.getLogger(QueryBuilderPlugin4.class);
    private ObjectStore os;
    private QueryBuilderListener qbl;
    private NetworkHandlerIface handler;

    /**
     * Set a ObjectStore that can be used to run queries
     * @param os ObjectStore to run queries on 
     */
    public void setObjectStore(ObjectStore os) {
        this.os = os;
    }

    /**
     * Set a NetworkHandler that runs on the client an has access to 
     * current changes on the network
     * @param handler the NetworkHandler to use in the user interface
     */
    public void setNetworkHandler(NetworkHandlerIface handler) {
        this.handler = handler;
    }

    /**
     * Retrieve this objects NetworkHandler
     * @return a NetworkHandler
     */
    public NetworkHandlerIface getNetworkHandler() {
        return this.handler;
    }

    /**
     * Builds the actual user interface that will be integrated in 
     * the Cytoscape plugin to provide functionality
     * @return a JComponent that represents the user interface 
     */
    public JComponent buildUserInterface() {
        JScrollPane sp = new JScrollPane();
        sp.setPreferredSize(new Dimension(150, 450));
        sp.setLayout(new ScrollPaneLayout());
        sp.setName("interactions by publications");
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        JLabel text1 = new JLabel("<html><b>Fetch interactions by publications</b></html>");
        c.anchor = GridBagConstraints.CENTER;
        c.fill = 0;
        c.insets = new Insets(10, 0, 40, 0);
        c.gridwidth = 2;
        c.gridy = 0;
        c.gridx = 0;
        panel.add(text1, c);
        
        JLabel text2 = new JLabel("get publications for this organism:");
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 10, 0, 0);
        c.gridy = 1;
        panel.add(text2, c);
        
        JList orgList = new JList(this.getOrganisms());
        orgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orgList.setVisibleRowCount(4);
        JScrollPane orgListScroller = new JScrollPane(orgList);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 0, 5);
        c.gridy = 2;
        panel.add(orgListScroller, c);
        
        JLabel text3 = new JLabel("select pubMedId:");
        c.fill = 0;
        c.insets = new Insets(15, 10, 0, 0);
        c.gridy = 3;
        panel.add(text3, c);
        
//        JList pubList = new JList(new Vector(10)); // does screw up the layout
        JList pubList = new JList(getEmptyVector());
        pubList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pubList.setVisibleRowCount(10);
        JScrollPane pubListScroller = new JScrollPane(pubList);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 5, 0, 5);
        c.gridy = 4;
        panel.add(pubListScroller, c);
        
        JButton updatePubButton = new JButton(
                new UI41Action(this, "update publication list", orgList, pubList));
        c.fill = 0;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 0, 5);
        c.gridy = 5;
        panel.add(updatePubButton, c);

        JLabel spaceLabel = new JLabel();
        c.weighty = 0.5;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.VERTICAL;
        c.gridy = 6;
        panel.add(spaceLabel, c);
        
        JButton run1Button = new JButton(new UI4Action(this, "append network", pubList, false));
        c.weighty = 0;
        c.fill = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 5, 10, 0);
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = GridBagConstraints.PAGE_END;
        panel.add(run1Button, c);

        JButton run2Button = new JButton(new UI4Action(this, "new network", pubList, true));
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 10, 5);
        c.gridx = 1;
        panel.add(run2Button, c);

        
        sp.getViewport().setView(panel);
        return sp;
    }

    /**
     * The QueryBuilderListener is used to integrate the inforation retrieved
     * by the user interface into the actual cytoscape network
     * @param qbl the listener that will take the results of the user interface
     */
    public void setQbl(QueryBuilderListener qbl) {
        this.qbl = qbl;
    }


    /**
     * Get the QueryBuilderListener
     * @return this objects QueryBuilderListener
     * @see org.flymine.networkview.querybuilder.QueryBuilderPlugin#getQbl()
     */
    public QueryBuilderListener getQbl() {
        return qbl;
    }


    /**
     * This method has no funktionality yet, since there is only one QueryBuilderListener
     * and that should not be removed
     * @param listener the listener to remove
     */
    public void removeQueryBuilderListener(QueryBuilderListener listener) {
        // TODO Auto-generated method stub
        
    }


    /**
     * This method will query the objectstore for organisms with protein interaction data
     * @return a Vector containing organism names
     */
    private Vector getOrganisms() {
        // store the organism names in a Vector
        Vector organisms = new Vector();
        
        // build query to retrieve all organism names currently in the database
        Query q = new Query();
        // create all needed query classes
        QueryClass qcPI = new QueryClass(ProteinInteraction.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcPItor = new QueryClass(ProteinInteractor.class);
        
        
        QueryCollectionReference qcr1 = new QueryCollectionReference(qcPI, "interactors");
        ContainsConstraint cc1 = new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qcPItor);
        QueryObjectReference qor1 = new QueryObjectReference(qcPItor, "protein");
        ContainsConstraint cc2 = new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcProtein);
        
        QueryObjectReference qor2 = new QueryObjectReference(qcProtein, "organism");
        ContainsConstraint cc3 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcOrganism);
        QueryField qf = new QueryField(qcOrganism, "name");
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc1);
        cs.addConstraint(cc2);
        cs.addConstraint(cc3);
        
        // build up query
        q.addToSelect(qf);
        q.addFrom(qcOrganism);
        q.addFrom(qcPI);
        q.addFrom(qcProtein);
        q.addFrom(qcPItor);
        q.setConstraint(cs);
        q.setDistinct(true);

        // get result
        LOG.debug("querying for organisms...");
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        
        // add results into Vector
        for (Iterator iter = res.iterator(); iter.hasNext();) {
            String org = (String) iter.next();
            organisms.add(org);
            LOG.debug("adding to list organism: " + org);
        }
        
        return organisms;
    }
    

    /**
     * This method will at all protein interction experiments for the specified organism
     * and will query for the pubMedIdS of publications associated with that experiments
     * @param organism the organism name of interest
     * @return a Vector of pubMedIDs
     */
    public Vector getPubForOrg(String organism) {
        LOG.debug("get publications for selected organism: " + organism);
        // store the publications in a Vector
        Vector pubIDs = new Vector();
        
        // build query to retrieve all pubMedIdS
        Query q = new Query();
        // create all needed query classes
        QueryClass qcPI = new QueryClass(ProteinInteraction.class);
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcPIExp = new QueryClass(ProteinInteractionExperiment.class);
        QueryClass qcPub = new QueryClass(Publication.class);
        QueryClass qcPItor = new QueryClass(ProteinInteractor.class);
        
//        QueryCollectionReference qcr = new QueryCollectionReference(qcPI, "proteins");
//        ContainsConstraint cc1 = new ContainsConstraint(qcr, ConstraintOp.CONTAINS, qcProtein);

        QueryCollectionReference qcr1 = new QueryCollectionReference(qcPI, "interactors");
        ContainsConstraint cc11 = new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qcPItor);
        QueryObjectReference qor1 = new QueryObjectReference(qcPItor, "protein");
        ContainsConstraint cc12 = new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcProtein);

        
        QueryObjectReference qor = new QueryObjectReference(qcProtein, "organism");
        ContainsConstraint cc2 = new ContainsConstraint(qor, ConstraintOp.CONTAINS, qcOrganism);
        QueryField qf = new QueryField(qcOrganism, "name");
        SimpleConstraint sc = new SimpleConstraint(qf, 
                ConstraintOp.EQUALS, new QueryValue(organism));
        QueryField qf2 = new QueryField(qcPub, "pubMedId");
        QueryObjectReference qor2 = new QueryObjectReference(qcPI, "experiment");
        ContainsConstraint cc3 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcPIExp);
        QueryObjectReference qor3 = new QueryObjectReference(qcPIExp, "publication");
        ContainsConstraint cc4 = new ContainsConstraint(qor3, ConstraintOp.CONTAINS, qcPub);
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc11);
        cs.addConstraint(cc12);
        cs.addConstraint(cc2);
        cs.addConstraint(cc3);
        cs.addConstraint(cc4);
        cs.addConstraint(sc);
        
        // build up query
        q.addToSelect(qf2);
        q.addFrom(qcOrganism);
        q.addFrom(qcPI);
        q.addFrom(qcPItor);
        q.addFrom(qcProtein);
        q.addFrom(qcPIExp);
        q.addFrom(qcPub);
        q.setConstraint(cs);
        q.setDistinct(true);

        // get result
        LOG.debug("querying for publicatons (pubMedIdS)");
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        
        // add results into Vector
        for (Iterator iter = res.iterator(); iter.hasNext();) {
            String pub = (String) iter.next();
            pubIDs.add(pub);
            LOG.debug("adding to list publication: " + pub);
        }
        
        return pubIDs;
    }
    

    /**
     * Queries for all protein interactions with a reference to the specified pubMedId
     * @param pubMedId the pubMedID of the publication
     * @return a FlyNetwork representing protein interactions described in 
     * the specified publication 
     */
    protected FlyNetwork getInteractionsForPub(String pubMedId) {
        LOG.debug("get protein interactions for selected publication: " + pubMedId);
        Query q = new Query();
        // create all needed query classes
        QueryClass qcPI = new QueryClass(ProteinInteraction.class);
        QueryClass qcPIExp = new QueryClass(ProteinInteractionExperiment.class);
        QueryClass qcPub = new QueryClass(Publication.class);
        

        QueryObjectReference qor1 = new QueryObjectReference(qcPI, "experiment");
        ContainsConstraint cc1 = new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcPIExp);
        QueryObjectReference qor2 = new QueryObjectReference(qcPIExp, "publication");
        ContainsConstraint cc2 = new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcPub);
        QueryField qf2 = new QueryField(qcPub, "pubMedId");
        SimpleConstraint sc = new SimpleConstraint(qf2, 
                ConstraintOp.EQUALS, new QueryValue(pubMedId));
        
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(sc);
        cs.addConstraint(cc1);
        cs.addConstraint(cc2);
        
        // build up query
        q.addToSelect(qcPI);
        q.addFrom(qcPI);
        q.addFrom(qcPIExp);
        q.addFrom(qcPub);
        q.setConstraint(cs);
        q.setDistinct(true);

        // get result
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setNoPrefetch();
        
        LOG.info("result size: " + res.size());
        
        FlyNetwork tmpNet = createFlyNetwork(res, pubMedId);
        return tmpNet;
    }
    
    /**
     * This method transforms a Collection of ProteinInteractionS into a FlyNetwork.
     * It is specially constructed to add attributes to the network that were obtained in 
     * this user interface
     * @param proteinInteractions a collection of ProteinInteractionS
     * @return a FlyNetwork representing all ProteinInteractionS
     */
    private FlyNetwork createFlyNetwork(Collection proteinInteractions, String pubMedId) {
        FlyNetwork fn = new FlyNetwork();
        LOG.info("starting to build the FlyNetwork from the result...");
        long piCount = 0;
        try {

        // TODO: check that Collection really contains ProteinInteractions

        for (Iterator iter = proteinInteractions.iterator(); iter.hasNext();) {
            Object o = iter.next();
            ProteinInteraction pIon = null;
            if (o instanceof ProteinInteraction) {
                pIon = (ProteinInteraction) o;
                LOG.debug("shortname: " + pIon.getShortName());
                piCount++;
                if (piCount % 100 == 0) {
                    LOG.debug("ProteinInteraction " + piCount
                            + ". Short name: " + pIon.getShortName());
                }
                LOG.debug("'ProteinInteraction' found. count:  " + piCount 
                        + ". Interaction: " + pIon.getShortName());
            } else {
                LOG.error("Found object that is not a ProteinInteraction!! ->" 
                        + o.getClass().toString());
            }
            //ProteinInteraction pIon = (ProteinInteraction) iter.next();
            
            // TODO: what features do we want to add as attributes?
            //       experiment?, publication? new edge? interaction-name?

            if (pIon != null) {
                ArrayList preys = new ArrayList();
                ArrayList baits = new ArrayList();
                ArrayList others = new ArrayList();

                Collection pIors = pIon.getInteractors();
                    if (pIors == null) {
                        LOG.error("Interactors collection is null!!! " 
                                + pIon.getShortName());
                    } else if (pIors.size() < 1) {
                        LOG.error("No Interactors!! for protein interaction : " 
                                + pIon.getShortName());
                    }
                for (Iterator iterator = pIors.iterator(); iterator.hasNext();) {
                    ProteinInteractor pIor = (ProteinInteractor) iterator.next();
                    if (pIor.getRole().equalsIgnoreCase("prey")) {
                        preys.add(pIor.getProtein());
                    } else if (pIor.getRole().equalsIgnoreCase("bait")) {
                        baits.add(pIor.getProtein());
                    } else {
                        others.add(pIor.getProtein());
                    }
                    
                }
               
                // now build the actual network...
                // one prey one bait -> standard case -> default edge type
                if (baits.size() == 1 && preys.size() == 1 && others.size() == 0) {
                    // TODO: check that all proteins have a primaryAccession
                    Protein pPrey = (Protein) preys.get(0);
                    Protein pBait = (Protein) baits.get(0);
                    FlyNode prey = new FlyNode(pPrey.getPrimaryAccession());
                    prey.setAttribute("species", pPrey.getOrganism().getName());
                    FlyNode bait = new FlyNode(pBait.getPrimaryAccession());
                    bait.setAttribute("species", pBait.getOrganism().getName());
                    // can we use the short name as id/label (it has to be unique)?
//                    FlyEdge fe = new FlyEdge(prey, bait, 
//                            FlyNetwork.DEFAULT_INTERACTION_TYPE, pIon.getShortName());
                    FlyEdge fe = new FlyEdge(prey, bait);
//                    fe.setAttribute("pubMedId", pubMedId);
                    fe.setAttribute("pubMedId", pubMedId, FlyValueWrapper.COUNT);
                    fe.setAttribute("synonyms", pIon.getShortName(), FlyValueWrapper.ADD);
                    
                    fn.addNode(prey);
                    fn.addNode(bait);
                    fn.addEdge(fe);
                } else { // not prey-bait standard case -> different edge type
                    // How to model?? -> for now: link all proteins to each other (complex)
                    // TODO: test this
                    others.addAll(preys);
                    others.addAll(baits);
                    Protein[] p = new Protein[1];
                    p = (Protein[]) others.toArray(p);
                    FlyNode[] nx = new FlyNode[p.length];
                    for (int i = 0; i < p.length; i++) {
                        String s = p[i].getPrimaryAccession();
                        nx[i] = new FlyNode(s);
                        nx[i].setAttribute("species", p[i].getOrganism().getName());
                        fn.addNode(nx[i]);
                    }
                    for (int i = 0; i < others.size(); i++) {
                        for (int j = i + 1; j < others.size(); j++) {
                            FlyEdge fe = new FlyEdge(nx[i], nx[j],
                                    FlyNetwork.COMPLEX_INTERACTION_TYPE);

//                            fe.setAttribute("pubMedId", pubMedId);
                            fe.setAttribute("pubMedId", pubMedId, FlyValueWrapper.COUNT);
                            fe.setAttribute("synonyms", pIon.getShortName(), FlyValueWrapper.ADD);

                            fn.addEdge(fe);
                        }
                    }
                }
                
            } else {
                LOG.error("one ProteinInteraction was null! This shouldn't happen!");
            }
        }
        
        } catch (Exception e) {
            LOG.error("Exception at ProteinInteraction No: " + piCount);
            e.printStackTrace();
        }
        
        
        //debugging
//        for (Iterator iter = fn.getNodes().iterator(); iter.hasNext();) {
//            FlyNode node = (FlyNode) iter.next();
//            LOG.info("number of nodes: " + fn.getNodes().size());
//            LOG.info("FlyNode: " + node.getLabel());
//        }
        for (Iterator iter = fn.getEdges().iterator(); iter.hasNext();) {
            FlyEdge edge = (FlyEdge) iter.next();
            LOG.debug("number of edges: " + fn.getEdges().size());
            LOG.debug("FlyEdge: " + edge.getLabel());
        }
        
        
        return fn;
    }

    
    /**
     * @return true if this user interface needs an ObjectStore
     */
    public boolean needsObjectStore() {
        return true;
    }


    /**
     * Data that is initially shown in the publication list
     * (it is neccessary to create a empty Vector to prevent the initial 
     * list layout from being to wide)
     * @return a #Vector containing no relevant data
     */
    private Vector getEmptyVector() {
        Vector v = new Vector();
        for (int i = 0; i < 10; i++) {
            v.add(" ");
        }
        return v;
    }
    
    
}
