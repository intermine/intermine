package org.intermine.bio.web.export;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Location;
import org.intermine.util.DynamicUtil;


/**
 * Tests for the GFF3Util class.
 */
public class GFF3UtilTest extends TestCase
{
    private Exon exon ;
    private Chromosome chromosome ;
    private Location exonLocation;
    private Map<String, List<String>> emptyAttributes = new HashMap<String, List<String>>();

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


    public void testMakeGFF3Record() {
        String expected = "4\tTestSource\texon\t200\t300\t0.9\t-\t.\tID=exon1;key1=value1,value2";
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put("key1", Arrays.asList(new String[] {"value1", "value2"}));
        String gffLine = GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", attributes).toGFF3();
        assertEquals(expected, gffLine);
    }

    public void testMakeGFF3RecordStrands() {
        // negative
        exonLocation.setStrand("-1");
        String expected = "4\tTestSource\texon\t200\t300\t0.9\t-\t.\tID=exon1";
        String gffLine = GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", emptyAttributes).toGFF3();
        assertEquals(expected, gffLine);

        // positive
        exonLocation.setStrand("1");
        expected = "4\tTestSource\texon\t200\t300\t0.9\t+\t.\tID=exon1";
        gffLine = GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", emptyAttributes).toGFF3();
        assertEquals(expected, gffLine);

        // no strand
        exonLocation.setStrand(null);
        expected = "4\tTestSource\texon\t200\t300\t0.9\t.\t.\tID=exon1";
        gffLine = GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", emptyAttributes).toGFF3();
        assertEquals(expected, gffLine);

        // 0 = also no strand
        exonLocation.setStrand("0");
        expected = "4\tTestSource\texon\t200\t300\t0.9\t.\t.\tID=exon1";
        gffLine = GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", emptyAttributes).toGFF3();
        assertEquals(expected, gffLine);
    }

    public void testMakeGFF3RecordInvalid() {
        // don't create records for chromosomes
        assertNull(GFF3Util.makeGFF3Record(chromosome, getSoClassNameMap(), "TestSource", emptyAttributes));

        // must have a chromosome reference
        exon.setChromosome(null);
        assertNull(GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", emptyAttributes));
        exon.setChromosome(chromosome);

        // must have a chromosomeLocation reference
        exon.setChromosomeLocation(null);
        assertNull(GFF3Util.makeGFF3Record(exon, getSoClassNameMap(), "TestSource", emptyAttributes));
        exon.setChromosomeLocation(exonLocation);
    }

    // if no entry in SO class name map just uses the class name
    public void testMakeGFF3RecordNoSoClassName() {
        Map<String, String> soClassNameMap = getSoClassNameMap();
        soClassNameMap.remove("Exon");
        String gffLine = GFF3Util.makeGFF3Record(exon, soClassNameMap, "TestSource", emptyAttributes).toGFF3();
        String expected = "4\tTestSource\tExon\t200\t300\t0.9\t-\t.\tID=exon1";
        assertEquals(expected, gffLine);
    }

    private Map<String, String> getSoClassNameMap() {
        Map<String, String> soClassNameMap = new LinkedHashMap<String, String>();
        soClassNameMap.put("Exon", "exon");
        soClassNameMap.put("Chromosome", "chromosome");

        return soClassNameMap;
    }
}
