package org.intermine.bio.dataconversion;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class FlybaseAllelesConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    FlybaseAllelesConverter converter;
    MockItemWriter itemWriter;

    public FlybaseAllelesConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new FlybaseAllelesConverter(itemWriter, model);
        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("allele_human_disease_model_data_fb_2017_02.tsv"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "flybase-alleles-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("FlyBaseAllelesConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
