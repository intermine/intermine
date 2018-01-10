package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;

/**
 * Tests for {@link NCBIFastaLoaderTask}
 * @author Kim Rutherford
 */
public class NCBIFastaLoaderTaskTest extends TestCase {

    private ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(NCBIFastaLoaderTaskTest.class);
    private String dataSetTitle = "ncbi test title";

    @Override
    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
    }

    public void testNcbiFasta() throws Exception {
        executeLoaderTask("org.intermine.model.bio.Chromosome", "test.fa");

        Results r = getResults();

        Map<String, String> actMap = new HashMap<String, String>();

        for (Object rr: r) {
            Chromosome chromosome = (Chromosome) ((ResultsRow) rr).get(0);
            assertNotNull(chromosome.getPrimaryIdentifier());
            actMap.put(chromosome.getPrimaryIdentifier(), chromosome.getSequence().getResidues().toString());
        }

        Map<String, String> expMap = new HashMap<String, String>();

        expMap.put("1",
                  "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"
                + "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN");

        assertEquals(1, r.size());
        assertEquals(expMap, actMap);
    }

    /**
     * @throws IOException
     */
    private void executeLoaderTask(String className, String fastaFile) throws IOException {
        FastaLoaderTask flt = new NCBIFastaLoaderTask();
        flt.setFastaTaxonId("9606");
        flt.setIgnoreDuplicates(true);
        flt.setClassName(className);
        flt.setClassAttribute("primaryIdentifier");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");
        flt.setDataSetTitle(dataSetTitle);
        flt.setDataSourceName("test-source");

        File fasta = null;
        try {
            fasta = new File(getClass().getClassLoader().getResource(fastaFile).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        File[] files = new File[1];
        files[0] = fasta;
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
        QueryClass chrQueryClass = new QueryClass(Chromosome.class);
        QueryClass seqQueryClass = new QueryClass(Sequence.class);
        q.addToSelect(chrQueryClass);
        q.addToSelect(seqQueryClass);
        q.addFrom(chrQueryClass);
        q.addFrom(seqQueryClass);

        QueryObjectReference qor = new QueryObjectReference(chrQueryClass, "sequence");
        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, seqQueryClass);

        q.setConstraint(cc);

        Results r = os.execute(q);
        return r;
    }

    @Override
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
            System.out.println("deleting: " + o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }

}
