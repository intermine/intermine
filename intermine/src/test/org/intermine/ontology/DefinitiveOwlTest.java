package org.intermine.ontology;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

import java.util.*;
import java.io.*;

import org.intermine.metadata.Model;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

// Test that merges two OWL models together (including restricted subclasses) and takes the
// output to test generation of InterMine model.  Also use output to run DataTranslator
// on items from each src format into target format.
// THE OWL FORMAT AND CONSTRUCTS USED IN MERGE SPEC HERE SHOULD BE TREATED AS DEFINITIVE
// EXAMPLES FOR PRODUCTION SYSTEM

public class DefinitiveOwlTest extends XMLTestCase
{
    private String ENDL = System.getProperty("line.separator");
    private final static String tgtNs = "http://www.intermine.org/model/test#";
    private final static String src1Ns = "http://www.intermine.org/model/source1#";
    private final static String src2Ns = "http://www.intermine.org/model/source2#";
    private final static String nullNs = "http://www.intermine.org/model/null#";
    protected Map itemMap;
    private ItemFactory itemFactory = new ItemFactory();

    public void setUp() throws Exception {
        itemMap = new HashMap();
    }

    // restricted subclasses (must be on references not collections)
    // cannot change the name of properties in restricted subclasses
    // restricted subclass is additionally sub/superclass of something else

    // map a class to null domain
    // map a property to null domain

    // change name of properties

    // change name of classes


    /* POINTS TO NOTE
       --------------
       xsd:String preferable to rdfs:Literal
    */

    public void testMergeOwl() throws Exception {
        OntModel ont = runMergeOwl();
        //ont.write(new FileWriter(new File("targetModel")), "N3");

        // target namespace should contain only these classes
        OntClass orgCls = ont.getOntClass(tgtNs + "Organisation");
        OntClass comCls = ont.getOntClass(tgtNs + "Company");
        OntClass chaCls = ont.getOntClass(tgtNs + "Charity");
        OntClass ltdCls = ont.getOntClass(tgtNs + "LtdCompany");
        OntClass depCls = ont.getOntClass(tgtNs + "Department");
        OntClass addCls = ont.getOntClass(tgtNs + "Address");
        OntClass otCls = ont.getOntClass(tgtNs + "OrganisationType");
        OntClass cmCls = ont.getOntClass(tgtNs + "CompanyModel");
        OntClass bbCls = ont.getOntClass(tgtNs + "BigBusiness");
        Set expected = new HashSet(Arrays.asList(new Object[] {orgCls, comCls, chaCls, ltdCls, depCls, addCls, otCls, cmCls, bbCls}));

        Set classes = new HashSet();
        Iterator i = ont.listClasses();
        while (i.hasNext()) {
            OntClass cls = (OntClass) i.next();
            if (!cls.isAnon() && cls.getNameSpace().equals(tgtNs)) {
                classes.add(cls);
            }
        }
        assertEquals(expected, classes);
    }

    public void testGenerateInterMineModel() throws Exception {
        Model expected  = new InterMineModelParser().process(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("DefinitiveOwlTest_model.xml")));
        Model model = new Owl2InterMine("test", "org.intermine.model.test").process(runMergeOwl(), tgtNs);
        assertEquals(expected, model);
    }

//     public void testDataTranslatorSrc1() throws Exception {
//         Set src1Items = getSrc1Items();
//         ItemStore src1ItemStore = new MockItemStore();
//         storeItems(src1Items, src1ItemStore);

//         Model model = generateInterMineModel();
//         DataTranslator translator2 = new DataTranslator(src1ItemStore, runMergeOwl(), tgtNs);

// //         System.out.println("templateMap: " + translator2.templateMap.toString());
// //         System.out.println("restrictionMap: " + translator2.restrictionMap.toString());
// //         System.out.println("equivMap: " + translator2.equivMap.toString());
// //         System.out.println("clsPropMap: " + translator2.clsPropMap.toString());

//         MockItemStore tgtItemStore = new MockItemStore();
//         translator2.translate(tgtItemStore);

//         Set expected = getSrc1TgtItems();
//         assertEquals(expected, tgtItemStore.getItemSet());

//     }

//     public void testDataTranslatorSrc2() throws Exception {
//         ItemWriter srcItemWriter = new MockItemWriter(itemMap);
//         for (Iterator i = getSrc2Items().iterator(); i.hasNext();) {
//             srcItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

//         DataTranslator translator = new DataTranslator(new MockItemReader(itemMap), runMergeOwl(), tgtNs);
//         MockItemWriter tgtItemWriter = new MockItemWriter(new HashMap());
//         translator.translate(tgtItemWriter);

//         assertEquals(getSrc2TgtItems(), tgtItemWriter.getItems());
//     }

    private OntModel runMergeOwl() throws Exception {
        String src1 = getSrc1Model();
        String src2 = getSrc2Model();
        String mergeSpec = getMergeSpec();

        OntModel testSrc1Model = ModelFactory.createOntologyModel();
        testSrc1Model.read(new StringReader(src1), null, "N3");
        OntModel testSrc2Model = ModelFactory.createOntologyModel();
        testSrc2Model.read(new StringReader(src2), null, "N3");
        OntModel testMergeSpec = ModelFactory.createOntologyModel();
        testMergeSpec.read(new StringReader(mergeSpec), null, "N3");

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNs, true);
        merger.addToTargetOwl(new StringReader(src2), src2Ns, "N3");
        merger.addToTargetOwl(new StringReader(src1), src1Ns, "N3");

        return merger.tgtModel;
    }

    private Model generateInterMineModel() throws Exception {
        Owl2InterMine o2i = new Owl2InterMine("test", "org.intermine.model.test");
        Model model = o2i.process(runMergeOwl(), tgtNs);
        return model;
    }

    private String getMergeSpec() {
        StringBuffer owl = new StringBuffer();

        owl.append("@prefix : <" + tgtNs + "> ." + ENDL
                   + "@prefix src1: <" + src1Ns + "> ." + ENDL + ENDL
                   + "@prefix src2: <" + src2Ns + "> ." + ENDL + ENDL
                   + "@prefix null: <" + nullNs + "> ." + ENDL + ENDL
                   + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
                   + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
                   + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
                   + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL);

        // Change name of a class, all properties should also change their name, restricted
        // subclasses will inherit correctly
        // src2:Org -> :Organisation
        owl.append(":Organisation a owl:Class ;" + ENDL
                   + "      owl:equivalentClass src2:Org ." + ENDL);

        // Company is a restricted subclass of src2:Org
        //    where Organisation.organisationType.type = business
        // in target model automatically becomes a subclass of Organisation
        // also equivalent to src1:Business, should get properties of both.
        owl.append(":Company a owl:Class ;" + ENDL
                   + "         rdfs:subClassOf :Organisation ;" + ENDL
                   + "         rdfs:subClassOf src2:Org ;" + ENDL
                   + "         rdfs:subClassOf" + ENDL
                   + "            [ a owl:Restriction ;" + ENDL
                   + "              owl:onProperty src2:Org__organisationType ;" + ENDL
                   + "              owl:hasValue" + ENDL
                   + "                 [ rdfs:subClassOf src2:OrganisationType ;" + ENDL
                   + "                   rdfs:subClassOf" + ENDL
                   + "                      [ a owl:Restriction ;" + ENDL
                   + "                        owl:onProperty src2:OrganisationType__type ;" + ENDL
                   + "                        owl:hasValue \"business\"" + ENDL
                   + "                      ] " + ENDL
                   + "                 ] " + ENDL
                   + "            ] ;" + ENDL
                   //+ "      owl:equivalentClass src2:Organisation ;" + ENDL
                   + "      owl:equivalentClass src1:Business ." + ENDL);

        // change name of property, do not need to re-state domain and range
        // name is inherited by Company from organisation but need to alter prefix accordingly
        // src1:Busines__companyName -> :Company__name
        owl.append(":Company__name a owl:DatatypeProperty ;" + ENDL
                   + "     rdfs:subPropertyOf :Organisation__name ;" + ENDL
                   + "     rdfs:domain :Company ;" + ENDL
                   + "     rdfs:range xsd:string ;" + ENDL
                   + "      owl:equivalentProperty src1:Business__companyName ." + ENDL);

        // change name of superclass property, restricted subclasses will inherit property with new name
        // As class name has changed (Org -> Organisation) need to reflect this in property name
        // src2:Org_organisationName -> :Organisation__name
        owl.append(":Organisation__name a owl:DatatypeProperty ;" + ENDL
                   + "      owl:equivalentProperty src2:Org__organisationName ." + ENDL);

        // indicate that a class should not appear in target model, properties with this class
        // as range or domain will not appear in InterMine model output
        // src1:Contractor -> null:Contractor
        owl.append("null:Contractor a owl:Class ;" + ENDL
                   + "      owl:equivalentClass src1:Contractor." + ENDL);

        // A restricted subclass can be assigned as a subclass of another class, in this
        // case another restricted subclass
        // LtdCompany is a restricted subclass of src2:Org
        //    where Organisation.organisationType.type = business
        //    and   Organisation.organisationType.companyModel.model = limited
        // in target model is a subclass of Company
        owl.append(":LtdCompany a owl:Class ;" + ENDL
                   + "      rdfs:subClassOf :Company ;" + ENDL
                   + "      rdfs:subClassOf src2:Org ;" + ENDL
                   + "      rdfs:subClassOf" + ENDL
                   + "         [ a owl:Restriction ;" + ENDL
                   + "           owl:onProperty src2:Org__organisationType ;" + ENDL
                   + "           owl:hasValue" + ENDL
                   + "              [ rdfs:subClassOf src2:OrganisationType ;" + ENDL
                   + "                rdfs:subClassOf" + ENDL
                   + "                   [ a owl:Restriction ;" + ENDL
                   + "                     owl:onProperty src2:OrganisationType__type ;" + ENDL
                   + "                     owl:hasValue \"business\"" + ENDL
                   + "                   ] ;" + ENDL
                   + "                rdfs:subClassOf" + ENDL
                   + "                   [ a owl:Restriction ;" + ENDL
                   + "                     owl:onProperty src2:OrganisationType__companyModel ;" + ENDL
                   + "                     owl:hasValue"
                   + "                       [ rdfs:subClassOf src2:CompanyModel ;" + ENDL
                   + "                         rdfs:subClassOf" + ENDL
                   + "                           [ a owl:Restriction ;" + ENDL
                   + "                             owl:onProperty src2:CompanyModel__model ;" + ENDL
                   + "                             owl:hasValue \"limited\"" + ENDL
                   + "                           ] " + ENDL
                   + "                       ] " + ENDL
                   + "                   ] " + ENDL
                   + "              ]" + ENDL
                   + "         ] ." + ENDL);
                   //+ "      owl:equivalentClass src2:Organisation ." + ENDL);

//         owl.append(":LtdCompany__name a owl:DatatypeProperty ;" + ENDL
//                    + "     rdfs:subPropertyOf :Company__name ;" + ENDL
//                    + "     rdfs:domain :LtdCompany ;" + ENDL
//                    + "     rdfs:range xsd:string ;" + ENDL
//                    + "     owl:equivalentProperty src2:Org__organisationName ." + ENDL);

        // Charity is a restricted subclass of src2:Organisation
        //      where src2:Organisation.organisationType.type = charity
        //      and   src2:Organsisation.profitable = false
        // in target model is a subclass of Organsiation
        owl.append(":Charity a owl:Class ; " + ENDL
                   + "      rdfs:subClassOf :Organisation ;" + ENDL
                   + "      rdfs:subClassOf src2:Org ;" + ENDL
                   + "      rdfs:subClassOf" + ENDL
                   + "            [ a owl:Restriction ;" + ENDL
                   + "              owl:onProperty src2:Org__organisationType ;" + ENDL
                   + "              owl:hasValue" + ENDL
                   + "                 [ rdfs:subClassOf src2:OrganisationType ;" + ENDL
                   + "                   rdfs:subClassOf" + ENDL
                   + "                      [ a owl:Restriction ;" + ENDL
                   + "                        owl:onProperty src2:OrganisationType__type ;" + ENDL
                   + "                        owl:hasValue \"charity\"" + ENDL
                   + "                      ] " + ENDL
                   + "                 ] " + ENDL
                   + "            ] ;" + ENDL
                   + "      rdfs:subClassOf" + ENDL
                   + "            [ a owl:Restriction ;" + ENDL
                   + "              owl:onProperty src2:Org__profitable ;" + ENDL
                   + "              owl:hasValue \"false\" ] ." + ENDL);
                   //+ "      owl:equivalentClass src2:Organisation ." + ENDL);


        return owl.toString();
    }

    private String getSrc1Model() {
        String owl = "@prefix : <" + src1Ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ":Business a owl:Class ;" + ENDL
            + "      rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :Business__address ] ." + ENDL
            + ":Business__companyName a owl:DatatypeProperty ;" + ENDL
            + "             rdfs:domain :Business ;" + ENDL
            + "             rdfs:range xsd:string ." + ENDL
            + ":Business__address a owl:ObjectProperty ;" + ENDL
            + "             rdfs:domain :Business ;" + ENDL
            + "             rdfs:range :Address ." + ENDL
            + ":Business__vatNumber a owl:ObjectProperty ;" + ENDL
            + "           rdfs:domain :Business ;" + ENDL
            + "           rdfs:range xsd:integer ." + ENDL
            + ":Business__departments a owl:ObjectProperty ;" + ENDL
            + "           rdfs:domain :Business ;" + ENDL
            + "           rdfs:range :Department ." + ENDL
            + ":Business__contractors a owl:ObjectProperty ;" + ENDL
            + "           rdfs:domain :Business ;" + ENDL
            + "           rdfs:range :Contractor ." + ENDL
            + ":BigBusiness a owl:Class ;" + ENDL
            + "      rdfs:subClassOf :Business ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":Address__address a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Address ; " + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":Department a owl:Class ;" + ENDL
            + "      rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :Department__company ] ." + ENDL
            // NOTE: Department__company and Company__departments are reverse references of
            // one another -> use inverseOf syntax
            + ":Department__company a owl:ObjectProperty ;" + ENDL
            + "      owl:inverseOf :Company__departments ." + ENDL
            + ":Contractor a owl:Class ." + ENDL
            + ":Contractor__companies a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :Contractor ;" + ENDL
            + "      rdfs:range :Business ." + ENDL;

        return owl;
    }

    private String getSrc2Model() {
        String owl = "@prefix : <" + src2Ns + "> ." + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ":Org a owl:Class ;" + ENDL
            + "     rdfs:subClassOf" + ENDL
            + "           [ a owl:Restriction ;" + ENDL
            + "             owl:maxCardinality \"1\" ;" + ENDL
            + "             owl:onProperty :Org__organisationType ] ." + ENDL
            + ":Org__organisationType a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :Org ;" + ENDL
            + "      rdfs:range :OrganisationType ." + ENDL
            + ":Org__organisationName a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Org ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":Org__profitable a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Org ;" + ENDL
            + "      rdfs:range xsd:boolean ." + ENDL
            + ":OrganisationType a owl:Class ;" + ENDL
            + "      rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :OrganisationType__companyModel ] ." + ENDL
            + ":OrganisationType__type a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :OrganisationType ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":OrganisationType__companyModel a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :OrganisationType ;" + ENDL
            + "      rdfs:range :CompanyModel ." + ENDL
            + ":CompanyModel a owl:Class ." + ENDL
            + ":CompanyModel__model a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :CompanyModel ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL;

        return owl;
    }

    private Set getSrc2Items() {
        // Items conforming to source2 model

        // maps to company
        Item src11 = itemFactory.makeItem();
        src11.setIdentifier("11");
        src11.setClassName(src2Ns + "Org");
        src11.setImplementations("");
        Attribute a11 = new Attribute();
        a11.setName("organisationName");
        a11.setValue("Company11");
        src11.addAttribute(a11);
        Attribute a12 = new Attribute();
        a12.setName("profitable");
        a12.setValue("true");
        src11.addAttribute(a12);
        Reference r11 = new Reference();
        r11.setName("organisationType");
        r11.setRefId("12");
        src11.addReference(r11);

        Item src12 = itemFactory.makeItem();
        src12.setIdentifier("12");
        src12.setClassName(src2Ns + "OrganisationType");
        src12.setImplementations("");
        Attribute a13 = new Attribute();
        a13.setName("type");
        a13.setValue("business");
        src12.addAttribute(a13);

        // maps to LtdCompany
        Item src21 = itemFactory.makeItem();
        src21.setIdentifier("21");
        src21.setClassName(src2Ns + "Org");
        src21.setImplementations("");
        Attribute a21 = new Attribute();
        a21.setName("organisationName");
        a21.setValue("LtdCompany21");
        src21.addAttribute(a21);
        Attribute a22 = new Attribute();
        a22.setName("profitable");
        a22.setValue("true");
        src21.addAttribute(a22);
        Reference r21 = new Reference();
        r21.setName("organisationType");
        r21.setRefId("22");
        src21.addReference(r21);

        Item src22 = itemFactory.makeItem();
        src22.setIdentifier("22");
        src22.setClassName(src2Ns + "OrganisationType");
        src22.setImplementations("");
        Attribute a23 = new Attribute();
        a23.setName("type");
        a23.setValue("business");
        src22.addAttribute(a23);
        Reference r22 = new Reference();
        r22.setName("companyModel");
        r22.setRefId("23");
        src22.addReference(r22);

        Item src23 = itemFactory.makeItem();
        src23.setIdentifier("23");
        src23.setClassName(src2Ns + "CompanyModel");
        src23.setImplementations("");
        Attribute a24 = new Attribute();
        a24.setName("model");
        a24.setValue("limited");
        src23.addAttribute(a24);

        // remain as Organisation
        Item src31 = itemFactory.makeItem();
        src31.setIdentifier("31");
        src31.setClassName(src2Ns + "Org");
        src31.setImplementations("");
        Attribute a31 = new Attribute();
        a31.setName("organisationName");
        a31.setValue("Organisation31");
        src31.addAttribute(a31);
        Attribute a32 = new Attribute();
        a32.setName("profitable");
        a32.setValue("false");
        src31.addAttribute(a32);
        Reference r31 = new Reference();
        r31.setName("organisationType");
        r31.setRefId("32");
        src31.addReference(r31);

        Item src32 = itemFactory.makeItem();
        src32.setIdentifier("32");
        src32.setClassName(src2Ns + "OrganisationType");
        src32.setImplementations("");
        Attribute a33 = new Attribute();
        a33.setName("type");
        a33.setValue("government");
        src32.addAttribute(a33);

        return new HashSet(Arrays.asList(new Object[] {src11, src12, src21, src22, src23, src31, src32}));
    }

    private Set getSrc2TgtItems() {
        // Company
        Item exp11 = itemFactory.makeItem();
        exp11.setIdentifier("11");
        exp11.setClassName(tgtNs + "Company");
        exp11.setImplementations("");
        Attribute ea11 = new Attribute();
        ea11.setName("name");
        ea11.setValue("Company11");
        exp11.addAttribute(ea11);
        Attribute ea12 = new Attribute();
        ea12.setName("profitable");
        ea12.setValue("true");
        exp11.addAttribute(ea12);
        Reference er11 = new Reference();
        er11.setName("organisationType");
        er11.setRefId("12");
        exp11.addReference(er11);

        Item exp12 = itemFactory.makeItem();
        exp12.setIdentifier("12");
        exp12.setClassName(tgtNs + "OrganisationType");
        exp12.setImplementations("");
        Attribute a13 = new Attribute();
        a13.setName("type");
        a13.setValue("business");
        exp12.addAttribute(a13);

        // LtdCompany
        Item exp21 = itemFactory.makeItem();
        exp21.setIdentifier("21");
        exp21.setClassName(tgtNs + "LtdCompany");
        exp21.setImplementations("");
        Attribute a21 = new Attribute();
        a21.setName("name");
        a21.setValue("LtdCompany21");
        exp21.addAttribute(a21);
        Attribute a22 = new Attribute();
        a22.setName("profitable");
        a22.setValue("true");
        exp21.addAttribute(a22);
        Reference r21 = new Reference();
        r21.setName("organisationType");
        r21.setRefId("22");
        exp21.addReference(r21);

        Item exp22 = itemFactory.makeItem();
        exp22.setIdentifier("22");
        exp22.setClassName(tgtNs + "OrganisationType");
        exp22.setImplementations("");
        Attribute a23 = new Attribute();
        a23.setName("type");
        a23.setValue("business");
        exp22.addAttribute(a23);
        Reference r22 = new Reference();
        r22.setName("companyModel");
        r22.setRefId("23");
        exp22.addReference(r22);

        Item exp23 = itemFactory.makeItem();
        exp23.setIdentifier("23");
        exp23.setClassName(tgtNs + "CompanyModel");
        exp23.setImplementations("");
        Attribute a24 = new Attribute();
        a24.setName("model");
        a24.setValue("limited");
        exp23.addAttribute(a24);

        // Organisation
        Item exp31 = itemFactory.makeItem();
        exp31.setIdentifier("31");
        exp31.setClassName(tgtNs + "Organisation");
        exp31.setImplementations("");
        Attribute a31 = new Attribute();
        a31.setName("name");
        a31.setValue("Organisation31");
        exp31.addAttribute(a31);
        Attribute a32 = new Attribute();
        a32.setName("profitable");
        a32.setValue("false");
        exp31.addAttribute(a32);
        Reference r31 = new Reference();
        r31.setName("organisationType");
        r31.setRefId("32");
        exp31.addReference(r31);

        Item exp32 = itemFactory.makeItem();
        exp32.setIdentifier("32");
        exp32.setClassName(tgtNs + "OrganisationType");
        exp32.setImplementations("");
        Attribute a33 = new Attribute();
        a33.setName("type");
        a33.setValue("government");
        exp32.addAttribute(a33);

        return new HashSet(Arrays.asList(new Object[] {exp11, exp12, exp21, exp22, exp23, exp31, exp32}));
    }
}
