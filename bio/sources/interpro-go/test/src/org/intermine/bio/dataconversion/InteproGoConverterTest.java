package org.intermine.bio.dataconversion;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class InteproGoConverterTest extends ItemsTestCase
{

   private String ENDL = System.getProperty("line.separator");
   Model model = Model.getInstanceByName("genomic");
   InterproGoConverter converter;
   MockItemWriter itemWriter;

   public InteproGoConverterTest(String arg) {
       super(arg);
   }

   public void setUp() throws Exception {
       super.setUp();
       itemWriter = new MockItemWriter(new HashMap());
       converter = new InterproGoConverter(itemWriter, model);
   }

   public void testProcess() throws Exception {

       Reader reader = new InputStreamReader(getClass().getClassLoader()
                                             .getResourceAsStream("InterproGoConverterTest_src.txt"));
       converter.process(reader);
       converter.close();

       // uncomment to write out a new target items file
       //writeItemsFile(itemWriter.getItems(), "interpro-go-tgt-items.xml");

       assertEquals(readItemSet("InterproGoConverterTest_tgt.xml"), itemWriter.getItems());
   }

}
