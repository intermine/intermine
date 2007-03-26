package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class OntologyUtilTest extends TestCase
{

    private final static String ns = "http://www.intermine.org/target#";
    private final static String ENDL = "\n";

    public void testGeneratePropertyName() throws Exception {
        AttributeDescriptor atd1 = new AttributeDescriptor("atd1", "java.lang.String");
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, Collections.singleton(atd1), new HashSet(), new HashSet());
        Model model = new Model("model", ns, Collections.singleton(cld1));
        assertEquals(ns + "Class1__atd1", OntologyUtil.generatePropertyName(atd1));
    }

    public void testGeneratePropertyNameProp() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":ceo a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range rdfs:Literal ." + ENDL
            + ":Company__name a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range rdfs:Literal ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        OntClass com = ont.getOntClass(ns + "Company");
        OntProperty ceo = ont.getOntProperty(ns + "ceo");
        OntProperty name = ont.getOntProperty(ns + "Company__name");
        assertEquals("Company__ceo", OntologyUtil.generatePropertyName(ceo, com));
        assertEquals("Company__name", OntologyUtil.generatePropertyName(name, com));
    }

    public void testGenerateFieldName() throws Exception {
        OntModel ont = ModelFactory.createOntologyModel();
        OntClass cls = ont.createClass(ns + "Company");
        OntProperty prop1 = ont.createOntProperty(ns + "Company__name");
        OntProperty prop2 = ont.createOntProperty(ns + "address");

        assertEquals("name", OntologyUtil.generateFieldName(prop1, cls));
        assertEquals("address", OntologyUtil.generateFieldName(prop2, cls));
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
        String tgtNs = "http://www.intermine.org/target#";
        String src1Ns = "http://www.intermine.org/source1#";
        String src2Ns = "http://www.intermine.org/source2#";

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
        assertTrue(model.getOntClass(tgtNs + "Company").getURI()
                   .equals((String) equiv.get(src1Ns + "LtdCompany")));
        assertNotNull(equiv.get(src2Ns + "Corporation"));
        assertTrue(model.getOntClass(tgtNs + "Company").getURI()
                   .equals((String) equiv.get(src2Ns + "Corporation")));
        assertNotNull(equiv.get(src1Ns + "companyName"));
        assertTrue(model.getOntProperty(tgtNs + "name").getURI()
                   .equals((String) equiv.get(src1Ns + "companyName")));
        assertNotNull(equiv.get(src2Ns + "corpName"));
        assertTrue(model.getOntProperty(tgtNs + "name").getURI()
                   .equals((String) equiv.get(src2Ns + "corpName")));
        assertNull(equiv.get(src1Ns + "Address"));
        assertNull(equiv.get(tgtNs + "Address"));
    }

    public void testBuildEquivalenceMapSrcNs() throws Exception {
        String tgtNs = "http://www.intermine.org/target#";
        String src1Ns = "http://www.intermine.org/source1#";
        String src2Ns = "http://www.intermine.org/source2#";

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
        assertTrue(model.getOntClass(tgtNs + "Company").getURI()
                   .equals((String) equiv.get(src1Ns + "LtdCompany")));
        assertNotNull(equiv.get(src1Ns + "companyName"));
        assertTrue(model.getOntProperty(tgtNs + "name").getURI()
                   .equals((String) equiv.get(src1Ns + "companyName")));
        assertNull(equiv.get(src1Ns + "Address"));
        assertNull(equiv.get(tgtNs + "Address"));
    }

    public void testReorganisePropertiesName() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "           rdfs:domain :Company ;" + ENDL
            + "           rdfs:range rdfs:Literal ." + ENDL
            + ":Company__address a rdf:Property ;" + ENDL
            + "           rdfs:domain :Company ;" + ENDL
            + "           rdfs:range rdfs:Literal ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        OntologyUtil.reorganiseProperties(model, ns);

        assertNotNull(model.getOntClass(ns + "Company"));
        assertNotNull(model.getOntProperty(ns + "Company__name"));
        assertNull(model.getOntProperty(ns + "name"));
        assertNotNull(model.getOntProperty(ns + "Company__address"));
        assertNull(model.getOntProperty(ns + "address"));
    }

    public void testReorganisePropertiesEquivalentProperty() throws Exception {
        String srcNs = "http://www.intermine.org/source#";

        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix src:  <" + srcNs + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "        rdfs:domain :Company ;" + ENDL
            + "        rdfs:range rdfs:Literal ;" + ENDL
            + "        owl:equivalentProperty src:name, src:otherName ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        assertTrue(model.getOntProperty(ns + "name").hasEquivalentProperty(model.createOntProperty(srcNs + "name")));
        assertTrue(model.getOntProperty(ns + "name").hasEquivalentProperty(model.createOntProperty(srcNs + "otherName")));

        OntologyUtil.reorganiseProperties(model, ns);

        assertNotNull(model.getOntClass(ns + "Company"));
        assertNotNull(model.getOntProperty(ns + "Company__name"));
        assertNull(model.getOntProperty(ns + "name"));
        assertTrue(model.getOntProperty(ns + "Company__name").hasEquivalentProperty(model.createOntProperty(srcNs + "name")));
        assertTrue(model.getOntProperty(ns + "Company__name").hasEquivalentProperty(model.createOntProperty(srcNs + "otherName")));
    }

    public void testReorganisePropertiesRestriction() throws Exception {
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
            + "              owl:onProperty :address ] ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":address a owl:ObjectProperty ;" + ENDL
            + "           rdfs:domain :Company ;" + ENDL
            + "           rdfs:range :Address ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        assertTrue(OntologyUtil.hasMaxCardinalityOne(model, model.getOntProperty(ns + "address"),
                                                     model.getOntClass(ns + "Company")));

        OntologyUtil.reorganiseProperties(model, ns);

        assertNotNull(model.getOntClass(ns + "Company"));
        assertNotNull(model.getOntProperty(ns + "Company__address"));
        assertNull(model.getOntProperty(ns + "address"));
        assertTrue(OntologyUtil.hasMaxCardinalityOne(model, model.getOntProperty(ns + "Company__address"),
                                                     model.getOntClass(ns + "Company")));
    }

    public void testReorganisePropertiesLabel() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "           rdfs:domain :Company ;" + ENDL
            + "           rdfs:range rdfs:Literal ;" + ENDL
            + "           rdfs:label \"a label\" ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        OntologyUtil.reorganiseProperties(model, ns);

        assertNotNull(model.getOntClass(ns + "Company"));
        assertNotNull(model.getOntProperty(ns + "Company__name"));
        assertNull(model.getOntProperty(ns + "name"));
        OntProperty comName = model.getOntProperty(ns + "Company__name");
        assertTrue(comName.getLabel(null).equals("a label"));
    }

    // call that applies all properties to subclasses is no longer used
//     public void testReorganisePropertiesSubclasses() throws Exception {
//         String owl = "@prefix : <" + ns + "> ." + ENDL
//             + ENDL
//             + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
//             + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
//             + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
//             + "@prefix null:  <http://www.intermine.org.model/null#> ." + ENDL
//             + ENDL
//             + ":Company a owl:Class ;" + ENDL
//             + "         rdfs:subClassOf" + ENDL
//             + "            [ a owl:Restriction ;" + ENDL
//             + "              owl:maxCardinality \"1\" ;" + ENDL
//             + "              owl:onProperty :address ] ." + ENDL
//             + ":address a owl:ObjectProperty ;" + ENDL
//             + "           rdfs:domain :Company ;" + ENDL
//             + "           rdfs:range :Address ." + ENDL
//             + ":name a owl:DatatypeProperty ;" + ENDL
//             + "      rdfs:domain :Company ;" + ENDL
//             + "      rdfs:range rdfs:Literal ." + ENDL
//             + ":LtdCompany a owl:Class ;" + ENDL
//             + "      rdfs:subClassOf :Company ." + ENDL
//             + ":BigLtdCompany a owl:Class ;" + ENDL
//             + "      rdfs:subClassOf :LtdCompany ." + ENDL
//             + ":Address a owl:Class ." + ENDL;

//         OntModel model = ModelFactory.createOntologyModel();
//         model.read(new StringReader(owl), null, "N3");

//         OntologyUtil.reorganiseProperties(model, ns);
//         //        model.write(new FileWriter(File.createTempFile("props", "")), "N3");

//         assertNotNull(model.getOntClass(ns + "Company"));
//         assertNotNull(model.getOntClass(ns + "LtdCompany"));
//         assertNotNull(model.getOntClass(ns + "BigLtdCompany"));
//         assertNull(model.getOntProperty(ns + "address"));
//         assertNull(model.getOntProperty(ns + "name"));
//         assertNotNull(model.getOntProperty(ns + "Company__address"));
//         assertNotNull(model.getOntProperty(ns + "LtdCompany__address"));
//         assertNotNull(model.getOntProperty(ns + "BigLtdCompany__address"));
//         assertNotNull(model.getOntProperty(ns + "Company__name"));
//         assertNotNull(model.getOntProperty(ns + "LtdCompany__name"));
//         assertNotNull(model.getOntProperty(ns + "BigLtdCompany__name"));

//         OntClass com = model.getOntClass(ns + "Company");
//         OntProperty comAdd = model.getOntProperty(ns + "Company__address");
//         assertTrue(OntologyUtil.hasMaxCardinalityOne(model, comAdd, com));

//         OntClass ltd = model.getOntClass(ns + "LtdCompany");
//         OntProperty ltdAdd = model.getOntProperty(ns + "LtdCompany__address");
//         assertTrue(OntologyUtil.hasMaxCardinalityOne(model, ltdAdd, ltd));

//         OntClass big = model.getOntClass(ns + "BigLtdCompany");
//         OntProperty bigAdd = model.getOntProperty(ns + "BigLtdCompany__address");
//         assertTrue(OntologyUtil.hasMaxCardinalityOne(model, bigAdd, big));
//     }


    public void testTranferEquivalenceStatements() throws Exception {
        String srcNs = "http://www.intermine.org/source#";

        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix src:  <" + srcNs + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "        rdfs:domain :Company ;" + ENDL
            + "        rdfs:range rdfs:Literal ;" + ENDL
            + "        owl:equivalentProperty src:name, src:otherName ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        OntProperty prop = model.getOntProperty(ns + "name");
        OntProperty srcProp1 = model.createOntProperty(srcNs + "name");
        OntProperty srcProp2 = model.createOntProperty(srcNs + "otherName");
        OntProperty newProp = model.createOntProperty(ns + "Company__name");

        assertTrue(hasStatement(model, prop, OntologyUtil.OWL_NAMESPACE + "equivalentProperty", srcProp1));
        assertTrue(hasStatement(model, prop, OntologyUtil.OWL_NAMESPACE + "equivalentProperty", srcProp2));
        OntologyUtil.transferEquivalenceStatements(prop, newProp, model);
        assertTrue(hasStatement(model, prop, OntologyUtil.OWL_NAMESPACE + "equivalentProperty", srcProp1));
        assertTrue(hasStatement(model, prop, OntologyUtil.OWL_NAMESPACE + "equivalentProperty", srcProp2));

        assertTrue(hasStatement(model, newProp, OntologyUtil.OWL_NAMESPACE + "equivalentProperty", srcProp1));
        assertTrue(hasStatement(model, newProp, OntologyUtil.OWL_NAMESPACE + "equivalentProperty", srcProp2));
    }

    public void testPickRange() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a owl:DatatypeProperty ;" + ENDL
            + "        rdfs:domain :Company ;" + ENDL
            + "        rdfs:range rdfs:Literal, xsd:integer ." + ENDL
            + ":ref1 a owl:ObjectProperty ;" + ENDL
            + "    rdfs:domain :Company ;" + ENDL
            + "    rdfs:range :Cls1, :Cls2, :Cls3 ." + ENDL
            + ":ref2 a owl:ObjectProperty ;" + ENDL
            + "    rdfs:domain :Company ;" + ENDL
            + "    rdfs:range :Cls1, :Cls4, :Cls3 ." + ENDL
            + ":Cls1 a owl:Class ." + ENDL
            + ":Cls2 a owl:Class ;" + ENDL
            + "    rdfs:subClassOf :Cls1." + ENDL
            + ":Cls3 a owl:Class ;" + ENDL
            + "    rdfs:subClassOf :Cls2." + ENDL
            + ":Cls4 a owl:Class ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        OntClass cls1 = model.getOntClass(ns + "Cls1");
        assertEquals(cls1, OntologyUtil.pickRange(model.getOntProperty(ns + "ref1")));

        try {
            OntologyUtil.pickRange(model.getOntProperty(ns + "ref2"));
            fail("Expected an Exception ");
        } catch (Exception e) {
        }

        try {
            OntologyUtil.pickRange(model.getOntProperty(ns + "name"));
            fail("Expected an Exception ");
        } catch (Exception e) {
        }
    }

    private boolean hasStatement(OntModel model, OntResource subject, String predicate, OntResource object) {
        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getSubject().equals(subject)
                && stmt.getPredicate().getURI().equals(predicate)) {

                Resource res = stmt.getResource();
                if (res.equals((Resource) object)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void testGenerateClassNamesNull() throws Exception {
        assertNull(OntologyUtil.generateClassNames(null, null));
    }

    public void testGenerateClassNamesEmpty() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("", OntologyUtil.generateClassNames("", model));
    }

    public void testGenerateClassNamesSingle() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("org.intermine.model.testmodel.Company", OntologyUtil.generateClassNames(model.getNameSpace() + "Company", model));
    }

    public void testGenerateClassNamesMultiple() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        String classNames = " " + model.getNameSpace() + "Company " + model.getNameSpace() + "Department ";
        String expected = "org.intermine.model.testmodel.Company org.intermine.model.testmodel.Department";
        assertEquals(expected, OntologyUtil.generateClassNames(classNames, model));
    }

    public void testGetRestrictionSubclassMap() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":Organisation__organisationType a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Organisation__profit a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"business\" ] ." + ENDL
            + ":Charity a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"charity\" ] ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__profit ;" + ENDL
            + "              owl:hasValue \"none\" ] ." + ENDL
            + ":OtherOrganisation a owl:Class ;" + ENDL
            + "                   rdfs:subClassOf :Organisation ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":Address__addressType a owl:DatatypeProperty ;" + ENDL
            + "             rdfs:domain :Address ;" + ENDL
            + "             rdfs:range rdfs:Literal ." + ENDL
            + ":PostalAddress a owl:Class ;" + ENDL
            + "          rdfs:subClassOf :Address ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Address__addressType ;" + ENDL
            + "              owl:hasValue \"postal\" ] ." + ENDL;


        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        Map expected = new HashMap();

        // SubclassRestriction sr1 maps to Business URI
        SubclassRestriction sr1 = new SubclassRestriction();
        sr1.addRestriction("Organisation.organisationType", "business");
        OntClass bus = model.getOntClass(ns + "Business");
        expected.put(sr1, bus.getURI());

        // SubclassRestriction sr2 maps to Charity URI
        SubclassRestriction sr2 = new SubclassRestriction();
        sr2.addRestriction("Organisation.organisationType", "charity");
        sr2.addRestriction("Organisation.profit", "none");
        OntClass cha = model.getOntClass(ns + "Charity");
        expected.put(sr2, cha.getURI());

        // SubclassRestriction sr3 maps to PostalAddress URI
        SubclassRestriction sr3 = new SubclassRestriction();
        sr3.addRestriction("Address.addressType", "postal");
        OntClass pos = model.getOntClass(ns + "PostalAddress");
        expected.put(sr3, pos.getURI());

        assertEquals(expected, OntologyUtil.getRestrictionSubclassMap(model, OntologyUtil.getRestrictedSubclassMap(model)));
    }

    public void testGetRestrictionSubclassTemplateMap() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":Organisation__organisationType a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Organisation__profit a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Partnership a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"partnership\" ] ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"business\" ] ." + ENDL
            + ":Charity a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"charity\" ] ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__profit ;" + ENDL
            + "              owl:hasValue \"none\" ] ." + ENDL
            + ":OtherOrganisation a owl:Class ;" + ENDL
            + "                   rdfs:subClassOf :Organisation ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":Address__addressType a owl:DatatypeProperty ;" + ENDL
            + "             rdfs:domain :Address ;" + ENDL
            + "             rdfs:range rdfs:Literal ." + ENDL
            + ":PostalAddress a owl:Class ;" + ENDL
            + "          rdfs:subClassOf :Address ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Address__addressType ;" + ENDL
            + "              owl:hasValue \"postal\" ] ." + ENDL;


        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        Map expected = new HashMap();

        // sr1 represents Business and Partnership restriction of Organisation
        SubclassRestriction sr1 = new SubclassRestriction();
        sr1.addRestriction("Organisation.organisationType", null);

        // sr2 represents Charity restriction of Organisation
        SubclassRestriction sr2 = new SubclassRestriction();
        sr2.addRestriction("Organisation.organisationType", null);
        sr2.addRestriction("Organisation.profit", null);

        // sr3 represents PostalAddress restriction of Address
        SubclassRestriction sr3 = new SubclassRestriction();
        sr3.addRestriction("Address.addressType", null);


        OntClass org = model.getOntClass(ns + "Organisation");
        OntClass add = model.getOntClass(ns + "Address");
        expected.put(org.getURI(), new HashSet(Arrays.asList(new Object[] {sr1, sr2})));
        expected.put(add.getURI(), new HashSet(Collections.singleton(sr3)));

        assertEquals(expected, OntologyUtil.getRestrictionSubclassTemplateMap(model, OntologyUtil.getRestrictedSubclassMap(model)));
    }

    public void testGetRestrictionSubclassSimple() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":Organisation__organisationType a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Organisation__profit a owl:DatatypeProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range xsd:string ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"business\" ] ." + ENDL
            + ":Charity a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue \"charity\" ] ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__profit ;" + ENDL
            + "              owl:hasValue \"none\" ] ." + ENDL
            + ":OtherOrganisation a owl:Class ;" + ENDL
            + "                   rdfs:subClassOf :Organisation ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        OntClass org = model.getOntClass(ns + "Organisation");

        SubclassRestriction sr1 = new SubclassRestriction();
        sr1.addRestriction("Organisation.organisationType", "business");
        OntClass bus = model.getOntClass(ns + "Business");
        assertEquals(sr1, OntologyUtil.createSubclassRestriction(model, bus, org.getLocalName(), null, true));

        SubclassRestriction sr2 = new SubclassRestriction();
        sr2.addRestriction("Organisation.organisationType", "charity");
        sr2.addRestriction("Organisation.profit", "none");
        OntClass cha = model.getOntClass(ns + "Charity");
        assertEquals(sr2, OntologyUtil.createSubclassRestriction(model, cha, org.getLocalName(), null, true));

        SubclassRestriction sr3 = new SubclassRestriction();
        OntClass oth = model.getOntClass(ns + "OtherOrganisation");
        assertEquals(sr3, OntologyUtil.createSubclassRestriction(model, oth, org.getLocalName(), null, true));
    }

    public void testGetRestrictionSubclassNested() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":Organisation__organisationType a owl:ObjectProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range :OrganisationType ." + ENDL
            + ":OrganisationType a owl:Class ." + ENDL
            + ":OrganisationType__type a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :OrganisationType ;" + ENDL
            + "      rdfs:range xsd:String ." + ENDL
            + ":OrganisationType__companyModel a owl:ObjectProperty ;" + ENDL
            + "              rdfs:domain :OrganisationType ;" + ENDL
            + "              rdfs:range :CompanyModel ." + ENDL
            + ":CompanyModel a owl:Class ." + ENDL
            + ":CompanyModel__model a owl:DatatypeProperty ;" + ENDL
            + "       rdfs:domain :CompanyModel ;" + ENDL
            + "       rdfs:range xsd:String ." + ENDL
            + ":Business a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [  rdfs:subClassOf :OrganisationType ;" + ENDL
            + "                  rdfs:subClassOf" + ENDL
            + "                    [ a owl:Restriction ;" + ENDL
            + "                      owl:onProperty :OrganisationType__type ;" + ENDL
            + "                      owl:hasValue \"business\"" + ENDL
            + "                    ] " + ENDL
            + "               ] " + ENDL
            + "            ] ." + ENDL
            + ":PrivateBusiness a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty :Organisation__organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [   rdfs:subClassOf :OrganisationType ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty :OrganisationType__companyModel ;" + ENDL
            + "                       owl:hasValue"
            + "                         [ rdfs:subClassOf :CompanyModel ;" + ENDL
            + "                           rdfs:subClassOf" + ENDL
            + "                             [ a owl:Restriction ;" + ENDL
            + "                               owl:onProperty :CompanyModel__model ;" + ENDL
            + "                               owl:hasValue \"limited\"" + ENDL
            + "                             ] " + ENDL
            + "                         ] " + ENDL
            + "                     ] ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty :OrganisationType__type ;" + ENDL
            + "                       owl:hasValue \"business\"" + ENDL
            + "                     ] " + ENDL
            + "                ] " + ENDL
            + "            ] ." + ENDL;

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new StringReader(owl), null, "N3");

        OntClass org = model.getOntClass(ns + "Organisation");

        SubclassRestriction sr1 = new SubclassRestriction();
        sr1.addRestriction("Organisation.organisationType.type", "business");
        OntClass bus = model.getOntClass(ns + "Business");
        assertEquals(sr1, OntologyUtil.createSubclassRestriction(model, bus, org.getLocalName(), null, true));

        SubclassRestriction sr2 = new SubclassRestriction();
        sr2.addRestriction("Organisation.organisationType.companyModel.model", "limited");
        sr2.addRestriction("Organisation.organisationType.type", "business");
        OntClass pri = model.getOntClass(ns + "PrivateBusiness");
        assertEquals(sr2, OntologyUtil.createSubclassRestriction(model, pri, org.getLocalName(), null, true));

    }

    public void testGetNamespaceFromClassName() throws Exception {
        assertEquals("http://www.shortname.org#ShortNameObject",
                     OntologyUtil.getNamespaceFromClassName("org.shortname.ShortNameObject"));
        assertEquals("http://www.flymine.org/model/test#SomeObject",
                     OntologyUtil.getNamespaceFromClassName("org.flymine.model.test.SomeObject"));
        assertEquals("http://www.intermine.org/model#InterMineObject",
                     OntologyUtil.getNamespaceFromClassName("org.intermine.model.InterMineObject")); 
   }
}
