package org.intermine.dataconversion;

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

import java.io.StringReader;
import java.util.*;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;

import org.intermine.ontology.OntologyUtil;
import org.intermine.ontology.SubclassRestriction;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;

public class DataTranslatorTest extends TestCase
{
    private String srcNs = "http://www.intermine.org/source#";
    private String nullNs = "http://www.intermine.org/null#";
    private String tgtNs = "http://www.intermine.org/target#";
    private DataTranslator translator;
    protected Map itemMap;

    public void setUp() throws Exception {
        itemMap = new HashMap();
    }

    public void testTranslateItems() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = new Item();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Item src3 = new Item();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");

        Collection srcItems = new ArrayList();
        srcItems.add(ItemHelper.convert(src1));
        srcItems.add(ItemHelper.convert(src2));
        srcItems.add(ItemHelper.convert(src3));
        new MockItemWriter(itemMap).storeAll(srcItems);

        Item tgt1 = new Item();
        tgt1.setIdentifier("1");
        tgt1.setClassName(tgtNs + "Company");
        Item tgt2 = new Item();
        tgt2.setIdentifier("2");
        tgt2.setClassName(tgtNs + "Address");
        Item tgt3 = new Item();
        tgt3.setIdentifier("3");
        tgt3.setClassName(tgtNs + "Department");
        Set expected = new HashSet(Arrays.asList(new Object[] {tgt1, tgt2, tgt3}));

        translator = new DataTranslator(new MockItemReader(itemMap), getInterMineOwl(), tgtNs);
        MockItemWriter tgtIs = new MockItemWriter(new HashMap());
        translator.translate(tgtIs);
        assertEquals(expected, tgtIs.getItems());
    }

    public void testTranslateItemSimple() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");

        translator = new DataTranslator(null, getInterMineOwl(), tgtNs);
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemAttributes() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Attribute a1 = new Attribute();
        a1.setName("name");
        a1.setValue("testname");
        src1.addAttribute(a1);

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Attribute a2 = new Attribute();
        a2.setName("name");
        a2.setValue("testname");
        expected.addAttribute(a2);

        translator = new DataTranslator(null, getInterMineOwl(), tgtNs);
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemNullAttributes() throws Exception {
        Item src1 = new Item();
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



        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Attribute ea1 = new Attribute();
        ea1.setName("name");
        ea1.setValue("testname");
        expected.addAttribute(ea1);

        translator = new DataTranslator(null, getInterMineOwl(), tgtNs);
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }


    public void testTranslateItemReferences() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = new Item();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Reference r2 = new Reference();
        r2.setName("address");
        r2.setRefId("2");
        expected.addReference(r2);

        translator = new DataTranslator(null, getInterMineOwl(), tgtNs);
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemCollections() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");

        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addRefId("2");
        r1.addRefId("3");
        src1.addCollection(r1);

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        ReferenceList r2 = new ReferenceList();
        r2.setName("departments");
        r2.addRefId("2");
        r2.addRefId("3");
        expected.addCollection(r2);

        translator = new DataTranslator(null, getInterMineOwl(), tgtNs);
        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemSubclass() throws Exception {
        String ENDL = System.getProperty("line.separator");

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL
            + "@prefix null: <" + nullNs + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ;" + ENDL
            + "              owl:equivalentClass src:Organisation ." + ENDL
            + "src:Organisation a owl:Class." + ENDL
            + "src:Organisation__name a owl:DatatypeProperty;" + ENDL
            + "       rdfs:domain src:Organisation;" + ENDL
            + "       rdfs:range xsd:string." + ENDL
            + ":Organisation__name a owl:DatatypeProperty;" + ENDL
            + "    rdfs:domain :Organisation;" + ENDL
            + "    rdfs:range xsd:string;" + ENDL
            + "    owl:equivalentProperty src:Organisation__name." + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "    rdfs:subClassOf :Organisation ;" + ENDL
            + "    owl:equivalentClass src:LtdCompany ." + ENDL
            + "src:LtdCompany a owl:Class;" + ENDL
            + "    rdfs:subClassOf src:Organisation." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");


        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Attribute a1 = new Attribute("name", "LtdCompanyName");
        src1.addAttribute(a1);

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        Attribute aa1 = new Attribute("name", "LtdCompanyName");
        expected.addAttribute(a1);

        translator = new DataTranslator(null, ont, tgtNs);

        System.out.println("templateMap: " + translator.templateMap.toString());
        System.out.println("restrictionMap: " + translator.restrictionMap.toString());
        System.out.println("equivMap: " + translator.equivMap.toString());
        System.out.println("clsPropMap: " + translator.clsPropMap.toString());

        assertEquals(expected, translator.translateItem(src1).iterator().next());
    }

    public void testTranslateItemRestrictedSubclassSingleLevel() throws Exception {
        String ENDL = System.getProperty("line.separator");

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL + ENDL

            + "src:Organisation a owl:Class ." + ENDL
            + ":Organisation a owl:Class ;" + ENDL
            + "    owl:equivalentClass src:Organisation ." + ENDL
            + ":Company a owl:Class ; " + ENDL
            + "    rdfs:subClassOf src:Organisation ;" + ENDL
            + "    rdfs:subClassOf" + ENDL
            + "    [ a owl:Restriction ;" + ENDL
            + "        owl:onProperty src:Organisation__organisationType ;" + ENDL
            + "        owl:hasValue \"business\" ] ." + ENDL
            + ":Charity a owl:Class ; " + ENDL
            + "    rdfs:subClassOf src:Organisation ;" + ENDL
            + "    rdfs:subClassOf" + ENDL
            + "    [ a owl:Restriction ;" + ENDL
            + "        owl:onProperty src:Organisation__organisationType ;" + ENDL
            + "        owl:hasValue \"charity\" ] ;" + ENDL
            + "        rdfs:subClassOf" + ENDL
            + "        [ a owl:Restriction ;" + ENDL
            + "            owl:onProperty src:Organisation__otherProp ;" + ENDL
            + "            owl:hasValue \"value\" ] ." + ENDL

            + "src:Organisation__organisationType a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain src:Organisation ;" + ENDL
            + "      rdfs:range rdfs:Literal ." + ENDL
            + ":Organisation__organisationType a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Organisation ;" + ENDL
            + "      rdfs:range rdfs:Literal ;" + ENDL
            + "      owl:equivalentProperty src:Organisation__organisationType ." + ENDL
            + ":Charity__organisationType a owl:DatatypeProperty ;" + ENDL
            + "                          rdfs:domain :Charity ; " + ENDL
            + "                          rdfs:range rdfs:Literal ;" + ENDL
            + "       rdfs:subPropertyOf :Organisation__organisationType ;" + ENDL
            + "       owl:equivalentProperty src:Organisation__organisationType ." + ENDL
            + ":Company__organisationType a owl:DatatypeProperty ;" + ENDL
            + "       rdfs:domain :Company ; " + ENDL
            + "       rdfs:range rdfs:Literal ;" + ENDL
            + "       rdfs:subPropertyOf :Organisation__organisationType ;" + ENDL
            + "       owl:equivalentProperty src:Organisation__organisationType ." + ENDL

            + "src:Organisation__otherProp a owl:DatatypeProperty ;" + ENDL
            + "    rdfs:domain src:Organisation ;" + ENDL
            + "    rdfs:range rdfs:Literal ." + ENDL
            + ":Organisation__otherProp a owl:DatatypeProperty ;" + ENDL
            + "    rdfs:domain :Organisation ;" + ENDL
            + "    rdfs:range rdfs:Literal ;" + ENDL
            + "    owl:equivalentProperty src:Organisation__otherProp ." + ENDL
            + ":Charity__otherProp a owl:DatatypeProperty ;" + ENDL
            + "    rdfs:domain :Charity ; " + ENDL
            + "    rdfs:range rdfs:Literal ;" + ENDL
            + "    rdfs:subPropertyOf :Organisation__otherProp ;" + ENDL
            + "    owl:equivalentProperty src:Organisation__otherProp ." + ENDL
            + ":Company__otherProp a owl:DatatypeProperty ;" + ENDL
            + "    rdfs:domain :Company ; " + ENDL
            + "    rdfs:range rdfs:Literal ;" + ENDL
            + "    rdfs:subPropertyOf :Organisation__otherProp ;" + ENDL
            + "    owl:equivalentProperty src:Organisation__otherProp ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        Map classesMap = new HashMap();
        Iterator clsIter = model.listClasses();
        while (clsIter.hasNext()) {
            OntClass cls = (OntClass) clsIter.next();
            if (!cls.isAnon()) {
                Set subs = OntologyUtil.findRestrictedSubclasses(model, cls);
                if (subs.size() > 0) {
                    classesMap.put(cls, subs);
                }
            }
        }

        // maps to business
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "Organisation");
        src1.setImplementations("");
        Attribute a1 = new Attribute();
        a1.setName("organisationType");
        a1.setValue("business");
        src1.addAttribute(a1);

        // maps to charity
        Item src2 = new Item();
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
        Item src3 = new Item();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Organisation");
        src3.setImplementations("");
        Attribute a3 = new Attribute();
        a3.setName("organisationType");
        a3.setValue("charity");
        src3.addAttribute(a3);

        // remain as organisation, organisationType has 'unspecified' value
        Item src4 = new Item();
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
        Item exp1 = new Item();
        exp1.setIdentifier("1");
        exp1.setClassName(tgtNs + "Company");
        Attribute ea1 = new Attribute();
        ea1.setName("organisationType");
        ea1.setValue("business");
        exp1.addAttribute(ea1);
        Item exp2 = new Item();
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
        Item exp3 = new Item();
        exp3.setIdentifier("3");
        exp3.setClassName(tgtNs + "Organisation");
        Attribute ea3 = new Attribute();
        ea3.setName("organisationType");
        ea3.setValue("charity");
        exp3.addAttribute(ea3);
        Item exp4 = new Item();
        exp4.setIdentifier("4");
        exp4.setClassName(tgtNs + "Organisation");
        Attribute ea4 = new Attribute();
        ea4.setName("organisationType");
        ea4.setValue("other");
        exp4.addAttribute(ea4);
        Set expected = new HashSet(Arrays.asList(new Object[] {exp1, exp2, exp3, exp4}));

        translator = new DataTranslator(new MockItemReader(itemMap), model, tgtNs);

//          System.out.println("templateMap: " + translator.templateMap.toString());
//          System.out.println("restrictionMap: " + translator.restrictionMap.toString());
//          System.out.println("equivMap: " + translator.equivMap.toString());
//          System.out.println("clsPropMap: " + translator.clsPropMap.toString());

        MockItemWriter tgtIs = new MockItemWriter(new HashMap());
        translator.translate(tgtIs);
        assertEquals(expected, tgtIs.getItems());
    }

    public void testGetRestrictionSubclassNested() throws Exception {
        String ENDL = System.getProperty("line.separator");

        // add max cardinality one restrictions
        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL

            + "src:Organisation a owl:Class ." + ENDL
            + "src:Organisation__organisationType a owl:ObjectProperty ;" + ENDL
            + "    rdfs:domain src:Organisation ;" + ENDL
            + "    rdfs:range src:OrganisationType ." + ENDL
            + "src:CompanyModel a owl:Class ." + ENDL
            + "src:CompanyModel__model a owl:DatatypeProperty ;" + ENDL
            + "    rdfs:domain src:CompanyModel ;" + ENDL
            + "    rdfs:range xsd:String ." + ENDL
            + "src:OrganisationType a owl:Class ." + ENDL
            + "src:OrganisationType__companyModel a owl:ObjectProperty ;" + ENDL
            + "    rdfs:domain src:OrganisationType ;" + ENDL
            + "    rdfs:range src:CompanyModel ." + ENDL
            + "src:OrganisationType__type a owl:DatatypeProperty ;" + ENDL
            + "    rdfs:domain src:OrganisationType ;" + ENDL
            + "    rdfs:range xsd:String ." + ENDL


            + ":Organisation a owl:Class ;" + ENDL
            + "      owl:equivalentClass src:Organisation ." + ENDL
            + ":OrganisationType a owl:Class ;" + ENDL
            + "      owl:equivalentClass src:OrganisationType ." + ENDL
            + ":CompanyModel a owl:Class ;" + ENDL
            + "      owl:equivalentClass src:CompanyModel ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf src:Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty src:Organisation__organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [  rdfs:subClassOf src:OrganisationType ;" + ENDL
            + "                  rdfs:subClassOf" + ENDL
            + "                    [ a owl:Restriction ;" + ENDL
            + "                      owl:onProperty src:OrganisationType__type ;" + ENDL
            + "                      owl:hasValue \"business\"" + ENDL
            + "                    ] " + ENDL
            + "               ] " + ENDL
            + "            ] ." + ENDL
            + ":PrivateBusiness a owl:Class ; " + ENDL
            + "          rdfs:subClassOf src:Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty src:Organisation__organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [  rdfs:subClassOf src:OrganisationType ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty src:OrganisationType__companyModel ;" + ENDL
            + "                       owl:hasValue"
            + "                         [ rdfs:subClassOf src:CompanyModel ;" + ENDL
            + "                           rdfs:subClassOf" + ENDL
            + "                             [ a owl:Restriction ;" + ENDL
            + "                               owl:onProperty src:CompanyModel__model ;" + ENDL
            + "                               owl:hasValue \"limited\"" + ENDL
            + "                             ] " + ENDL
            + "                         ] " + ENDL
            + "                     ] ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty src:OrganisationType__type ;" + ENDL
            + "                       owl:hasValue \"business\"" + ENDL
            + "                     ] " + ENDL
            + "                ] " + ENDL
            + "            ] ." + ENDL

            + ":Organisation__organisationType a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :Organisation ;" + ENDL
            + "      rdfs:range :OrganisationType ;" + ENDL
            + "      owl:equivalentProperty src:Organisation__organisationType ." + ENDL
            + ":Business__organisationType a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :Business ;" + ENDL
            + "      rdfs:range :OrganisationType ;" + ENDL
            + "      rdfs:subPropertyOf :Organisation__organisationType ;" + ENDL
            + "      owl:equivalentProperty src:Organisation__organisationType ." + ENDL
            + ":PrivateBusiness__organisationType a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :PrivateBusiness ;" + ENDL
            + "      rdfs:range :OrganisationType ;" + ENDL
            + "      rdfs:subPropertyOf :Organisation__organisationType ;" + ENDL
            + "      owl:equivalentProperty src:Organisation__organisationType ." + ENDL

            + ":OrganisationType__type a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :OrganisationType ;" + ENDL
            + "      rdfs:range xsd:String ;" + ENDL
            + "      owl:equivalentProperty src:OrganisationType__type ." + ENDL
            + ":OrganisationType__companyModel a owl:ObjectProperty ;" + ENDL
            + "              rdfs:domain :OrganisationType ;" + ENDL
            + "              rdfs:range :CompanyModel ;" + ENDL
            + "              owl:equivalentProperty src:OrganisationType__companyModel ." + ENDL

            + ":CompanyModel__model a owl:DatatypeProperty ;" + ENDL
            + "       rdfs:domain :CompanyModel ;" + ENDL
            + "       rdfs:range xsd:String ;" + ENDL
            + "      owl:equivalentProperty src:CompanyModel__model ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        // maps to business
        Item src11 = new Item();
        src11.setIdentifier("11");
        src11.setClassName(srcNs + "Organisation");
        src11.setImplementations("");
        Item src12 = new Item();
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
        Item src21 = new Item();
        src21.setIdentifier("21");
        src21.setClassName(srcNs + "Organisation");
        src21.setImplementations("");
        Reference r21 = new Reference();
        r21.setName("organisationType");
        r21.setRefId("22");
        src21.addReference(r21);

        Item src22 = new Item();
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

        Item src23 = new Item();
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
        Item exp11 = new Item();
        exp11.setIdentifier("11");
        exp11.setClassName(tgtNs + "Business");
        exp11.setImplementations("");
        Item exp12 = new Item();
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

        Item exp21 = new Item();
        exp21.setIdentifier("21");
        exp21.setClassName(tgtNs + "PrivateBusiness");
        exp21.setImplementations("");
        Item exp22 = new Item();
        exp22.setIdentifier("22");
        exp22.setClassName(tgtNs + "OrganisationType");
        exp22.setImplementations("");
        Item exp23 = new Item();
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

        translator = new DataTranslator(new MockItemReader(itemMap), model, tgtNs);

//         System.out.println("templateMap: " + translator.templateMap.toString());
//         System.out.println("restrictionMap: " + translator.restrictionMap.toString());
//         System.out.println("equivMap: " + translator.equivMap.toString());
//         System.out.println("clsPropMap: " + translator.clsPropMap.toString());

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
        translator = new DataTranslator(new MockItemReader(itemMap), getInterMineOwl(), tgtNs);
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
        translator = new DataTranslator(new MockItemReader(itemMap), getInterMineOwl(), tgtNs);

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
        Item src1 = new Item("1", srcNs + "src1", "");

        Item src2 = new Item("2", srcNs + "src2", "");

        Attribute a1 = new Attribute("a1", "attribute a1");
        src2.addAttribute(a1);

        Reference r1 = new Reference("r1", "3");
        src2.addReference(r1);

        ReferenceList c1 = new ReferenceList("c1", Collections.singletonList("3"));
        src2.addCollection(c1);

        Reference toSrc2 = new Reference("toSrc2", "2");
        src1.addReference(toSrc2);

        Item exp1 = new Item("1", srcNs + "src1", "");
        exp1.addReference(toSrc2);
        exp1.addAttribute(a1);

        Item tgt = new Item("1", srcNs + "src1", "");
        tgt.addReference(toSrc2);

        ItemWriter itemWriter = new MockItemWriter(itemMap);
        itemWriter.store(ItemHelper.convert(src1));
        itemWriter.store(ItemHelper.convert(src2));
        translator = new DataTranslator(new MockItemReader(itemMap), getInterMineOwl(), tgtNs);
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
        Item src1 = new Item("2", srcNs + "src1", "");

        Attribute a1 = new Attribute("a1", "attribute a1");
        src1.addAttribute(a1);

        Reference r1 = new Reference("r1", "3");
        src1.addReference(r1);

        ReferenceList c1 = new ReferenceList("c1", Collections.singletonList("3"));
        src1.addCollection(c1);

        ItemWriter itemWriter = new MockItemWriter(itemMap);
        itemWriter.store(ItemHelper.convert(src1));
        translator = new DataTranslator(new MockItemReader(itemMap), getInterMineOwl(), tgtNs);


        Item exp1 = new Item("1", srcNs + "src1", "");
        exp1.addAttribute(new Attribute("new_a1", "attribute a1"));

        Item tgt = new Item("1", srcNs + "src1", "");

        translator.moveField(src1, tgt, "a1", "new_a1");
        assertEquals(exp1, tgt);

        // test with field name that does not exist
        translator.moveField(src1, tgt, "not_a_field", "not_a_field");
        assertEquals(exp1, tgt);

        exp1.addReference(new Reference("new_r1", "3"));
        translator.moveField(src1, tgt, "r1", "new_r1");
        assertEquals(exp1, tgt);

        exp1.addCollection(new ReferenceList("new_c1", Collections.singletonList("3")));
        translator.moveField(src1, tgt, "c1", "new_c1");
        assertEquals(exp1, tgt);
    }



    public Map getSrcItems() {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "Organisation");
        src1.setImplementations("");
        Attribute a1 = new Attribute();
        a1.setName("organisationType");
        a1.setValue("business");
        src1.addAttribute(a1);


        Item src11 = new Item();
        src11.setIdentifier("11");
        src11.setClassName(srcNs + "Organisation");
        src11.setImplementations("");
        Item src12 = new Item();
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


        Item src21 = new Item();
        src21.setIdentifier("21");
        src21.setClassName(srcNs + "Organisation");
        src21.setImplementations("");
        Item src22 = new Item();
        src22.setIdentifier("22");
        src22.setClassName(srcNs + "OrganisationType");
        src22.setImplementations("");
        Item src23 = new Item();
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

    private OntModel getInterMineOwl() {
        String ENDL = System.getProperty("line.separator");

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL
            + "@prefix null: <" + nullNs + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class;" + ENDL
            + "    owl:equivalentClass src:Organisation." + ENDL
            + ":Company a owl:Class;" + ENDL
            + "         rdfs:subClassOf :Organisation ;" + ENDL
            + "         rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :Company_address ] ;" + ENDL
            + "         owl:equivalentClass src:LtdCompany ." + ENDL
            + ":Company__name a owl:DatatypeProperty ;" + ENDL
            + "              rdfs:domain :Company ;" + ENDL
            + "              rdfs:range xsd:string ;" + ENDL
            + "              owl:equivalentProperty src:LtdCompany__name ." + ENDL
            + "null:LtdCompany__vatNumber a owl:DatatypeProperty ;" + ENDL
            + "              owl:equivalentProperty src:LtdCompany__vatNumber ." + ENDL
            + ":Address a owl:Class ;" + ENDL
            + "         owl:equivalentClass src:Address ." + ENDL
            + ":Department a owl:Class ;" + ENDL
            + "            owl:equivalentClass src:Department ." + ENDL
            + ":Company__address a owl:ObjectProperty ;" + ENDL
            + "                 rdfs:domain :Company ;" + ENDL
            + "                 rdfs:range :Address ;" + ENDL
            + "                 owl:equivalentProperty src:LtdCompany__address ." + ENDL
            + ":Company__departments a owl:ObjectProperty ;" + ENDL
            + "                     rdfs:domain :Company ;" + ENDL
            + "                     rdfs:range :Address ;" + ENDL
            + "                     owl:equivalentProperty src:LtdCompany__departments ." + ENDL
            + "src:Organisation a owl:Class." + ENDL
            + "src:Address a owl:Class." + ENDL
            + "src:LtdCompany a owl:Class;" + ENDL
            + "    rdfs:subClassOf" + ENDL
            + "       [ a owl:Restriction ;" + ENDL
            + "           owl:maxCardinality \"1\" ;" + ENDL
            + "           owl:onProperty :Company_address ] ." + ENDL
            + "src:LtdCompany__name a owl:DatatypeProperty;" + ENDL
            + "    rdfs:domain src:LtdCompany;" + ENDL
            + "    rdfs:range xsd:string." + ENDL
            + "src:LtdCompany__vatNumber a owl:DatatypeProperty;" + ENDL
            + "    rdfs:domain src:LtdCompany;" + ENDL
            + "    rdfs:range xsd:integer." + ENDL
            + "src:LtdCompany__address a owl:ObjectProperty;" + ENDL
            + "    rdfs:domain :Company;" + ENDL
            + "    rdfs:range :Address." + ENDL
            + "src:LtdCompany__departments a owl:ObjectProperty;" + ENDL
            + "    rdfs:domain :Company;" + ENDL
            + "    rdfs:range :Address." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");
        return ont;
    }
}
