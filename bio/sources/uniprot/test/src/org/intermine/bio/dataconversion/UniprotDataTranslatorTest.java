package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import junit.framework.TestCase;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.bio.dataconversion.UniprotDataTranslator;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.XmlConverter;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.metadata.Model;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.ModelParser;
import org.intermine.modelproduction.xml.InterMineModelParser;

public class UniprotDataTranslatorTest extends DataTranslatorTestCase
{
    public UniprotDataTranslatorTest(String arg) {
        super(arg, "osw.bio-fulldata-test");
    }

    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testTranslate() throws Exception {

        Collection srcItems = getSrcItems();
        Collection expTgtItems = getExpectedItems();

        storeItems(srcItems);

        if (osw.isInTransaction()) {
            osw.commitTransaction();
        }

        ObjectStore os = (osw.getObjectStore());
        ItemReader srcItemReader = new ObjectStoreItemReader(os);

        // uncomment to generate a new source items file from some uniprot xml
        //retrieveFromUniprotExample("UniprotSrc.xml", new File("generatedSrcItems.xml"));

        DataTranslator translator = new UniprotDataTranslator(srcItemReader, mapping, srcModel, getTargetModel(tgtNs));
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        // print differences
        Set tgtItems = tgtIw.getItems();

        String diff = printCompareItemSets(new HashSet(expTgtItems), tgtItems);
        if (!diff.equals("")) {
            System.err.println(diff);
        }

        // uncomment to write out a new target items file
        //FileWriter fw = new FileWriter(new File("uniprot_tgt.xml"));
        //fw.write(FullRenderer.render(tgtIw.getItems()));
        //fw.close();
        assertEquals(new LinkedHashSet(getExpectedItems()), tgtIw.getItems());
    }


    private void retrieveFromUniprotExample(String uniprot, File output) throws Exception {
        Model model = getModel();
        Reader srcReader = (new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/UniprotSrc.xml")));
        MockItemWriter mockIw = new MockItemWriter(new HashMap());

        //Reader xsdReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/uniprot.xsd"));
        Reader xsdReader = new FileReader(new File("../srcmodel/uniprot.xsd"));

        XmlConverter converter = new XmlConverter(model, xsdReader, mockIw);
        converter.process(srcReader);
        mockIw.close();
        FileWriter fw = new FileWriter(output);
        fw.write(FullRenderer.render(mockIw.getItems()));
        fw.close();
    }


    public void testGetItemsInCollection() throws Exception {
        Item transcript = createItem(tgtNs + "transcript", "0_101");
        Item exon1 = createItem(tgtNs + "exon", "1_101");
        Item exon2 = createItem(tgtNs + "exon", "1_102");
        Item exon3 = createItem(tgtNs + "exon", "1_103");
        ReferenceList exons = new ReferenceList("exons", Arrays.asList(new Object[] {exon2.getIdentifier(),
                                                    exon1.getIdentifier(), exon3.getIdentifier()}));
        transcript.addCollection(exons);

        // write items to database, can't use MockItemWriter
        List items = Arrays.asList(new Object[] {transcript, exon1, exon2, exon3});
        storeItems(items);
        // check that exons collection comes back in the same order
        ObjectStore os = osw.getObjectStore();
        ItemReader srcItemReader = new ObjectStoreItemReader(os);
        UniprotDataTranslator translator = new UniprotDataTranslator(srcItemReader, mapping, srcModel, getTargetModel(tgtNs));

        List exonList = new ArrayList(Arrays.asList(new Object[] {exon2, exon1, exon3}));
        List resExons = translator.getItemsInCollection(exons);
        assertEquals(exonList, resExons);
    }

    private Model getModel() throws Exception {
        ModelParser parser = new InterMineModelParser();
        return parser.process(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("uniprot_model.xml")));
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/UniprotDataTranslatorTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/UniprotDataTranslatorTest_src.xml"));
    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "uniprot";
    }

    private Item createItem(String className, String identifier) {
        Item item = new Item(identifier, className, "");
        return item;
    }
}
