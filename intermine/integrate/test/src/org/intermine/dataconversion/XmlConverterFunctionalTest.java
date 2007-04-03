package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.ModelParser;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.xml.full.FullParser;

public class XmlConverterFunctionalTest extends TestCase {

    String MODEL = "xmlschematest";

    public void testProcess() throws Exception {
        Model model = getModel();
        Reader srcReader = (new InputStreamReader(getClass().getClassLoader().getResourceAsStream("XmlConverterFunctionalTest_src.xml")));
        MockItemWriter mockIw = new MockItemWriter(new HashMap());
        Reader xsdReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(MODEL + ".xsd"));

        XmlConverter converter = new XmlConverter(model, xsdReader, mockIw);
        System.out.println(converter.xmlInfo.toString());
        converter.process(srcReader);
        mockIw.close();

        Set tgtItems = getTgtItems();

        System.out.println(ItemsTestCase.compareItemSets(tgtItems, mockIw.getItems()));
        assertEquals(tgtItems, mockIw.getItems());
    }

    private Model getModel() throws Exception {
        ModelParser parser = new InterMineModelParser();
        return parser.process(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("xmlschematest.xml")));
    }

    private Set getTgtItems() throws Exception {
        return new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("XmlConverterFunctionalTest_tgt.xml")));
    }

}
