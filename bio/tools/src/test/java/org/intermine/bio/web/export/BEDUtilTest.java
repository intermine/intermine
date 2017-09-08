package org.intermine.bio.web.export;

import java.util.Collections;

import junit.framework.TestCase;

import org.intermine.bio.io.bed.BEDRecord;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Location;
import org.intermine.util.DynamicUtil;

public class BEDUtilTest extends TestCase
{

    private Exon exon ;
    private Chromosome chromosome ;
    private Location exonLocation;


    public void setUp() {
        exon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        chromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        exonLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));

        exon.setChromosome(chromosome);
        exon.setChromosomeLocation(exonLocation);
        exon.setPrimaryIdentifier("exon1");
        exon.setScore(0.9);

        exonLocation.setStart(new Integer(200));
        exonLocation.setEnd(new Integer(300));
        exonLocation.setStrand("-1");

        chromosome.setPrimaryIdentifier("4");
        chromosome.setLength(new Integer(1000));
    }


    public void testMakeBEDRecord() {
        BEDRecord actual = BEDUtil.makeBEDRecord(exon);
        System.out.println(actual.getChrom());
        assertEquals("chr4", actual.getChrom());

        assertEquals(199, actual.getChromStart());
        assertEquals(300, actual.getChromEnd());

        assertEquals("exon1", actual.getName());
        assertEquals(0, actual.getScore());
        assertEquals("-", actual.getStrand());

    }

}
