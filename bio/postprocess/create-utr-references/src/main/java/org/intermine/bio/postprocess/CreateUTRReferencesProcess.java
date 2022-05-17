package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Iterator;



import org.intermine.bio.util.Constants;
import org.intermine.bio.util.PostProcessUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.DynamicUtil;

import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;


import java.sql.SQLException;

/**
 * Calculate additional mappings between annotation after loading into genomic ObjectStore.
 * Currently designed to cope with situation after loading ensembl, may need to change
 * as other annotation is loaded.  New Locations (and updated BioEntities) are stored
 * back in originating ObjectStore.
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CreateUTRReferencesProcess extends PostProcessor
{
    /**
     * Create a new instance of FlyBasePostProcess
     *
     * @param osw object store writer
     */
    public CreateUTRReferencesProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
            throws ObjectStoreException {

        Model model = Model.getInstanceByName("genomic");

        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcMRNA = new QueryClass(model.getClassDescriptorByName("MRNA").getType());
        q.addFrom(qcMRNA);
        q.addToSelect(qcMRNA);
        q.addToOrderBy(qcMRNA);

        QueryClass qcUTR = new QueryClass(model.getClassDescriptorByName("UTR").getType());
        q.addFrom(qcUTR);
        q.addToSelect(qcUTR);
        q.addToOrderBy(qcUTR);

        QueryCollectionReference mrnaUtrsRef = new QueryCollectionReference(qcMRNA, "UTRs");
        ContainsConstraint mrnaUtrsConstraint =
                new ContainsConstraint(mrnaUtrsRef, ConstraintOp.CONTAINS, qcUTR);

        QueryObjectReference fivePrimeRef = new QueryObjectReference(qcMRNA, "fivePrimeUTR");
        ContainsConstraint fivePrimeNullConstraint =
                new ContainsConstraint(fivePrimeRef, ConstraintOp.IS_NULL);
        QueryObjectReference threePrimeRef = new QueryObjectReference(qcMRNA, "threePrimeUTR");
        ContainsConstraint threePrimeNullConstraint =
                new ContainsConstraint(threePrimeRef, ConstraintOp.IS_NULL);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(mrnaUtrsConstraint);
        cs.addConstraint(fivePrimeNullConstraint);
        cs.addConstraint(threePrimeNullConstraint);

        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants
                .PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 500, true, true, true);

        InterMineObject lastMRNA = null;

        InterMineObject fivePrimeUTR = null;
        InterMineObject threePrimeUTR = null;

        osw.beginTransaction();

        Class<? extends FastPathObject> fivePrimeUTRCls =
                model.getClassDescriptorByName("FivePrimeUTR").getType();

        Iterator<?> resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            InterMineObject mrna = (InterMineObject) rr.get(0);
            InterMineObject utr = (InterMineObject) rr.get(1);

            if (lastMRNA != null && !mrna.getId().equals(lastMRNA.getId())) {
                try {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempMRNA = PostProcessUtil.cloneInterMineObject(lastMRNA);
                    if (fivePrimeUTR != null) {
                        tempMRNA.setFieldValue("fivePrimeUTR", fivePrimeUTR);
                        fivePrimeUTR = null;
                    }
                    if (threePrimeUTR != null) {
                        tempMRNA.setFieldValue("threePrimeUTR", threePrimeUTR);
                        threePrimeUTR = null;
                    }
                    osw.store(tempMRNA);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to clone mRNA object:" + e);
                }
            }

            if (DynamicUtil.isInstance(utr, fivePrimeUTRCls)) {
                fivePrimeUTR = utr;
            } else {
                threePrimeUTR = utr;
            }

            lastMRNA = mrna;
        }

        if (lastMRNA != null) {
            try {
                // clone so we don't change the ObjectStore cache
                InterMineObject tempMRNA = PostProcessUtil.cloneInterMineObject(lastMRNA);
                tempMRNA.setFieldValue("fivePrimeUTR", fivePrimeUTR);
                tempMRNA.setFieldValue("threePrimeUTR", threePrimeUTR);
                osw.store(tempMRNA);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to clone mRNA object: " + e);
            }
        }
        osw.commitTransaction();


        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName("MRNA");
            try {
                DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld,
                    false);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to analyse database: " + e);
            }
        }
    }
}
