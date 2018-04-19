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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.FivePrimeUTR;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.MRNA;
import org.intermine.model.bio.ThreePrimeUTR;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
/**
 * Tests for the CreateUTReferencesProcess class.
 */
public class CreateUTRReferencesProcessTest extends TestCase {

    private ObjectStoreWriter osw;
    private Chromosome storedChromosome = null;
    private Gene storedGene1 = null;
    private Gene storedGene2 = null;
    private MRNA storedTranscript1 = null;
    private MRNA storedTranscript2 = null;
    private Exon storedExon1 = null;
    private Exon storedExon2 = null;
    private Exon storedExon3 = null;
    private Location storedGeneLocation1 = null;
    private Location storedGeneLocation2 = null;
    private ThreePrimeUTR storedThreePrimeUTR = null;
    private FivePrimeUTR storedFivePrimeUTR = null;

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        createData();
    }

    public void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();
    }

    public void testCreateUtrRefs() throws Exception {
        CreateUTRReferencesProcess cr = new CreateUTRReferencesProcess(osw);
        cr.postProcess();

        Query q = new Query();
        QueryClass qcMRNA = new QueryClass(osw.getModel().getClassDescriptorByName("MRNA").getType());
        q.addFrom(qcMRNA);
        q.addToSelect(qcMRNA);
        QueryField qfPrimaryIdentifier = new QueryField(qcMRNA, "primaryIdentifier");
        SimpleConstraint sc = new SimpleConstraint(qfPrimaryIdentifier, ConstraintOp.EQUALS, new QueryValue("transcript1"));
        q.setConstraint(sc);

        ObjectStore os = osw.getObjectStore();
        Results res = os.execute(q);
        ResultsRow row = (ResultsRow) res.iterator().next();

        MRNA resMRNA = (MRNA) row.get(0);

        assertEquals(storedThreePrimeUTR.getId(), resMRNA.getThreePrimeUTR().getId());
        assertEquals(storedFivePrimeUTR.getId(), resMRNA.getFivePrimeUTR().getId());
    }


    private void createData() throws Exception {
        osw.flushObjectById();

        storedChromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setPrimaryIdentifier("chr1");

        storedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene1.setPrimaryIdentifier("gene1");

        storedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene2.setPrimaryIdentifier("gene2");

        storedGeneLocation1 =
                (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation1.setLocatedOn(storedChromosome);
        storedGeneLocation1.setFeature(storedGene1);

        storedGeneLocation2 =
                (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation2.setLocatedOn(storedChromosome);
        storedGeneLocation2.setFeature(storedGene2);

        storedTranscript1 =
                (MRNA) DynamicUtil.createObject(Collections.singleton(MRNA.class));
        storedTranscript1.setPrimaryIdentifier("transcript1");
        storedTranscript1.setGene(storedGene1);

        storedTranscript2 =
                (MRNA) DynamicUtil.createObject(Collections.singleton(MRNA.class));
        storedTranscript2.setPrimaryIdentifier("transcript2");
        storedTranscript2.setGene(storedGene1);

        storedExon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon1.setPrimaryIdentifier("exon1");

        storedExon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon2.setPrimaryIdentifier("exon2");

        storedExon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon3.setPrimaryIdentifier("exon3");

        storedTranscript1.addExons(storedExon1);
        storedTranscript1.addExons(storedExon2);
        storedTranscript2.addExons(storedExon3);

        storedThreePrimeUTR =
                (ThreePrimeUTR) DynamicUtil.createObject(Collections.singleton(ThreePrimeUTR.class));
        storedThreePrimeUTR.setPrimaryIdentifier("utr1-threePrimeUTR");
        storedThreePrimeUTR.addTranscripts(storedTranscript1);

        storedFivePrimeUTR =
                (FivePrimeUTR) DynamicUtil.createObject(Collections.singleton(FivePrimeUTR.class));
        storedFivePrimeUTR.setPrimaryIdentifier("utr2-fivePrimeUTR");
        storedFivePrimeUTR.addTranscripts(storedTranscript1);

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                storedChromosome,
                storedGene1, storedGene2,
                storedGeneLocation1, storedGeneLocation2,
                storedTranscript1, storedTranscript2,
                storedExon1, storedExon2, storedExon3,
                storedThreePrimeUTR, storedFivePrimeUTR
        }));

        Iterator i = toStore.iterator();
        osw.beginTransaction();
        while (i.hasNext()) {
            InterMineObject object = (InterMineObject) i.next();
            osw.store(object);
        }

        osw.commitTransaction();
    }
}
