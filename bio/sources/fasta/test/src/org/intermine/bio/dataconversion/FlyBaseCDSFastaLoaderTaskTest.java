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

import java.util.Iterator;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.intermine.model.bio.CDS;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * Tests for {@link FlyBaseCDSFastaLoaderTask}
 * @author Kim Rutherford
 */
public class FlyBaseCDSFastaLoaderTaskTest extends TestCase {

    private ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(FlyBaseCDSFastaLoaderTaskTest.class);
    private String dataSetTitle = "cds test title";

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
    }

    public void testFastaCDSLoad() throws Exception {
        executeLoaderTask("org.intermine.model.bio.CDS",
                          "dmel-all-CDS.fasta");

        Results r = getResults();

        boolean seenCG6844PB = false;
        boolean seenCG4027PB = false;

        for (Object rr: r) {
            CDS cds = (CDS) ((ResultsRow) rr).get(0);
            assertNotNull(cds.getChromosomeLocation());
            DataSet dataSet = cds.getDataSets().iterator().next();
            assertEquals(dataSetTitle, dataSet.getName());
            if ("CG4027-PB_CDS".equals(cds.getPrimaryIdentifier())) {
                seenCG4027PB = true;
                Location loc = cds.getChromosomeLocation();
                assertEquals(5796731, loc.getStart().intValue());
                assertEquals(5797861, loc.getEnd().intValue());
                assertEquals("1", loc.getStrand());
                assertEquals("X", loc.getLocatedOn().getPrimaryIdentifier());
                assertEquals("FBtr0070823", cds.getTranscript().getPrimaryIdentifier());
                assertEquals(36329, cds.getOrganism().getTaxonId().intValue());
            } else {
                if ("CG6844-PB_CDS".equals(cds.getPrimaryIdentifier())) {
                    seenCG6844PB = true;
                    Location loc = cds.getChromosomeLocation();
                    assertEquals(20311671, loc.getStart().intValue());
                    assertEquals(20316734, loc.getEnd().intValue());
                    assertEquals("1", loc.getStrand());
                    assertEquals("3R", loc.getLocatedOn().getPrimaryIdentifier());
                    assertEquals("FBtr0084640", cds.getTranscript().getPrimaryIdentifier());
                    assertEquals(36329, cds.getOrganism().getTaxonId().intValue());
                }
            }
        }

        if (!seenCG6844PB) {
            fail("CG6844-PB CDS not seen");
        }
        if (!seenCG4027PB) {
            fail("CG4027-PB CDS not seen");
        }
        assertEquals(3, r.size());
    }

    /**
     * @throws IOException
     */
    private void executeLoaderTask(String className, String cdsFastaFile) throws IOException {
        FastaLoaderTask flt = new FlyBaseCDSFastaLoaderTask();
        flt.setFastaTaxonId("36329");
        flt.setIgnoreDuplicates(true);
        flt.setClassName(className);
        flt.setClassAttribute("primaryIdentifier");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");
        flt.setDataSetTitle(dataSetTitle);
        flt.setDataSourceName("test-source");

        File tmpFile = File.createTempFile("FlyBaseCDSFastaLoaderTaskTest", "tmp");
        FileWriter fw = new FileWriter(tmpFile);

        InputStream is =
            getClass().getClassLoader().getResourceAsStream(cdsFastaFile);
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
