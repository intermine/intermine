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
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Location;


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
        gene.setIdentifier("gene1");

        geneLocation.setStart(new Integer(100));
        geneLocation.setEnd(new Integer(800));
        geneLocation.setStrand("1");
        
        exon.setChromosome(chromosome);
        exon.setChromosomeLocation(exonLocation);
        exon.setIdentifier("exon1");

        exonLocation.setStart(new Integer(200));
        exonLocation.setEnd(new Integer(300));
        exonLocation.setStrand("-1");
        
        chromosome.setIdentifier("4");
        chromosome.setLength(new Integer(1000));
 
        Map extraAttributes = new LinkedHashMap();

        // test adding strings
        extraAttributes.put("name1", "value1");
        // test adding ints
        extraAttributes.put("name2", new Integer(2));
        // test adding multiple values
        List valList = new ArrayList();
        valList.add(new Integer(3));
        valList.add("4");
        extraAttributes.put("name3", valList);

        Map soClassNameMap = new LinkedHashMap();
        
        soClassNameMap.put("Gene", "gene");
        soClassNameMap.put("Exon", "exon");
        soClassNameMap.put("Chromosome", "chromosome");
        
        GFF3Record gff3Gene = GFF3Util.makeGFF3Record(gene, soClassNameMap, extraAttributes);

        GFF3Record gff3Exon = GFF3Util.makeGFF3Record(exon, soClassNameMap, new HashMap());
        GFF3Record gff3Chromosome =
            GFF3Util.makeGFF3Record(chromosome, soClassNameMap, new HashMap());

        System.err.println (gff3Gene);
        System.err.println (gff3Exon);
        System.err.println (gff3Chromosome);

        System.err.println (gff3Gene.toGFF3());
        System.err.println (gff3Exon.toGFF3());
        System.err.println (gff3Chromosome.toGFF3());

        assertEquals("4	FlyMine	gene	100	800	.	+	.	name1=value1;name2=2;name3=3,4;ID=gene1",
                     gff3Gene.toGFF3());
        assertEquals("4	FlyMine	exon	200	300	.	-	.	ID=exon1",
                     gff3Exon.toGFF3());
        assertEquals("4	FlyMine	chromosome	1	1000	.	.	.	ID=4",
                     gff3Chromosome.toGFF3());
    }
}
