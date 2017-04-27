package org.intermine.bio.dataconversion;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class SignorConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    SignorConverter converter;
    MockItemWriter itemWriter;

    public SignorConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new SignorConverter(itemWriter, model);
        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("all_data.csv"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "signor-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("SignorConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
