package org.intermine.bio.dataconversion;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class ClinvarConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    ClinvarConverter converter;
    MockItemWriter itemWriter;
    private final String currentFile = "variant_summary.txt";

    public ClinvarConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new ClinvarConverter(itemWriter, model);
    }


    public void testProcess() throws Exception {
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(currentFile));

        converter.process(reader);
        converter.close();
        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "clinvar-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("ClinVarConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
