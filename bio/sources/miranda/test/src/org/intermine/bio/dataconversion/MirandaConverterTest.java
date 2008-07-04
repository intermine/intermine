package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

/**
 * Test for data from miRanda
 * 
 * @author "Xavier Watkins"
 *
 */
public class MirandaConverterTest extends ItemsTestCase
{
    MirandaGFF3RecordHandler handler;
    private List featureIdentifiers;
    private ItemFactory itemFactory;
    private String tgtNs;
    private Model tgtModel;
    private GFF3Converter converter;
    private MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
    private String seqClsName = "Chromosome";
    private String taxonId = "DM";
    private String dataSourceName = "Sanger";
    private String dataSetTitle = "miRanda";
    
    public MirandaConverterTest(String arg) {
        super(arg);
    }

    protected void setUp() throws Exception {
        super.setUp();
        tgtModel = Model.getInstanceByName("genomic");
        handler = new MirandaGFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(tgtIw, seqClsName, taxonId, dataSourceName,
                        "FlyBase", dataSetTitle, tgtModel, handler, null);
        itemFactory = handler.getItemFactory();
        tgtNs = tgtModel.getNameSpace().toString();
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("mir-92b"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("mir-312"));
        handler.resolverFactory = resolverFactory;
    }

    protected void tearDown() throws Exception {
        converter.close();
    }
    
    public void testMirandaHandler() throws Exception {
        String gff =
            "3R\tmiRanda\tmiRNA_target\t9403\t9424\t16.9418\t+\t.\ttarget=CG11023-RA;score=3.057390e-02;ID=dme-miR-312"
                     + ENDL
                     + "3R\tmiRanda\tmiRNA_target\t9403\t9424\t17.7377\t+\t.\ttarget=CG11023-RA;score=1.179130e-02;ID=dme-miR-92b"
                     + ENDL;
        
        
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

            featureIdentifiers.add(feature.getIdentifier());

            allItems.addAll(handler.getItems());
        }
                
        // uncomment to write a new tgt items file
        writeItemsFile(allItems, "/tmp/miranda-tgt-items.xml");

        Set expected = readItemSet("miranda-tgt-items.xml");
       System.out.println(ItemsTestCase.compareItemSets(expected, allItems));
        assertEquals(expected, allItems);
    }

}
