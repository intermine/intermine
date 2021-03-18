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

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.ReversePrimer;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.ItemFactory;

public class MakeSpanningLocationsProcessTest extends TestCase
{

    private ObjectStoreWriter osw;
    private Chromosome chromosome = null;
    private Model model;
    private ItemFactory itemFactory;

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
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

    private void createOverlapTestData() throws Exception {
        Chromosome chr =
                (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(1000));
        chr.setId(new Integer(101));

        Set<InterMineObject> toStore = new HashSet<InterMineObject>();

        toStore.add(chr);

        int [][] exonInfo = {
                { 1000, 1, 1 },
                { 1001, 2, 10 },
                { 1002, 10, 15 },
                { 1003, 16, 19 },
                { 1004, 16, 19 },
                { 1005, 20, 29 },
                { 1006, 30, 100 },
                { 1007, 30, 34 },
                { 1008, 32, 95 },
                { 1009, 38, 53 },
                { 1010, 40, 50 },
                { 1011, 44, 44 },
                { 1012, 54, 54 },
                { 1013, 54, 54 },
                { 1014, 60, 70 },
                { 1015, 120, 140 },
                { 1016, 141, 145 },
                { 1017, 146, 180 },
                { 1018, 220, 240 },
                { 1019, 240, 245 },
                { 1020, 245, 280 },
        };

        Exon[] exons = new Exon[exonInfo.length];
        Location[] exonLocs = new Location[exonInfo.length];

        for (int i = 0; i < exons.length; i++) {
            exons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            int exonId = exonInfo[i][0];
            int start = exonInfo[i][1];
            int end = exonInfo[i][2];
            exons[i].setId(new Integer(exonId));
            exons[i].setLength(new Integer(end - start + 1));
            exons[i].setChromosome(chr);
            exonLocs[i] = createLocation(chr, exons[i], "1", start, end, Location.class);
            exonLocs[i].setId(new Integer(1000 + exonId));
        }

        ReversePrimer rp =
                (ReversePrimer) DynamicUtil.createObject(Collections.singleton(ReversePrimer.class));
        rp.setId(new Integer(3000));
        rp.setLength(new Integer(100));
        rp.setChromosome(chr);

        Location rpLoc = createLocation(chr, rp, "1", 1, 100, Location.class);
        rpLoc.setId(new Integer(3001));

        toStore.add(rp);
        toStore.add(rpLoc);
        toStore.addAll(Arrays.asList(exons));
        toStore.addAll(Arrays.asList(exonLocs));

        for (InterMineObject imo : toStore) {
            osw.store(imo);
        }
    }


    public void testCreateSpanningLocations() throws Exception {
        Exon exon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon1.setId(new Integer(107));
        Exon exon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon2.setId(new Integer(108));
        Exon exon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon3.setId(new Integer(109));

        Location exon1OnChr = createLocation(getChromosome(), exon1, "1", 51, 100, Location.class);
        exon1OnChr.setId(new Integer(1010));
        Location exon2OnChr = createLocation(getChromosome(), exon2, "1", 201, 250, Location.class);
        exon2OnChr.setId(new Integer(1011));
        Location exon3OnChr = createLocation(getChromosome(), exon3, "1", 201, 400, Location.class);
        exon3OnChr.setId(new Integer(1012));

        Transcript trans1 =
                (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        trans1.setId(new Integer(201));

        Transcript trans2 =
                (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        trans2.setId(new Integer(202));

        Location trans2OnChr = createLocation(getChromosome(), trans2, "1", 61, 300, Location.class);

        Gene gene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene.setId(new Integer(301));

        exon1.setTranscripts(new HashSet<Transcript>(Arrays.asList(new Transcript [] {trans1})));
        exon2.setTranscripts(new HashSet<Transcript>(Arrays.asList(new Transcript [] {trans1})));

        // the location of exon3 should be ignored by createSpanningLocations() because trans2
        // already has a location
        exon3.setTranscripts(new HashSet<Transcript>(Arrays.asList(new Transcript [] {trans2})));

        trans1.setGene(gene);
        trans2.setGene(gene);

        Set<InterMineObject> toStore =
                new HashSet<InterMineObject>(Arrays.asList(new InterMineObject[] {
                        getChromosome(), gene, trans1, trans2,
                        exon1, exon2, exon3,
                        exon1OnChr, exon2OnChr, trans2OnChr
                }));

        for (InterMineObject imo : toStore) {
            osw.store(imo);
        }

        MakeSpanningLocationsProcess cl = new MakeSpanningLocationsProcess(osw);
        cl.createSpanningLocations("Transcript", "Exon", "exons");
        cl.createSpanningLocations("Gene", "Transcript", "transcripts");

        ObjectStore os = osw.getObjectStore();
        Transcript resTrans1 = (Transcript) os.getObjectById(new Integer(201));

        Assert.assertEquals(1, resTrans1.getLocations().size());
        Location resTrans1Location = (Location) resTrans1.getLocations().iterator().next();
        Assert.assertEquals(51, resTrans1Location.getStart().intValue());
        Assert.assertEquals(250, resTrans1Location.getEnd().intValue());

        Transcript resTrans2 = (Transcript) os.getObjectById(new Integer(202));

        Assert.assertEquals(1, resTrans2.getLocations().size());
        Location resTrans2Location = (Location) resTrans2.getLocations().iterator().next();
        Assert.assertEquals(61, resTrans2Location.getStart().intValue());
        Assert.assertEquals(300, resTrans2Location.getEnd().intValue());

        Gene resGene = (Gene) os.getObjectById(new Integer(301));
        Assert.assertEquals(1, resGene.getLocations().size());
        Location resGeneLocation = (Location) resGene.getLocations().iterator().next();
        Assert.assertEquals(51, resGeneLocation.getStart().intValue());
        Assert.assertEquals(300, resGeneLocation.getEnd().intValue());
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
