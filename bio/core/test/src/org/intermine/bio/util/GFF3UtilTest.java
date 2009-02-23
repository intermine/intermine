package org.intermine.bio.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.util.DynamicUtil;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Location;


/**
 * Tests for the GFF3Util class.
 */
public class GFF3UtilTest extends TestCase
{
    /*
     * Test method for 'org.intermine.bio.io.gff3.GFF3Util.makeGFF3Record(LocatedSequenceFeature)'
     */
    public void testMakeGFF3Record() {
        Gene gene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        Exon exon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        Chromosome chromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        Location geneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        Location exonLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));

        gene.setChromosome(chromosome);
        gene.setChromosomeLocation(geneLocation);
        gene.setPrimaryIdentifier("gene1");

        geneLocation.setStart(new Integer(100));
        geneLocation.setEnd(new Integer(800));
        geneLocation.setStrand("1");

        exon.setChromosome(chromosome);
        exon.setChromosomeLocation(exonLocation);
        exon.setPrimaryIdentifier("exon1");

        exonLocation.setStart(new Integer(200));
        exonLocation.setEnd(new Integer(300));
        exonLocation.setStrand("-1");

        chromosome.setPrimaryIdentifier("4");
        chromosome.setLength(new Integer(1000));

        Map<String, List<String>> extraAttributes = new LinkedHashMap<String, List<String>>();

        // test adding multiple values
        List valList = new ArrayList();
        valList.add("test_string1");
        valList.add("test_string2");
        extraAttributes.put("name3", valList);

        Map soClassNameMap = new LinkedHashMap();

        soClassNameMap.put("Gene", "gene");
        soClassNameMap.put("Exon", "exon");
        soClassNameMap.put("Chromosome", "chromosome");

        GFF3Record gff3Gene = GFF3Util.makeGFF3Record(gene, soClassNameMap, extraAttributes);

        GFF3Record gff3Exon = GFF3Util.makeGFF3Record(exon, soClassNameMap,
                                                      new HashMap<String, List<String>>());
        GFF3Record gff3Chromosome =
            GFF3Util.makeGFF3Record(chromosome, soClassNameMap, new HashMap());

        System.err.println (gff3Gene);
        System.err.println (gff3Exon);
        System.err.println (gff3Chromosome);

        System.err.println (gff3Gene.toGFF3());
        System.err.println (gff3Exon.toGFF3());
        System.err.println (gff3Chromosome.toGFF3());

        assertEquals("4\tFlyMine\tgene\t100\t800\t.\t+\t.\tname3=test_string1,test_string2;ID=gene1",
                     gff3Gene.toGFF3());
        assertEquals("4\tFlyMine\texon\t200\t300\t.\t-\t.\tID=exon1",
                     gff3Exon.toGFF3());
        assertEquals("4\tFlyMine\tchromosome\t1\t1000\t.\t.\t.\tID=4",
                     gff3Chromosome.toGFF3());
    }
}
