package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;

public class PsiComplexesConverterTest extends ItemsTestCase
{
    public PsiComplexesConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        PsiComplexesConverter converter = new PsiComplexesConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.setPsiOrganisms("4932");
        //Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("PsiComplexesConverterTest_src.xml"));
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("swr1_yeast-1.xml"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "psi-complexes-tgt-items.xml");

        Set expected = readItemSet("PsiComplexesConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());

    }

    /**
     * Render the given Item as XML
     * @param item the Item to render
     * @return an XML representation of the Item
     */
    public static String render(XMLOutputFactory factory, Item item) {
        StringWriter sw = new StringWriter();
        factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer;
        try {
            writer = factory.createXMLStreamWriter(sw);
            FullRenderer.renderImpl(writer, item);
        } catch (XMLStreamException e) {
            throw new RuntimeException("unexpected failure while creating Item XML", e);
        }
        return sw.toString();
    }
}
