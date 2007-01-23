package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.HashMap;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStore;

import org.intermine.sql.Database;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.util.DatabaseUtil;
import org.intermine.util.TypeUtil;
import org.flymine.model.genomic.*;

import org.apache.log4j.Logger;

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
       //  LOG.info("insertReferences stage 1");

        // Transcript.exons / Exon.transcripts
        insertCollectionField(Transcript.class, "subjects", RankedRelation.class, "subject",
                              Exon.class, "transcripts", false);
        insertCollectionField(Transcript.class, "subjects", SimpleRelation.class, "subject",
                              Exon.class, "transcripts", false);

        // Intron.MRNAs / MRNA.introns
        insertCollectionField(Transcript.class, "subjects", SimpleRelation.class, "subject",
                              Intron.class, "introns", true);
        insertCollectionField(Transcript.class, "subjects", SimpleRelation.class, "subject",
                              Exon.class, "exons", true);

        LOG.info("insertReferences stage 2");
        // Gene.transcript / Transcript.gene
        insertReferenceField(Gene.class, "subjects", SimpleRelation.class, "subject",
                             Transcript.class, "gene");

        LOG.info("insertReferences stage 3");
        insertReferenceField(Chromosome.class, "subjects", Location.class, "subject",
                             LocatedSequenceFeature.class, "chromosome");

        LOG.info("insertReferences stage 4");
        // fill in collections on Chromosome
        insertCollectionField(Gene.class, "objects", Location.class, "object",
                              Chromosome.class, "genes", false);
        insertCollectionField(Transcript.class, "objects", Location.class, "object",
                              Chromosome.class, "transcripts", false);
        insertCollectionField(Exon.class, "objects", Location.class, "object",
                              Chromosome.class, "exons", false);
        insertCollectionField(ChromosomeBand.class, "subjects", Location.class, "subject",
                              Chromosome.class, "chromosomeBands", false);

        LOG.info("insertReferences stage 5");
        // Exon.gene / Gene.exons
        insertReferenceField(Gene.class, "transcripts", Transcript.class, "exons",
                             Exon.class, "gene");
        LOG.info("insertReferences stage 6");
        // UTR.gene / Gene.UTRs
        insertReferenceField(Gene.class, "transcripts", MRNA.class, "UTRs",
                             UTR.class, "gene");
        LOG.info("insertReferences stage 7");
        // Gene.chromosome / Chromosome.genes
        insertReferenceField(Chromosome.class, "exons", Exon.class, "gene",
                             Gene.class, "chromosome");
        LOG.info("insertReferences stage 8");
        // Transcript.chromosome / Chromosome.transcripts
        insertReferenceField(Chromosome.class, "exons", Exon.class, "transcripts",
                             Transcript.class, "chromosome");
        LOG.info("insertReferences stage 9");
        // Protein.genes / Gene.proteins
        insertCollectionField(Gene.class, "transcripts", Transcript.class, "protein",
                              Protein.class, "genes", false);

        // CDS.gene / Gene.CDSs
        insertReferenceField(Gene.class, "transcripts", MRNA.class, "CDSs",
                             CDS.class, "gene");

        // Gene.translations / Translations.gene
        insertReferenceField(Gene.class, "transcripts", MRNA.class, "translation",
                             Translation.class, "gene");

        LOG.info("insertReferences stage 10");
        insertReferenceField(Gene.class, "CDSs", CDS.class, "polypeptides",
                             Translation.class, "gene");

        LOG.info("insertReferences stage 11");
        insertReferenceField(Translation.class, "CDSs", CDS.class, "MRNA",
                             MRNA.class, "translation");

        LOG.info("insertReferences stage 12");
        // Gene.CDSs.polypeptides
        insertReferenceField(MRNA.class, "CDSs", CDS.class, "polypeptides",
                             Translation.class, "transcript");

        ObjectStore os = osw.getObjectStore();
        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            DatabaseUtil.analyse(db, false);
        }

        LOG.info("insertReferences stage 13");
        insertGeneAnnotationReferences();

        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            DatabaseUtil.analyse(db, false);
        }

        LOG.info("insertReferences stage 16");
        createUtrRefs();

        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            DatabaseUtil.analyse(db, false);
        }
    }

    /**
     * Fill in the "orthologues" collection of Gene.  Needs to be run after
     * UpdateOrthologues which in turn relies on CreateReferences -&gt; so has
     * become a separate method.
     * @throws Exception if anything goes wrong
     */
    public void populateOrthologuesCollection() throws Exception {
        insertReferences(Gene.class, Orthologue.class, "subjects", "orthologues");
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
     * @param sourceClass the first class in the query
     * @param sourceClassFieldName the field in the sourceClass which should contain the
     * connectingClass
     * @param connectingClass the class referred to by sourceClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * destinationClass
     * @param destinationClass the class referred to by
     * connectingClass.connectingClassFieldName
     * @param createFieldName the reference field in the destinationClass - the
     * collection to create/set
     * @throws Exception if anything goes wrong
     */
    protected void insertReferenceField(Class sourceClass, String sourceClassFieldName,
                                        Class connectingClass, String connectingClassFieldName,
                                        Class destinationClass, String createFieldName)
        throws Exception {
        LOG.info("Beginning insertReferences("
                 + sourceClass.getName() + ", "
                 + sourceClassFieldName + ", "
                 + connectingClass.getName() + ", "
                 + connectingClassFieldName + ","
                 + destinationClass.getName() + ", "
                 + createFieldName + ")");

        Iterator resIter =
            PostProcessUtil.findConnectingClasses(osw.getObjectStore(),
                                          sourceClass, sourceClassFieldName,
                                          connectingClass, connectingClassFieldName,
                                          destinationClass, true);

        // results will be sourceClass ; destClass (ordered by sourceClass)
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisSourceObject = (InterMineObject) rr.get(0);
            InterMineObject thisDestObject = (InterMineObject) rr.get(1);

            try {
                // clone so we don't change the ObjectStore cache
                InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(thisDestObject);
                TypeUtil.setFieldValue(tempObject, createFieldName, thisSourceObject);
                count++;
                if (count % 10000 == 0) {
                    LOG.info("Created " + count + " references in " + destinationClass.getName()
                             + " to " + sourceClass.getName()
                             + " via " + connectingClass.getName());
                }
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + thisDestObject.getId()
                          + " has no " + createFieldName + " field");
            }
        }

        LOG.info("Finished: created " + count + " references in " + destinationClass.getName()
                 + " to " + sourceClass.getName() + " via " + connectingClass.getName());
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(destinationClass.getName());
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
     * @param firstClass the first class in the query
     * @param firstClassFieldName the field in the firstClass which should contain the
     * connectingClass
     * @param connectingClass the class referred to by firstClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * secondClass
     * @param secondClass the class referred to by
     * connectingClass.connectingClassFieldName
     * @param createFieldName the collection field in the secondClass - the
     * collection to create/set
     * @param createInFirstClass if true create the new collection field in firstClass, otherwise
     * create in secondClass
     * @throws Exception if anything goes wrong
     */
    protected void insertCollectionField(Class firstClass, String firstClassFieldName,
                                         Class connectingClass, String connectingClassFieldName,
                                         Class secondClass, String createFieldName,
                                         boolean createInFirstClass)
        throws Exception {
        InterMineObject lastDestObject = null;
        Set newCollection = new HashSet();

        LOG.info("Beginning insertReferences("
                 + firstClass.getName() + ", "
                 + firstClassFieldName + ", "
                 + connectingClass.getName() + ", "
                 + connectingClassFieldName + ","
                 + secondClass.getName() + ", "
                 + createFieldName + ", "
                 + createInFirstClass + ")");

        Iterator resIter =
            PostProcessUtil.findConnectingClasses(osw.getObjectStore(),
                                          firstClass, firstClassFieldName,
                                          connectingClass, connectingClassFieldName,
                                          secondClass, createInFirstClass);

        // results will be firstClass ; destClass (ordered by firstClass)
        osw.beginTransaction();
        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisSourceObject;
            InterMineObject thisDestObject;

            if (createInFirstClass) {
                thisDestObject = (InterMineObject) rr.get(0);
                thisSourceObject = (InterMineObject) rr.get(1);
            } else {
                thisDestObject = (InterMineObject) rr.get(1);
                thisSourceObject = (InterMineObject) rr.get(0);
            }

            if (lastDestObject == null
                || !thisDestObject.getId().equals(lastDestObject.getId())) {

                if (lastDestObject != null) {
                    try {
                        InterMineObject tempObject =
                            PostProcessUtil.cloneInterMineObject(lastDestObject);
                        Set oldCollection = (Set) TypeUtil.getFieldValue(tempObject,
                                createFieldName);
                        newCollection.addAll(oldCollection);
                        TypeUtil.setFieldValue(tempObject, createFieldName, newCollection);
                        count += newCollection.size();
                        osw.store(tempObject);
                   } catch (IllegalAccessException e) {
                        LOG.error("Object with ID " + thisDestObject.getId()
                                  + " has no " + createFieldName + " field", e);
                    }
                }

                newCollection = new HashSet();
            }

            newCollection.add(thisSourceObject);

            lastDestObject = thisDestObject;
        }

        if (lastDestObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastDestObject);
            TypeUtil.setFieldValue(tempObject, createFieldName, newCollection);
            count += newCollection.size();
            osw.store(tempObject);
        }

        LOG.info("Created " + count + " references in " + secondClass.getName() + " to "
                 + firstClass.getName() + " via " + connectingClass.getName());
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(secondClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }


    /**
     * Create specific named collection of a particular Relation type.
     * For example Gene.subjects contains Orthologues and other Relations, create collection
     * called Gene.orthologues with just Orthologues in (but remain duplicated in Gene.subjects.
     * BioEntity.collection1 -> Relation   ==>   BioEntity.collection2 -> subclass of Relation
     * @param thisClass the class of the objects to fill in a collection for
     * @param collectionClass the type of Relation in the collection
     * @param oldCollectionName the name of the collection to find objects in
     * @param newCollectionName the name of the collection to add objects to
     * @throws Exception if anything goes wrong
     */
    protected void insertReferences(Class thisClass, Class collectionClass,
                                    String oldCollectionName, String newCollectionName)
        throws Exception {

        LOG.info("Beginning insertReferences(" + thisClass.getName() + ", "
                 + collectionClass.getName() + ", " + oldCollectionName + ", "
                 + newCollectionName + ")");

        InterMineObject lastObject = null;
        Set newCollection = new HashSet();
        Iterator resIter = PostProcessUtil.findConnectedClasses(osw.getObjectStore(), thisClass,
                                                         collectionClass, oldCollectionName);
        // results will be: thisClass ; collectionClass  (ordered by thisClass)
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisObject = (InterMineObject) rr.get(0);
            InterMineObject collectionObject = (InterMineObject) rr.get(1);

            if (lastObject == null || !thisObject.getId().equals(lastObject.getId())) {
                if (lastObject != null) {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

                    TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
                    count += newCollection.size();
                    osw.store(tempObject);
                }

                newCollection = new HashSet();
            }

            newCollection.add(collectionObject);

            lastObject = thisObject;
        }

        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);
            TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
            count += newCollection.size();
            osw.store(tempObject);
        }
        LOG.info("Created " + count + " " + newCollectionName + " collections in "
                 + thisClass.getName() + " of " + collectionClass.getName());
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(thisClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }


    /**
     * Fill in missing references/collections in model by querying relations
     * BioEntity -> Annotation -> BioProperty   ==>   BioEntity -> BioProperty
     * @param entityClass the class of the objects to which the collection will be added
     * @param propertyClass the class of the BioProperty to transkfer
     * @param newCollectionName the collection in entityClass to add objects to
     * @throws Exception if anything goes wrong
     */
    protected void insertReferences(Class entityClass, Class propertyClass,
                                    String newCollectionName)
        throws Exception {

        LOG.info("Beginning insertReferences(" + entityClass.getName() + ", "
                 + propertyClass.getName() + ", " + newCollectionName + ")");

        InterMineObject lastObject = null;
        Set newCollection = new HashSet();
        Iterator resIter = PostProcessUtil.findProperties(osw.getObjectStore(), entityClass,
                                                         propertyClass);
        // results will be:  entityClass ; properyClass (ordered by entityClass)
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisObject = (InterMineObject) rr.get(0);
            InterMineObject thisProperty = (InterMineObject) rr.get(1);

            if (lastObject == null || !thisObject.getId().equals(lastObject.getId())) {
                if (lastObject != null) {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

                    TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
                    count += newCollection.size();
                    osw.store(tempObject);
                }

                newCollection = new HashSet();
            }

            newCollection.add(thisProperty);

            lastObject = thisObject;
        }

        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);
            TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
            count += newCollection.size();
            osw.store(tempObject);
        }
        LOG.info("Created " + count + " " + newCollectionName + " collections in "
                 + entityClass.getName() + " of " + propertyClass.getName());
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(entityClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }

    /**
     * Set the collection in objectClass by the name of collectionFieldName by querying for pairs of
     * objects in the bioEntities collection of relationClass (which should be a
     * SymmetricalRelation).
     * @param objectClass the class of the objects to which the collection will be added and which
     * should be in the bioEntities collection of the relationClass
     * @param relationClass the class that relates objectClass objects togeather
     * @param collectionFieldName if there is a collection other than bioEntities which is available
     * @throws Exception if anything goes wrong
     */
    protected void insertSymmetricalRelationReferences(
            Class objectClass, Class relationClass, String collectionFieldName)
        throws Exception {
        LOG.info("Beginning insertSymmetricalReferences(" + objectClass.getName() + ", "
                 + relationClass.getName() + ", "
                 + collectionFieldName + ")");

        InterMineObject lastObject = null;
        Set newCollection = new HashSet();
        // results will be:  object1, relation, object2  (ordered by object1)
        Iterator resIter =
            PostProcessUtil.findSymmetricalRelation(
                    osw.getObjectStore(), objectClass, relationClass);

        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            BioEntity object1 = (BioEntity) rr.get(0);
            BioEntity object2 = (BioEntity) rr.get(2);

            if (lastObject == null || !object1.getId().equals(lastObject.getId())) {
                if (lastObject != null) {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

                    TypeUtil.setFieldValue(tempObject, collectionFieldName, newCollection);
                    count += newCollection.size();
                    osw.store(tempObject);
                }

                newCollection = new HashSet();
            }

            if (!object1.getId().equals(object2.getId())) {
                // don't add the object to its own collection
                newCollection.add(object2);
            }

            lastObject = object1;
        }

        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);
            TypeUtil.setFieldValue(tempObject, collectionFieldName, newCollection);
            count += newCollection.size();
            osw.store(tempObject);
        }
        LOG.info("Created " + count + " references in " + objectClass.getName() + " to "
                 + objectClass.getName() + " via the " + collectionFieldName + " field");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(objectClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }

    }


    /**
     * Copy all GO annotations from the Protein objects to the corresponding Gene(s)
     * @throws Exception if anything goes wrong
     */
    protected void insertGeneAnnotationReferences()
        throws Exception {
        LOG.debug("Beginning insertGeneAnnotationReferences()");

        osw.beginTransaction();

        Iterator resIter = findProteinProperties(false);

        int count = 0;
        Gene lastGene = null;
        Set allGoCollection = new HashSet();
        Set goCollection = new HashSet();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Gene thisGene = (Gene) rr.get(0);
            GOAnnotation thisAnnotation = (GOAnnotation) rr.get(1);

            GOAnnotation tempAnnotation =
                (GOAnnotation) PostProcessUtil.cloneInterMineObject(thisAnnotation);
            // generate a new ID
            tempAnnotation.setId(null);
            tempAnnotation.setSubject(thisGene);
            if (lastGene != null && !(lastGene.equals(thisGene))) {
                TypeUtil.setFieldValue(lastGene, "allGoAnnotation", allGoCollection);
                TypeUtil.setFieldValue(lastGene, "goAnnotation", goCollection);
                LOG.debug("store gene " + lastGene.getIdentifier() + " with "
                          + lastGene.getAllGoAnnotation().size() + " allGO and "
                          + lastGene.getGoAnnotation().size() + " GO.");
                osw.store(lastGene);

                lastGene = thisGene;
                allGoCollection = new HashSet();
                goCollection = new HashSet();
            }
            allGoCollection.add(tempAnnotation);
            if (tempAnnotation.getIsPrimaryAssignment().equals(Boolean.TRUE)) {
                goCollection.add(tempAnnotation);
            }
            osw.store(tempAnnotation);

            lastGene = thisGene;
            count++;
        }

        if (lastGene != null) {
            TypeUtil.setFieldValue(lastGene, "allGoAnnotation", allGoCollection);
            TypeUtil.setFieldValue(lastGene, "goAnnotation", goCollection);
            LOG.debug("store gene " + lastGene.getIdentifier() + " with "
                      + lastGene.getAllGoAnnotation().size() + " allGO and "
                      + lastGene.getGoAnnotation().size() + " GO.");
            osw.store(lastGene);
        }

        LOG.info("Created " + count + " new Go annotation collections for Genes");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(Annotation.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }

    /**
     * Read the UTRs collection of MRNA then set the fivePrimeUTR and threePrimeUTR fields with the
     * corresponding UTRs.
     * @throws Exception if anything goes wrong
     */
    protected void createUtrRefs() throws Exception {
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcMRNA = new QueryClass(MRNA.class);
        q.addFrom(qcMRNA);
        q.addToSelect(qcMRNA);
        q.addToOrderBy(qcMRNA);

        QueryClass qcUTR = new QueryClass(UTR.class);
        q.addFrom(qcUTR);
        q.addToSelect(qcUTR);
        q.addToOrderBy(qcUTR);

        QueryCollectionReference mrnaUtrsRef =
            new QueryCollectionReference(qcMRNA, "UTRs");
        ContainsConstraint mrnaUtrsConstraint =
            new ContainsConstraint(mrnaUtrsRef, ConstraintOp.CONTAINS, qcUTR);

        q.setConstraint(mrnaUtrsConstraint);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        int count = 0;
        MRNA lastMRNA = null;

        FivePrimeUTR fivePrimeUTR = null;
        ThreePrimeUTR threePrimeUTR = null;

        osw.beginTransaction();

        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            MRNA mrna = (MRNA) rr.get(0);
            UTR utr = (UTR) rr.get(1);

            if (lastMRNA != null && !mrna.getId().equals(lastMRNA.getId())) {
                // clone so we don't change the ObjectStore cache
                MRNA tempMRNA = (MRNA) PostProcessUtil.cloneInterMineObject(lastMRNA);
                if (fivePrimeUTR != null) {
                    TypeUtil.setFieldValue(tempMRNA, "fivePrimeUTR", fivePrimeUTR);
                    fivePrimeUTR = null;
                }
                if (threePrimeUTR != null) {
                    TypeUtil.setFieldValue(tempMRNA, "threePrimeUTR", threePrimeUTR);
                    threePrimeUTR = null;
                }
                osw.store(tempMRNA);
                count++;
            }

            if (utr instanceof FivePrimeUTR) {
                fivePrimeUTR = (FivePrimeUTR) utr;
            } else {
                threePrimeUTR = (ThreePrimeUTR) utr;
            }

            lastMRNA = mrna;
        }

        if (lastMRNA != null) {
            // clone so we don't change the ObjectStore cache
            MRNA tempMRNA = (MRNA) PostProcessUtil.cloneInterMineObject(lastMRNA);
            TypeUtil.setFieldValue(tempMRNA, "fivePrimeUTR", fivePrimeUTR);
            TypeUtil.setFieldValue(tempMRNA, "threePrimeUTR", threePrimeUTR);
            osw.store(tempMRNA);
            count++;
        }
        LOG.info("Stored MRNA " + count + " times (" + count * 2 + " fields set)");
        osw.commitTransaction();


        // now ANALYSE tables relating to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(MRNA.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }

    /**
     * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene,
     *  Protein and GOTerm.
     *
     * @param restrictToPrimaryGoTermsOnly Only get primary Annotation items linking the gene
     *  and the go term.
     */
    private Iterator findProteinProperties(boolean restrictToPrimaryGoTermsOnly) throws Exception {
        Query q = new Query();

        q.setDistinct(false);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);

        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);

        QueryClass qcAnnotation = null;

        if (restrictToPrimaryGoTermsOnly) {
            qcAnnotation = new QueryClass(GOAnnotation.class);
            q.addFrom(qcAnnotation);
            q.addToSelect(qcAnnotation);
        } else {
            qcAnnotation = new QueryClass(Annotation.class);
            q.addFrom(qcAnnotation);
            q.addToSelect(qcAnnotation);
        }

        QueryClass qcGOTerm = new QueryClass(GOTerm.class);
        q.addFrom(qcGOTerm);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference geneProtRef = new QueryCollectionReference(qcProtein, "genes");
        ContainsConstraint geneProtConstraint =
            new ContainsConstraint(geneProtRef, ConstraintOp.CONTAINS, qcGene);
        cs.addConstraint(geneProtConstraint);

        QueryCollectionReference protAnnRef =
            new QueryCollectionReference(qcProtein, "annotations");
        ContainsConstraint protAnnConstraint =
            new ContainsConstraint(protAnnRef, ConstraintOp.CONTAINS, qcAnnotation);
        cs.addConstraint(protAnnConstraint);

        QueryObjectReference annPropertyRef =
            new QueryObjectReference(qcAnnotation, "property");
        ContainsConstraint annPropertyConstraint =
            new ContainsConstraint(annPropertyRef, ConstraintOp.CONTAINS, qcGOTerm);
        cs.addConstraint(annPropertyConstraint);

        if (restrictToPrimaryGoTermsOnly) {
            QueryField isPrimaryTermQueryField =
                    new QueryField(qcAnnotation, "isPrimaryAssignment");
            QueryValue trueValue = new QueryValue(Boolean.TRUE);
            SimpleConstraint primeConst =
                    new SimpleConstraint(isPrimaryTermQueryField, ConstraintOp.EQUALS, trueValue);
            cs.addConstraint(primeConst);
        }

        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        return res.iterator();
    }

    /**
     * Performs the business of linking Proteins to ProteinInteractions as well as other proteins
     * that are related back to the protein via the interaction object.
     *
     * @throws Exception - if there is a problem...
     * */
    public void linkProteinToProtenInteractionAndRelatedProteins() throws Exception {

        Query q = new Query();

        q.setDistinct(false);

        QueryClass proteinQC = new QueryClass(Protein.class);
        q.addToSelect(proteinQC);
        q.addFrom(proteinQC);
        q.addToOrderBy(proteinQC);

        QueryClass interactorQC = new QueryClass(ProteinInteractor.class);
        q.addFrom(interactorQC);

        QueryClass interactionQC = new QueryClass(ProteinInteraction.class);
        q.addToSelect(interactionQC);
        q.addFrom(interactionQC);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference proteinToInteractorRef =
                new QueryCollectionReference(proteinQC, "interactionRoles");
        ContainsConstraint proteinInteractorConstraint =
                new ContainsConstraint(proteinToInteractorRef, ConstraintOp.CONTAINS, interactorQC);
        cs.addConstraint(proteinInteractorConstraint);

        QueryCollectionReference interactionToInteractorRef =
                new QueryCollectionReference(interactionQC, "interactors");
        ContainsConstraint interactionInteractorConstraint =
                new ContainsConstraint(interactionToInteractorRef,
                        ConstraintOp.CONTAINS, interactorQC);
        cs.addConstraint(interactionInteractorConstraint);

        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        HashMap proteinToInteractionsMap = new HashMap();
        HashMap interactionToProteinsMap = new HashMap();

        LOG.debug("CHECKING linkProteinToProtenInteractionAndRelatedProteins RESULTS");
        for (Iterator resIter = res.iterator(); resIter.hasNext();) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Protein protein = (Protein) rr.get(0);
            ProteinInteraction interaction = (ProteinInteraction) rr.get(1);

            addProteinAndInterationPairToMap(protein, interaction, proteinToInteractionsMap, true);
            addProteinAndInterationPairToMap(protein, interaction, interactionToProteinsMap, false);

            LOG.debug("PROTEIN:" + protein.getPrimaryAccession()
                    + " -> INTERACTION:" + interaction.getShortName());
        }

        osw.beginTransaction();

        LOG.debug("Setting values for interaction.proteins");
        for (Iterator i2pIt = interactionToProteinsMap.keySet().iterator(); i2pIt.hasNext();) {
            ProteinInteraction nextInteraction = (ProteinInteraction) i2pIt.next();
            Set nextProteinSet = (Set) interactionToProteinsMap.get(nextInteraction);
            nextInteraction.setProteins(nextProteinSet);
            osw.store(nextInteraction);
            LOG.debug("INTERACTION:" + nextInteraction.getShortName()
                    + " HAS THIS MANY PROTEINS:" + nextProteinSet.size());

        }

        LOG.debug("Setting values for protein.interactingProteins");
        for (Iterator p2iIt2 = proteinToInteractionsMap.keySet().iterator(); p2iIt2.hasNext();) {
            Protein nextProtein = (Protein) p2iIt2.next();
            Set interactingProteinSet = new HashSet();
            //Explicitly add the starting protein so we can remove it blindly later.
            interactingProteinSet.add(nextProtein);

            Set nextInteractionSet = (Set) proteinToInteractionsMap.get(nextProtein);

            for (Iterator iIt = nextInteractionSet.iterator(); iIt.hasNext(); ) {
                ProteinInteraction nextInteraction = (ProteinInteraction) iIt.next();
                Set nextInteractingProteinSet = (Set) interactionToProteinsMap.get(nextInteraction);
                interactingProteinSet.addAll(nextInteractingProteinSet);
            }

            //TODO: DO WE WANT TO ADD/REMOVE SELF REFS ? I.E. WHERE A PROTEIN INTERACTS WITH ITSELF
            //Explictly remove the starting protein so it wont refer to itself - for the moment!.
            interactingProteinSet.remove(nextProtein);
            nextProtein.setInteractingProteins(interactingProteinSet);
            osw.store(nextProtein);
            LOG.debug("PROTEIN:" + nextProtein.getPrimaryAccession()
                    + " HAS THIS MANY RELATED PROTEINS:" + interactingProteinSet.size());
        }
        osw.commitTransaction();
    }

    /**
     * Little helper method for the linkProteinToProtenInteractionAndRelatedProteins method.
     *
     * @param protein - a protein
     * @param interaction - an interaction the protein appeared in
     * @param map - either a protein-interactions map or vice versa
     * @param useProteinAsTheKey - a boolean indicating which sort of map we have to deal with
     * */
    private void addProteinAndInterationPairToMap(Protein protein, ProteinInteraction interaction,
                                                  HashMap map, boolean useProteinAsTheKey) {

        //Treat the protein as the key
        if (useProteinAsTheKey) {

            if (map.containsKey(protein)) {
                Set interactionSet = (Set) map.get(protein);
                interactionSet.add(interaction);
            } else {
                Set newInteractionSet = new HashSet();
                newInteractionSet.add(interaction);
                map.put(protein, newInteractionSet);
            }
        //Treat the interaction as the key
        } else {

            if (map.containsKey(interaction)) {
                Set proteinSet = (Set) map.get(interaction);
                proteinSet.add(protein);
            } else {
                Set newProteinSet = new HashSet();
                newProteinSet.add(protein);
                map.put(interaction, newProteinSet);
            }
        }
    }
}
