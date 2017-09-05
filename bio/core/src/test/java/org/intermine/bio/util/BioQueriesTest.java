package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CDS;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SOTerm;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the BioQueries class.
 *
 * @author Julie
 */
public class BioQueriesTest extends TestCase
{
    private ObjectStoreWriter osw;
    private Chromosome storedChromosome = null;
    private Gene storedGene1 = null;
    private Gene storedGene2 = null;
    private Location storedLocation1 = null;
    private Location storedLocation2 = null;


    public void testFindLocationAndObjects() throws Exception {

        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        createData();

        Class<?> geneCls = null;
        try {
            geneCls = Class.forName("org.intermine.model.bio.Gene");
        } catch (ClassNotFoundException e) {
            fail("Unexpected ClassNotFoundException. The model is wrong.");
        }

        Class<?> chromosomeCls = null;
        try {
            chromosomeCls = Class.forName("org.intermine.model.bio.Chromosome");
        } catch (ClassNotFoundException e) {
            fail("Unexpected ClassNotFoundException. The model is wrong.");
        }

        Results res = null;
        try {
            res = BioQueries.findLocationAndObjects(osw.getObjectStore(), chromosomeCls, geneCls, false, false, false, 2);
        } catch (ObjectStoreException e) {
            fail("Unexpected ObjectStoreException. The model is wrong.");
        }
        Iterator<Object> resIter = res.iterator();
        HashSet<Integer> actualLocationIds = new HashSet();
        HashSet<Integer> actualGeneIds = new HashSet();
        Integer objectId = null;
        while (resIter.hasNext()) {
            ResultsRow row = (ResultsRow) resIter.next();
            objectId = (Integer) row.get(0);
            Gene resGene = (Gene) row.get(1);
            actualGeneIds.add(resGene.getId());
            Location resLocation = (Location) row.get(2);
            actualLocationIds.add(resLocation.getId());
        }
        HashSet<Integer> expectedLocationIds = new HashSet(Arrays.asList(new Integer[] {storedLocation1.getId(), storedLocation2.getId()}));
        assertEquals(expectedLocationIds, actualLocationIds);

        HashSet<Integer> expectedGeneIds = new HashSet(Arrays.asList(new Integer[] {storedGene1.getId(), storedGene2.getId()}));
        assertEquals(expectedGeneIds, actualGeneIds);

        assertTrue(objectId == 1001);

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

    private void createData() throws Exception {
        osw.flushObjectById();

        Set toStore = new HashSet();

        storedChromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setPrimaryIdentifier("X");
        storedChromosome.setId(new Integer(1001));
        toStore.add(storedChromosome);

        storedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene1.setPrimaryIdentifier("gene1");
        storedGene1.setId(new Integer(2001));
        toStore.add(storedGene1);

        storedLocation1 = createLocation(storedChromosome, storedGene1, "1", 1, 2001, 3001);
        toStore.add(storedLocation1);

        storedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene2.setPrimaryIdentifier("gene2");
        storedGene2.setId(new Integer(2002));
        toStore.add(storedGene2);

        storedLocation2 = createLocation(storedChromosome, storedGene2, "0", 2, 2002, 3002);
        toStore.add(storedLocation2);

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }
    }

    private static Location createLocation(Chromosome object, Gene subject, String strand, int start, int end, int id) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        loc.setLocatedOn(object);
        loc.setFeature(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setId(id);
        return loc;
    }
}
