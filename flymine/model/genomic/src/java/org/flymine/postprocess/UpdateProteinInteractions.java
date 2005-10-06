package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.*;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.ProteinInteraction;

import java.util.*;

/**
 * @author Peter McLaren
 */
public class UpdateProteinInteractions
{

    private static final Logger LOG = Logger.getLogger(UpdateProteinInteractions.class);

    protected ObjectStoreWriter osw;
    protected ObjectStore os;

    /**
     * Constructor
     *
     * @param osw - the object store writer
     * */
    public UpdateProteinInteractions(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

    /**
     * @throws ObjectStoreException - thrown if there is a problem.
     * */
    public void updateProteinInteractions() throws ObjectStoreException {

        Map complexMap = getComplexInteractions();
        setInteractions(complexMap);
        storeInteractions(complexMap);

        Map pairwseMap = getPairwiseInteractions();
        setInteractions(pairwseMap);
        storeInteractions(pairwseMap);
    }

    /**
     * @return map of all interactions with more than 2 proteins mapped to their respective proteins
     * @throws ObjectStoreException
     * */
    private Map getComplexInteractions() throws ObjectStoreException {

        //p2pic - protein 2 protein interaction complex
        Query p2picQuery = new Query();
        p2picQuery.setDistinct(false);

        QueryClass interationQC = new QueryClass(ProteinInteraction.class);
        p2picQuery.addToSelect(interationQC);
        p2picQuery.addFrom(interationQC);
        p2picQuery.addToOrderBy(interationQC);

        QueryClass proteinQC = new QueryClass(Protein.class);
        p2picQuery.addToSelect(proteinQC);
        p2picQuery.addFrom(proteinQC);

        QueryCollectionReference piComplexQCR =
                new QueryCollectionReference(interationQC, "complex");

        ContainsConstraint pipConstraint =
                new ContainsConstraint(piComplexQCR, ConstraintOp.CONTAINS, proteinQC);

        p2picQuery.setConstraint(pipConstraint);

        Results results = os.execute(p2picQuery);
        Iterator resIter = results.iterator();

        HashMap resMap = new HashMap();

        while (resIter.hasNext()) {

            ResultsRow rr = (ResultsRow) resIter.next();
            ProteinInteraction interaction = (ProteinInteraction) rr.get(0);
            Protein protein = (Protein) rr.get(1);

            if (resMap.containsKey(interaction)) {
                Set proteinSet = (Set) resMap.get(interaction);
                proteinSet.add(protein);
            } else {
                HashSet newProteinSet = new HashSet();
                newProteinSet.add(protein);
                resMap.put(interaction, newProteinSet);
            }
        }

        return resMap;
    }

    private Map getPairwiseInteractions() throws ObjectStoreException {

        Query baitAndPreyNotNullQuery = new Query();
        baitAndPreyNotNullQuery.setDistinct(false);

        QueryClass interationQC = new QueryClass(ProteinInteraction.class);
        baitAndPreyNotNullQuery.addToSelect(interationQC);
        baitAndPreyNotNullQuery.addFrom(interationQC);
        baitAndPreyNotNullQuery.addToOrderBy(interationQC);

        QueryObjectReference baitRef = new QueryObjectReference(interationQC, "bait");
        QueryObjectReference preyRef = new QueryObjectReference(interationQC, "prey");

        ContainsConstraint baitNotNullConstraint =
                new ContainsConstraint(baitRef, ConstraintOp.IS_NOT_NULL);
        ContainsConstraint preyNotNullConstraint =
                new ContainsConstraint(preyRef, ConstraintOp.IS_NOT_NULL);

        ConstraintSet constraintSet = new ConstraintSet(ConstraintOp.AND);
        constraintSet.addConstraint(baitNotNullConstraint);
        constraintSet.addConstraint(preyNotNullConstraint);

        baitAndPreyNotNullQuery.setConstraint(constraintSet);

        Results results = os.execute(baitAndPreyNotNullQuery);
        Iterator resIter = results.iterator();
        HashMap resMap = new HashMap();

        while (resIter.hasNext()) {

            ResultsRow rr = (ResultsRow) resIter.next();
            ProteinInteraction interaction = (ProteinInteraction) rr.get(0);

            Protein baitProtein = interaction.getBait();
            Protein preyProtein = interaction.getPrey();

            if (resMap.containsKey(interaction)) {
                Set proteinSet = (Set) resMap.get(interaction);
                proteinSet.add(baitProtein);
                proteinSet.add(preyProtein);
            } else {
                Set proteinSet = new HashSet();
                proteinSet.add(baitProtein);
                proteinSet.add(preyProtein);
                resMap.put(interaction, proteinSet);
            }
        }

        return resMap;
    }

    private void setInteractions(Map resMap) {

        HashMap proteins2Interactions = new HashMap();

        for (Iterator resMapKeyIt = resMap.keySet().iterator(); resMapKeyIt.hasNext();) {

            ProteinInteraction nextInteraction = (ProteinInteraction) resMapKeyIt.next();

            Set complexSet = (Set) resMap.get(nextInteraction);

            for (Iterator csIt = complexSet.iterator(); csIt.hasNext();) {

                Protein nextProtein = (Protein) csIt.next();

                HashSet proteinInteractions = (HashSet) proteins2Interactions.get(nextProtein);

                if (proteinInteractions == null) {
                    proteinInteractions = new HashSet();
                    proteins2Interactions.put(nextProtein, proteinInteractions);
                }

                proteinInteractions.add(nextInteraction);
            }
        }

        for (Iterator p2iKeyIt = proteins2Interactions.keySet().iterator(); p2iKeyIt.hasNext(); ) {

            Protein nextProtein = (Protein) p2iKeyIt.next();
            nextProtein.setInteractions((Set) proteins2Interactions.get(nextProtein));
        }

    }

    private void storeInteractions(Map resMap) throws ObjectStoreException {

        if (resMap == null) {
            LOG.warn("UPI: ResultMap is null!");
        } else if (resMap.isEmpty()) {
            LOG.warn("UPI: ResultMap is empty!");
        } else {

            osw.beginTransaction();

            for (Iterator resMapKeyIt = resMap.keySet().iterator(); resMapKeyIt.hasNext();) {

                ProteinInteraction nextInteraction = (ProteinInteraction) resMapKeyIt.next();
                LOG.info("UPI: INTERACTION:" + nextInteraction.getShortName());
                osw.store(nextInteraction);

                Set complexSet = (Set) resMap.get(nextInteraction);

                if (complexSet == null || complexSet.isEmpty()) {

                    LOG.info("UPI: NO PROTEINS FOUND FOR INTERACTION:"
                            + nextInteraction.getShortName());
                } else {

                    for (Iterator csIt = complexSet.iterator(); csIt.hasNext();) {

                        Protein nextProtein = (Protein) csIt.next();
                        LOG.info("UPI:  PROTEIN:" + nextProtein.getPrimaryAccession());
                        osw.store(nextProtein);
                    }
                }
            }

            osw.commitTransaction();
        }
    }
}