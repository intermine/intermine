package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStore;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
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
     * @throws MetaDataException if problem getting genomic Model
     */
    public CreateReferences(ObjectStoreWriter osw) throws MetaDataException {
        this.osw = osw;
        this.model = Model.getInstanceByName("genomic");
    }

    /**
     * Fill in missing references/collections in model by querying relations
     * @throws Exception if anything goes wrong
     */
    public void insertReferences() throws Exception {
        insertReferences(Transcript.class, Exon.class, RankedRelation.class, "exons");
        insertReferences(Gene.class, Transcript.class, SimpleRelation.class, "transcripts");

        insertReferences(Chromosome.class, "subjects", Location.class, "subject",
                         Exon.class, "chromosome");
        insertReferences(Chromosome.class, "subjects", Location.class, "subject",
                         ChromosomeBand.class, "chromosome");
        insertReferences(Gene.class, "subjects", SimpleRelation.class, "subject",
                         Transcript.class, "gene");
        insertReferences(Gene.class, "transcripts", Transcript.class, "exons",
                         Exon.class, "gene");
        insertReferences(Chromosome.class, "exons", Exon.class, "gene",
                         Gene.class, "chromosome");
        insertReferences(Chromosome.class, "exons", Exon.class, "transcripts",
                         Transcript.class, "chromosome");

        insertReferences(Gene.class, Orthologue.class, "subjects", "orthologues");
        insertReferences(Protein.class, ProteinInteraction.class, "subjects", "interactions");
        insertReferences(Protein.class, ProteinInteraction.class, "objects", "interactions");

        insertGeneAnnotationReferences();

        insertReferences(Gene.class, GOTerm.class, "GOTerms");
        insertReferences(Gene.class, Phenotype.class, "phenotypes");
    }

    /**
     * Fill in missing references/collectiosn in model by querying relations
     * BioEntity1 -> Relation -> BioEntity2   ==>   BioEntity1 -> BioEntity2
     * @param objectClass the class of the objects to which the collection will be added
     * @param subjectClass the class of the objects added to the collection
     * @param relationClass the class that relates subjectClass and objectClass
     * @param collectionFieldName the field in the object class to which the subjects will be added
     * @throws Exception if anything goes wrong
     */
    protected void insertReferences(Class objectClass, Class subjectClass,
                                    Class relationClass, String collectionFieldName)
        throws Exception {

        LOG.info("Beginning insertReferences(" + objectClass.getName() + ", "
                 + subjectClass.getName() + ", " + relationClass.getName() + ", "
                 + collectionFieldName + ")");

        InterMineObject lastObject = null;
        List newCollection = new ArrayList();
        Iterator resIter = PostProcessUtil.findRelations(osw.getObjectStore(), objectClass,
                                                         subjectClass, relationClass);
        // results will be:  object ; subject ; relation  (ordered by object_
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisObject = (InterMineObject) rr.get(0);
            InterMineObject thisSubject = (InterMineObject) rr.get(1);

            if (lastObject == null || !thisObject.getId().equals(lastObject.getId())) {
                if (lastObject != null) {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

                    try {
                        TypeUtil.setFieldValue(tempObject, collectionFieldName, newCollection);
                        count += newCollection.size();

                        osw.store(tempObject);
                    } catch (IllegalAccessException e) {
                        LOG.error("Object with ID: " + tempObject.getId()
                                  + " has no " + collectionFieldName + " field");
                    }
                }

                newCollection = new ArrayList();
            }

            newCollection.add(thisSubject);

            lastObject = thisObject;
        }

        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);
            try {
                TypeUtil.setFieldValue(tempObject, collectionFieldName, newCollection);
                count += newCollection.size();
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + tempObject.getId()
                          + " has no " + collectionFieldName + " field");
            }
        }
        LOG.info("Created " + count + " references in " + objectClass.getName() + " to "
                 + subjectClass.getName() + " via the " + collectionFieldName + " field");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(objectClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }

    /**
     * Add a collection of objects of type X to objects of type Y by using a connecting class.
     * Eg. Add a collection of Exon objects to Gene by examining the Transcript objects in the
     * transcripts collection of the Gene, which would use a query like:
     *   SELECT DISTINCT gene, exon FROM Gene AS gene, Transcript AS transcript, Exon AS exon WHERE
     *   (gene.transcripts CONTAINS transcript AND transcript.exons CONTAINS exon) ORDER BY gene
     * and then set exon.gene
     * BioEntity1 -> BioEntity2 -> BioEntity3   ==>   BioEntitiy1 -> BioEntity3
     * @param sourceClass the first class in the query
     * @param sourceClassFieldName the field in the sourceClass which should contain the
     * connectingClass
     * @param connectingClass the class referred to by sourceClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * destinationClass
     * @param destinationClass the class referred to by
     * connectingClass.connectingClassFieldName
     * @param destinationFieldName the collection field in the destinationClass - the
     * collection to create/set
     * @throws Exception if anything goes wrong
     */
    protected void insertReferences(Class sourceClass, String sourceClassFieldName,
                                    Class connectingClass, String connectingClassFieldName,
                                    Class destinationClass, String destinationFieldName)
        throws Exception {
        InterMineObject lastObject = null;
        List newCollection = new ArrayList();

        LOG.info("Beginning insertReferences("
                 + sourceClass.getName() + ", "
                 + sourceClassFieldName + ", "
                 + connectingClass.getName() + ", "
                 + connectingClassFieldName + ","
                 + destinationClass.getName() + ", "
                 + destinationFieldName + ")");

        Iterator resIter =
            PostProcessUtil.findRelations(osw.getObjectStore(),
                                          sourceClass, sourceClassFieldName,
                                          connectingClass, connectingClassFieldName,
                                          destinationClass);

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
                TypeUtil.setFieldValue(tempObject, destinationFieldName, thisSourceObject);
                count++;
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + thisDestObject.getId()
                          + " has no " + destinationFieldName + " field");
            }
        }

        LOG.info("Created " + count + " references in " + destinationClass.getName() + " to "
                 + sourceClass.getName() + " via " + connectingClass.getName());
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(destinationClass.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }



    /**
     * Create specific named collection of a particulay Relation type.
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
        List newCollection = new ArrayList();
        Iterator resIter = PostProcessUtil.findRelations(osw.getObjectStore(), thisClass,
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

                    try {
                        TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
                        count += newCollection.size();

                        osw.store(tempObject);
                    } catch (IllegalAccessException e) {
                        LOG.error("Object with ID: " + tempObject.getId()
                                  + " has no " + newCollectionName + " field");
                    }
                }

                newCollection = new ArrayList();
            }

            newCollection.add(collectionObject);

            lastObject = thisObject;
        }

        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);
            try {
                TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
                count += newCollection.size();
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + tempObject.getId()
                          + " has no " + newCollectionName + " field");
            }
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
        List newCollection = new ArrayList();
        Iterator resIter = PostProcessUtil.findProperties(osw.getObjectStore(), entityClass,
                                                         propertyClass);
        // results will be:  entityClass ; properyClass (ordered by entityClass)
        osw.beginTransaction();

        int count = 0;

        while (resIter.hasNext()) {
            LOG.info("next results....");
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisObject = (InterMineObject) rr.get(0);
            InterMineObject thisProperty = (InterMineObject) rr.get(1);

            if (lastObject == null || !thisObject.getId().equals(lastObject.getId())) {
                if (lastObject != null) {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

                    try {
                        TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
                        count += newCollection.size();

                        osw.store(tempObject);
                    } catch (IllegalAccessException e) {
                        LOG.error("Object with ID: " + tempObject.getId()
                                  + " has no " + newCollectionName + " field");
                    }
                }

                newCollection = new ArrayList();
                LOG.info("newCollection: " + newCollection);
            }

            newCollection.add(thisProperty);

            lastObject = thisObject;
        }

        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);
            try {
                LOG.info("set " + newCollectionName + " to " + newCollection);
                TypeUtil.setFieldValue(tempObject, newCollectionName, newCollection);
                count += newCollection.size();
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + tempObject.getId()
                          + " has no " + newCollectionName + " field");
            }
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
     * Copy all GO annotations from the Protein objects to the corresponding Gene(s)
     * @throws Exception if anything goes wrong
     */
    protected void insertGeneAnnotationReferences()
        throws Exception {
        LOG.info("Beginning insertGeneAnnotationReferences()");

        osw.beginTransaction();

        Iterator resIter = findProteinProperties();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Gene thisGene = (Gene) rr.get(0);
            Protein thisProtein = (Protein) rr.get(1);
            Annotation thisAnnotation = (Annotation) rr.get(2);

            Annotation tempAnnotation =
                (Annotation) PostProcessUtil.cloneInterMineObject(thisAnnotation);
            tempAnnotation.setSubject(thisGene);
            osw.store(tempAnnotation);

            count++;
        }

        LOG.info("Created " + count + " new Annotations on Genes");
        osw.commitTransaction();

        // now ANALYSE tables relation to class that has been altered - may be rows added
        // to indirection tables
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            ClassDescriptor cld = model.getClassDescriptorByName(Annotation.class.getName());
            DatabaseUtil.analyse(((ObjectStoreWriterInterMineImpl) osw).getDatabase(), cld, false);
        }
    }

    /**
     * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene, Protein and
     * GOTerm.
     */
    private Iterator findProteinProperties() {
        Query q = new Query();

        q.setDistinct(false);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);

        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);
        q.addToSelect(qcProtein);

        QueryClass qcAnnotation = new QueryClass(Annotation.class);
        q.addFrom(qcAnnotation);
        q.addToSelect(qcAnnotation);

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

        QueryObjectReference annPropertyRef =
            new QueryObjectReference(qcAnnotation, "property");
        ContainsConstraint annPropertyConstraint =
            new ContainsConstraint(annPropertyRef, ConstraintOp.CONTAINS, qcGOTerm);
        cs.addConstraint(annPropertyConstraint);

        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(10000);

        return res.iterator();
    }
}
