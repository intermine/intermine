package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.xml.full.ItemFactory;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Sequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

public class FastaLoaderTaskTest extends TestCase {

    private ObjectStoreWriter osw;
    private Model model;
    private static final Logger LOG = Logger.getLogger(FastaLoaderTaskTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
    }

    public void testFastaLoad() throws Exception {
        FastaLoaderTask flt = new FastaLoaderTask();
        flt.setFastaTaxonId(new Integer(36329));
        flt.setIgnoreDuplicates(true);
        flt.setClassName("org.flymine.model.genomic.Gene");
        flt.setClassAttribute("organismDbId");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");

        File[] files = new File[2];
        for (int i = 0; i < 2; i++) {
            files[i] = File.createTempFile("MAL1_trimed.fasta_" + i, "tmp");
            FileWriter fw = new FileWriter(files[i]);
            InputStream is =
                getClass().getClassLoader().getResourceAsStream("MAL" + (i + 1) + "_trimed.fasta");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while ((line = br.readLine()) != null) {
                fw.write(line + "\n");
            }

            fw.close();
            files[i].deleteOnExit();
        }
        flt.setFileArray(files);
        flt.execute();
        //Check the results to see if we have some data...
        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass lsfQueryClass = new QueryClass(LocatedSequenceFeature.class);
        QueryClass seqQueryClass = new QueryClass(Sequence.class);
        q.addToSelect(lsfQueryClass);
        q.addToSelect(seqQueryClass);
        q.addFrom(lsfQueryClass);
        q.addFrom(seqQueryClass);

        QueryObjectReference qor = new QueryObjectReference(lsfQueryClass, "sequence");
        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, seqQueryClass);

        q.setConstraint(cc);

        Results r = os.execute(q);

        assertEquals(2, r.size());
    }

    public void testProteinFastaLoad() throws Exception {
        FastaLoaderTask flt = new FastaLoaderTask();
        flt.setFastaTaxonId(new Integer(36329));
        flt.setIgnoreDuplicates(true);
        flt.setSequenceType("protein");
        flt.setClassName("org.flymine.model.genomic.Protein");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");

        File[] files = new File[1];
        files[0] = File.createTempFile("pombe_sid2_short.fasta", "tmp");
        FileWriter fw = new FileWriter(files[0]);
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("pombe_sid2_short.fasta");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }

        fw.close();
        files[0].deleteOnExit();
        flt.setFileArray(files);
        flt.execute();

        //Check the results to see if we have some data...
        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass queryClass = new QueryClass(Protein.class);
        QueryClass seqQueryClass = new QueryClass(Sequence.class);
        q.addToSelect(queryClass);
        q.addToSelect(seqQueryClass);
        q.addFrom(queryClass);
        q.addFrom(seqQueryClass);

        QueryObjectReference qor = new QueryObjectReference(queryClass, "sequence");
        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, seqQueryClass);

        q.setConstraint(cc);

        Results r = os.execute(q);

        assertEquals(1, r.size());

        Protein protein = (Protein) ((List) r.get(0)).get(0);

        assertEquals("MNRVNDMSPVEGDLGLQLSSEADKKFDAYMKRHGLFEPGNLSNNDKERNLEDQFNSMKLS"
                     + "PVASSKENYPDNHMHSKHISKLPIASPIPRGLDRSGELSYKDNNHWSDRSSTGSPRWENG"
                     + "SMNLSVEEMEKVVQPKVKRMATICQM", protein.getSequence().getResidues());
        // TODO FIXME XXX - uncomment when Protein has a length field
        //        assertEquals(new Integer(146), protein.getLength());
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
        SingletonResults res
                = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore().getSequence());
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
