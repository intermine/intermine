package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;

import org.flymine.xml.full.Item;
import org.flymine.xml.full.Field;
import org.flymine.xml.full.ReferenceList;
import org.flymine.ontology.OntologyUtil;

public class DataTranslatorTest extends TestCase
{
    private String srcNs = "http://www.flymine.org/source#";
    private String tgtNs = "http://www.flymine.org/target#";

    public void testTranslateItems() throws Exception {

        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        src1.setImplementations(srcNs + "Organisation");
        Item src2 = new Item();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Item src3 = new Item();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");
        List srcItems = new ArrayList(Arrays.asList(new Object[] {src1, src2, src3}));

        Item tgt1 = new Item();
        tgt1.setIdentifier("1");
        tgt1.setClassName(tgtNs + "Company");
        tgt1.setImplementations(tgtNs + "Organisation");
        Item tgt2 = new Item();
        tgt2.setIdentifier("2");
        tgt2.setClassName(tgtNs + "Address");
        Item tgt3 = new Item();
        tgt3.setIdentifier("3");
        tgt3.setClassName(tgtNs + "Department");
        List expected = Arrays.asList(new Object[] {tgt1, tgt2, tgt3});

        assertEquals(expected, new ArrayList(DataTranslator.translate(srcItems, getFlyMineOwl())));

    }

    public void testTranslateItemSimple() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        src1.setImplementations(srcNs + "Organisation");

        Map equivMap = DataTranslator.buildEquivalenceMap(getFlyMineOwl());

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        expected.setImplementations(tgtNs + "Organisation");
        assertEquals(expected, DataTranslator.translateItem(src1, equivMap));
    }


    public void testTranslateItemFields() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        src1.setImplementations(srcNs + "Organisation");
        Field f1 = new Field();
        f1.setName("name");
        f1.setValue("testname");
        src1.addField(f1);


        System.out.println(OntologyUtil.getNamespaceFromURI(src1.getClassName()) + "......");

        Map equivMap = DataTranslator.buildEquivalenceMap(getFlyMineOwl());
        System.out.println(equivMap.toString());
        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        expected.setImplementations(tgtNs + "Organisation");
        Field f2 = new Field();
        f2.setName("Company_name");
        f2.setValue("testname");
        expected.addField(f2);
        assertEquals(expected, DataTranslator.translateItem(src1, equivMap));
    }
    public void testTranslateItemReferences() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        src1.setImplementations(srcNs + "Organisation");
        Item src2 = new Item();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Field f1 = new Field();
        f1.setName("address");
        f1.setValue("2");
        src1.addReference(f1);

        Map equivMap = DataTranslator.buildEquivalenceMap(getFlyMineOwl());

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        expected.setImplementations(tgtNs + "Organisation");
        Field f2 = new Field();
        f2.setName("Company_address");
        f2.setValue("2");
        expected.addReference(f2);
        assertEquals(expected, DataTranslator.translateItem(src1, equivMap));
    }

    public void testTranslateItemCollections() throws Exception {
        Item src1 = new Item();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        src1.setImplementations(srcNs + "Organisation");

        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addValue("2");
        r1.addValue("3");
        src1.addCollection(r1);

        Map equivMap = DataTranslator.buildEquivalenceMap(getFlyMineOwl());

        Item expected = new Item();
        expected.setIdentifier("1");
        expected.setClassName(tgtNs + "Company");
        expected.setImplementations(tgtNs + "Organisation");
        ReferenceList r2 = new ReferenceList();
        r2.setName("Company_departments");
        r2.addValue("2");
        r2.addValue("3");
        expected.addCollection(r2);
        assertEquals(expected, DataTranslator.translateItem(src1, equivMap));
    }

    private OntModel getFlyMineOwl() {
        String ENDL = System.getProperty("line.separator");

        String owl = "@prefix : <" + tgtNs + "> ." + ENDL
            + "@prefix src: <" + srcNs + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL

            + ENDL
            + ":Organisation a owl:Class ;" + ENDL
            + "              owl:equivalentClass src:Organisation ." + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "         rdfs:subClassOf :Organisation ;" + ENDL
            + "         rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :Company_address ] ;" + ENDL
            + "         owl:equivalentClass src:LtdCompany ." + ENDL
            + ":Company_name a owl:DatatypeProperty ;" + ENDL
            + "              rdfs:domain :Company ;" + ENDL
            + "              rdfs:range xsd:string ;" + ENDL
            + "              owl:equivalentProperty src:name ." + ENDL
            + ":Address a owl:Class ;" + ENDL
            + "         owl:equivalentClass src:Address ." + ENDL
            + ":Department a owl:Class ;" + ENDL
            + "            owl:equivalentClass src:Department ." + ENDL
            + ":Company_address a owl:ObjectProperty ;" + ENDL
            + "                 rdfs:domain :Company ;" + ENDL
            + "                 rdfs:range :Address ;" + ENDL
            + "                 owl:equivalentProperty src:address ." + ENDL
            + ":Company_departments a owl:ObjectProperty ;" + ENDL
            + "                     rdfs:domain :Company ;" + ENDL
            + "                     rdfs:range :Address ;" + ENDL
            + "                     owl:equivalentProperty src:departments ." + ENDL;


        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(new StringReader(owl), null, "N3");
        return ont;
    }


}
