package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.bio.dataconversion.MageFlatFileConverter;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.ItemWriter;

/**
 * Test for translating MAGE data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 */

public class MageFlatFileConverterTest extends TestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private String srcNs = "http://www.intermine.org/model/mage#";
    private ItemFactory srcItemFactory;
    private ItemFactory tgtItemFactory;
    private File file;
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());

    public void setUp() throws Exception {
        super.setUp();
        srcItemFactory = new ItemFactory(Model.getInstanceByName("mage"));
        tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));

        // String ENDL = System.getProperty("line.separator");
//         file = new File("build/model/genomic/mage_config.properties");
//         String propertiesFile="E-SMDB-3450.experimentName=Rossi et al, 2005" + ENDL
//             + "E-SMDB-3450.description=Compare blood stem cells from young vs old mice" + ENDL
//             + "E-SMDB-3450.pmid=15967997" + ENDL;

//         FileWriter fw = new FileWriter(file);
//         fw.write(propertiesFile);
//         fw.close();
    }


    public void tearDown() throws Exception {
        super.tearDown();
        //file.delete();
    }


    public void testProcess() throws Exception {

        BufferedReader srcReader = new BufferedReader(new
            InputStreamReader(getClass().getClassLoader().getResourceAsStream("mageFlat.txt")));

        MageFlatFileConverter converter = new MageFlatFileConverter(writer);

        converter.process(srcReader);
        converter.close();

        FileWriter writerSrc = new FileWriter(new File("mageFlat.tgt"));
        writerSrc.write(FullRenderer.render(writer.getItems()));
        writerSrc.close();

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("mageFlatTgt.xml")));

        String expectedNotActual = "in expected, not actual: " + compareItemSets(expected, writer.getItems());
        String actualNotExpected = "in actual, not expected: " + compareItemSets(writer.getItems(), expected);
        if (expectedNotActual.length() > 25) {
            System.out.println(expectedNotActual);
            System.out.println(actualNotExpected);
        }
        assertEquals(expected, writer.getItems());
    }

    protected Set compareItemSets(Set a, Set b) {
        Set diff = new HashSet(a);
        Iterator i = a.iterator();
        while (i.hasNext()) {
            Item itemA = (Item) i.next();
            Iterator j = b.iterator();
            while (j.hasNext()) {
                Item itemB = (Item) j.next();
                if (itemA.equals(itemB)) {
                    diff.remove(itemA);
                }
            }
        }
        return diff;
    }

}
