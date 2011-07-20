package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Temporary task to update chromosomes with a unique secondaryIdentifier to be used when upgrading
 * to the new list upgrade/ bagvalues system.  It can be discarded once used.
 * @author Richard Smith
 *
 */
public final class MakeUniqueChromosomeIdentifiers
{
    private static final Logger LOG = Logger.getLogger(MakeUniqueChromosomeIdentifiers.class);

    private MakeUniqueChromosomeIdentifiers() {
    }


    /**
     * Create unique secondaryIdentifiers for chromosomes that are the primaryIdentifier pre-
     * pended with an organism abbreviation.
     * @param osw the object store writer
     */
    public static void make(ObjectStoreWriter osw) {
        ObjectStore os = osw.getObjectStore();
        Model model = os.getModel();

        Query q = new Query();
        QueryClass qcChr = new QueryClass(model.getClassDescriptorByName("Chromosome").getType());
        QueryClass qcOrg = new QueryClass(model.getClassDescriptorByName("Organism").getType());

        q.addFrom(qcChr);
        q.addFrom(qcOrg);

        q.addToSelect(qcChr);
        q.addToSelect(qcOrg);

        QueryObjectReference orgRef = new QueryObjectReference(qcChr, "organism");
        ContainsConstraint cc = new ContainsConstraint(orgRef, ConstraintOp.CONTAINS, qcOrg);
        q.setConstraint(cc);

        Results res = os.execute(q);
        Iterator resIter = res.iterator();


        try {
            osw.beginTransaction();
            while (resIter.hasNext()) {
                ResultsRow row = (ResultsRow) resIter.next();
                InterMineObject chr = (InterMineObject) row.get(0);
                InterMineObject org = (InterMineObject) row.get(1);

                String primaryIdentifier = (String) chr.getFieldValue("primaryIdentifier");

                String genus = (String) org.getFieldValue("genus");
                String species = (String) org.getFieldValue("species");

                String abbrev = genus.substring(0, 1).toLowerCase() + species.substring(0, 3);

                if (!primaryIdentifier.startsWith(abbrev)) {
                    String secondaryIdentifier = abbrev + "_" + primaryIdentifier;

                    LOG.info("Setting chr secondaryIdentifier: " + secondaryIdentifier + " for chr "
                            + primaryIdentifier + " - " + genus + " " + species);


                    InterMineObject clonedChr = PostProcessUtil.cloneInterMineObject(chr);
                    clonedChr.setFieldValue("secondaryIdentifier", secondaryIdentifier);
                    osw.store(clonedChr);
                }
            }
            osw.commitTransaction();
        } catch (Exception e) {
            LOG.info("Failed to make unique chromosome locations", e);
        }
    }
}

