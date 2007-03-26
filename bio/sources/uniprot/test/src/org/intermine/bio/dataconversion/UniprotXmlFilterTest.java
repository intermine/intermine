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

import java.util.Set;
import java.util.HashSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.InputStreamReader;

import org.intermine.bio.dataconversion.UniprotXmlFilter;
import  org.intermine.util.StringUtil;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

public class UniprotXmlFilterTest extends XMLTestCase
{
    File tmpFile;

    public void setUp() throws Exception {
        tmpFile = File.createTempFile("uniprot_filter_tmp", "");
    }

    public void tearDown() {
        tmpFile.delete();
    }

    public void testFilter() throws Exception {
        Set organisms = new HashSet();
        organisms.add("7227");
        UniprotXmlFilter filter = new UniprotXmlFilter(organisms);

        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader()
                                      .getResourceAsStream("test/UniprotXmlFilterTest_src.xml")));

        BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
        filter.filter(srcReader, out);
        out.flush();
        out.close();

        InputStreamReader expectedReader = new InputStreamReader(getClass().getClassLoader()
                                      .getResourceAsStream("test/UniprotXmlFilterTest_tgt.xml"));
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(expectedReader, new FileReader(tmpFile));
    }

    public void testEscape() throws Exception {
        String in ="Novel protein\\n\\";
        
        String out = StringUtil.escapeBackslash(in);
        String expected = "Novel protein/n/";
        System.out.println("escapeOut " + out);
        assertEquals(expected, out);
    }
    
}
