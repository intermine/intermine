package org.intermine.bio.postprocess;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.GeneFlankingRegion;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

import junit.framework.TestCase;

public class CreateFlankingRegionsTest extends TestCase {

    private ObjectStoreWriter osw;


    public CreateFlankingRegionsTest(String arg) {
        super(arg);
    }


    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        setupData();
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


    public void testCreateFlankingFeatures() throws Exception {
        CreateFlankingRegions cfr = new CreateFlankingRegions(osw);
        cfr.createFlankingFeatures();

        Query q = new Query();
        QueryClass qcRegion = new QueryClass(GeneFlankingRegion.class);
        q.addFrom(qcRegion);
        q.addToSelect(qcRegion);

        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            GeneFlankingRegion o = (GeneFlankingRegion) resIter.next();
            System.out.println(o.getId() + ": " + o.getDirection() + " " + o.getDistance() + " length: " + o.getLength());
            Location loc = o.getChromosomeLocation();
            System.out.println(loc.getStart() + ", " + loc.getEnd() + ", " + loc.getStrand());
            assertNotNull(o.getChromosome());
            assertNotNull(o.getGene());
            assertNotNull(o.getOrganism());
            assertNotNull(o.getChromosomeLocation());
        }
    }


    private void setupData() throws Exception {
        Set<InterMineObject> toStore = new HashSet<InterMineObject>();

        Organism organism =
            (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        organism.setTaxonId(new Integer(7227));

        toStore.add(organism);

        Chromosome chr =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(200000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);

        toStore.add(chr);

        Gene gene =
            (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene.setChromosome(chr);
        gene.setPrimaryIdentifier("gene1");
        gene.setOrganism(organism);

        toStore.add(gene);

        Location loc =  (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        loc.setStart(new Integer(100000));
        loc.setEnd(new Integer(101000));
        loc.setStrand("-1");
        loc.setLocatedOn(chr);
        loc.setFeature(gene);

        toStore.add(loc);

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }
    }

}
