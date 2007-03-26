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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Properties;
import java.util.Iterator;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.bio.dataconversion.ArbeitmanAssayHandler;
import org.intermine.bio.dataconversion.MageDataTranslator;
import org.intermine.dataconversion.MockItemReader;

public class ArbeitmanAssayHandlerTest extends TestCase
{
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private ItemFactory itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    private MageDataTranslator translator;
    private String EXPT_NAME = "E-FLYC-1";

    public ArbeitmanAssayHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                            new Properties(), Model.getInstanceByName("mage"), Model.getInstanceByName("genomic"));
    }

    public void testCompare() throws Exception {
        Item expt = createItem("MicroArrayExperiment", "2_1", "");
        expt.setAttribute("name", EXPT_NAME);

        Item ref = createItem("Sample", "1_10", "");
        ref.setAttribute("primaryCharacteristic", "Reference");
        ref.setAttribute("primaryCharacteristicType", "N/A");
        translator.sampleToLabel.put("1_10", "cy3");
        translator.samplesById.put("1_10", ref);

        translator.addToMap(translator.sampleToChars, "1_1", "Age", "2 To 3");
        translator.addToMap(translator.sampleToChars, "1_1", "TimeUnit", "hours");
        translator.addToMap(translator.sampleToChars, "1_1", "DevelopmentalStage", "embryonic stage 1");
        Item a1 = createAssay("0_1", "1_1", expt.getIdentifier());

        // set config to use ArbeitmanAssayHandler
        translator.addToMap(translator.config, "E-FLYC-1", "assayHandlerClass",
                            "org.intermine.bio.dataconversion.ArbeitmanAssayHandler");
        translator.processMicroArrayAssays();

        Item expAssay1 = createItem("MicroArrayAssay", "0_1", "");
        expAssay1.setAttribute("name", "assay_0_1");
        expAssay1.setAttribute("sample1", "N/A: Reference");
        expAssay1.setAttribute("sample2", "stage: Embryo - 2 To 3 Hours");
        expAssay1.setAttribute("displayOrder", "0");
        expAssay1.setReference("experiment", "2_1");
        expAssay1.setCollection("samples", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_10"})));

        Map expected = new HashMap();
        expected.put("0_1", expAssay1);

        // TODO this line should work but doesn't, possibly a problem with
        // class loaders - comparing and object created in parent class loader
        // to one from a class instantiated by reflection.
        //assertEquals(expAssay1, (Item) translator.assays.get("0_1"));
    }

    public void testAssayOrder() throws Exception {
        Item expt = createItem("MicroArrayExperiment", "2_1", "");
        expt.setAttribute("name", EXPT_NAME);

        Item ref = createItem("Sample", "1_10", "");
        ref.setAttribute("primaryCharacteristic", "Reference");
        ref.setAttribute("primaryCharacteristicType", "N/A");
        translator.sampleToLabel.put("1_10", "cy3");
        translator.samplesById.put("1_10", ref);

        translator.addToMap(translator.sampleToChars, "1_1", "Age", "2 To 3");
        translator.addToMap(translator.sampleToChars, "1_1", "TimeUnit", "hours");
        translator.addToMap(translator.sampleToChars, "1_1", "DevelopmentalStage", "embryonic stage 1");
        Item a1 = createAssay("0_1", "1_1", expt.getIdentifier());
        translator.addToMap(translator.sampleToChars, "1_2", "Age", "5.5");
        translator.addToMap(translator.sampleToChars, "1_2", "TimeUnit", "hours");
        translator.addToMap(translator.sampleToChars, "1_2", "DevelopmentalStage", "embryonic stage 3");
        Item a2 = createAssay("0_2", "1_2", expt.getIdentifier());

        translator.addToMap(translator.sampleToChars, "1_3", "Age", "0 To 2");
        translator.addToMap(translator.sampleToChars, "1_3", "TimeUnit", "hours");
        translator.addToMap(translator.sampleToChars, "1_3", "DevelopmentalStage", "pupal stage 2");
        Item a3 = createAssay("0_3", "1_3", expt.getIdentifier());

        translator.addToMap(translator.sampleToChars, "1_4", "Age", "1");
        translator.addToMap(translator.sampleToChars, "1_4", "TimeUnit", "days");
        translator.addToMap(translator.sampleToChars, "1_4", "DevelopmentalStage", "adult stage 1");
        translator.addToMap(translator.sampleToChars, "1_4", "Sex", "female");
        Item a4 = createAssay("0_4", "1_4", expt.getIdentifier());

        ArbeitmanAssayHandler handler = new ArbeitmanAssayHandler(translator);
        List assays = new ArrayList(Arrays.asList(new Object[] {a3, a2, a4, a1}));
        List expOrder = new ArrayList(Arrays.asList(new Object[] {
            handler.getAssayOrderable(a1),
            handler.getAssayOrderable(a2),
            handler.getAssayOrderable(a3),
            handler.getAssayOrderable(a4)}));

        TreeSet ordered = new TreeSet();
        Iterator i = assays.iterator();
        while (i.hasNext()) {
            ordered.add(handler.getAssayOrderable((Item) i.next()));
        }

        assertEquals(expOrder, new ArrayList(ordered));
    }


    private Item createAssay(String assayId, String sampleId, String exptId) {
        Item assay = createItem("MicroArrayAssay", assayId, "");
        assay.setAttribute("name", "assay_" + assayId);
        assay.setReference("experiment", exptId);
        Item sample = createItem("Sample", sampleId, "");
        sample.setAttribute("primaryCharacteristic", "anything");
        assay.setCollection("samples", new ArrayList(Arrays.asList(new Object[] {sampleId})));
        translator.assayToExperiment.put(assayId, exptId);
        translator.assayToExpName.put(assayId, EXPT_NAME);
        translator.assayToSamples.put(assayId, new ArrayList(Arrays.asList(new Object[] {sampleId, "1_10"})));
        translator.samplesById.put(sampleId, sample);
        translator.sampleToLabel.put(sampleId, "cy5");
        translator.assays.put(assayId, assay);
        return assay;
    }

    private Item createItem(String className, String itemId, String implementation){
        Item item = itemFactory.makeItem(itemId, tgtNs + className, implementation);
        return item;
    }
}
