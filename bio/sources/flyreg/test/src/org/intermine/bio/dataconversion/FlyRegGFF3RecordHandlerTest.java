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
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

/**
 * Tests for the FlyRegGFF3RecordHandler class.
 *
 * @author Kim Rutherford
 */

public class FlyRegGFF3RecordHandlerTest extends ItemsTestCase
{
    private Model tgtModel;
    private FlyRegGFF3RecordHandler handler;
    private String seqClsName = "Chromosome";
    private String orgAbbrev = "DM";
    private String dataSourceName = "FlyReg";
    private String dataSetTitle = "FlyReg data set";
    private String tgtNs;
    private ItemFactory itemFactory;
    private GFF3Converter converter;
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap());

    public FlyRegGFF3RecordHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        tgtNs = tgtModel.getNameSpace().toString();
        handler = new FlyRegGFF3RecordHandler(tgtModel);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("dpp"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("dl"));
        handler.resolverFactory = resolverFactory;
        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, dataSourceName,
                                      "FlyBase", dataSetTitle, tgtModel, handler, null);
        itemFactory = handler.getItemFactory();
    }

    public void tearDown() throws Exception {
        converter.close();
    }

    public void testFlyRegHandler() throws Exception {
        String gff =
            "2L\tREDfly\tregulatory_region\t2456365\t2456372\t.\t.\t.\tID=Unspecified_dpp:REDFLY:TF000068; Dbxref=Flybase:FBgn0000490, PMID:8543160, REDfly:644, FlyBase:; Evidence=footprint/binding assay; Factor=Unspecified; Target=dpp\n"
            + "2L\tREDfly\tregulatory_region\t2456352\t2456369\t.\t.\t.\tID=dl_dpp:REDFLY:TF000069; Dbxref=Flybase:FBgn0000490, PMID:8458580, REDfly:645, FlyBase:FBgn0000463; Evidence=footprint/binding assay; Factor=dl; Target=dpp\n"
            + "2L\tREDfly\tregulatory_region\t2456423\t2456433\t.\t.\t.\tID=Unspecified_dpp:REDFLY:TF000067; Dbxref=Flybase:FBgn0000490, PMID:8543160, REDfly:643, FlyBase:; Evidence=footprint/binding assay; Factor=Unspecified; Target=dpp\n";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        HashSet allItems = new LinkedHashSet();

        Iterator iter = GFF3Parser.parse(srcReader);

        while (iter.hasNext()) {
            GFF3Record record = (GFF3Record) iter.next();
            String term = record.getType();
            String className = TypeUtil.javaiseClassName(term);
            Item feature = itemFactory.makeItem(null, tgtNs + className, "");

            handler.setFeature(feature);
            handler.clearEvidenceReferenceList();
            handler.process(record);
            // evidence collection is normally set in GFF3Converter, we just want to check Publication
            feature.addCollection(handler.getEvidenceReferenceList());
            allItems.addAll(handler.getItems());
        }

        // uncomment to write a new target items files
        //writeItemsFile(allItems, "flyreg-tgt-items.xml");

        Set expected = readItemSet("FlyRegGFF3RecordHandlerTest.xml");

        assertEquals(expected, allItems);
    }

}
