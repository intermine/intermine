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
import org.flymine.model.genomic.*;

/**
 * Fill in additional references/collections in genomic model
 *
 * @author Richard Smith
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
        // Gene.transcripts

        Gene newGene = null;
        List transcripts = null;
        Iterator resIter = PostProcessUtil.findRelations(osw.getObjectStore(), Gene.class,
                                                         Transcript.class, SimpleRelation.class);
        // select will be ordered by gene
        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Gene tmpGene = (Gene) rr.get(0);
            if (tmpGene.getId().equals(newGene.getId())) {
                // add transcript to collection
                transcripts.add((Transcript) rr.get(1));
            } else {
                // store previous gene with update collection
                newGene.setTranscripts(transcripts);
                osw.store(newGene);

                newGene = (Gene) PostProcessUtil.cloneInterMineObject((InterMineObject) rr.get(0));
                transcripts = new ArrayList(newGene.getTranscripts());
            }
        }
        osw.abortTransaction();

        // Transcript.exons

    }

}
