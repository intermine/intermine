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
//import com.hp.hpl.jena.rdf.model.*;

import org.flymine.metadata.*;


public class Owl2FlyMineTest extends TestCase
{
    private final String tgtNamespace = "http://www.flymine.org/target/";
    private Owl2FlyMine generator;
    private final String ENDL = "\n";

    public Owl2FlyMineTest(String arg) {
        super(arg);
        generator = new Owl2FlyMine("testmodel", "org.flymine.model.testmodel");
    }

    public void testProcessClass() throws Exception {
        String owl = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + "@prefix junk: <http://www.flymine.org/junk#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + "junk:Test a owl:Class ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, tgtNamespace);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertFalse(model.hasClassDescriptor("org.flymine.model.testmodel.Test"));
    }

    public void testProcessSubclassOf() throws Exception {
        String owl = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
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

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, tgtNamespace);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Child"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Child");
        Set supers = cld.getSuperDescriptors();
        assertTrue(supers.size() == 2);
        assertTrue(supers.contains(model.getClassDescriptorByName("org.flymine.model.testmodel.Parent1"))
                   && supers.contains(model.getClassDescriptorByName("org.flymine.model.testmodel.Parent3")));
    }

    public void testProcessDataTypeProperty() throws Exception {
        String owl = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":name a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Company ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":vatNumber a owl:DatatypeProperty ;" + ENDL
            + "           rdfs:domain :Company ;" + ENDL
            + "           rdfs:range xsd:positiveInteger ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, tgtNamespace);
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
        String owl = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + "@prefix junk: <http://www.flymine.org/junk#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ." + ENDL
            + ":Department a owl:Class ." + ENDL
            + ":departments a rdf:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :Department ." + ENDL;

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, tgtNamespace);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Department"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        assertNotNull(cld.getCollectionDescriptorByName("departments"));
        CollectionDescriptor cod = cld.getCollectionDescriptorByName("departments");
        assertTrue(cod.getReferencedClassDescriptor().getName().equals("org.flymine.model.testmodel.Department"));
    }

    public void testProcessReferenceProperty() throws Exception {
        String owl = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
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


        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");

        Model model = generator.process(ont, tgtNamespace);
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.Company"));
        assertTrue(model.hasClassDescriptor("org.flymine.model.testmodel.CEO"));
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        assertNotNull(cld.getReferenceDescriptorByName("ceo"));
        ReferenceDescriptor rfd = cld.getReferenceDescriptorByName("ceo");
        assertTrue(rfd.getReferencedClassDescriptor().getName().equals("org.flymine.model.testmodel.CEO"));
    }


    public void testHasMaxCardinalityOne() throws Exception {
        String owl = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
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

        assertTrue(generator.hasMaxCardinalityOne(ont, ont.getOntProperty(tgtNamespace + "ceo"),
                                        (OntResource) ont.getOntClass(tgtNamespace + "Company")));
        assertFalse(generator.hasMaxCardinalityOne(ont, ont.getOntProperty(tgtNamespace + "departments"),
                                        (OntResource) ont.getOntClass(tgtNamespace + "Company")));
    }


}
