package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;


import org.intermine.xml.full.Item;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.FileConverter;


public class EnsemblDiseaseConverterTest extends TestCase {

    public void testProcess() throws Exception {

        String ENDL = "\n";
        String TAB = "\t";
        String input = "Ensembl Gene ID"+TAB+"Disease OMIM ID"+TAB+"Disease description"+ENDL
+"ENSG00000162688"+TAB+"232400"+TAB+"Glycogen storage disease IIIa (1)"+ENDL
+"ENSG00000162688"+TAB+"232400"+TAB+"Glycogen storage disease IIIb (3)"+ENDL
+"ENSG00000107829"+TAB+"600095"+TAB+"Split hand/foot malformation, type 3 (2)"+ENDL
+"ENSG00000198691"+TAB+"601691"+TAB+"Cone-rod dystrophy 3 (3)"+ENDL
+"ENSG00000198691"+TAB+"601691"+TAB+"Fundus flavimaculatus with macular dystrophy, 248200 (3)"+ENDL
+"ENSG00000198691"+TAB+"601718"+TAB+"Retinitis pigmentosa-19 (2)"+ENDL
+"ENSG00000198691"+TAB+"601691"+TAB+"Retinitis pigmentosa-19, 601718 (3)"+ENDL
+"ENSG00000198691"+TAB+"601691"+TAB+"Stargardt disease-1, 248200 (3)"+ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new EnsemblDiseaseConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        FileWriter writer = new FileWriter(new File("diseasetmp"));
        writer.write(FullRenderer.render(itemWriter.getItems()));
        writer.close();

        Set expected=new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/DiseaseTestTgt.xml")));

        String expectedNotActual = "in expected, not actual: " + compareItemSets(expected, itemWriter.getItems());
        String actualNotExpected = "in actual, not expected: " + compareItemSets(itemWriter.getItems(), expected);

        if (expectedNotActual.length() > 25) {
            System.out.println(expectedNotActual);
            System.out.println(actualNotExpected);
        }

        assertEquals(expected, itemWriter.getItems());
    }

     /**
     * Given two sets of Items (a and b) return a set of Items that are present in a
     * but not b.
     * @param a a set of Items
     * @param b a set of Items
     * @return the set of Items in a but not in b
     */
    public Set compareItemSets(Set a, Set b) {
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
