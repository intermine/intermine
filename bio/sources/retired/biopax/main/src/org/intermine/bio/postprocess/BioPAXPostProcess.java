package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.DynamicUtil;


/**
 * Copy over Protein.pathways to Gene.pathways
 *
 * @author Julie Sullivan
 */
public class BioPAXPostProcess extends PostProcessor
{
    private static final Logger LOG = Logger.getLogger(BioPAXPostProcess.class);
    private DataSet reactomeDataSet = null;
    private Model model;

    /**
     * Create a new instance of BioPAXPostProcess.
     *
     * @param osw object store writer
     */
    public BioPAXPostProcess(ObjectStoreWriter osw) {
        super(osw);
        model = Model.getInstanceByName("genomic");
    }

    /**
     * {@inheritDoc}
     * <br/>
     *  Copy over Protein.pathways to Gene.pathways
     */
    @Override
    public void postProcess() {
        try {
            copyProteinPathways();
        } catch (Exception e) {
            throw new RuntimeException("exception in biopax post-processing", e);
        }
    }

    private void copyProteinPathways()
        throws ObjectStoreException, IllegalAccessException, SQLException {

        reactomeDataSet = (DataSet) DynamicUtil.createObject(Collections.singleton(DataSet.class));
        reactomeDataSet.setName("Reactome data set");
        reactomeDataSet =
            (DataSet) osw.getObjectByExample(reactomeDataSet, Collections.singleton("name"));

        if (reactomeDataSet == null) {
            LOG.error("Failed to find reactome DataSet object");
            return;
        }

        Results results = findProteinPathways(osw.getObjectStore());
        int count = 0;
        Gene lastGene = null;
        Set<Pathway> newCollection = new HashSet<Pathway>();

        osw.beginTransaction();

        Iterator<?> resIter = results.iterator();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            Gene thisGene = (Gene) rr.get(0);
            Pathway pathway = (Pathway) rr.get(1);

            if (lastGene == null || !thisGene.getId().equals(lastGene.getId())) {
                if (lastGene != null) {
                    // clone so we don't change the ObjectStore cache
                    Gene tempGene = PostProcessUtil.cloneInterMineObject(lastGene);
                    tempGene.setFieldValue("pathways", newCollection);
                    osw.store(tempGene);
                    count++;
                }
                newCollection = new HashSet<Pathway>();
            }
            newCollection.add(pathway);
            lastGene = thisGene;
        }

        if (lastGene != null) {
            // clone so we don't change the ObjectStore cache
            Gene tempGene = PostProcessUtil.cloneInterMineObject(lastGene);
            tempGene.setFieldValue("pathways", newCollection);
            osw.store(tempGene);
            count++;
        }
        LOG.info("Created " + count + " Gene.pathways collections");
        osw.commitTransaction();

        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(Gene.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }

    }

    /**
     * Run a query that returns all proteins, genes, and associated pathways.
     *
     * @param os the objectstore
     * @return the Results object
     * @throws ObjectStoreException if there is an error while reading from the ObjectStore
     */
    protected static Results findProteinPathways(ObjectStore os)
        throws ObjectStoreException {
        Query q = new Query();
        QueryClass qcProtein = new QueryClass(Protein.class);
        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcPathway = new QueryClass(Pathway.class);

        q.addFrom(qcGene);
        q.addFrom(qcProtein);
        q.addFrom(qcPathway);

        q.addToSelect(qcGene);
        q.addToSelect(qcPathway);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // Protein.genes
        QueryCollectionReference c1 = new QueryCollectionReference(qcProtein, "genes");
        cs.addConstraint(new ContainsConstraint(c1, ConstraintOp.CONTAINS, qcGene));

        // Protein.pathways
        QueryCollectionReference c2 = new QueryCollectionReference(qcProtein, "pathways");
        cs.addConstraint(new ContainsConstraint(c2, ConstraintOp.CONTAINS, qcPathway));

        q.setConstraint(cs);

        ObjectStoreInterMineImpl osimi = (ObjectStoreInterMineImpl) os;
        osimi.precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q);

        return res;
    }
}
