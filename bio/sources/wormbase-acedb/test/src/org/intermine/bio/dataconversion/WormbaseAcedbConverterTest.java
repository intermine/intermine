package org.intermine.bio.dataconversion;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class WormbaseAcedbConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    WormbaseAcedbConverter converter;
    MockItemWriter itemWriter;

    public WormbaseAcedbConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new WormbaseAcedbConverter(itemWriter, model);

        // TODO add keyfile
        // converter.setKeyFile(keyFilePath);
        converter.setKeyFile("resources/strain_mapping.properties");
        converter.setMappingFile("resources/strain_mapping.properties");
        converter.setSourceClass("Strain");

        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("strain_prepped.xml"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "wormbase-acedb-tgt-items.xml");

        // update this with the correct XML file you want.
        Set<org.intermine.xml.full.Item> expected = readItemSet("WormbaseAcedbConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
