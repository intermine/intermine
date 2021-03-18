package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2021 FlyMine
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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.ItemFactory;
import org.intermine.bio.postprocess.CreateChromosomeLocationsProcess;

public class CreateChromosomeLocationsProcessTest extends TestCase
{

    private ObjectStoreWriter osw;
    private Chromosome chromosome = null;
    private Model model;
    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(CreateChromosomeLocationsProcessTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
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
            LOG.info("deleting: " +o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }

    public void testSetChromosomeLocationsAndLengths() throws Exception {
        Chromosome chr1 = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr1.setPrimaryIdentifier("1");
        chr1.setId(new Integer(101));
        Chromosome chr2 = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr1.setPrimaryIdentifier("2");
        chr1.setId(new Integer(102));

        Exon exon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon1.setId(new Integer(107));
        exon1.setLength(new Integer(1000));
        Exon exon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon2.setId(new Integer(108));
        Exon exon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon3.setId(new Integer(109));

        // exon 2 has two chromosome locations, shouldn't get chromosome[Location] references
        Location exon1OnChr = createLocation(chr1, exon1, "1", 51, 100, Location.class);
        exon1OnChr.setId(new Integer(1010));
        Location exon2OnChr = createLocation(chr2, exon2, "1", 201, 250, Location.class);
        exon2OnChr.setId(new Integer(1011));
        Location exon2OnChrDup = createLocation(chr1, exon2, "1", 501, 550, Location.class);
        exon2OnChrDup.setId(new Integer(1012));
        Location exon3OnChr = createLocation(chr2, exon3, "1", 601, 650, Location.class);
        exon3OnChr.setId(new Integer(1013));

        Set<InterMineObject> toStore =
            new HashSet<InterMineObject>(Arrays.asList(new InterMineObject[] {
                    chr1, chr2, exon1, exon2, exon3,
                    exon1OnChr, exon2OnChr, exon2OnChrDup, exon3OnChr
            }));

        for (InterMineObject imo : toStore) {
            osw.store(imo);
        }

        CreateChromosomeLocationsProcess cl = new CreateChromosomeLocationsProcess(osw);
        cl.postProcess();

        ObjectStore os = osw.getObjectStore();
        Exon resExon1 = (Exon) os.getObjectById(new Integer(107));
        Exon resExon2 = (Exon) os.getObjectById(new Integer(108));
        Exon resExon3 = (Exon) os.getObjectById(new Integer(109));

        assertEquals(chr1.getId(), resExon1.getChromosome().getId());
        assertEquals(exon1OnChr.getId(), resExon1.getChromosomeLocation().getId());

        assertNull(resExon2.getChromosome());
        assertNull(resExon2.getChromosomeLocation());

        assertEquals(chr2.getId(), resExon3.getChromosome().getId());
        assertEquals(exon3OnChr.getId(), resExon3.getChromosomeLocation().getId());

        // exon1 has length set so should stay as 1000, exon3 should get length 50 set from location
        assertEquals(new Integer(1000), resExon1.getLength());
        assertEquals(new Integer(50), resExon3.getLength());
        // nothing done to exon2
        assertNull(resExon2.getLength());
    }


    private Location createLocation(BioEntity object, BioEntity subject, String strand,
                                    int start, int end, Class<?> locationClass) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(locationClass));
        loc.setLocatedOn(object);
        loc.setFeature(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStrand(strand);
        return loc;
    }

    private Chromosome getChromosome() {
        if (chromosome == null) {
            chromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
            chromosome.setPrimaryIdentifier("X");
            chromosome.setLength(new Integer(10000));
            chromosome.setId(new Integer(101));
        }
        return chromosome;
    }
}
