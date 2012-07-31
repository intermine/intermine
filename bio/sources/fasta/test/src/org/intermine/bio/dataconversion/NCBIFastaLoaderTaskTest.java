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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;
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
 * Tests for {@link NCBIFastaLoaderTask}
 * @author Kim Rutherford
 */
public class NCBIFastaLoaderTaskTest extends TestCase {

    private ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(NCBIFastaLoaderTaskTest.class);
    private String dataSetTitle = "cds test title";

    @Override
    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
    }

    public void testNcbiFasta() throws Exception {
        executeLoaderTask("org.intermine.model.bio.Protein",
                          "ncbi_test.fasta");

        Results r = getResults();

        Map<String, String> actMap = new HashMap<String, String>();

        for (Object rr: r) {
            Protein protein = (Protein) ((ResultsRow) rr).get(0);
            assertNotNull(protein.getPrimaryIdentifier());
            actMap.put(protein.getPrimaryIdentifier(), protein.getSequence().getResidues().toString());
        }

        Map<String, String> expMap = new HashMap<String, String>();

        expMap.put("NP_047184",
                  "MKSRKCYIHNPNPIFTPPKNNKRRPSFICYAMKKAKEIDVARCELNYFLQPKNIKTGLHLKRFRQLNKHRASAMRAMVLA"
                + "MLYHFNISSELVQASVEQLSDECGLSTISQSGNKSITRASRLITNFMEPMGFVNCEEIWDKILGNYMPKMIILTPLFFML"
                + "LDISEKKLMNAKQQQLGWINKNLISKGLKPITMLDAKRRSKDIRMKSIFKYRMSRHAFYKKKKNAQRLIALDEKEARQTI"
                + "LRALVTKYTLSELTKLGPSGLKKQVNISYHYLRKIATNMY");

        expMap.put("AAD44166",
                   "LCLYTHIGRNIYYGSYLYSETWNTGIMLLLITMATAFMGYVLPWGQMSFWGATVITNLFSAIPYIGTNLV"
                   + "EWIWGGFSVDKATLNRFFAFHFILPFTMVALAGVHLTFLHETGSNNPLGLTSDSDKIPFHPYYTIKDFLG"
                   + "LLILILLLLLLALLSPDMLGDPDNHMPADPLNTPLHIKPEWYFLFAYAILRSVPNKLGGVLALFLSIVIL"
                   + "GLMPFLHTSKHRSMMLRPLSQALFWTLTMDLLTLTWIGSQPVEYPYTIIGQMASILYFSIILAFLPIAGX"
                   + "IENY");

        expMap.put("YP_203325",
                "MLYNPLEQFTVNKIISLYTVYYSMSLTNSSLYFIIAAIISFFIFKYSANIPYVSLINKNNYSILTESLYK"
                + "TILKMVKEQIGDKYTIYMPLIFSLFIIILVSNLVGLIPYGFSPTALFALPLGLSVTIIISVTVIGFVKYH"
                + "LKYFSVLLPSGTPLGLVPLLLVVELLSYIARAFSLGIRLAANITSGHILLNIISGFLFKTSGIALLFVII"
                + "PFTLFIALTGLELIVAILQAYVWSILTCIYIKDSLILH");

        expMap.put("ZEN1_DROME",
                "MSSVMHYYPVHQAKVGSYSADPSEVKYSDLIYGHHHDVNPIGLPPNYNQMNSNPTTLNDH"
                + "CSPQHVHQQHVSSDENLPSQPNHDSQRVKLKRSRTAFTSVQLVELENEFKSNMYLYRTRR"
                + "IEIAQRLSLCERQVKIWFQNRRMKFKKDIQGHREPKSNAKLAQPQAEQSAHRGIVKRLMS"
                + "YSQDPREGTAAAEKRPMMAVAPVNPKPDYQASQKMKTEASTNNGMCSSADLSEILEHLAQ"
                + "TTAAPQVSTATSSTGTSTNSASSSSSGHYSYNVDLVLQSIKQDLEAAAQAWSKSKSAPIL"
                + "ATQSWHPSSQSQVPTSVHAAPSMNLSWGEPAAKSRKLSVNHMNPCVTSYNYPN");

        assertEquals(4, r.size());
        assertEquals(expMap, actMap);
    }

    /**
     * @throws IOException
     */
    private void executeLoaderTask(String className, String utrFastaFile) throws IOException {
        FastaLoaderTask flt = new NCBIFastaLoaderTask();
        flt.setFastaTaxonId("36329");
        flt.setIgnoreDuplicates(true);
        flt.setClassName(className);
        flt.setClassAttribute("primaryIdentifier");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");
        flt.setSequenceType("protein");
        flt.setDataSetTitle(dataSetTitle);
        flt.setDataSourceName("test-source");

        File tmpFile = File.createTempFile("NCBIFastaLoaderTaskTest", "tmp");
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
        QueryClass proteinQueryClass = new QueryClass(Protein.class);
        QueryClass seqQueryClass = new QueryClass(Sequence.class);
        q.addToSelect(proteinQueryClass);
        q.addToSelect(seqQueryClass);
        q.addFrom(proteinQueryClass);
        q.addFrom(seqQueryClass);

        QueryObjectReference qor = new QueryObjectReference(proteinQueryClass, "sequence");
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
            LOG.info("deleting: " + o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }

}
