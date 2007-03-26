package org.intermine.bio.dataconversion;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.bio.io.gff3.GFF3Record;

import junit.framework.TestCase;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests for GFF3Record
 *
 * @author Kim Rutherford
 */

public class GFF3RecordTest extends TestCase
{
    public void testParse() throws Exception {
        String gff =
            "1\tfirstEF\tpromoter\t5\t100\t1000\t+\t.\tname1=value1,value2;ID=id%3Btest;name2=value2;name3=%26ggr%3B";

        GFF3Record gff3Record = new GFF3Record(gff);

        assertEquals("1", gff3Record.getSequenceID());
        assertEquals("firstEF", gff3Record.getSource());
        assertEquals("promoter", gff3Record.getType());
        assertEquals(5, gff3Record.getStart());
        assertEquals(100, gff3Record.getEnd());
        assertEquals(new Double(1000.0), gff3Record.getScore());
        assertEquals("+", gff3Record.getStrand());
        assertNull(gff3Record.getPhase());
        assertEquals("id;test", gff3Record.getId());

        Map expectedAttributes = new LinkedHashMap();

        expectedAttributes.put("name1", Arrays.asList(new Object[]{"value1", "value2"}));
        expectedAttributes.put("ID", Arrays.asList(new Object[] {"id;test"}));
        expectedAttributes.put("name2", Arrays.asList(new Object[] {"value2"}));
        expectedAttributes.put("name3", Arrays.asList(new Object[] {"&gamma;"}));

        assertEquals(expectedAttributes, gff3Record.getAttributes());
    }
    
    /**
     * Test that we throw an exception if an attribute contains an unescaped semicolon (FlyBase was
     * doing this for a while)
     */
    public void testSemiColonInAttr() throws Exception {
        String gff =
            "1\tfirstEF\tpromoter\t5\t100\t1000\t+\t.\tID=id;test";

        try {
            GFF3Record gff3Record = new GFF3Record(gff);
            fail("expected an IOException");
        } catch (IOException e) {
            // expected
        }
    }

    public void testToString() throws Exception {
        String gff =
            "X\t.\tgene\t21115389\t21115496\t.\t+\t.\tID=CR32525;Dbxref=FlyBase%3AFBan0032525,FlyBase%3AFBgn0052525;cyto_range=19F3-19F3;dbxref_2nd=FlyBase%3AFBgn0012015,FlyBase%3AFBgn0060008;gbunit=AE003568;synonym=CR32525;synonym_2nd=AE002620.trna4-TyrGTA,CG32525,tRNA%3Atyr1%3A19F,tRNA%3Cup%3ETyr%3C%2Fup%3E%3Cdown%3E1%26gamma%3B%3C%2Fdown%3E,tRNA%3AY1%3A19F,tRNA%3AY%3AGTA%3AAE002620-b";
        GFF3Record gff3Record = new GFF3Record(gff);
        List synonym2ndList = (List) gff3Record.getAttributes().get("synonym_2nd");

        assertEquals("tRNA<up>Tyr</up><down>1&gamma;</down>", synonym2ndList.get(3));

        String newGff = gff3Record.toGFF3();
        
        assertEquals(gff, gff3Record.toGFF3());
    }

    /**
     * Test URL encoding of sequence ID
     */
    public void testToString2() throws Exception {
        String gff =
            "X%26gamma%3BY\t.\tgene\t21115389\t21115496\t.\t+\t.\tID=CR32525";
        GFF3Record gff3Record = new GFF3Record(gff);
        String id = gff3Record.getId();
        assertEquals("X&gamma;Y", gff3Record.getSequenceID());
    }
}
