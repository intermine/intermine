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

import org.intermine.model.InterMineObject;
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

    /**
     * Construct with an ObjectStoreWriter, read and write from same ObjectStore
     * @param osw an ObjectStore to write to
     */
    public CreateReferences(ObjectStoreWriter osw) {
        this.osw = osw;
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
        insertReferences(Contig.class, "subjects", Location.class, "subject",
                         Exon.class, "contig");
        insertReferences(Gene.class, "subjects", SimpleRelation.class, "subject",
                         Transcript.class, "gene");
        insertReferences(Gene.class, "transcripts", Transcript.class, "exons",
                         Exon.class, "gene");
        insertReferences(Chromosome.class, "exons", Exon.class, "gene",
                         Gene.class, "chromosome");
        insertReferences(Chromosome.class, "exons", Exon.class, "transcripts",
                         Transcript.class, "chromosome");
    }

    /**
     * Fill in missing references/collectiosn in model by querying relations
     * @param objectClass the class of the objects to which the collection will be added
     * @param subjectClass the class to the objects to add to the new collection
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
        // results will be ordered by object
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
        LOG.debug("Created " + count + " references in " + objectClass.getName() + " to "
                  + subjectClass.getName() + " via the " + collectionFieldName + " field");
        osw.commitTransaction();
    }

    /**
     * Add a collection of objects of type X to objects of type Y by using a connecting class.
     * Eg. Add a collection of Exon objects to Gene by examining the Transcript objects in the
     * transcripts collection of the Genem, which would use a query like:
     *   SELECT DISTINCT gene, exon FROM Gene AS gene, Transcript AS transcript, Exon AS exon WHERE
     *   (gene.transcripts CONTAINS transcript AND transcript.exons CONTAINS exon) ORDER BY gene
     * and then set exon.gene
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

        // results will be ordered by object
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
                LOG.info("stored: " + tempObject.getId());
                osw.store(tempObject);
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + thisDestObject.getId()
                          + " has no " + destinationFieldName + " field");
            }
        }

        LOG.info("Created " + count + " references in " + destinationClass.getName() + " to "
                  + sourceClass.getName() + " via " + connectingClass.getName());
        osw.commitTransaction();
    }
}
