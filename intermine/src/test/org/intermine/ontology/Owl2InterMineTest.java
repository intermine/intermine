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

import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.OntProperty;


import org.flymine.metadata.*;


public class Owl2FlyMineTest extends TestCase
{
    private final String ns = "http://www.flymine.org/target#";
    private Owl2FlyMine generator;
    private final String ENDL = "\n";
    private OntModel ont;

    public Owl2FlyMineTest(String arg) {
        super(arg);
        generator = new Owl2FlyMine("testmodel", "org.flymine.model.testmodel");
        ont = ModelFactory.createOntologyModel();
    }

    public void testProcessClass() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix junk: <http://www.flymine.org/junk#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + "junk:Test a owl:Class ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertFalse(model.hasClassDescriptor("org.flymine.model.testmodel.Test"));
    }

    public void testProcessSubclassOf() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix junk: <http://www.flymine.org/junk#> ." + ENDL
            + ENDL
            + ":Parent1 a owl:Class ." + ENDL
            + ":Parent2 a owl:Class ." + ENDL
            + ":Parent3 a owl:Class ;" + ENDL
            + "         rdfs:subClassOf :Parent2 ." + ENDL
            + "junk:Parent3 a owl:Class ." + ENDL
            + ":Child a owl:Class ;" + ENDL
            + "       rdfs:subClassOf :Parent1 ;" + ENDL
            + "       rdfs:subClassOf :Parent3 ;" + ENDL
            + "       rdfs:subClassOf junk:Parent3 ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Child"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Child");
        Set supers = cld.getSuperDescriptors();
        assertTrue(supers.size() == 2);
        assertTrue(supers.contains(model.getClassDescriptorByName("org.flymine.model.testmodel.Parent1"))
                   && supers.contains(model.getClassDescriptorByName("org.flymine.model.testmodel.Parent3")));
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
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
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
            + "@prefix junk: <http://www.flymine.org/junk#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :Department ." + ENDL;

        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Department"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        assertNotNull(cld.getCollectionDescriptorByName("departments"));
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("departments");
        assertTrue(cod.getReferencedClassDescriptor().getName().equals("org.flymine.model.testmodel.Department"));
    }

    public void testProcessReferenceProperty() throws Exception {
        String owl = "@prefix : <" + ns + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix junk: <http://www.flymine.org/junk#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "         rdf:subClassOf" + ENDL
            + "            [ a owl:restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :ceo ] ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":ceo a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :CEO ." + ENDL;


        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.CEO"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        assertNotNull(cld.getReferenceDescriptorByName("ceo"));
        ReferenceDescriptor rfd = cld.getReferenceDescriptorByName("ceo");
        assertTrue(rfd.getReferencedClassDescriptor().getName().equals("org.flymine.model.testmodel.CEO"));
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
//         OntProperty ontProp1 = ont.getOntProperty(ns + "ceo");
//         assertTrue(ontProp1.hasInverse());
//         OntProperty ontProp2 = ont.getOntProperty(ns + "company");
//         assertTrue(ontProp2.hasInverse());

        Model model = generator.process(ont, ns);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.CEO"));
        ClassDescriptor cld1 = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        System.out.println(cld1.toString());
        CollectionDescriptor cod1 = cld1.getCollectionDescriptorByName("ceoX");
        assertNotNull(cod1);

        ClassDescriptor cld2 = model.getClassDescriptorByName("org.flymine.model.testmodel.CEO");
        System.out.println(cld2.toString());
        CollectionDescriptor cod2 = cld2.getCollectionDescriptorByName("companyX");
        assertNotNull(cod2);

        assertEquals(cod2, cod1.getReverseReferenceDescriptor());
        assertEquals(cod1, cod2.getReverseReferenceDescriptor());

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

        ont.read(new StringReader(owl), null, "N3");

        assertTrue(generator.isDatatypeProperty(ont.getOntProperty(ns + "prop1")));
        assertTrue(generator.isDatatypeProperty(ont.getOntProperty(ns + "prop2")));
        assertFalse(generator.isDatatypeProperty(ont.getOntProperty(ns + "prop3")));
        assertFalse(generator.isDatatypeProperty(ont.getOntProperty(ns + "prop4")));
        assertTrue(generator.isDatatypeProperty(ont.getOntProperty(ns + "prop5")));
        assertTrue(generator.isObjectProperty(ont.getOntProperty(ns + "prop3")));
        assertTrue(generator.isObjectProperty(ont.getOntProperty(ns + "prop4")));
        assertFalse(generator.isObjectProperty(ont.getOntProperty(ns + "prop1")));
        assertFalse(generator.isObjectProperty(ont.getOntProperty(ns + "prop2")));
        assertFalse(generator.isObjectProperty(ont.getOntProperty(ns + "prop5")));
    }
}
