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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;
import java.sql.SQLException;
import org.intermine.sql.DatabaseUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.bio.util.PostProcessUtil;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * Calculate additional mappings between annotation after loading into genomic ObjectStore.
 * Currently designed to cope with situation after loading ensembl, may need to change
 * as other annotation is loaded.  New Locations (and updated BioEntities) are stored
 * back in originating ObjectStore.
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CreateReferencesProcess extends PostProcessor
{
    private Model model;
    private static final Logger LOG = Logger.getLogger(CreateReferencesProcess.class);
    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public CreateReferencesProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess() throws ObjectStoreException {

        model = Model.getInstanceByName("genomic");

        LOG.info("insertReferences stage 1");
        // fill in collections on Chromosome
        insertCollectionField("ChromosomeBand", "locations",
            "Location", "locatedOn",
            "Chromosome", "chromosomeBands",
            false);

        LOG.info("insertReferences stage 2");
        // Exon.gene / Gene.exons
        insertReferenceField("Gene", "transcripts",
            "Transcript", "exons",
            "Exon", "gene");
        LOG.info("insertReferences stage 3");
        // UTR.gene / Gene.UTRs
        insertReferenceField("Gene", "transcripts",
            "Transcript", "UTRs",
            "UTR", "gene");

        LOG.info("insertReferences stage 4");
        // CDS.gene / Gene.CDSs
        insertReferenceField("Gene", "transcripts",
            "Transcript", "CDSs",
            "CDS", "gene");
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
     * @throws ObjectStoreException if anything goes wrong
     */
    protected void insertReferenceField(String sourceClsName, String sourceClassFieldName,
        String connectingClsName, String connectingClassFieldName, String destinationClsName,
        String createFieldName) throws ObjectStoreException {

        String insertMessage = "insertReferences("
                + sourceClsName + ", "
                + sourceClassFieldName + ", "
                + connectingClsName + ", "
                + connectingClassFieldName + ","
                + destinationClsName + ", "
                + createFieldName + ")";

        // Check that classes and fields specified exist in model
        if (model == null) {
            model = Model.getInstanceByName("genomic");
        }
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

        Iterator<ResultsRow<InterMineObject>> resIter = null;

        if (model == null) {
            model = Model.getInstanceByName("genomic");
        }
        try {
            resIter = PostProcessUtil.findConnectingClasses(
                    osw.getObjectStore(),
                    model.getClassDescriptorByName(sourceClsName).getType(),
                    sourceClassFieldName,
                    model.getClassDescriptorByName(connectingClsName).getType(),
                    connectingClassFieldName,
                    model.getClassDescriptorByName(destinationClsName).getType(), true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("couldn't find connecting classes " + e);
        }


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
            try {
                DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld,
                        false);
            } catch (SQLException e) {
                throw new RuntimeException("Couldn't analyse database " + e);
            }
        }
    }

    /**
     * Add a collection of objects of type X to objects of type Y by using a connecting class.
     * Eg. Add a collection of Protein objects to Gene by examining the Transcript objects in
     * the transcripts collection of the Gene, which would use a query like:
     *   SELECT DISTINCT gene FROM Gene AS gene, Transcript AS transcript, Protein AS protein
     *   WHERE (gene.transcripts CONTAINS transcript AND transcript.protein CONTAINS protein)
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
     * @param createInFirstClass if true create the new collection field in firstClass,
     * otherwise create in secondClass
     * @throws ObjectStoreException if anything goes wrong
     */
    protected void insertCollectionField(String firstClsName, String firstClassFieldName,
        String connectingClsName, String connectingClassFieldName, String secondClsName,
        String createFieldName, boolean createInFirstClass) throws ObjectStoreException {
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
        if (model == null) {
            model = Model.getInstanceByName("genomic");
        }
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
            String msg = "Error running post-process `create-references` for `"
                    + createFieldName + "` since this collection doesn't exist in the model.";
            LOG.error(msg);
            return;
        }

        if (col.relationType() == CollectionDescriptor.M_N_RELATION) {
            manyToMany = true;
        }

        Iterator<ResultsRow<InterMineObject>> resIter = null;
        try {
            resIter = PostProcessUtil.findConnectingClasses(
                    osw.getObjectStore(),
                    model.getClassDescriptorByName(firstClsName).getType(),
                    firstClassFieldName,
                    model.getClassDescriptorByName(connectingClsName).getType(),
                    connectingClassFieldName,
                    model.getClassDescriptorByName(secondClsName).getType(), createInFirstClass);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("couldn't find connecting classes " + e);
        }

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
            try {
                // clone so we don't change the ObjectStore cache
                InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastDestObject);
                tempObject.setFieldValue(createFieldName, newCollection);
                count += newCollection.size();
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to clone " + e);
            }
        }
        LOG.info("Finished: created " + count + " references in " + secondClsName + " to "
                + firstClsName + " via " + connectingClsName
                + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(secondClsName);
            try {
                DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld,
                        false);
            } catch (SQLException e) {
                throw new RuntimeException("Couldn't analyse database " + e);
            }
        }
    }
}
