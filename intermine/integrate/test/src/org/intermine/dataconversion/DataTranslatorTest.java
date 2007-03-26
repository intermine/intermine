package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.ontology.SubclassRestriction;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

public class DataTranslatorTest extends TestCase
{
    private String srcNs = "http://www.intermine.org/source#";
    private String tgtNs = "http://www.intermine.org/target#";
    private DataTranslator translator;
    protected Map itemMap;
    private ItemFactory itemFactory = new ItemFactory();

    public void setUp() throws Exception {
        itemMap = new HashMap();
    }

    public void testTranslateItems() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");

        Collection srcItems = new ArrayList();
        srcItems.add(ItemHelper.convert(src1));
        srcItems.add(ItemHelper.convert(src2));
        srcItems.add(ItemHelper.convert(src3));
        new MockItemWriter(itemMap).storeAll(srcItems);

        Item tgt1 = itemFactory.makeItem();
        tgt1.setIdentifier("1");
        tgt1.setClassName(tgtNs + "Company");
        Item tgt2 = itemFactory.makeItem();
        tgt2.setIdentifier("2");
        tgt2.setClassName(tgtNs + "Address");
        Item tgt3 = itemFactory.makeItem();
        tgt3.setIdentifier("3");
        tgt3.setClassName(tgtNs + "Department");
        Set expected = new HashSet(Arrays.asList(new Object[] {tgt1, tgt2, tgt3}));

        translator = new DataTranslator(new MockItemReader(itemMap), getMappings(), getSrcModel(), getTgtModel());
        MockItemWriter tgtIs = new MockItemWriter(new HashMap());
        translator.translate(tgtIs);
        assertEquals(expected, tgtIs.getItems());
    }

    public void testTranslateItemSimple() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");

        Item expected = itemFactory.makeItem();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");

        translator = new DataTranslator(null, getMappings(), getSrcModel(), getTgtModel());
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemAttributes() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Attribute a1 = new Attribute();
        a1.setName("name");
        a1.setValue("testname");
        src1.addAttribute(a1);

        Item expected = itemFactory.makeItem();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Attribute a2 = new Attribute();
        a2.setName("name");
        a2.setValue("testname");
        expected.addAttribute(a2);

        translator = new DataTranslator(null, getMappings(), getSrcModel(), getTgtModel());
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemNullAttributes() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Attribute a1 = new Attribute();
        a1.setName("name");
        a1.setValue("testname");
        src1.addAttribute(a1);
        Attribute a2 = new Attribute();
        a2.setName("vatNumber");
        a2.setValue("10");
        src1.addAttribute(a2);

        Item expected = itemFactory.makeItem();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Attribute ea1 = new Attribute();
        ea1.setName("name");
        ea1.setValue("testname");
        expected.addAttribute(ea1);

        translator = new DataTranslator(null, getMappings(), getSrcModel(), getTgtModel());
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemReferences() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);

        Item expected = itemFactory.makeItem();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Reference r2 = new Reference();
        r2.setName("address");
        r2.setRefId("2");
        expected.addReference(r2);

        translator = new DataTranslator(null, getMappings(), getSrcModel(), getTgtModel());
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemCollections() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");

        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addRefId("2");
        r1.addRefId("3");
        src1.addCollection(r1);

        Item expected = itemFactory.makeItem();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        ReferenceList r2 = new ReferenceList();
        r2.setName("departments");
        r2.addRefId("2");
        r2.addRefId("3");
        expected.addCollection(r2);

        translator = new DataTranslator(null, getMappings(), getSrcModel(), getTgtModel());
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemSubclass() throws Exception {
        Properties mappings = new Properties();
        mappings.put("Organisation", "Organisation");
        mappings.put("Organisation.name", "Organisation.name");
        mappings.put("Company", "LtdCompany");
        //hoping that Company.name -> LtdCompany.name as Company subclasses Organisation

        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Attribute a1 = new Attribute("name", "LtdCompanyName");
        src1.addAttribute(a1);

        Item expected = itemFactory.makeItem();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Attribute aa1 = new Attribute("name", "LtdCompanyName");
        expected.addAttribute(a1);

        translator = new DataTranslator(null, mappings, getSrcModel(), getTgtModel());

        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemRestrictedSubclassSingleLevel() throws Exception {
        Properties mappings = new Properties();
        mappings.put("Organisation", "Organisation");
        mappings.put("Organisation.organisationType", "Organisation.organisationType");
        mappings.put("Organisation.otherProp", "Organisation.otherProp");
        mappings.put("Company", "Organisation [organisationType=business]");
        mappings.put("Charity", "Organisation [organisationType=charity] [otherProp=value]");

        // maps to business
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "Organisation");
        src1.setImplementations("");
        Attribute a1 = new Attribute();
        a1.setName("organisationType");
        a1.setValue("business");
        src1.addAttribute(a1);

        // maps to charity
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Organisation");
        src2.setImplementations("");
        Attribute a2 = new Attribute();
        a2.setName("organisationType");
        a2.setValue("charity");
        src2.addAttribute(a2);
        Attribute a2a = new Attribute();
        a2a.setName("otherProp");
        a2a.setValue("value");
        src2.addAttribute(a2a);

        // remain as organisation, only has one of Charity restrictions
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Organisation");
        src3.setImplementations("");
        Attribute a3 = new Attribute();
        a3.setName("organisationType");
        a3.setValue("charity");
        src3.addAttribute(a3);

        // remain as organisation, organisationType has 'unspecified' value
        Item src4 = itemFactory.makeItem();
        src4.setIdentifier("4");
        src4.setClassName(srcNs + "Organisation");
        src4.setImplementations("");
        Attribute a4 = new Attribute();
        a4.setName("organisationType");
        a4.setValue("other");
        src4.addAttribute(a4);
        Collection srcItems = new ArrayList();
        srcItems.add(ItemHelper.convert(src1));
        srcItems.add(ItemHelper.convert(src2));
        srcItems.add(ItemHelper.convert(src3));
        srcItems.add(ItemHelper.convert(src4));
        new MockItemWriter(itemMap).storeAll(srcItems);

        // expected items
        Item exp1 = itemFactory.makeItem();
        exp1.setIdentifier("1");
        exp1.setClassName(tgtNs + "Company");
        Attribute ea1 = new Attribute();
        ea1.setName("organisationType");
        ea1.setValue("business");
        exp1.addAttribute(ea1);
        Item exp2 = itemFactory.makeItem();
        exp2.setIdentifier("2");
        exp2.setClassName(tgtNs + "Charity");
        Attribute ea2 = new Attribute();
        ea2.setName("organisationType");
        ea2.setValue("charity");
        Attribute ea2a = new Attribute();
        ea2a.setName("otherProp");
        ea2a.setValue("value");
        exp2.addAttribute(ea2);
        exp2.addAttribute(ea2a);
        Item exp3 = itemFactory.makeItem();
        exp3.setIdentifier("3");
        exp3.setClassName(tgtNs + "Organisation");
        Attribute ea3 = new Attribute();
        ea3.setName("organisationType");
        ea3.setValue("charity");
        exp3.addAttribute(ea3);
        Item exp4 = itemFactory.makeItem();
        exp4.setIdentifier("4");
        exp4.setClassName(tgtNs + "Organisation");
        Attribute ea4 = new Attribute();
        ea4.setName("organisationType");
        ea4.setValue("other");
        exp4.addAttribute(ea4);
        Set expected = new HashSet(Arrays.asList(new Object[] {exp1, exp2, exp3, exp4}));

        translator = new DataTranslator(new MockItemReader(itemMap), mappings,
                                        getSrcModel(), getTgtModel2());

        MockItemWriter tgtIs = new MockItemWriter(new HashMap());
        translator.translate(tgtIs);
        assertEquals(expected, tgtIs.getItems());
    }

    public void testGetRestrictionSubclassNested() throws Exception {
        Properties mappings = new Properties();
        mappings.put("Organisation", "Organisation");
        mappings.put("Business", "Organisation [organisationType.type=business]");
        mappings.put("PrivateBusiness", "Organisation [organisationType.companyModel.model=limited] [organisationType.type=business]");
        mappings.put("Organisation.organisationType", "Organisation.organisationType");
        mappings.put("OrganisationType", "OrganisationType");
        mappings.put("OrganisationType.type", "OrganisationType.type");
        mappings.put("OrganisationType.companyModel", "OrganisationType.companyModel");
        mappings.put("CompanyModel", "CompanyModel");
        mappings.put("CompanyModel.model", "CompanyModel.model");

        ReferenceDescriptor ref1 = new ReferenceDescriptor("organisationType", "org.intermine.source.OrganisationType", null);
        ClassDescriptor cld1 = new ClassDescriptor("org.intermine.source.Organisation", null, false,
                                                   new HashSet(),
                                                   new HashSet(Arrays.asList(new Object[] {ref1})),
                                                   new HashSet());
        AttributeDescriptor att1 = new AttributeDescriptor("model", "java.lang.String");
        ClassDescriptor cld2 = new ClassDescriptor("org.intermine.source.CompanyModel", null, false,
                                                   new HashSet(Arrays.asList(new Object[] {att1})),
                                                   new HashSet(),
                                                   new HashSet());
        AttributeDescriptor att2 = new AttributeDescriptor("type", "java.lang.String");
        ReferenceDescriptor ref2 = new ReferenceDescriptor("companyModel", "org.intermine.source.CompanyModel", null);
        ClassDescriptor cld3 = new ClassDescriptor("org.intermine.source.OrganisationType", null, false,
                                                   new HashSet(Arrays.asList(new Object[] {att2})),
                                                   new HashSet(Arrays.asList(new Object[] {ref2})),
                                                   new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3}));
        Model srcModel = new Model("modelname", srcNs, clds);

        // maps to business
        Item src11 = itemFactory.makeItem();
        src11.setIdentifier("11");
        src11.setClassName(srcNs + "Organisation");
        src11.setImplementations("");
        Item src12 = itemFactory.makeItem();
        src12.setIdentifier("12");
        src12.setClassName(srcNs + "OrganisationType");
        src12.setImplementations("");
        Reference r11 = new Reference();
        r11.setName("organisationType");
        r11.setRefId("12");
        src11.addReference(r11);
        Attribute a11 = new Attribute();
        a11.setName("type");
        a11.setValue("business");
        src12.addAttribute(a11);

        // maps to private business
        Item src21 = itemFactory.makeItem();
        src21.setIdentifier("21");
        src21.setClassName(srcNs + "Organisation");
        src21.setImplementations("");
        Reference r21 = new Reference();
        r21.setName("organisationType");
        r21.setRefId("22");
        src21.addReference(r21);

        Item src22 = itemFactory.makeItem();
        src22.setIdentifier("22");
        src22.setClassName(srcNs + "OrganisationType");
        src22.setImplementations("");
        Attribute a22 = new Attribute();
        a22.setName("type");
        a22.setValue("business");
        src22.addAttribute(a22);
        Reference r22 = new Reference();
        r22.setName("companyModel");
        r22.setRefId("23");
        src22.addReference(r22);

        Item src23 = itemFactory.makeItem();
        src23.setIdentifier("23");
        src23.setClassName(srcNs + "CompanyModel");
        src23.setImplementations("");
        Attribute a23 = new Attribute();
        a23.setName("model");
        a23.setValue("limited");
        src23.addAttribute(a23);

        Collection srcItems = new ArrayList();
        srcItems.add(ItemHelper.convert(src11));
        srcItems.add(ItemHelper.convert(src12));
        srcItems.add(ItemHelper.convert(src21));
        srcItems.add(ItemHelper.convert(src22));
        srcItems.add(ItemHelper.convert(src23));
        new MockItemWriter(itemMap).storeAll(srcItems);

        // expected items
        Item exp11 = itemFactory.makeItem();
        exp11.setIdentifier("11");
        exp11.setClassName(tgtNs + "Business");
        exp11.setImplementations("");
        Item exp12 = itemFactory.makeItem();
        exp12.setIdentifier("12");
        exp12.setClassName(tgtNs + "OrganisationType");
        exp12.setImplementations("");
        Reference er11 = new Reference();
        er11.setName("organisationType");
        er11.setRefId("12");
        exp11.addReference(er11);
        Attribute ea11 = new Attribute();
        ea11.setName("type");
        ea11.setValue("business");
        exp12.addAttribute(ea11);

        Item exp21 = itemFactory.makeItem();
        exp21.setIdentifier("21");
        exp21.setClassName(tgtNs + "PrivateBusiness");
        exp21.setImplementations("");
        Item exp22 = itemFactory.makeItem();
        exp22.setIdentifier("22");
        exp22.setClassName(tgtNs + "OrganisationType");
        exp22.setImplementations("");
        Item exp23 = itemFactory.makeItem();
        exp23.setIdentifier("23");
        exp23.setClassName(tgtNs + "CompanyModel");
        exp23.setImplementations("");
        Reference er21 = new Reference();
        er21.setName("organisationType");
        er21.setRefId("22");
        exp21.addReference(er21);
        Reference er22 = new Reference();
        er22.setName("companyModel");
        er22.setRefId("23");
        exp22.addReference(er22);
        Attribute ea21 = new Attribute();
        ea21.setName("model");
        ea21.setValue("limited");
        exp23.addAttribute(ea21);
        Attribute ea22 = new Attribute();
        ea22.setName("type");
        ea22.setValue("business");
        exp22.addAttribute(ea22);
        Set expected = new HashSet(Arrays.asList(new Object[] {exp11, exp12, exp21, exp22, exp23}));

        translator = new DataTranslator(new MockItemReader(itemMap), mappings, srcModel, getTgtModel());

        MockItemWriter tgtIs = new MockItemWriter(new HashMap());
        translator.translate(tgtIs);
        assertEquals(expected, tgtIs.getItems());
    }

    public void testBuildRestriction() throws Exception {
        Map srcItems = getSrcItems();
        ItemWriter itemWriter = new MockItemWriter(itemMap);
        for (Iterator i = srcItems.values().iterator(); i.hasNext();) {
            itemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        String path = "Organisation.organisationType";
        StringTokenizer t = new StringTokenizer(path, ".");
        t.nextToken();
        translator = new DataTranslator(new MockItemReader(itemMap), new Properties(), null, null);
        assertEquals("business", translator.buildRestriction(t, (Item) srcItems.get("src1")));

        path = "Organisation.organisationType.type";
        t = new StringTokenizer(path, ".");
        t.nextToken();
        assertEquals("business", translator.buildRestriction(t, (Item) srcItems.get("src11")));

        path = "Organisation.organisationType.type";
        t = new StringTokenizer(path, ".");
        t.nextToken();
        assertEquals("business", translator.buildRestriction(t, (Item) srcItems.get("src21")));

        path = "Organisation.organisationType.companyModel.model";
        t = new StringTokenizer(path, ".");
        t.nextToken();
        assertEquals("limited", translator.buildRestriction(t, (Item) srcItems.get("src21")));
    }

    public void testBuildSubclassRestriction() throws Exception {
        Map srcItems = getSrcItems();
        ItemWriter itemWriter = new MockItemWriter(itemMap);
        for (Iterator i = srcItems.values().iterator(); i.hasNext();) {
            itemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        // model we use here is irrelevant
        translator = new DataTranslator(new MockItemReader(itemMap), new Properties(), null, null);

        SubclassRestriction template1 = new SubclassRestriction();
        template1.addRestriction("Organisation.organisationType", null);
        SubclassRestriction sr1 = new SubclassRestriction();
        sr1.addRestriction("Organisation.organisationType", "business");
        assertEquals(sr1, translator.buildSubclassRestriction((Item) srcItems.get("src1"), template1));

        SubclassRestriction template2 = new SubclassRestriction();
        template2.addRestriction("Organisation.organisationType.type", null);
        SubclassRestriction sr2 = new SubclassRestriction();
        sr2.addRestriction("Organisation.organisationType.type", "business");
        assertEquals(sr2, translator.buildSubclassRestriction((Item) srcItems.get("src11"), template2));

        SubclassRestriction template3 = new SubclassRestriction();
        template3.addRestriction("Organisation.organisationType.type", null);
        template3.addRestriction("Organisation.organisationType.companyModel.model", null);
        SubclassRestriction sr3 = new SubclassRestriction();
        sr3.addRestriction("Organisation.organisationType.type", "business");
        sr3.addRestriction("Organisation.organisationType.companyModel.model", "limited");
        assertEquals(sr3, translator.buildSubclassRestriction((Item) srcItems.get("src21"), template3));
    }

    public void testPromoteField() throws Exception {
        Item src1 = itemFactory.makeItem("1", srcNs + "src1", "");
        Item src2 = itemFactory.makeItem("2", srcNs + "src2", "");
        Attribute a1 = new Attribute("a1", "attribute a1");
        src2.addAttribute(a1);
        Reference r1 = new Reference("r1", "3");
        src2.addReference(r1);
        ReferenceList c1 = new ReferenceList("c1", Collections.singletonList("3"));
        src2.addCollection(c1);
        Reference toSrc2 = new Reference("toSrc2", "2");
        src1.addReference(toSrc2);

        Item exp1 = itemFactory.makeItem("1", srcNs + "src1", "");
        exp1.addReference(toSrc2);
        exp1.addAttribute(a1);

        Item tgt = itemFactory.makeItem("1", srcNs + "src1", "");
        tgt.addReference(toSrc2);

        ItemWriter itemWriter = new MockItemWriter(itemMap);
        itemWriter.store(ItemHelper.convert(src1));
        itemWriter.store(ItemHelper.convert(src2));
        translator = new DataTranslator(new MockItemReader(itemMap), new Properties(), null, null);
        translator.promoteField(tgt, src1, "a1", "toSrc2", "a1");
        assertEquals(exp1, tgt);

        exp1.addReference(r1);
        translator.promoteField(tgt, src1, "r1", "toSrc2", "r1");
        assertEquals(exp1, tgt);

        exp1.addCollection(c1);
        translator.promoteField(tgt, src1, "c1", "toSrc2", "c1");
        assertEquals(exp1, tgt);
    }

    public void testMoveField() throws Exception {
        Item src1 = itemFactory.makeItem("2", srcNs + "src1", "");
        Attribute a1 = new Attribute("a1", "attribute a1");
        src1.addAttribute(a1);
        Reference r1 = new Reference("r1", "3");
        src1.addReference(r1);
        ReferenceList c1 = new ReferenceList("c1", Collections.singletonList("3"));
        src1.addCollection(c1);

        Item exp1 = itemFactory.makeItem("1", srcNs + "src1", "");
        exp1.addAttribute(new Attribute("new_a1", "attribute a1"));

        Item tgt = itemFactory.makeItem("1", srcNs + "src1", "");

        DataTranslator.moveField(src1, tgt, "a1", "new_a1");
        assertEquals(exp1, tgt);

        // test with field name that does not exist
        DataTranslator.moveField(src1, tgt, "not_a_field", "not_a_field");
        assertEquals(exp1, tgt);

        exp1.addReference(new Reference("new_r1", "3"));
        DataTranslator.moveField(src1, tgt, "r1", "new_r1");
        assertEquals(exp1, tgt);

        exp1.addCollection(new ReferenceList("new_c1", Collections.singletonList("3")));
        DataTranslator.moveField(src1, tgt, "c1", "new_c1");
        assertEquals(exp1, tgt);
    }

    public Map getSrcItems() {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "Organisation");
        src1.setImplementations("");
        Attribute a1 = new Attribute();
        a1.setName("organisationType");
        a1.setValue("business");
        src1.addAttribute(a1);

        Item src11 = itemFactory.makeItem();
        src11.setIdentifier("11");
        src11.setClassName(srcNs + "Organisation");
        src11.setImplementations("");
        Item src12 = itemFactory.makeItem();
        src12.setIdentifier("12");
        src12.setClassName(srcNs + "OrganisationType");
        src12.setImplementations("");
        Reference r11 = new Reference();
        r11.setName("organisationType");
        r11.setRefId("12");
        src11.addReference(r11);
        Attribute a11 = new Attribute();
        a11.setName("type");
        a11.setValue("business");
        src12.addAttribute(a11);

        Item src21 = itemFactory.makeItem();
        src21.setIdentifier("21");
        src21.setClassName(srcNs + "Organisation");
        src21.setImplementations("");
        Item src22 = itemFactory.makeItem();
        src22.setIdentifier("22");
        src22.setClassName(srcNs + "OrganisationType");
        src22.setImplementations("");
        Item src23 = itemFactory.makeItem();
        src23.setIdentifier("23");
        src23.setClassName(srcNs + "CompanyModel");
        src23.setImplementations("");
        Reference r21 = new Reference();
        r21.setName("organisationType");
        r21.setRefId("22");
        src21.addReference(r21);
        Reference r22 = new Reference();
        r22.setName("companyModel");
        r22.setRefId("23");
        src22.addReference(r22);
        Attribute a21 = new Attribute();
        a21.setName("model");
        a21.setValue("limited");
        src23.addAttribute(a21);
        Attribute a22 = new Attribute();
        a22.setName("type");
        a22.setValue("business");
        src22.addAttribute(a22);

        Map srcItems = new HashMap();
        srcItems.put("src1", src1);
        srcItems.put("src11", src11);
        srcItems.put("src12", src12);
        srcItems.put("src21", src21);
        srcItems.put("src22", src22);
        srcItems.put("src23", src23);
        return srcItems;
    }

    private Properties getMappings() {
        Properties mappings = new Properties();
        mappings.put("Organisation", "Organisation");
        mappings.put("Company", "LtdCompany");
        mappings.put("Company.address", "LtdCompany.address");
        mappings.put("Company.departments", "LtdCompany.departments");
        mappings.put("Company.name", "LtdCompany.name");
        mappings.put("Address", "Address");
        mappings.put("Department", "Department");
        return mappings;
    }

    /*
    public void testMultipleInclusion() throws Exception{

        Properties mappings = new Properties();
        mappings.put("Charity", "Organisation[organisationType = charity]");
        mappings.put("Company", "Organisation[organisationType = company]");

        HashMap ourItemMap = new HashMap();

        Item org1 = itemFactory.makeItem("1", srcNs + "Organisation", "");
        org1.setAttribute("organisationType", "charity");

        Item org2 = itemFactory.makeItem("2", srcNs + "Organisation", "");
        org2.setAttribute("organisationType", "company");

        Item org3 = itemFactory.makeItem("3", srcNs + "Organisation", "");
        org3.setAttribute("organisationType", "mafia");

        ItemWriter itemWriter = new MockItemWriter(ourItemMap);
        itemWriter.store(ItemHelper.convert(org1));
        itemWriter.store(ItemHelper.convert(org2));
        itemWriter.store(ItemHelper.convert(org3));

        translator = new DataTranslator(new MockItemReader(ourItemMap),
                mappings, getSrcModel(), getTgtModel());

        assertEquals("http://www.intermine.org/target#Charity", translator.getTgtItemClassName(org1));
        assertEquals("http://www.intermine.org/target#Company", translator.getTgtItemClassName(org2));
        assertNull(translator.getTgtItemClassName(org3));
    }


    public void testSingleExclusion() throws Exception{

        Properties mappings = new Properties();
        mappings.put("Company", "Organisation[organisationType != charity]");

        HashMap ourItemMap = new HashMap();

        Item org1 = itemFactory.makeItem("1", srcNs + "Organisation", "");
        org1.setAttribute("organisationType", "charity");

        Item org2 = itemFactory.makeItem("2", srcNs + "Organisation", "");
        org2.setAttribute("organisationType", "company");

        Item org3 = itemFactory.makeItem("3", srcNs + "Organisation", "");
        org3.setAttribute("organisationType", "mafia");

        ItemWriter itemWriter = new MockItemWriter(ourItemMap);
        itemWriter.store(ItemHelper.convert(org1));
        itemWriter.store(ItemHelper.convert(org2));
        itemWriter.store(ItemHelper.convert(org3));

        translator = new DataTranslator(new MockItemReader(ourItemMap),
                mappings, getSrcModel(), getTgtModel());

        assertNull(translator.getTgtItemClassName(org1));
        assertEquals("http://www.intermine.org/target#Company", translator.getTgtItemClassName(org2));
        assertEquals("http://www.intermine.org/target#Company", translator.getTgtItemClassName(org3));
    }
    */


    private Model getSrcModel() throws Exception {
        AttributeDescriptor att1 = new AttributeDescriptor("name", "java.lang.String");
        AttributeDescriptor att2 = new AttributeDescriptor("organisationType", "java.lang.String");
        AttributeDescriptor att3 = new AttributeDescriptor("otherProp", "java.lang.String");
        ClassDescriptor cld1 =
            new ClassDescriptor("org.intermine.source.Organisation", null, false,
                                new HashSet(Arrays.asList(new Object[] {att1, att2, att3})),
                                new HashSet(),
                                new HashSet());
        ReferenceDescriptor ref1 =
            new ReferenceDescriptor("address", "org.intermine.source.Address", null);
        CollectionDescriptor col1 =
            new CollectionDescriptor("departments", "org.intermine.source.Department", null);
        ClassDescriptor cld2 =
            new ClassDescriptor("org.intermine.source.LtdCompany",
                                "org.intermine.source.Organisation", false,
                                new HashSet(),
                                new HashSet(Arrays.asList(new Object[] {ref1})),
                                new HashSet(Arrays.asList(new Object[] {col1})));
        ClassDescriptor cld3 =
            new ClassDescriptor("org.intermine.source.Address", null, false,
                                new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld4 =
            new ClassDescriptor("org.intermine.source.Department", null, false,
                                new HashSet(), new HashSet(), new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3, cld4}));
        return new Model("modelname", srcNs, clds);
    }

    private Model getTgtModel() throws Exception {
        AttributeDescriptor att1 = new AttributeDescriptor("name", "java.lang.String");
        ReferenceDescriptor ref1 =
            new ReferenceDescriptor("address", "org.intermine.target.Address", null);
        CollectionDescriptor col1 =
            new CollectionDescriptor("departments", "org.intermine.target.Department", null);
        ClassDescriptor cld1 =
            new ClassDescriptor("org.intermine.target.Company", null, false,
                                new HashSet(Arrays.asList(new Object[] {att1})),
                                new HashSet(Arrays.asList(new Object[] {ref1})),
                                new HashSet(Arrays.asList(new Object[] {col1})));
        ClassDescriptor cld2 =
            new ClassDescriptor("org.intermine.target.Address", null, false,
                                new HashSet(), new HashSet(), new HashSet());
        ClassDescriptor cld3 =
            new ClassDescriptor("org.intermine.target.Department", null, false,
                                new HashSet(), new HashSet(), new HashSet());
        AttributeDescriptor att2 = new AttributeDescriptor("type", "java.lang.String");
        ReferenceDescriptor ref2 =
            new ReferenceDescriptor("companyModel", "org.intermine.target.CompanyModel", null);
        ClassDescriptor cld4 =
            new ClassDescriptor("org.intermine.target.OrganisationType", null, false,
                                new HashSet(Arrays.asList(new Object[] {att2})),
                                new HashSet(Arrays.asList(new Object[] {ref2})),
                                new HashSet());
        ReferenceDescriptor ref3 =
            new ReferenceDescriptor("organisationType", "org.intermine.target.OrganisationType",
                                    null);
        ClassDescriptor cld5 =
            new ClassDescriptor("org.intermine.target.PrivateBusiness", null, false,
                                new HashSet(),
                                new HashSet(Arrays.asList(new Object[] {ref3})),
                                new HashSet());
        ReferenceDescriptor ref4 =
            new ReferenceDescriptor("organisationType", "org.intermine.target.OrganisationType",
                                    null);
        AttributeDescriptor att4 = new AttributeDescriptor("otherProp", "java.lang.String");
        ClassDescriptor cld6 =
            new ClassDescriptor("org.intermine.target.Charity", null, false,
                                new HashSet(Arrays.asList(new Object[] {att4})),
                                new HashSet(Arrays.asList(new Object[] {ref4})),
                                new HashSet());
        AttributeDescriptor att5 = new AttributeDescriptor("model", "java.lang.String");
        ClassDescriptor cld7 =
            new ClassDescriptor("org.intermine.target.CompanyModel", null, false,
                                new HashSet(Arrays.asList(new Object[] {att5})),
                                new HashSet(),
                                new HashSet());
        ReferenceDescriptor ref5 =
            new ReferenceDescriptor("organisationType", "org.intermine.target.OrganisationType",
                                    null);
        ClassDescriptor cld8 =
            new ClassDescriptor("org.intermine.target.Business", null, false,
                                new HashSet(),
                                new HashSet(Arrays.asList(new Object[]{ref5})),
                                new HashSet());
        Set clds =
            new HashSet(Arrays.asList(new Object[] {cld1, cld2, cld3, cld4,
                                                    cld5, cld6, cld7, cld8}));
        return new Model("target", tgtNs, clds);
    }

    // for testTranslateItemRestrictedSubclassSingleLevel()
    private Model getTgtModel2() throws Exception {
        AttributeDescriptor att0 = new AttributeDescriptor("organisationType", "java.lang.String");
        AttributeDescriptor att1 = new AttributeDescriptor("name", "java.lang.String");
        ReferenceDescriptor ref1 =
            new ReferenceDescriptor("address", "org.intermine.target.Address", null);
        CollectionDescriptor col1 =
            new CollectionDescriptor("departments", "org.intermine.target.Department", null);
        ClassDescriptor cld1 =
            new ClassDescriptor("org.intermine.target.Company", null, false,
                                new HashSet(Arrays.asList(new Object[] {att0, att1})),
                                new HashSet(Arrays.asList(new Object[] {ref1})),
                                new HashSet(Arrays.asList(new Object[] {col1})));
         ClassDescriptor cld2 =
             new ClassDescriptor("org.intermine.target.Address", null, false,
                                 new HashSet(), new HashSet(), new HashSet());
         ClassDescriptor cld3 =
             new ClassDescriptor("org.intermine.target.Department", null, false,
                                 new HashSet(), new HashSet(), new HashSet());
        AttributeDescriptor att2 = new AttributeDescriptor("organisationType", "java.lang.String");
        ClassDescriptor cld4 =
            new ClassDescriptor("org.intermine.target.Organisation", null, false,
                                new HashSet(Arrays.asList(new Object[] {att2})),
                                new HashSet(),
                                new HashSet());
         AttributeDescriptor att4 = new AttributeDescriptor("otherProp", "java.lang.String");
         AttributeDescriptor att5 = new AttributeDescriptor("organisationType", "java.lang.String");
         ClassDescriptor cld6 =
             new ClassDescriptor("org.intermine.target.Charity", null, false,
                                 new HashSet(Arrays.asList(new Object[] {att4, att5})),
                                 new HashSet(Arrays.asList(new Object[] {})),
                                 new HashSet());
        Set clds = new HashSet(Arrays.asList(new Object[] { cld1, cld2, cld3, cld4, cld6 }));
        return new Model("target", tgtNs, clds);
    }
}
