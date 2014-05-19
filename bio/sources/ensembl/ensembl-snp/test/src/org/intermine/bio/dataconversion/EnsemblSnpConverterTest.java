package org.intermine.bio.dataconversion;
/*
* Copyright (C) 2002-2014 FlyMine
*
* This code may be freely distributed and modified under the
* terms of the GNU Lesser General Public Licence. This should
* be distributed with the code. See the LICENSE file for more
* information or http://www.gnu.org/copyleft/lesser.html.
*
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class EnsemblSnpConverterTest extends ItemsTestCase
{

    EnsemblSnpConverter converter;
    Model model = Model.getInstanceByName("genomic");
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, Item>());
    String seqClsName = "Chromosome";
    String taxonId = "9606";
    String dataSetTitle = "dbSNP data set";
    String dataSourceName = "Ensembl";
    String fileName = "ensembl.gvf";

    public EnsemblSnpConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        converter = new EnsemblSnpConverter(writer, model);
    }

    public void testParse() throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(fileName)));
        converter.process(new BufferedReader(reader));


        // uncomment to write a new items xml file
        writeItemsFile(writer.getItems(), "ensembl-snp_items.xml");

        Set<?> expected = readItemSet("ensembl-snp-expected.xml");
        assertEquals(expected, writer.getItems());
    }
}