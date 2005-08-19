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

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;

/**
 * image clone converter functional test.
 * @author Richard Smith
 */
public class ImageCloneConverterTest extends TestCase
{

    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());

    public void testProcess() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
            InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/ImageClone.test")));

        ImageCloneConverter converter = new ImageCloneConverter(writer);

        converter.process(srcReader);
        converter.close();

        FileWriter writerSrc = new FileWriter(new File("ImageClone.xml"));
        writerSrc.write(FullRenderer.render(writer.getItems()));
        writerSrc.close();

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/ImageClone.xml")));

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
