package org.intermine.ontology;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.OntProperty;


import org.intermine.metadata.*;


public class Owl2InterMineTest extends TestCase
{
    private static final String ns = "http://www.intermine.org/target#";
    private Owl2InterMine generator;
    private static final String ENDL = "\n";
    private OntModel ont;

    public Owl2InterMineTest(String arg) {
        super(arg);
        generator = new Owl2InterMine("testmodel", "org.intermine.model.testmodel");
        ont = ModelFactory.createOntologyModel();
    }

    public void testProcessClass() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + "null:Test a owl:Class ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        assertFalse(model.hasClassDescriptor("org.intermine.model.testmodel.Test"));
    }

    public void testProcessSubclassOf() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Parent1 a owl:Class ." + ENDL
            + ":Parent2 a owl:Class ." + ENDL
            + ":Parent3 a owl:Class ;" + ENDL
            + "         rdfs:subClassOf :Parent2 ." + ENDL
            + "null:Parent3 a owl:Class ." + ENDL
            + ":Child a owl:Class ;" + ENDL
            + "       rdfs:subClassOf :Parent1 ;" + ENDL
            + "       rdfs:subClassOf :Parent3 ;" + ENDL
            + "       rdfs:subClassOf null:Parent3 ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Child"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Child");
        Set supers = cld.getSuperDescriptors();
        assertTrue(supers.size() == 2);
        assertTrue(supers.contains(model.getClassDescriptorByName("org.intermine.model.testmodel.Parent1"))
                   && supers.contains(model.getClassDescriptorByName("org.intermine.model.testmodel.Parent3")));
    }

    public void testProcessDataTypeProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd: <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Company ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":vatNumber a owl:DatatypeProperty ;" + ENDL
            + "           rdfs:domain :Company ;" + ENDL
            + "           rdfs:range xsd:positiveInteger ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        AttributeDescriptor atd1 = cld.getAttributeDescriptorByName("name");
        assertTrue(atd1.isAttribute());
        assertEquals("java.lang.String", atd1.getType());
        AttributeDescriptor atd2 = cld.getAttributeDescriptorByName("vatNumber");
        assertTrue(atd2.isAttribute());
        assertEquals("java.lang.Integer", atd2.getType());
    }


    public void testProcessCollectionProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :Department ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Department"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        assertNotNull(cld.getCollectionDescriptorByName("departments"));
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("departments");
        assertTrue(cod.getReferencedClassDescriptor().getName().equals("org.intermine.model.testmodel.Department"));
    }

    public void testProcessReferenceProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "         rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :ceo ] ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":ceo a rdfs:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :CEO ." + ENDL;


        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.CEO"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        assertNotNull(cld.getReferenceDescriptorByName("ceo"));
        ReferenceDescriptor rfd = cld.getReferenceDescriptorByName("ceo");
        assertTrue(rfd.isReference());
        assertTrue(rfd.getReferencedClassDescriptor().getName().equals("org.intermine.model.testmodel.CEO"));
    }


    public void testProcessInverseProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":ceoX a owl:ObjectProperty ;" + ENDL
            + "     rdfs:domain :Company ;" + ENDL
            + "     rdfs:range :CEO ." + ENDL
            + ":companyX a owl:ObjectProperty ;" + ENDL
            + "     owl:inverseOf :ceoX ." + ENDL;


        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.CEO"));
        ClassDescriptor cld1 = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        CollectionDescriptor cod1 = cld1.getCollectionDescriptorByName("ceoX");
        assertNotNull(cod1);

        ClassDescriptor cld2 = model.getClassDescriptorByName("org.intermine.model.testmodel.CEO");
        CollectionDescriptor cod2 = cld2.getCollectionDescriptorByName("companyX");
        assertNotNull(cod2);

        assertEquals(cod2, cod1.getReverseReferenceDescriptor());
        assertEquals(cod1, cod2.getReverseReferenceDescriptor());
    }


    public void testProcessInverseSubProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "    rdfs:subClassOf :Organisation ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":Organisation__ceo a owl:ObjectProperty ;" + ENDL
            + "     rdfs:domain :Organisation ;" + ENDL
            + "     rdfs:range :CEO ." + ENDL
            + ":Company__ceo a owl:ObjectProperty ;" + ENDL
            + "     rdfs:domain :Company ;" + ENDL
            + "     rdfs:range :CEO ;" + ENDL
            + "     rdfs:subPropertyOf :Organisation__ceo ." + ENDL
            + ":CEO__company a owl:ObjectProperty ;" + ENDL
            + "     owl:inverseOf :Organisation__ceo, :Company__ceo ." + ENDL;


        ont.read(new StringReader(owl), null, "N3");

        try {
            Model model = generator.process(ont, ns);
        } catch (Exception e) {
            fail("Exception was thrown when calculating inverse properties: " + e.getMessage());
        }
    }


    public void testUltimateSuperProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "    rdfs:subClassOf :Organisation ." + ENDL
            + ":LtdCompany a owl:Class ;" + ENDL
            + "    rdfs:subClassOf :Company ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":Organisation__ceo a owl:ObjectProperty ;" + ENDL
            + "     rdfs:domain :Organisation ;" + ENDL
            + "     rdfs:range :CEO ." + ENDL
            + ":Company__ceo a owl:ObjectProperty ;" + ENDL
            + "     rdfs:domain :Company ;" + ENDL
            + "     rdfs:range :CEO ;" + ENDL
            + "     rdfs:subPropertyOf :Organisation__ceo ." + ENDL
            + ":LtdCompany__ceo a owl:ObjectProperty ;" + ENDL
            + "     rdfs:domain :LtdCompany ;" + ENDL
            + "     rdfs:range :CEO ;" + ENDL
            + "     rdfs:subPropertyOf :Company__ceo ." + ENDL
            + ":CEO__company a owl:ObjectProperty ;" + ENDL
            + "     owl:inverseOf :Organisation__ceo, :Company__ceo ." + ENDL;


        ont.read(new StringReader(owl), null, "N3");
        OntProperty org = ont.getOntProperty(ns + "Organisation__ceo");
        OntProperty com = ont.getOntProperty(ns + "Company__ceo");
        OntProperty ltd = ont.getOntProperty(ns + "LtdCompany__ceo");
        assertEquals(org, generator.ultimateSuperProperty(org, ns));
        assertEquals(org, generator.ultimateSuperProperty(com, ns));
        assertEquals(org, generator.ultimateSuperProperty(ltd, ns));
    }


    public void testProcessPropertyMultipleDomain() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain null:Company, null:Organisation ;" + ENDL
            + "             rdfs:range :Department ." + ENDL
            + ":secretarys a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company, null:Business ;" + ENDL
            + "             rdfs:range :Department ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Department"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        assertNull(cld.getCollectionDescriptorByName("departments"));
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("secretarys");
        assertTrue(cod.getReferencedClassDescriptor().getName().equals("org.intermine.model.testmodel.Department"));
    }

    public void testProcessPropertyMultipleDomainInvalid() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company, :Organisation ;" + ENDL
            + "             rdfs:range :Department ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        try {
            Model model = generator.process(ont, ns);
            fail("Expected Exception to be thrown, property has more than one domain in target namespace");
        } catch (Exception e) {
        }
    }



    public void testProcessPropertyMultipleRangeValid() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :Department, null:Section ." + ENDL
            + ":secretarys a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range null:Secretary ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.intermine.model.testmodel.Department"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        assertNotNull(cld.getCollectionDescriptorByName("departments"));
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("departments");
        assertTrue(cod.getReferencedClassDescriptor().getName().equals("org.intermine.model.testmodel.Department"));

        assertNull(cld.getCollectionDescriptorByName("secretarys"));
    }

    public void testProcessPropertyMultipleRangeInvalid() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
            + "@prefix null: <http://www.intermine.org/null#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :Department, xsd:String ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        try {
            Model model = generator.process(ont, ns);
            fail("Expected Exception to be thrown, property has more than one range");
        } catch (Exception e) {
        }
    }

}
