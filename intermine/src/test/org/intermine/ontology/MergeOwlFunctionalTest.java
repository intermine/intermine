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

public class MergeOwlFunctionalTest extends TestCase
{
    public static final String ENDL = "\n";
    private final String tgtNamespace = "http://www.flymine.org/target/";
    private final String src1Namespace = "http://www.flymine.org/source1/";
    private final String src2Namespace = "http://www.flymine.org/source2/";


    public void testMergeByEquivalenceClass() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(getSrc1()), null, "N3");

        // merge first source
        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNull(merger.tgtModel.getOntClass(src1Namespace + "LtdCompany"));
        assertNull(merger.tgtModel.getOntClass(tgtNamespace + "LtdCompany"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "name"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "compName"));
        assertNull(merger.tgtModel.getOntProperty(tgtNamespace + "compName"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "vatNumber"));


        // merge second source
        OntModel src2 = ModelFactory.createOntologyModel();
        src2.read(new StringReader(getSrc2()), null, "N3");

        merger.mergeByEquivalence(src2, src2Namespace);
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "corpNumber"));

    }


    public void testMergeByEquivalenceSimpleClass() throws Exception {
        String mergeSpec = getMergeSpec();

        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        // classes: LtdCompany -> Company, Address -> Address.  check owl:equivalentClass
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNull(merger.tgtModel.getOntClass(src1Namespace + "LtdCompany"));
        assertNull(merger.tgtModel.getOntClass(tgtNamespace + "LtdCompany"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Address"));
        assertNull(merger.tgtModel.getOntClass(src1Namespace + "Address"));

        OntClass tgtCompany = merger.tgtModel.getOntClass(tgtNamespace + "Company");
        OntClass srcLtdCompany = src1.getOntClass(src1Namespace + "LtdCompany");
        OntClass tgtAddress = merger.tgtModel.getOntClass(tgtNamespace + "Address");
        OntClass srcAddress = src1.getOntClass(src1Namespace + "Address");
        assertTrue(hasStatement(merger.tgtModel, tgtCompany, "equivalentClass", srcLtdCompany));
        assertTrue(hasStatement(merger.tgtModel, tgtAddress, "equivalentClass", srcAddress));
    }



    public void testMergeByEquivalenceProperty() throws Exception {
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

        // properties: companyName -> Name, vatNumber -> vatNumber, check owl:equivalentProperty
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "name"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "name"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "vatNumber"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "vatNumber"));

        OntProperty tgtName = merger.tgtModel.getOntProperty(tgtNamespace + "name");
        OntProperty srcCompanyName = src1.getOntProperty(src1Namespace + "companyName");
        OntProperty tgtvatNumber = merger.tgtModel.getOntProperty(tgtNamespace + "vatNumber");
        OntProperty srcvatNumber = src1.getOntProperty(src1Namespace + "vatNumber");
        assertTrue(hasStatement(merger.tgtModel, tgtName, "equivalentProperty", srcCompanyName));
        assertTrue(hasStatement(merger.tgtModel, tgtvatNumber, "equivalentProperty", srcvatNumber));
    }



    public void testMergeByEquivalencePropertyDomains() throws Exception {
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
            + "           rdfs:domain :LtdCompany ." + ENDL
            + ":postcode a rdf:Property ;" + ENDL
            + "          rdfs:domain :Address ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "name"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "postcode"));

        // Name has domain that has changed (LtdCompany -> Company), postcode unchanged (Address)
        OntProperty name = merger.tgtModel.getOntProperty(tgtNamespace + "name");
        assertTrue(name.getDomain().equals((OntResource) merger.tgtModel
                                           .getOntClass(tgtNamespace + "Company")));
        OntProperty postcode = merger.tgtModel.getOntProperty(tgtNamespace + "postcode");
        assertTrue(postcode.getDomain().equals((OntResource) merger.tgtModel
                                           .getOntClass(tgtNamespace + "Address")));
    }

    public void testMergeByEquivalenceLabels() throws Exception {
        String mergeSpec = getMergeSpec();

        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ;" + ENDL
            + "            rdfs:label \"company label\" ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":companyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":vatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ;" + ENDL
            + "           rdfs:label \"vat number label\" ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        OntClass company = merger.tgtModel.getOntClass(tgtNamespace + "Company");
        assertTrue(company.getLabel(null).equals("company label"));
        OntClass address = merger.tgtModel.getOntClass(tgtNamespace + "Address");
        assertFalse(address.listLabels(null).hasNext());
        OntProperty name = merger.tgtModel.getOntProperty(tgtNamespace + "name");
        assertFalse(name.listLabels(null).hasNext());
        OntProperty vatNumber = merger.tgtModel.getOntProperty(tgtNamespace + "vatNumber");
        assertTrue(vatNumber.getLabel(null).equals("vat number label"));

    }
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
