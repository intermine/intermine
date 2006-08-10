package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.io.File;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.*;
import org.intermine.xml.full.ItemFactory;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Sequence;

public class FastaLoaderTaskTest extends TestCase {

    private ObjectStoreWriter osw;
    private Model model;
    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(FastaLoaderTaskTest.class);

    public void setUp() throws Exception {

        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
    }

    public void testFastaLoad() throws Exception {

        FastaLoaderTask flt = new FastaLoaderTask();
        flt.setFastaTaxonId(new Integer(36329));
        flt.setIgnoreDuplicates(true);
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");

        File[] files = new File[2];
        files[0] = new File("resources/MAL1_trimed.fasta");
        files[1] = new File("resources/MAL2_trimed.fasta");
        flt.setFileArray(files);

        if (files[0].exists()) { LOG.info("File One Exists!"); }
        if (files[1].exists()) { LOG.info("File Two Exists!"); }

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
