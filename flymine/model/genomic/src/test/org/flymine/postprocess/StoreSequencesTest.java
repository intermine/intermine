package org.flymine.postprocess;

import junit.framework.TestCase;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.intermine.metadata.Model;
import java.sql.*;
import java.util.Iterator;
import java.util.Collections;

import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.DynamicUtil;
import org.intermine.model.InterMineObject;


import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

public class StoreSequencesTest extends TestCase {

    private ObjectStoreWriter osw;
    private String ensemblDb = "db.ensembl-human";

    private static final Logger LOG = Logger.getLogger(StoreSequencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        storeContigs();
    }

    public void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(),
                                                    osw.getObjectStore().getSequence());
        Iterator resIter = res.iterator();
        //osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        //osw.commitTransaction();
        osw.close();
    }

    public void testGetSequence() throws Exception{
        StoreSequences ss = new StoreSequences(osw, ensemblDb);
        String seq = ss.getSequence("CR381709.1.2001.2054");
        String expectedSequence = "TTCCTAGGAGGTTCTAATCAATGCAACTATAGGTATTTTCTGCCAAGGTCTAGC";
        assertEquals(expectedSequence, seq);

    }

    public void testStoreContigSequences() throws Exception {
        storeContigs();
        StoreSequences ss = new StoreSequences(osw, ensemblDb);
        ss.storeContigSequences();

        Query q1 = new Query();
        QueryClass qc = new QueryClass(Contig.class);
        q1.addToSelect(qc);
        q1.addFrom(qc);
        QueryField qf = new QueryField(qc, "identifier");
        SimpleConstraint sc1 = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                               new QueryValue("CR381709.1.2001.2054"));
        q1.setConstraint(sc1);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res1 = new SingletonResults(q1, os, os.getSequence());
        Contig con1 = (Contig) res1.get(0);
        String seq1 =  con1.getSequence().getResidues();
        String expectedSequence = "TTCCTAGGAGGTTCTAATCAATGCAACTATAGGTATTTTCTGCCAAGGTCTAGC";
        assertEquals(expectedSequence, seq1);

        Query q2 = new Query();
        q2.addToSelect(qc);
        q2.addFrom(qc);
        SimpleConstraint sc2 = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                               new QueryValue("AADD01209098.1.15791.15883"));
        q2.setConstraint(sc2);
        SingletonResults res2 = new SingletonResults(q2, os, os.getSequence());
        Contig con2 = (Contig) res2.get(0);
        String seq2 =  con2.getSequence().getResidues();
        expectedSequence = "TAAGTCTCTCAAAAACCCCTGGAAGACTGTATCAAGGGGTTGTTGTTGGTGGCACTGGTGTGATAATGGATCTGATATTCATTGTGATAGCAG";
        assertEquals(expectedSequence, seq2);
    }

    private void storeContigs() throws Exception{
        Contig contig1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        Contig contig2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));

        contig1.setIdentifier("AADD01209098.1.15791.15883");
        contig2.setIdentifier("CR381709.1.2001.2054");
        osw.beginTransaction();
        osw.store(contig1);
        osw.store(contig2);
        osw.commitTransaction();
    }
}

