package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.dataconversion.FlyRegGFF3RecordHandler;
import org.intermine.bio.dataconversion.GFF3Converter;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;



import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Tests for the FlyRegGFF3RecordHandler class.
 *
 * @author Kim Rutherford
 */

public class FlyRegGFF3RecordHandlerTest extends TestCase
{
    private Model tgtModel;
    private FlyRegGFF3RecordHandler handler;
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    private String seqClsName = "Chromosome";
    private String orgAbbrev = "DM";
    private String dataSourceName = "FlyReg";
    private String dataSetTitle = "FlyReg data set";
    private String tgtNs;
    private ItemFactory itemFactory;
    private List featureIdentifiers;
    private GFF3Converter converter;
    private Item organism;

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new FlyRegGFF3RecordHandler(tgtModel);
        tgtNs = tgtModel.getNameSpace().toString();
        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, dataSourceName,
                                      "FlyBase", dataSetTitle, tgtModel, handler);
        itemFactory = handler.getItemFactory();
        organism = itemFactory.makeItem("0_0", tgtNs + "Organism", "");
    }

    public void testFlyRegHandler() throws Exception {
        String gff =
            "2L\tBergman_data\tTF_binding_site\t2452227\t2452269\t.\t.\t.\tFactor=Ubx; Target=dpp; PMID=1673656; ID=000448\n"
            + "2L\tBergman_data\tTF_binding_site\t2454657\t2454685\t.\t.\t.\tFactor=Adf1; Target=dpp; PMID=7791801; ID=003665\n"
            + "2L\tBergman_data\tTF_binding_site\t14615472\t14615509\t.\t.\t.\tFactor=Adf1; Target=Adh; PMID=2105454; ID=005046";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        LinkedHashSet allItems = new LinkedHashSet();
        
        Iterator iter = GFF3Parser.parse(srcReader);

        featureIdentifiers = new ArrayList();

        while (iter.hasNext()) {
            GFF3Record record = (GFF3Record) iter.next();
            String term = record.getType();
            String className = TypeUtil.javaiseClassName(term);
            Item feature = itemFactory.makeItem(null, tgtNs + className, "");

            handler.setFeature(feature);
            handler.process(record);
            feature.setAttribute("identifier", record.getId());
            
            featureIdentifiers.add(feature.getIdentifier());
            
            allItems.addAll(handler.getItems());
        }
        
        Iterator itemIter = allItems.iterator();
     
        Item expectedPub1 = itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(),
                                                 tgtNs + "Publication", "");
        expectedPub1.setAttribute("pubMedId", "1673656");
        
        Item expectedBindingSite1 = itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(),
                                                         tgtNs + "TFBindingSite", "");
        expectedBindingSite1.setAttribute("identifier", "000448");

        Item expectedGene1 =
            itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(), tgtNs + "Gene", "");
        expectedGene1.setAttribute("symbol", "dpp");
        expectedGene1.setReference("organism", "0_0");

        Item expectedGene2 =
            itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(), tgtNs + "Gene", "");
        expectedGene2.setAttribute("symbol", "Ubx");
        expectedGene2.setReference("organism", "0_0");

        Item expectedGene3 =
            itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(), tgtNs + "Gene", "");
        expectedGene3.setAttribute("symbol", "Adf1");
        expectedGene3.setReference("organism", "0_0");

        Item expectedBindingSite2 = itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(),
                                                         tgtNs + "TFBindingSite", "");
        expectedBindingSite2.setAttribute("identifier", "003665");
        
        Item expectedPub2 = itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(),
                                                 tgtNs + "Publication", "");
        expectedPub2.setAttribute("pubMedId", "7791801");

        Item expectedGene4 =
            itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(), tgtNs + "Gene", "");
        expectedGene4.setAttribute("symbol", "Adh");
        expectedGene4.setReference("organism", "0_0");

        Item expectedBindingSite3 = itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(),
                                                         tgtNs + "TFBindingSite", "");
        expectedBindingSite3.setAttribute("identifier", "005046");

        Item expectedPub3 = itemFactory.makeItem(((Item) itemIter.next()).getIdentifier(),
                                                 tgtNs + "Publication", "");
        expectedPub3.setAttribute("pubMedId", "2105454");


        expectedBindingSite1.setReference("factor", expectedGene2.getIdentifier());
        expectedBindingSite1.setReference("gene", expectedGene1.getIdentifier());
        expectedBindingSite1.addToCollection("evidence", expectedPub1);

        expectedBindingSite2.setReference("factor", expectedGene3.getIdentifier());
        expectedBindingSite2.setReference("gene", expectedGene1.getIdentifier());
        expectedBindingSite2.addToCollection("evidence", expectedPub2);

        expectedBindingSite3.setReference("factor", expectedGene3.getIdentifier());
        expectedBindingSite3.setReference("gene", expectedGene4.getIdentifier());
        expectedBindingSite3.addToCollection("evidence", expectedPub3);


        Set expectedItems = new LinkedHashSet(Arrays.asList(new Object [] {
            expectedPub1,
            expectedBindingSite1,
            expectedGene1,
            expectedGene2,
            expectedGene3,
            expectedBindingSite2,
            expectedPub2,
            expectedGene4,
            expectedBindingSite3,
            expectedPub3,
            }));

        assertEquals(10, allItems.size());

        assertEquals(expectedItems, allItems);
    }

}
