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

import junit.framework.TestCase;

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.dataloader.XmlDataLoader;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.datatracking.Source;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;

import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

/**
 * Tests for the TransferSequences class
 */
public class TransferSequencesTest extends TestCase
{
    private ObjectStoreWriter osw;
    private Contig [] storedContigs;
    private Exon [] storedExons;
    private Location [] storedExonContigLocations;
    private Model model;

    private static final Logger LOG = Logger.getLogger(TransferSequencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        model = Model.getInstanceByName("genomic");
    }

    public void tearDown() throws Exception {
        LOG.error("in tear down");
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        LOG.error("created results");
        Iterator resIter = res.iterator();
        //osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.error("deleting: " +o.getId());
            osw.delete(o);
        }
        //osw.commitTransaction();
        LOG.error("committed transaction");
        osw.close();
        LOG.error("closed objectstore");
    }

    public void testExonSequence() throws Exception {

    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedContigs[0] =
            (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        storedContigs[0].setName("contig0");
        storedContigs[0].setLength(new Integer(1000));

        storedContigs[1] =
            (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        storedContigs[0].setName("contig1");
        storedContigs[0].setLength(new Integer(1500));

        storedExons = new Exon [6];
        for (int i = 0 ; i < 6 ; i++) {
            storedExons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            storedExons[i].setName("exon" + i);
        }

        storedExonContigLocations[0] =
            createLocation(storedContigs[0], storedExons[0], 1, 120, 140);

//         storedExon2ContigLocation =
//             (Location) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
//         storedExon2ContigLocation.setObject(storedContig);
//         storedExon2ContigLocation.setSubject(storedExon2);

//         Set toStore = new HashSet(Arrays.asList(new Object[] {
//                                                     storedContig, storedExon1, storedExon2,
//                                                     storedExon1ContigLocation,
//                                                     storedExon2ContigLocation,
//                                                 }));
//         Iterator i = toStore.iterator();
//         osw.beginTransaction();
//         while (i.hasNext()) {
//             osw.store((InterMineObject) i.next());
//         }
//         osw.commitTransaction();
    }

    private Location createLocation(BioEntity object, BioEntity subject,
                                    int strand, int start, int end) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        loc.setObject(object);
        loc.setSubject(subject);
        loc.setStrand(new Integer(strand));
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStartIsPartial(Boolean.FALSE);
        loc.setEndIsPartial(Boolean.FALSE);
        loc.setStrand(new Integer(strand));
        return loc;
    }
}
