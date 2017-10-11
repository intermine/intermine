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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.MetaDataException;
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

/**
 * Fill in additional references/collections in genomic model
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CreateReferences
{
    private static final Logger LOG = Logger.getLogger(CreateReferences.class);

    protected ObjectStoreWriter osw;
    private Model model;

    /**
     * Construct with an ObjectStoreWriter, read and write from same ObjectStore
     * @param osw an ObjectStore to write to
     */
    public CreateReferences(ObjectStoreWriter osw) {
        this.osw = osw;
        this.model = Model.getInstanceByName("genomic");
    }

    /**
     * Fill in missing references/collections in model by querying relations
     * @throws Exception if anything goes wrong
     */
    public void insertReferences() throws Exception {

        LOG.info("insertReferences stage 1");
        // fill in collections on Chromosome
        insertCollectionField("ChromosomeBand", "locations", "Location", "locatedOn",
                "Chromosome", "chromosomeBands", false);

        LOG.info("insertReferences stage 2");
        // Exon.gene / Gene.exons
        insertReferenceField("Gene", "transcripts", "Transcript", "exons", "Exon", "gene");
        LOG.info("insertReferences stage 3");
        // UTR.gene / Gene.UTRs
        insertReferenceField("Gene", "transcripts", "Transcript", "UTRs", "UTR", "gene");

        LOG.info("insertReferences stage 4");
        // CDS.gene / Gene.CDSs
        insertReferenceField("Gene", "transcripts", "Transcript", "CDSs", "CDS", "gene");
    }

    /**
     * Add a reference to and object of type X in objects of type Y by using a connecting class.
     * Eg. Add a reference to Gene objects in Exon by examining the Transcript objects in the
     * transcripts collection of the Gene, which would use a query like:
     *   SELECT DISTINCT gene FROM Gene AS gene, Transcript AS transcript, Exon AS exon WHERE
     *   (gene.transcripts CONTAINS transcript AND transcript.exons CONTAINS exon) ORDER BY gene
     * and then set exon.gene
     *
     * in overview we are doing:
     * BioEntity1 -&gt; BioEntity2 -&gt; BioEntity3   ==&gt;   BioEntitiy1 -&gt; BioEntity3
     * @param sourceClsName the first class in the query
     * @param sourceClassFieldName the field in the sourceClass which should contain the
     * connectingClass
     * @param connectingClsName the class referred to by sourceClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * destinationClass
     * @param destinationClsName the class referred to by
     * connectingClass.connectingClassFieldName
     * @param createFieldName the reference field in the destinationClass - the
     * collection to create/set
     * @throws Exception if anything goes wrong
     */
    protected void insertReferenceField(String sourceClsName,
            String sourceClassFieldName, String connectingClsName,
            String connectingClassFieldName, String destinationClsName,
            String createFieldName) throws Exception {

        String insertMessage = "insertReferences("
                + sourceClsName + ", "
                + sourceClassFieldName + ", "
                + connectingClsName + ", "
                + connectingClassFieldName + ","
                + destinationClsName + ", "
                + createFieldName + ")";

        // Check that classes and fields specified exist in model
        try {
            String errorMessage = "Not performing " + insertMessage;
            PostProcessUtil.checkFieldExists(model, sourceClsName, sourceClassFieldName,
                    errorMessage);
            PostProcessUtil.checkFieldExists(model, connectingClsName, connectingClassFieldName,
                    errorMessage);
            PostProcessUtil.checkFieldExists(model, destinationClsName, createFieldName,
                    errorMessage);
        } catch (MetaDataException e) {
            return;
        }

        LOG.info("Beginning " + insertMessage);
        long startTime = System.currentTimeMillis();

        Iterator<ResultsRow<InterMineObject>> resIter = PostProcessUtil.findConnectingClasses(
                osw.getObjectStore(),
                model.getClassDescriptorByName(sourceClsName).getType(),
                sourceClassFieldName,
                model.getClassDescriptorByName(connectingClsName).getType(),
                connectingClassFieldName,
                model.getClassDescriptorByName(destinationClsName).getType(), true);

        // results will be sourceClass ; destClass (ordered by sourceClass)
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow<InterMineObject> rr = resIter.next();
            InterMineObject thisSourceObject = rr.get(0);
            InterMineObject thisDestObject = rr.get(1);

            try {
                // clone so we don't change the ObjectStore cache
                InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(thisDestObject);
                tempObject.setFieldValue(createFieldName, thisSourceObject);
                count++;
                if (count % 10000 == 0) {
                    LOG.info("Created " + count + " references in " + destinationClsName
                             + " to " + sourceClsName
                             + " via " + connectingClsName);
                }
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + thisDestObject.getId()
                          + " has no " + createFieldName + " field");
            }
        }

        LOG.info("Finished: created " + count + " references in " + destinationClsName
                 + " to " + sourceClsName + " via " + connectingClsName
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(destinationClsName);
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }

    /**
     * Add a collection of objects of type X to objects of type Y by using a connecting class.
     * Eg. Add a collection of Protein objects to Gene by examining the Transcript objects in the
     * transcripts collection of the Gene, which would use a query like:
     *   SELECT DISTINCT gene FROM Gene AS gene, Transcript AS transcript, Protein AS protein WHERE
     *   (gene.transcripts CONTAINS transcript AND transcript.protein CONTAINS protein)
     *   ORDER BY gene
     * and then set protected gene.protein (if created
     * BioEntity1 -&gt; BioEntity2 -&gt; BioEntity3   ==&gt;   BioEntity1 -&gt; BioEntity3
     * @param firstClsName the first class in the query
     * @param firstClassFieldName the field in the firstClass which should contain the
     * connectingClass
     * @param connectingClsName the class referred to by firstClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * secondClass
     * @param secondClsName the class referred to by
     * connectingClass.connectingClassFieldName
     * @param createFieldName the collection field in the secondClass - the
     * collection to create/set
     * @param createInFirstClass if true create the new collection field in firstClass, otherwise
     * create in secondClass
     * @throws Exception if anything goes wrong
     */
    protected void insertCollectionField(String firstClsName,
            String firstClassFieldName, String connectingClsName,
            String connectingClassFieldName, String secondClsName,
            String createFieldName, boolean createInFirstClass) throws Exception {
        InterMineObject lastDestObject = null;
        Set<InterMineObject> newCollection = new HashSet<InterMineObject>();

        String insertMessage = "insertCollectionField("
            + firstClsName + ", "
            + firstClassFieldName + ", "
            + connectingClsName + ", "
            + connectingClassFieldName + ","
            + secondClsName + ", "
            + createFieldName + ", "
            + createInFirstClass + ")";

        // Check that classes and fields specified exist in model
        try {
            String errorMessage = "Not performing " + insertMessage;
            PostProcessUtil.checkFieldExists(model, firstClsName, firstClassFieldName,
                    errorMessage);
            PostProcessUtil.checkFieldExists(model, connectingClsName, connectingClassFieldName,
                    errorMessage);
            PostProcessUtil.checkFieldExists(model, secondClsName, createFieldName, errorMessage);
        } catch (MetaDataException e) {
            return;
        }

        LOG.info("Beginning " + insertMessage);
        long startTime = System.currentTimeMillis();

        // if this is a many to many collection we can use ObjectStore.addToCollection which will
        // write directly to the database.
        boolean manyToMany = false;
        ClassDescriptor destCld;
        if (createInFirstClass) {
            destCld = model.getClassDescriptorByName(firstClsName);
        } else {
            destCld = model.getClassDescriptorByName(secondClsName);
        }
        CollectionDescriptor col = destCld.getCollectionDescriptorByName(createFieldName);
        if (col == null) {
            String msg = "Error running post-process `create-references` for `" + createFieldName
                + "` since this collection doesn't exist in the model.";
            LOG.error(msg);
            return;
        }

        if (col.relationType() == CollectionDescriptor.M_N_RELATION) {
            manyToMany = true;
        }

        Iterator<ResultsRow<InterMineObject>> resIter = PostProcessUtil.findConnectingClasses(
                osw.getObjectStore(),
                model.getClassDescriptorByName(firstClsName).getType(),
                firstClassFieldName,
                model.getClassDescriptorByName(connectingClsName).getType(),
                connectingClassFieldName,
                model.getClassDescriptorByName(secondClsName).getType(), createInFirstClass);

        // results will be firstClass ; destClass (ordered by firstClass)
        osw.beginTransaction();
        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow<InterMineObject> rr = resIter.next();

            InterMineObject thisSourceObject;
            InterMineObject thisDestObject;

            if (createInFirstClass) {
                thisDestObject = rr.get(0);
                thisSourceObject = rr.get(1);
            } else {
                thisDestObject = rr.get(1);
                thisSourceObject = rr.get(0);
            }

            if (!manyToMany && (lastDestObject == null
                || !thisDestObject.getId().equals(lastDestObject.getId()))) {

                if (lastDestObject != null) {
                    try {
                        InterMineObject tempObject =
                            PostProcessUtil.cloneInterMineObject(lastDestObject);
                        Set<InterMineObject> oldCollection
                            = (Set<InterMineObject>) tempObject.getFieldValue(createFieldName);
                        newCollection.addAll(oldCollection);
                        tempObject.setFieldValue(createFieldName, newCollection);
                        count += newCollection.size();
                        osw.store(tempObject);
                    } catch (IllegalAccessException e) {
                        LOG.error("Object with ID " + thisDestObject.getId()
                                  + " has no " + createFieldName + " field", e);
                    }
                }

                newCollection = new HashSet<InterMineObject>();
            }

            if (manyToMany) {
                osw.addToCollection(thisDestObject.getId(), destCld.getType(),
                        createFieldName, thisSourceObject.getId());
            } else {
                newCollection.add(thisSourceObject);
            }

            lastDestObject = thisDestObject;
        }

        if (!manyToMany && lastDestObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastDestObject);
            tempObject.setFieldValue(createFieldName, newCollection);
            count += newCollection.size();
            osw.store(tempObject);
        }
        LOG.info("Finished: created " + count + " references in " + secondClsName + " to "
                 + firstClsName + " via " + connectingClsName
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(secondClsName);
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }



    /**
     * Read the UTRs collection of MRNA then set the fivePrimeUTR and threePrimeUTR fields with the
     * corresponding UTRs.
     * @throws Exception if anything goes wrong
     */
    public void createUtrRefs() throws Exception {
        long startTime = System.currentTimeMillis();
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

        int count = 0;
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
                count++;
            }

            if (DynamicUtil.isInstance(utr, fivePrimeUTRCls)) {
                fivePrimeUTR = utr;
            } else {
                threePrimeUTR = utr;
            }

            lastMRNA = mrna;
        }

        if (lastMRNA != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempMRNA = PostProcessUtil.cloneInterMineObject(lastMRNA);
            tempMRNA.setFieldValue("fivePrimeUTR", fivePrimeUTR);
            tempMRNA.setFieldValue("threePrimeUTR", threePrimeUTR);
            osw.store(tempMRNA);
            count++;
        }
        LOG.info("Stored MRNA " + count + " times (" + count * 2 + " fields set)"
                 + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();


        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName("MRNA");
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }
}
