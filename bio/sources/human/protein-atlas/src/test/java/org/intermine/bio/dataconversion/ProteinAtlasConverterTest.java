package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
import org.intermine.xml.full.FullParser;

public class ProteinAtlasConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public ProteinAtlasConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String input = "\"Gene\",\"Tissue\",\"Cell type\",\"Level\",\"Expression type\",\"Reliability\"" + ENDL
            + "\"ENSG00000000003\",\"adrenal gland\",\"glandular cells\",\"Negative\",\"Staining\",\"Supportive\"" + ENDL
            + "\"ENSG00000000003\",\"appendix\",\"glandular cells\",\"Moderate\",\"Staining\",\"Supportive\"" + ENDL;


        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new ProteinAtlasConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));
        converter.setCurrentFile(new File("normal_tissue.csv"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "protein-atlas-tgt.xml");

        assertEquals(readItemSet("ProteinAtlasConverterTest.xml"), itemWriter.getItems());
    }

    @SuppressWarnings("rawtypes")
    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ProteinAtlasConverterTest.xml"));
    }

    public void testXMLParsing() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new ProteinAtlasConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));

        File testFile = new File(getClass().getClassLoader()
                .getResource("proteinatlas.xml").toURI());
        converter.setCurrentFile(testFile);
        converter.process(null);
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "protein-atlas-tgt.xml");

        assertEquals(readItemSet("ProteinAtlasConverterXMLParsingTest.xml"), itemWriter.getItems());
    }
}