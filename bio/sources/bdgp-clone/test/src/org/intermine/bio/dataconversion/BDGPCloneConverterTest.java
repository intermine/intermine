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

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.FullParser;

/**
 * BDGP clone converter functional test.
 * @author Richard Smith
 */
public class BDGPCloneConverterTest extends TestCase
{

    public void testProcess() throws Exception {
        String ENDL = System.getProperty("line.separator");
        String input = "# comment" + ENDL
            + "CG9480\tGlycogenin\tFBgn0034603\tRE02181;RE21586" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BDGPCloneConverter converter = new BDGPCloneConverter(itemWriter);

        converter.process(new StringReader(input));
        converter.close();

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("BDGPCloneConverterTest.xml")));
        assertEquals(expected, itemWriter.getItems());
    }
}
