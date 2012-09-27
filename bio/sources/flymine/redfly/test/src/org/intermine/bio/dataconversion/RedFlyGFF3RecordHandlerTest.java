package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Tests for the RedFlyGFF3RecordHandler class.
 *
 * @author Kim Rutherford
 */
public class RedFlyGFF3RecordHandlerTest extends ItemsTestCase
{
    private Model tgtModel;
    private RedFlyGFF3RecordHandler handler;
    private String seqClsName = "Chromosome";
    private String taxonId = "DM";
    private String dataSourceName = "FlyReg";
    private String dataSetTitle = "FlyReg data set";
    private GFF3Converter converter;
    private String ENDL = System.getProperty("line.separator");
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, Item>());

    public RedFlyGFF3RecordHandlerTest(String arg) {
        super(arg);
    }


    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new RedFlyGFF3RecordHandler(tgtModel);
        // call the GFF3Converter constructor to initialise the handler
        converter = new GFF3Converter(writer, seqClsName, taxonId, dataSourceName,
                          dataSetTitle, tgtModel, handler, null);

        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn0001", Collections.singleton("FBgn0003145"));
        resolverFactory.addResolverEntry("7227", "FBgn0002", Collections.singleton("FBgn0003339"));
        handler.resolverFactory = resolverFactory;
    }

    public void tearDown() throws Exception {
        converter.close();
    }

    public void testFlyRegHandler() throws Exception {
        String gff =
            "2L\tREDfly\tregulatory_region\t12092691\t12095792\t.\t.\t.\tID=\"prd_A8_repressor\"; Dbxref=\"Flybase:FBgn0003145\", \"PMID:7873402\", \"REDfly:391\"; Evidence=\"reporter construct (in vivo)\"; Ontology_term=\"FBbt:00005304\",\"FBbt:00005427\",\"FBbt:00005414\"" + ENDL
            + "2L\tREDfly\tregulatory_region\t12087891\t12088492\t.\t.\t.\tID=\"prd_P1_enhancer\"; Dbxref=\"Flybase:FBgn0003145\", \"PMID:7873402\", \"REDfly:392\"; Evidence=\"reporter construct (in vivo)\"; Ontology_term=\"FBbt:00005304\",\"FBbt:00000111\"" + ENDL
            + "3R\tREDfly\tregulatory_region\t2667896\t2676484\t.\t.\t.\tID=\"Scr_BSR\"; Dbxref=\"Flybase:FBgn0003339\", \"PMID:7713432\", \"REDfly:576\"; Evidence=\"reporter construct (in vivo)\"; Ontology_term=\"FBbt:00000090\"";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write a new tgt items file
        //writeItemsFile(writer.getItems(), "redfly-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = new HashSet<org.intermine.xml.full.Item>(readItemSet("RedFlyGFF3RecordHandlerTest.xml"));
        //System.out.println(ItemsTestCase.compareItemSets(expected, allItems));
        assertEquals(expected, writer.getItems());
    }
}
