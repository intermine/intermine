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
    private final String tgtNamespace = "http://www.flymine.org/target/";
    private final String src1Namespace = "http://www.flymine.org/source1/";
    private final String src2Namespace = "http://www.flymine.org/source2/";
    private final String owlNamespace = "http:://www.w3.org/2002/07/owl#";
    private final String rdfNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private final String rdfsNamespace = "http://www.w3.org/2000/01/rdf-schema#>";

    public MergeOwlTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        assertNotNull(merger.tgtModel);
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Name"));
    }

    public void testMergeByEquivalenceClass() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(getSrc1()), null, "N3");

        // merge first source
        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNull(merger.tgtModel.getOntClass(src1Namespace + "LtdCompany"));
        assertNull(merger.tgtModel.getOntClass(tgtNamespace + "LtdCompany"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Name"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "CompName"));
        assertNull(merger.tgtModel.getOntProperty(tgtNamespace + "CompName"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "VatNumber"));


        // merge second source
        OntModel src2 = ModelFactory.createOntologyModel();
        src2.read(new StringReader(getSrc2()), null, "N3");

        merger.mergeByEquivalence(src2, src2Namespace);
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "CorpNumber"));

    }


    public void testMergeByEquivalenceSimpleClass() throws Exception {
        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ." + ENDL;

        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
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
        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ." + ENDL
            + ":Name a rdf:Property ;" + ENDL
            + "      rdfs:domain :Company ;" + ENDL
            + "      owl:equivalentProperty src1:CompanyName ." + ENDL;


        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":CompanyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":VatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ." + ENDL;


        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        // properties: CompanyName -> Name, VatNumber -> VatNumber, check owl:equivalentProperty
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Name"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "Name"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "VatNumber"));
        assertNull(merger.tgtModel.getOntProperty(src1Namespace + "VatNumber"));

        OntProperty tgtName = merger.tgtModel.getOntProperty(tgtNamespace + "Name");
        OntProperty srcCompanyName = src1.getOntProperty(src1Namespace + "CompanyName");
        OntProperty tgtVatNumber = merger.tgtModel.getOntProperty(tgtNamespace + "VatNumber");
        OntProperty srcVatNumber = src1.getOntProperty(src1Namespace + "VatNumber");
        assertTrue(hasStatement(merger.tgtModel, tgtName, "equivalentProperty", srcCompanyName));
        assertTrue(hasStatement(merger.tgtModel, tgtVatNumber, "equivalentProperty", srcVatNumber));
    }


    public void testMergeByEquivalencePropertyDomains() throws Exception {

        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ." + ENDL
            + ":Name a rdf:Property ;" + ENDL
            + "      rdfs:domain :Company ;" + ENDL
            + "      owl:equivalentProperty src1:CompanyName ." + ENDL;


        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":CompanyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":VatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ." + ENDL
            + ":Postcode a rdf:Property ;" + ENDL
            + "          rdfs:domain :Address ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Name"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Postcode"));

        // Name has domain that has changed (LtdCompany -> Company), Postcode unchanged (Address)
        OntProperty name = merger.tgtModel.getOntProperty(tgtNamespace + "Name");
        assertTrue(name.getDomain().equals((OntResource) merger.tgtModel
                                           .getOntClass(tgtNamespace + "Company")));
        OntProperty postcode = merger.tgtModel.getOntProperty(tgtNamespace + "Postcode");
        assertTrue(postcode.getDomain().equals((OntResource) merger.tgtModel
                                           .getOntClass(tgtNamespace + "Address")));
    }


    public void testMergeByEquivalenceLabels() throws Exception {
        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL;

        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ;" + ENDL
            + "            rdfs:label \"company label\" ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":CompanyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":VatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ;" + ENDL
            + "           rdfs:label \"vat number label\" ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        OntClass ltdCompany = merger.tgtModel.getOntClass(tgtNamespace + "LtdCompany");
        assertTrue(ltdCompany.getLabel(null).equals("company label"));
        OntClass address = merger.tgtModel.getOntClass(tgtNamespace + "Address");
        assertFalse(address.listLabels(null).hasNext());
        OntProperty companyName = merger.tgtModel.getOntProperty(tgtNamespace + "CompanyName");
        assertFalse(companyName.listLabels(null).hasNext());
        OntProperty vatNumber = merger.tgtModel.getOntProperty(tgtNamespace + "VatNumber");
        assertTrue(vatNumber.getLabel(null).equals("vat number label"));

    }


    private boolean hasStatement(OntModel model, OntResource tgt, String predicate, OntResource src) {
        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getSubject().equals(tgt)
                && stmt.getPredicate().getLocalName().equals(predicate)) {

                Resource res = stmt.getResource();
                if (res.equals((Resource) src)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void testWriteTargetModel() throws Exception {


        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ." + ENDL
            + ":Name a rdf:Property ;" + ENDL
            + "      rdfs:domain :Company ;" + ENDL
            + "      owl:equivalentProperty src1:CompanyName ." + ENDL;


        String src1Str = "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":CompanyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":Postcode a rdf:Property ;" + ENDL
            + "          rdfs:domain :Address ." + ENDL;



        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace);
        OntModel src1 = ModelFactory.createOntologyModel();
        //src1.read(new StringReader(getSrc1()), null, "N3");
        src1.read(new StringReader(src1Str), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);
        File out = new File("/home/rns/tgt.owl");

        OntModel tgtModel = merger.getTargetModel();
        tgtModel.write(new BufferedWriter(new FileWriter(out)), "N3");
    }


    public String getMergeSpec() {
        return "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src1: <" + src1Namespace + "> ." + ENDL
            + "@prefix src2: <" + src2Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":Company a owl:Class ;" + ENDL
            + "           owl:equivalentClass src1:LtdCompany ;" + ENDL
            + "           owl:equivalentClass src2:Corporation ." + ENDL
            + ENDL
            + ":Name a rdf:Property ;" + ENDL
            + "        rdfs:domain :Company ;" + ENDL
            + "        owl:equivalentProperty src1:CompanyName ;" + ENDL
            + "        owl:equivalentProperty src2:CorpName ." + ENDL;
    }

    public String getSrc1() {
        return "@prefix : <" + src1Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":LtdCompany a owl:Class ." + ENDL
            + ":CompanyName a rdf:Property ;" + ENDL
            + "             rdfs:domain :LtdCompany ." + ENDL
            + ":VatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ." + ENDL
            + ":Address a owl:Class ." + ENDL;
    }

    public String getSrc2() {
        return "@prefix : <" + src2Namespace + "> ." + ENDL
            + ENDL
            + "@prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." + ENDL
            + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." + ENDL
            + "@prefix owl:  <http://www.w3.org/2002/07/owl#> ." + ENDL
            + ENDL
            + ":Corporation a owl:Class ." + ENDL
            + ":CorpName a rdf:Property ;" + ENDL
            + "          rdfs:domain :Corporation ." + ENDL
            + ":CorpNumber a rdf:Property ;" + ENDL
            + "            rdfs:domain :Corporation ." + ENDL;
    }


}
