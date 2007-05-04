package org.intermine.bio.networkview;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;

import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinInteraction;
import org.flymine.model.genomic.ProteinInteractor;

import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.SingletonResults;

/**
 * @author Florian Reisinger
 *
 */
public class ProteinInteractionRetriever 
    {
    private ObjectStore os;

    /**
     * Constructor
     * @param os ObjectStore to use
     */
    public ProteinInteractionRetriever(ObjectStore os) {
        this.os = os;
    }

    /*
     public FlyNetwork expandNetworkFromProtein(String acc) {
     // TODO: make sure identifier is primaryAccession and is unique
     Query q = new Query();
     
     // create all needed classes
     QueryClass qcProtein = new QueryClass(Protein.class);
     QueryClass qcInteractor = new QueryClass(ProteinInteractor.class);
     QueryClass qcInteraction = new QueryClass(ProteinInteraction.class);

     // create needed references between the classes
     QueryObjectReference qor = new QueryObjectReference(qcInteractor,
     "protein");
     QueryCollectionReference ccr = new QueryCollectionReference(
     qcInteraction, "interactors");

     // build up constraint
     ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
     QueryField qf = new QueryField(qcProtein, "primaryAccession");
     // constrain primaryAccession to acc
     SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS,
     new QueryValue(acc));
     // join protein and nteractors
     ContainsConstraint cc1 = new ContainsConstraint(qor,
     ConstraintOp.CONTAINS, qcProtein);
     // join interactors and proteininteractions
     ContainsConstraint cc2 = new ContainsConstraint(ccr,
     ConstraintOp.CONTAINS, qcInteractor);
     
     // build up query
     cs.addConstraint(sc);
     cs.addConstraint(cc1);
     cs.addConstraint(cc2);
     q.setConstraint(cs);
     q.addToSelect(qcInteraction);
     q.addFrom(qcProtein);
     q.addFrom(qcInteractor);
     q.addFrom(qcInteraction);
     
     // get results
     SingletonResults res = os.executeSingleton(q);

     // TODO: make sure res is List of ProteinInteractions
     return FlyNetworkCreator.createFlyNetwork(res);
     }
     
     */

    /**
     * This method will take a list of protein primary accessions and query 
     * the objectstore for all protein interactions containing at least one
     * of the specified proteins.
     * @param accs Collection of Strings containing protein primary accessions
     * @return a network build from all interactions of the specified proteins
     */
    public FlyNetwork expandNetworkFromProteins(Collection accs) {
        // TODO: check that all elements in accs are of type String
        Query q = new Query();

        // create all needed query classes
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcInteractor = new QueryClass(ProteinInteractor.class);
        QueryClass qcInteraction = new QueryClass(ProteinInteraction.class);

        // create needed references between the classes
        QueryObjectReference qor = new QueryObjectReference(qcInteractor,
                "protein");
        QueryCollectionReference ccr = new QueryCollectionReference(
                qcInteraction, "interactors");

        // build up constraint
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryField qf = new QueryField(qcProtein, "primaryAccession");
        // constrain primaryAccession to acc from accs
        BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, accs);
        // join protein and nteractors
        ContainsConstraint cc1 = new ContainsConstraint(qor,
                ConstraintOp.CONTAINS, qcProtein);
        // join interactors and proteininteractions
        ContainsConstraint cc2 = new ContainsConstraint(ccr,
                ConstraintOp.CONTAINS, qcInteractor);

        // build up query
        cs.addConstraint(bc);
        cs.addConstraint(cc1);
        cs.addConstraint(cc2);
        q.setConstraint(cs);
        q.addToSelect(qcInteraction);
        q.addFrom(qcProtein);
        q.addFrom(qcInteractor);
        q.addFrom(qcInteraction);

        // get results
        SingletonResults res = os.executeSingleton(q);

        return FlyNetworkCreator.createFlyNetwork(res);
    }

    /**
     * This will repeatedly call expandNetworkFromProteins(accs) each time containing
     * the list of proteins from the previous call.
     * @param accs Collection of Strings containing protein primary accessions
     * @param distance maximum number of interactions (distance) between two proteins
     * @return a network build from all found interactions
     * @see #expandNetworkFromProteins(Collection)
     */
    public FlyNetwork expandNetworkFromProteins(Collection accs, int distance) {
        // TODO: think of more effective way
        FlyNetwork net = new FlyNetwork();
        for (int i = 0; i < distance; i++) {
            net = expandNetworkFromProteins(accs);
            Collection nodes = net.getNodes();
            for (Iterator iter = nodes.iterator(); iter.hasNext();) {
                FlyNode node = (FlyNode) iter.next();
                if (!accs.contains(node.getLabel())) {
                    accs.add(node.getLabel());
                }
            }
        }
        return net;
    }

}
