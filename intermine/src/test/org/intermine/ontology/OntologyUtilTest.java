package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.io.StringReader;
import java.util.Set;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


import org.flymine.metadata.*;


public class OntologyUtilTest extends TestCase
{
    private final String ns = "http://www.flymine.org/target#";
    private final String ENDL = "\n";

    public void testGeneratePropertyName() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", false, "java.lang.String");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, Collections.singleton(atd1), new HashSet(), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));
        assertEquals(ns + "Class1__atd1", OntologyUtil.generatePropertyName(atd1));
    }


    public void testGenerateFieldName() throws Exception {
        OntModel ont = ModelFactory.createOntologyModel();
        OntClass cls = ont.createClass(ns + "Company");
        OntProperty prop1 = ont.createOntProperty(ns + "Company__name");
        OntProperty prop2 = ont.createOntProperty(ns + "address");

        assertEquals("name", OntologyUtil.generateFieldName(prop1, cls));
        assertEquals("address", OntologyUtil.generateFieldName(prop2, cls));

    }

    public void testXmlToJava() throws Exception {
        assertEquals("java.lang.String", OntologyUtil.xmlToJavaType("string"));
        assertEquals("java.lang.String", OntologyUtil.xmlToJavaType("normalizedString"));
        assertEquals("java.lang.String", OntologyUtil.xmlToJavaType("language"));
        assertEquals("java.lang.String", OntologyUtil.xmlToJavaType("Name"));
        assertEquals("java.lang.String", OntologyUtil.xmlToJavaType("NCName"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("positiveInteger"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("negativeInteger"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("int"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("nonNegativeInteger"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("unsignedInt"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("integer"));
        assertEquals("java.lang.Integer", OntologyUtil.xmlToJavaType("nonPositiveInteger"));
        assertEquals("java.lang.Short", OntologyUtil.xmlToJavaType("short"));
        assertEquals("java.lang.Short", OntologyUtil.xmlToJavaType("unsignedShort"));
        assertEquals("java.lang.Long", OntologyUtil.xmlToJavaType("long"));
        assertEquals("java.lang.Long", OntologyUtil.xmlToJavaType("unsignedLong"));
        assertEquals("java.lang.Byte", OntologyUtil.xmlToJavaType("byte"));
        assertEquals("java.lang.Byte", OntologyUtil.xmlToJavaType("unsignedByte"));
        assertEquals("java.lang.Float", OntologyUtil.xmlToJavaType("float"));
        assertEquals("java.lang.Float", OntologyUtil.xmlToJavaType("decimal"));
        assertEquals("java.lang.Double", OntologyUtil.xmlToJavaType("double"));
        assertEquals("java.lang.Boolean", OntologyUtil.xmlToJavaType("boolean"));
        assertEquals("java.net.URI", OntologyUtil.xmlToJavaType("anyURI"));
        assertEquals("java.util.Date", OntologyUtil.xmlToJavaType("dateTime"));

        try {
            OntologyUtil.xmlToJavaType("rubbish");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testJavaToXml() throws Exception {
        assertEquals(OntologyUtil.XSD_NAMESPACE + "string", OntologyUtil.javaToXmlType("java.lang.String"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "integer", OntologyUtil.javaToXmlType("java.lang.Integer"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "integer", OntologyUtil.javaToXmlType("int"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "short", OntologyUtil.javaToXmlType("java.lang.Short"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "short", OntologyUtil.javaToXmlType("short"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "long", OntologyUtil.javaToXmlType("java.lang.Long"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "long", OntologyUtil.javaToXmlType("long"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "double", OntologyUtil.javaToXmlType("java.lang.Double"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "double", OntologyUtil.javaToXmlType("double"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "float", OntologyUtil.javaToXmlType("java.lang.Float"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "float", OntologyUtil.javaToXmlType("float"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "boolean", OntologyUtil.javaToXmlType("java.lang.Boolean"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "boolean", OntologyUtil.javaToXmlType("boolean"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "byte", OntologyUtil.javaToXmlType("java.lang.Byte"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "byte", OntologyUtil.javaToXmlType("byte"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "anyURI", OntologyUtil.javaToXmlType("java.net.URI"));
        assertEquals(OntologyUtil.XSD_NAMESPACE + "dateTime", OntologyUtil.javaToXmlType("java.util.Date"));
        try {
            OntologyUtil.javaToXmlType("rubbish");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testHasMaxCardinalityOne() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "         rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :ceo ] ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":ceo a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :CEO ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "            rdfs:domain :Company ;" + ENDL
            + "            rdfs:range :Department ." + ENDL;


        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        assertTrue(OntologyUtil.hasMaxCardinalityOne(ont, ont.getOntProperty(ns + "ceo"),
                                        (OntResource) ont.getOntClass(ns + "Company")));
        assertFalse(OntologyUtil.hasMaxCardinalityOne(ont, ont.getOntProperty(ns + "departments"),
                                        (OntResource) ont.getOntClass(ns + "Company")));
    }


    public void testGetStatementFor() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":address a owl:ObjectProperty ;" + ENDL
            + "       rdfs:domain :Company ;" + ENDL
            + "       rdfs:range :Address ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        Set stmts1 = OntologyUtil.getStatementsFor(ont, ont.getOntProperty(ns + "address"),
                                                   OntologyUtil.RDFS_NAMESPACE + "range");
        Iterator iter1 = stmts1.iterator();
        Statement stmt1 = (Statement) iter1.next();
        assertEquals(ns + "Address", ((Resource) stmt1.getObject()).getURI());
        assertFalse(iter1.hasNext());

        Set stmts2 = OntologyUtil.getStatementsFor(ont, ont.getOntProperty(ns + "address"),
                                                   OntologyUtil.RDFS_NAMESPACE + "domain");
        Iterator iter2 = stmts2.iterator();
        Statement stmt2 = (Statement) iter2.next();
        assertEquals(ns + "Company", ((Resource) stmt2.getObject()).getURI());
        assertFalse(iter2.hasNext());

        Set stmts3 = OntologyUtil.getStatementsFor(ont, ont.getOntClass(ns + "Company"),
                                                   OntologyUtil.RDF_NAMESPACE + "type");
        Iterator iter3 = stmts3.iterator();
        Set types = new HashSet();
        while (iter3.hasNext()) {
            types.add(((Resource) ((Statement) iter3.next()).getObject()).getURI());
        }
        Set expected = new HashSet(Arrays.asList(new String[] {OntologyUtil.OWL_NAMESPACE + "Class",
                                                               OntologyUtil.RDFS_NAMESPACE + "Class",
                                                               OntologyUtil.RDFS_NAMESPACE + "Resource"}));

        assertEquals(expected, types);
    }


    public void testCorrectNamespace() throws Exception {
        assertEquals("http://www.flymine.org/test#",
                     OntologyUtil.correctNamespace("http://www.flymine.org/test#junk"));
        assertEquals("http://www.flymine.org/test#",
                     OntologyUtil.correctNamespace("http://www.flymine.org/test#junk#morejunk"));
        assertEquals("http://www.flymine.org/test#",
                     OntologyUtil.correctNamespace("http://www.flymine.org/test/"));
        assertEquals("http://www.flymine.org/test#",
                     OntologyUtil.correctNamespace("http://www.flymine.org/test"));
    }


    public void testFindRestrictedSubclassesDatatype() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":organisationType a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :organisationType ;" + ENDL
            + "              owl:hasValue \"business\" ] ." + ENDL
            + ":Charity a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :organisationType ;" + ENDL
            + "              owl:hasValue \"charity\" ] ." + ENDL
            + ":OtherOrganisation a owl:Class ;" + ENDL
            + "                   rdfs:subClassOf :Organisation ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        assertNotNull(ont.getOntClass(ns + "Organisation"));
        OntClass cls1 = ont.getOntClass(ns + "Organisation");
        assertNotNull(ont.getOntClass(ns + "Business"));
        assertNotNull(ont.getOntClass(ns + "Charity"));
        assertNotNull(ont.getOntClass(ns + "OtherOrganisation"));
        assertNotNull(ont.getOntProperty(ns + "organisationType"));
        OntProperty prop1 = ont.getOntProperty(ns + "organisationType");
        assertEquals(prop1, (OntProperty) cls1.listDeclaredProperties(false).next());
        assertTrue(cls1.hasSubClass(ont.getOntClass(ns + "Business")));
        assertTrue(cls1.hasSubClass(ont.getOntClass(ns + "Charity")));
        assertTrue(cls1.hasSubClass(ont.getOntClass(ns + "OtherOrganisation")));


        OntClass cls2 = ont.getOntClass(ns + "Business");
        OntClass cls3 = ont.getOntClass(ns + "Charity");
        Set expected = new HashSet(Arrays.asList(new Object[] {cls2, cls3}));

        assertEquals(expected, OntologyUtil.findRestrictedSubclasses(ont, cls1));
    }

    public void testFindRestrictedSubclassesObject() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":organisationType a owl:ObjectProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range :OrganisationType ." + ENDL
            + ":OrganisationType a owl:Class ." + ENDL
            + ":type a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :OrganisationType ;" + ENDL
            + "      rdfs:range xsd:String ." + ENDL
            + ":companyModel a owl:ObjectProperty ;" + ENDL
            + "              rdfs:domain :OrganisationType ;" + ENDL
            + "              rdfs:range :CompanyModel ." + ENDL
            + ":CompanyModel a owl:Class ." + ENDL
            + ":model a owl:DatatypeProperty ;" + ENDL
            + "       rdfs:domain :CompanyModel ;" + ENDL
            + "       rdfs:range xsd:String ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [  rdfs:subClassOf :OrganisationType ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty :type ;" + ENDL
            + "                       owl:hasValue \"business\"" + ENDL
            + "                     ] " + ENDL
            + "                ] " + ENDL
            + "            ] ." + ENDL
            + ":PrivateBusiness a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [  rdfs:subClassOf :OrganisationType ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty :companyModel ;" + ENDL
            + "                       owl:hasValue"
            + "                         [ rdfs:subClassOf :CompanyModel ;" + ENDL
            + "                           rdfs:subClassOf" + ENDL
            + "                             [ a owl:Restriction ;" + ENDL
            + "                               owl:onProperty :model ;" + ENDL
            + "                               owl:hasValue \"limited\"" + ENDL
            + "                             ] " + ENDL
            + "                         ] " + ENDL
            + "                     ] " + ENDL
            + "                ] " + ENDL
            + "            ] ." + ENDL
            + ":OtherOrganisation a owl:Class ;" + ENDL
            + "                   rdfs:subClassOf :Organisation ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        assertNotNull(ont.getOntClass(ns + "Organisation"));
        assertNotNull(ont.getOntClass(ns + "Business"));
        assertNotNull(ont.getOntClass(ns + "PrivateBusiness"));
        assertNotNull(ont.getOntClass(ns + "OtherOrganisation"));
        assertNotNull(ont.getOntClass(ns + "OrganisationType"));
        assertNotNull(ont.getOntClass(ns + "CompanyModel"));

        assertNotNull(ont.getOntProperty(ns + "organisationType"));
        assertNotNull(ont.getOntProperty(ns + "companyModel"));
        assertNotNull(ont.getOntProperty(ns + "type"));
        assertNotNull(ont.getOntProperty(ns + "model"));

        OntClass cls1 = ont.getOntClass(ns + "Organisation");

        assertTrue(cls1.hasSubClass(ont.getOntClass(ns + "Business")));
        assertTrue(cls1.hasSubClass(ont.getOntClass(ns + "PrivateBusiness")));
        assertTrue(cls1.hasSubClass(ont.getOntClass(ns + "OtherOrganisation")));

        assertTrue(ont.getOntProperty(ns + "organisationType").hasDomain(ont.getOntClass(ns + "Organisation")));
        assertTrue(ont.getOntProperty(ns + "type").hasDomain(ont.getOntClass(ns + "OrganisationType")));
        assertTrue(ont.getOntProperty(ns + "companyModel").hasDomain(ont.getOntClass(ns + "OrganisationType")));
        assertTrue(ont.getOntProperty(ns + "model").hasDomain(ont.getOntClass(ns + "CompanyModel")));

        OntClass cls2 = ont.getOntClass(ns + "Business");
        OntClass cls3 = ont.getOntClass(ns + "PrivateBusiness");
        Set expected = new HashSet(Arrays.asList(new Object[] {cls2, cls3}));
        assertEquals(expected, OntologyUtil.findRestrictedSubclasses(ont, cls1));
    }


    public void testPropertyTypes() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":prop1 a owl:DatatypeProperty ;" + ENDL
            + "       rdfs:domain :Company ;" + ENDL
            + "       rdfs:range xsd:String." + ENDL
            + ":prop2 a rdf:Property ;" + ENDL
            + "         rdfs:domain :Company ;" + ENDL
            + "         rdfs:range \"this is a literal\" ." + ENDL
            + ":prop3 a owl:ObjectProperty ;" + ENDL
            + "       rdfs:domain :Company ;" + ENDL
            + "       rdfs:range :Address ." + ENDL
            + ":prop4 a rdf:Property ;" + ENDL
            + "         rdfs:domain :Company ;" + ENDL
            + "         rdfs:range :Address ." + ENDL
            + ":prop5 a rdf:Property ;" + ENDL
            + "       rdfs:domain :Company ;" + ENDL
            + "       rdfs:range xsd:String." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        assertTrue(OntologyUtil.isDatatypeProperty(ont.getOntProperty(ns + "prop1")));
        assertTrue(OntologyUtil.isDatatypeProperty(ont.getOntProperty(ns + "prop2")));
        assertFalse(OntologyUtil.isDatatypeProperty(ont.getOntProperty(ns + "prop3")));
        assertFalse(OntologyUtil.isDatatypeProperty(ont.getOntProperty(ns + "prop4")));
        assertTrue(OntologyUtil.isDatatypeProperty(ont.getOntProperty(ns + "prop5")));
        assertTrue(OntologyUtil.isObjectProperty(ont.getOntProperty(ns + "prop3")));
        assertTrue(OntologyUtil.isObjectProperty(ont.getOntProperty(ns + "prop4")));
        assertFalse(OntologyUtil.isObjectProperty(ont.getOntProperty(ns + "prop1")));
        assertFalse(OntologyUtil.isObjectProperty(ont.getOntProperty(ns + "prop2")));
        assertFalse(OntologyUtil.isObjectProperty(ont.getOntProperty(ns + "prop5")));
    }

    public void testBuildEquivalenceMap() throws Exception {
        String tgtNs = "http://www.flymine.org/target#";
        String src1Ns = "http://www.flymine.org/source1#";
        String src2Ns = "http://www.flymine.org/source2#";

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src1: <" + src1Ns + "> ." + ENDL
            + "@prefix src2: <" + src2Ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ;" + ENDL
            + "           owl:equivalentClass src2:Corporation ." + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "        owl:equivalentProperty src1:companyName ;" + ENDL
            + "        owl:equivalentProperty src2:corpName ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":vatNumber a rdf:Property ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");
        Map equiv = OntologyUtil.buildEquivalenceMap(model);

        assertNotNull(equiv);
        assertNotNull(equiv.get(src1Ns + "LtdCompany"));
        assertTrue(model.getOntClass(tgtNs + "Company")
                   .equals((Resource) equiv.get(src1Ns + "LtdCompany")));
        assertNotNull(equiv.get(src2Ns + "Corporation"));
        assertTrue(model.getOntClass(tgtNs + "Company")
                   .equals((Resource) equiv.get(src2Ns + "Corporation")));
        assertNotNull(equiv.get(src1Ns + "companyName"));
        assertTrue(model.getOntProperty(tgtNs + "name")
                   .equals((Resource) equiv.get(src1Ns + "companyName")));
        assertNotNull(equiv.get(src2Ns + "corpName"));
        assertTrue(model.getOntProperty(tgtNs + "name")
                   .equals((Resource) equiv.get(src2Ns + "corpName")));
        assertNull(equiv.get(src1Ns + "Address"));
        assertNull(equiv.get(tgtNs + "Address"));
    }



    public void testBuildEquivalenceMapSrcNs() throws Exception {
        String tgtNs = "http://www.flymine.org/target#";
        String src1Ns = "http://www.flymine.org/source1#";
        String src2Ns = "http://www.flymine.org/source2#";

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src1: <" + src1Ns + "> ." + ENDL
            + "@prefix src2: <" + src2Ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ;" + ENDL
            + "           owl:equivalentClass src2:Corporation ." + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "        owl:equivalentProperty src1:companyName ;" + ENDL
            + "        owl:equivalentProperty src2:corpName ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":vatNumber a rdf:Property ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");
        Map equiv = OntologyUtil.buildEquivalenceMap(model, src1Ns);

        assertNotNull(equiv);
        assertNotNull(equiv.get(src1Ns + "LtdCompany"));
        assertTrue(model.getOntClass(tgtNs + "Company")
                   .equals((Resource) equiv.get(src1Ns + "LtdCompany")));
        assertNotNull(equiv.get(src1Ns + "companyName"));
        assertTrue(model.getOntProperty(tgtNs + "name")
                   .equals((Resource) equiv.get(src1Ns + "companyName")));
        assertNull(equiv.get(src1Ns + "Address"));
        assertNull(equiv.get(tgtNs + "Address"));
    }

}
