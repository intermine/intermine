package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.FivePrimeUTR;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.ThreePrimeUTR;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;

/**
 * Tests for {@link FlyBaseUTRFastaLoaderTask}
 * @author Kim Rutherford
 */
public class FlyBaseUTRFastaLoaderTaskTest extends TestCase {

    private ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(FlyBaseUTRFastaLoaderTaskTest.class);
    private String dataSetTitle = "utr test title";

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
    }

    public void testFasta5PrimeLoad() throws Exception {
        executeLoaderTask("org.intermine.model.bio.FivePrimeUTR",
                          "dmel-all-five_prime_UTR.fasta");

        Results r = getResults();

        boolean seenFBtr0112632 = false;
        boolean seenFBtr0100521 = false;

        for (Object rr: r) {
            FivePrimeUTR utr = (FivePrimeUTR) ((ResultsRow) rr).get(0);
            assertEquals(1, utr.getDataSets().size());
            DataSet dataSet = utr.getDataSets().iterator().next();
            assertEquals(dataSetTitle, dataSet.getName());

            assertNotNull(utr.getChromosomeLocation());
            if ("FBtr0112632-5-prime-utr".equals(utr.getPrimaryIdentifier())) {
                seenFBtr0112632 = true;
                Location loc = utr.getChromosomeLocation();
                assertEquals(10258903, loc.getStart().intValue());
                assertEquals(10307410, loc.getEnd().intValue());
                assertEquals("-1", loc.getStrand());
                assertEquals("3R", loc.getLocatedOn().getPrimaryIdentifier());
                Transcript transcript = utr.getTranscripts().iterator().next();
                assertEquals("FBtr0112632", transcript.getPrimaryIdentifier());
                assertEquals(36329, utr.getOrganism().getTaxonId().intValue());
            } else {
                if ("FBtr0100521-5-prime-utr".equals(utr.getPrimaryIdentifier())) {
                    seenFBtr0100521 = true;
                    Location loc = utr.getChromosomeLocation();
                    assertEquals(18024494, loc.getStart().intValue());
                    assertEquals(18050424, loc.getEnd().intValue());
                    assertEquals("1", loc.getStrand());
                    assertEquals("2R", loc.getLocatedOn().getPrimaryIdentifier());
                    Transcript transcript = utr.getTranscripts().iterator().next();
                    assertEquals("FBtr0100521", transcript.getPrimaryIdentifier());
                    assertEquals(36329, transcript.getOrganism().getTaxonId().intValue());
                }
            }
        }

        if (!seenFBtr0100521) {
            fail("FBtr0100521 5' UTR not seen");
        }
        if (!seenFBtr0112632) {
            fail("FBtr0112632 5' UTR not seen");
        }
        assertEquals(5, r.size());
    }

    public void testFasta3PrimeLoad() throws Exception {
        executeLoaderTask("org.intermine.model.bio.ThreePrimeUTR",
                          "dmel-all-three_prime_UTR.fasta");

        Results r = getResults();

        boolean seenFBtr0071764 = false;
        boolean seenFBtr0082533 = false;

        for (Object rr: r) {
            ThreePrimeUTR utr = (ThreePrimeUTR) ((ResultsRow) rr).get(0);
            assertEquals(1, utr.getDataSets().size());
            DataSet dataSet = utr.getDataSets().iterator().next();
            assertEquals(dataSetTitle, dataSet.getName());

            assertNotNull(utr.getChromosomeLocation());
            if ("FBtr0071764-3-prime-utr".equals(utr.getPrimaryIdentifier())) {
                seenFBtr0071764 = true;
                Location loc = utr.getChromosomeLocation();
                assertEquals(18060033, loc.getStart().intValue());
                assertEquals(18060346, loc.getEnd().intValue());
                assertEquals("2R", loc.getLocatedOn().getPrimaryIdentifier());
                Transcript transcript = utr.getTranscripts().iterator().next();
                assertEquals("FBtr0071764", transcript.getPrimaryIdentifier());
                assertEquals(36329, utr.getOrganism().getTaxonId().intValue());
            } else {
                if ("FBtr0082533-3-prime-utr".equals(utr.getPrimaryIdentifier())) {
                    seenFBtr0082533 = true;
                    Location loc = utr.getChromosomeLocation();
                    assertEquals(7594335, loc.getStart().intValue());
                    assertEquals(7595561, loc.getEnd().intValue());
                    assertEquals("3R", loc.getLocatedOn().getPrimaryIdentifier());
                    Transcript transcript = utr.getTranscripts().iterator().next();
                    assertEquals("FBtr0082533", transcript.getPrimaryIdentifier());
                    assertEquals(36329, transcript.getOrganism().getTaxonId().intValue());
                }
            }
        }

        if (!seenFBtr0071764) {
            fail("FBtr0071764 3' UTR not seen");
        }
        if (!seenFBtr0082533) {
            fail("FBtr0082533 3' UTR not seen");
        }
        assertEquals(2, r.size());
    }

    /**
     * @throws IOException
     */
    private void executeLoaderTask(String className, String utrFastaFile) throws IOException {
        FastaLoaderTask flt = new FlyBaseUTRFastaLoaderTask();
        flt.setFastaTaxonId("36329");
        flt.setIgnoreDuplicates(true);
        flt.setClassName(className);
        flt.setClassAttribute("primaryIdentifier");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");
        flt.setDataSetTitle(dataSetTitle);
        flt.setDataSourceName("test-source");

        File tmpFile = File.createTempFile("FlyBaseUTRFastaLoaderTaskTest", "tmp");
        FileWriter fw = new FileWriter(tmpFile);

        InputStream is =
            getClass().getClassLoader().getResourceAsStream(utrFastaFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }

        fw.close();
        tmpFile.deleteOnExit();

        File[] files = new File[1];
        files[0] = tmpFile;
        flt.setFileArray(files);
        flt.execute();
    }

    /**
     * @return
     */
    private Results getResults() {
        //Check the results to see if we have some data...
        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass lsfQueryClass = new QueryClass(SequenceFeature.class);
        QueryClass seqQueryClass = new QueryClass(Sequence.class);
        q.addToSelect(lsfQueryClass);
        q.addToSelect(seqQueryClass);
        q.addFrom(lsfQueryClass);
        q.addFrom(seqQueryClass);

        QueryObjectReference qor = new QueryObjectReference(lsfQueryClass, "sequence");
        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, seqQueryClass);

        q.setConstraint(cc);

        Results r = os.execute(q);
        return r;
    }

    public void tearDown() throws Exception {
        LOG.info("in tear down");
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        LOG.info("created results");
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.info("deleting: " + o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }

}
