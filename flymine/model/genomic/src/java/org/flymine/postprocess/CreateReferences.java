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

/**
 * Fill in additional references/collections in genomic model
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CreateReferences
{
    protected ObjectStoreWriter osw;

    /**
     * Construct with an ObjectStoreWriter, read and write from same ObjectStore
     * @param osw an ObjectStore to write to
     */
    public CreateReferences(ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Fill in references/collectiosn in model by querying relations
     * @throws Exception if anything goes wrong
     */
    public void insertReferences() throws Exception {
        insertReferences(Chromosome.class, Transcript.class, Relation.class, "transcripts");
        insertReferences(Chromosome.class, Exon.class, Relation.class, "exons");
        insertReferences(Chromosome.class, Gene.class, Relation.class, "genes");
        insertReferences(Chromosome.class, Contig.class, Relation.class, "contigs");
        insertReferences(Chromosome.class, Supercontig.class, Relation.class, "supercontigs");
        insertReferences(Chromosome.class, ChromosomeBand.class, Relation.class, "chromosomeBands");
        
        insertReferences(ChromosomeBand.class, Transcript.class, Relation.class, "transcripts");
        insertReferences(ChromosomeBand.class, Exon.class, Relation.class, "exons");
        insertReferences(ChromosomeBand.class, Gene.class, Relation.class, "genes");
        insertReferences(ChromosomeBand.class, Contig.class, Relation.class, "contigs");
        insertReferences(ChromosomeBand.class, Supercontig.class, Relation.class, "supercontigs");
        
        insertReferences(Supercontig.class, Transcript.class, Relation.class, "transcripts");
        insertReferences(Supercontig.class, Exon.class, Relation.class, "exons");
        insertReferences(Supercontig.class, Gene.class, Relation.class, "genes");
        
        insertReferences(Contig.class, Transcript.class, Relation.class, "transcripts");
        insertReferences(Contig.class, Gene.class, Relation.class, "genes");
        insertReferences(Contig.class, Exon.class, Relation.class, "exons");
        
        insertReferences(Gene.class, Exon.class, Relation.class, "exons");
        insertReferences(Gene.class, Transcript.class, SimpleRelation.class, "transcripts");

        insertReferences(Transcript.class, Exon.class, RankedRelation.class, "exons");
    }

    private void insertReferences(Class objectClass, Class subjectClass,
                                  Class relationClass, String collectionFieldName)
        throws Exception {

        InterMineObject lastObject = null;
        List newCollection = new ArrayList();
        Iterator resIter = PostProcessUtil.findRelations(osw.getObjectStore(), objectClass,
                                                         subjectClass, relationClass);
        // results will be ordered by object
        osw.beginTransaction();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            InterMineObject thisObject = (InterMineObject) rr.get(0);
            InterMineObject thisSubject = (InterMineObject) rr.get(1);

            if (lastObject == null || !thisObject.getId().equals(lastObject.getId())) {
                if (lastObject != null) {
                    // clone so we don't change the ObjectStore cache
                    InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

                    TypeUtil.setFieldValue(tempObject, collectionFieldName, newCollection);

                    osw.store(tempObject);
                }

                newCollection = new ArrayList();
            }

            newCollection.add(thisSubject);

            lastObject = thisObject;
        }
        
        if (lastObject != null) {
            // clone so we don't change the ObjectStore cache
            InterMineObject tempObject = PostProcessUtil.cloneInterMineObject(lastObject);

            TypeUtil.setFieldValue(tempObject, collectionFieldName, newCollection);

            osw.store(tempObject);
        }
        osw.commitTransaction();
    }

}
