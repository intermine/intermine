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

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

import java.io.StringReader;
import java.util.*;
import java.io.*;

public class MergeOwlTest extends TestCase
{
    public static final String ENDL = "\n";
    private final String tgtNamespace = "http://www.flymine.org/target#";
    private final String src1Namespace = "http://www.flymine.org/source1#";
    private final String src2Namespace = "http://www.flymine.org/source2#";

    public MergeOwlTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        assertNotNull(merger.tgtModel);
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "name"));
    }

    public void testAddToTargetOwl() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        // add multiple sources
        merger.addToTargetOwl(new StringReader(getSrc1()), src1Namespace, "N3");
        merger.addToTargetOwl(new StringReader(getSrc2()), src2Namespace, "N3");

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Address"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "name"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "vatNumber"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "corpNumber"));
    }


    public void testAddToTargetOwlFormat() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);
        try {
            merger.addToTargetOwl(new StringReader(getSrc1()), src1Namespace, "wrong");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testMergeByEquivalenceRdfType() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(getSrc1()), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNull(merger.tgtModel.getOntClass(src1Namespace + "LtdCompany"));
        assertNull(merger.tgtModel.getOntClass(tgtNamespace + "LtdCompany"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "name"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "compName"));
        assertNull(merger.tgtModel.getOntProperty(tgtNamespace + "compName"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "vatNumber"));
    }


    public void testMergeByEquivalenceAnonymous() throws Exception {
        String owl = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ;" + ENDL
            + "         rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:maxCardinality \"1\" ;" + ENDL
            + "              owl:onProperty :ceo ] ." + ENDL
            + ":companyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":vatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ." + ENDL
            + ":Address a owl:Class ." + ENDL
                        + ":CEO a owl:Class ." + ENDL
            + ":ceo a rdfs:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :CEO ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(owl), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        OntClass ontCls1 = merger.tgtModel.getOntClass(tgtNamespace + "Company");
        OntProperty ontProp1 = merger.tgtModel.getOntProperty(tgtNamespace + "ceo");
        assertTrue(OntologyUtil.hasMaxCardinalityOne(merger.tgtModel, ontProp1, ontCls1));
    }

    // getTargetResource() should find resource from equiv Map
    public void testGetTargetResourceExists() throws Exception {
        String mergeSpec = getMergeSpec();


        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Test a owl:Class ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.equiv = new HashMap();
        merger.equiv.put(src1.getOntClass(src1Namespace + "Test").getURI(),
                            merger.tgtModel.createClass(tgtNamespace + "Test"));
        // create tgtNamesapce:Test in src1 to compare, has same URI as desired target
        // Resource so will be .equals()
        src1.createClass(tgtNamespace + "Test");

        Resource test = merger.getTargetResource(src1.getOntClass(src1Namespace + "Test"), src1Namespace);
        assertTrue(src1.getOntClass(tgtNamespace + "Test").equals(test));
    }

    // getTargetResource() should create new Class in tgtModel
    public void testGetTargetResourceNotExists() throws Exception {
        String mergeSpec = getMergeSpec();


        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Test a owl:Class ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        merger.equiv = new HashMap();

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        // create tgtNamesapce:Test in src1 to compare, has same URI as desired target
        // Resource so will be .equals()
        src1.createClass(tgtNamespace + "Test");

        Resource test = merger.getTargetResource(src1.getOntClass(src1Namespace + "Test"), src1Namespace);
        assertTrue(src1.getOntClass(tgtNamespace + "Test").equals(test));
    }


    public void testEquivMap() throws Exception {
        String mergeSpec = getMergeSpec();

        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":companyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":vatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ." + ENDL;


        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.equiv);
        assertNotNull(merger.equiv.get(src1Namespace + "LtdCompany"));
        assertTrue(merger.tgtModel.getOntClass(tgtNamespace + "Company")
                   .equals((Resource) merger.equiv.get(src1Namespace + "LtdCompany")));
        assertNotNull(merger.equiv.get(src1Namespace + "companyName"));
        assertTrue(merger.tgtModel.getOntProperty(tgtNamespace + "name")
                   .equals((Resource) merger.equiv.get(src1Namespace + "companyName")));
        assertNull(merger.equiv.get(src1Namespace + "Address"));
        assertNull(merger.equiv.get(src1Namespace + "vatNumber"));
    }


    public void testAddEquivalenceStatement() throws Exception {
        String mergeSpec = getMergeSpec();


        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":TestClass rdf:type owl:Class ." + ENDL
            + ":testProperty a rdf:Property ;" + ENDL
            + "              rdfs:domain :TestClass ." + ENDL
            + ":testIndividual a owl:Individual ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        // test equivalentClass statement added
        Resource subject = src1.getOntClass(src1Namespace + "TestClass");
        Resource target = merger.tgtModel.createClass(tgtNamespace + "TestClass");
        Resource object = src1.getResource(OntologyUtil.OWL_NAMESPACE + "Class");

        ArrayList statements = new ArrayList();

        merger.addEquivalenceStatement(target, object, subject, statements);
        assertTrue(statements.size() == 1);
        Statement s = (Statement) statements.get(0);
        assertTrue(s.getSubject().equals(target));
        assertTrue(s.getPredicate().getURI().equals(OntologyUtil.OWL_NAMESPACE + "equivalentClass"));
        assertTrue(s.getResource().equals(subject));


        // test equivalentProperty statement added
        subject = src1.getProperty(src1Namespace + "testProperty");
        target = merger.tgtModel.createProperty(tgtNamespace + "testProperty");
        object = src1.getResource(OntologyUtil.RDF_NAMESPACE + "Property");
        statements = new ArrayList();

        merger.addEquivalenceStatement(target, object, subject, statements);
        assertTrue(statements.size() == 1);
        s = (Statement) statements.get(0);
        assertTrue(s.getSubject().equals(target));
        assertTrue(s.getPredicate().getURI().equals(OntologyUtil.OWL_NAMESPACE + "equivalentProperty"));
        assertTrue(s.getResource().equals(subject));

        // test sameAs statement added
        subject = src1.getProperty(src1Namespace + "testIndicidual");
        target = merger.tgtModel.createProperty(tgtNamespace + "testIndividual");
        object = src1.getResource(OntologyUtil.OWL_NAMESPACE + "Individual");
        statements = new ArrayList();

        merger.addEquivalenceStatement(target, object, subject, statements);
        assertTrue(statements.size() == 1);
        s = (Statement) statements.get(0);
        assertTrue(s.getSubject().equals(target));
        assertTrue(s.getPredicate().getURI().equals(OntologyUtil.OWL_NAMESPACE + "sameAs"));
        assertTrue(s.getResource().equals(subject));
    }

    //================================================================================================


    private boolean hasStatement(OntModel model, OntResource subject, String predicate, OntResource object) {
        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getSubject().equals(subject)
                && stmt.getPredicate().getLocalName().equals(predicate)) {

                Resource res = stmt.getResource();
                if (res.equals((Resource) object)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getMergeSpec() {
        return "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + "@prefix src2: <" + src2Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ;" + ENDL
            + "           owl:equivalentClass src2:Corporation ." + ENDL
            + ENDL
            + ":name a rdf:Property ;" + ENDL
            + "        rdfs:domain :Company ;" + ENDL
            + "        owl:equivalentProperty src1:companyName ;" + ENDL
            + "        owl:equivalentProperty src2:corpName ." + ENDL;
    }


    private String getSrc1() {
        return "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":companyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":vatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ." + ENDL
            + ":Address a owl:Class ." + ENDL;
    }


    private String getSrc2() {
        return "@prefix : <" + src2Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":Corporation a owl:Class ." + ENDL
            + ":corpName a rdf:Property ;" + ENDL
            + "          rdfs:domain :Corporation ." + ENDL
            + ":corpNumber a rdf:Property ;" + ENDL
            + "            rdfs:domain :Corporation ." + ENDL;
    }
}
