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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class MergeOwlTest extends TestCase
{
    public static final String ENDL = System.getProperty("line.separator");
    private final static String tgtNamespace = "http://www.intermine.org/target#";
    private final static String src1Namespace = "http://www.intermine.org/source1#";
    private final static String src2Namespace = "http://www.intermine.org/source2#";
    private final static String nullNamespace = "http://www.intermine.org/null#";

    public MergeOwlTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace, true);

        assertNotNull(merger.tgtModel);
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__name"));
    }

    public void testAddToTargetOwl() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace, true);

        // add multiple sources
        merger.addToTargetOwl(new StringReader(getSrc1()), src1Namespace, "N3");
        merger.addToTargetOwl(new StringReader(getSrc2()), src2Namespace, "N3");


        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Address"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__name"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__vatNumber"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__corpNumber"));
    }


    public void testAddToTargetOwlFormat() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace, true);
        try {
            merger.addToTargetOwl(new StringReader(getSrc1()), src1Namespace, "wrong");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testMergeByEquivalenceRdfType() throws Exception {
        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace, true);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(getSrc1()), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntClass(src1Namespace + "LtdCompany"));
        assertNull(merger.tgtModel.getOntClass(tgtNamespace + "LtdCompany"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__name"));
        OntProperty name = merger.tgtModel.getOntProperty(tgtNamespace + "Company__name");
        assertTrue(name.isDatatypeProperty());
        assertNotNull(merger.tgtModel.getOntProperty(src1Namespace + "companyName"));
        assertNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__companyName"));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__vatNumber"));
        OntProperty vat = merger.tgtModel.getOntProperty(tgtNamespace + "Company__vatNumber");
        assertTrue(vat.canAs(DatatypeProperty.class));

        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__address"));
        OntProperty address = merger.tgtModel.getOntProperty(tgtNamespace + "Company__address");
        assertTrue(address.canAs(ObjectProperty.class));


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
            + "             rdfs:domain :LtdCompany ;" + ENDL
            + "             rdfs:range rdfs:Literal ." + ENDL
            + ":vatNumber a rdf:Property ;" + ENDL
            + "           rdfs:domain :LtdCompany ;" + ENDL
            + "           rdfs:range rdfs:Literal ." + ENDL
            + ":Address a owl:Class ." + ENDL
            + ":CEO a owl:Class ." + ENDL
            + ":ceo a rdfs:Property ;" + ENDL
            + "             rdfs:domain :Company ;" + ENDL
            + "             rdfs:range :CEO ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(getMergeSpec()), tgtNamespace, true);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(owl), null, "N3");

        merger.mergeByEquivalence(src1, src1Namespace);

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        OntClass ontCls1 = merger.tgtModel.getOntClass(tgtNamespace + "Company");
        OntProperty ontProp1 = merger.tgtModel.getOntProperty(tgtNamespace + "Company__ceo");
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

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace, true);

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        merger.equiv = new HashMap();
        OntClass cls = merger.tgtModel.createClass(tgtNamespace + "Test");
        merger.equiv.put(src1.getOntClass(src1Namespace + "Test").getURI(), cls.getURI());
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

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace, true);
        merger.equiv = new HashMap();

        OntModel src1 = ModelFactory.createOntologyModel();
        src1.read(new StringReader(src1Str), null, "N3");

        // create tgtNamesapce:Test in src1 to compare, has same URI as desired target
        // Resource so will be .equals()
        src1.createClass(tgtNamespace + "Test");

        Resource test = merger.getTargetResource(src1.getOntClass(src1Namespace + "Test"), src1Namespace);
        assertTrue(src1.getOntClass(tgtNamespace + "Test").equals(test));
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
            + ":testProperty a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :TestClass ;" + ENDL
            + "      rdfs:range rdfs:Literal ." + ENDL
            + ":TestIndividual a owl:Individual ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace, true);
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

        // test that no statement is added if target and original in same namespace
        statements = new ArrayList();
        merger.addEquivalenceStatement(subject, object, subject, statements);
        assertTrue(statements.size() == 0);

        // test equivalentProperty statement added
        subject = src1.getProperty(src1Namespace + "testProperty");
        target = merger.tgtModel.createProperty(tgtNamespace + "TestClass__testProperty");
        object = src1.getResource(OntologyUtil.OWL_NAMESPACE + "DatatypeProperty");
        statements = new ArrayList();

        merger.addEquivalenceStatement(target, object, subject, statements);
        assertTrue(statements.size() == 1);
        s = (Statement) statements.get(0);
        assertTrue(s.getSubject().equals(target));
        assertTrue(s.getPredicate().getURI().equals(OntologyUtil.OWL_NAMESPACE + "equivalentProperty"));
        assertTrue(s.getResource().equals(subject));

        // test sameAs statement added
        subject = src1.getProperty(src1Namespace + "testIndividual");
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


    public void testRestrictedSubclassDataType() throws Exception {
        // Organisation has name, type and address
        String srcStr = "@prefix : <" + src1Namespace + "> ." + ENDL + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL + ENDL
            + ":Entity a owl:Class ." + ENDL
            + ":Organisation a owl:Class ;" + ENDL
            + "      rdfs:subClassOf :Entity ." + ENDL
            + ":address a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :Organisation ;" + ENDL
            + "      rdfs:range :Address ." + ENDL
            + ":organisationName a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Organisation ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":organisationType a owl:DatatypeProperty ;" + ENDL
            + "      rdfs:domain :Organisation ;" + ENDL
            + "      rdfs:range xsd:string ." + ENDL
            + ":Address a owl:Class ." + ENDL;

        // Company is an organisation with type "business"
        // Charity is an organisation with type "charity"
        // Organisation.organisationName becomes (Company|Charity).name
        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src: <" + src1Namespace + "> ." + ENDL + ENDL
            + "@prefix null: <" + nullNamespace + "> ." + ENDL + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL + ENDL
            + ":Company a owl:Class ; " + ENDL
            + "         rdfs:subClassOf :Organisation ;" + ENDL
            + "         rdfs:subClassOf src:Organisation ;" + ENDL
            + "         rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty src:organisationType ;" + ENDL
            + "              owl:hasValue \"business\" ] ." + ENDL
            + ":Charity a owl:Class ; " + ENDL
            + "          rdfs:subClassOf :Organisation ;" + ENDL
            + "          rdfs:subClassOf src:Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty src:organisationType ;" + ENDL
            + "              owl:hasValue \"charity\" ] ." + ENDL
            + ":Organisation__name a owl:DatatypeProperty ;" + ENDL
            + "        owl:equivalentProperty src:organisationName ." + ENDL;

        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace, true);
        OntModel src = ModelFactory.createOntologyModel();
        src.read(new StringReader(srcStr), null, "N3");

        merger.mergeByEquivalence(src, src1Namespace);
        //merger.tgtModel.write(new FileWriter(File.createTempFile("merge_owl", "")), "N3");

        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Company"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Charity"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Organisation"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Entity"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__name"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Charity__name"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__address"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Charity__address"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Company__organisationType"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "Charity__organisationType"));

        OntClass entCls = merger.tgtModel.getOntClass(tgtNamespace + "Entity");
        OntClass comCls = merger.tgtModel.getOntClass(tgtNamespace + "Company");
        OntClass chaCls = merger.tgtModel.getOntClass(tgtNamespace + "Charity");

        // Company and Charity should be direct subclasses of Entity
        assertTrue(entCls.hasSubClass(comCls));
        assertTrue(entCls.hasSubClass(chaCls));

        // company and charity should not be subclass of themselves or eachother
        assertFalse(comCls.hasSubClass(chaCls));
        assertFalse(chaCls.hasSubClass(comCls));

        // name, address and organisationType should have Company and Charity as domains
        //OntProperty comNameProp = merger.tgtModel.getOntProperty(tgtNamespace + "Company__name");
        //OntProperty chaNameProp = merger.tgtModel.getOntProperty(tgtNamespace + "Charity__name");
        //OntProperty comAddrProp = merger.tgtModel.getOntProperty(tgtNamespace + "Company__address");
        //OntProperty chaAddrProp = merger.tgtModel.getOntProperty(tgtNamespace + "Charity__address");
        //OntProperty comOrgTypeProp = merger.tgtModel.getOntProperty(tgtNamespace + "Company__organisationType");
        //OntProperty chaOrgTypeProp = merger.tgtModel.getOntProperty(tgtNamespace + "Charity__organisationType");

        //assertTrue(comNameProp.hasDomain(comCls));
        //assertTrue(comAddrProp.hasDomain(comCls));
        //assertTrue(comOrgTypeProp.hasDomain(comCls));

        //assertTrue(chaNameProp.hasDomain(chaCls));
        //assertTrue(chaAddrProp.hasDomain(chaCls));
        //assertTrue(chaOrgTypeProp.hasDomain(chaCls));

    }



    // Organisation has and OrganisationType
    // OrganisationType has a CompanyModel
    public void testRestrictedSubclassObject() throws Exception {
        String srcStr = "@prefix : <" + src1Namespace + "> ." + ENDL + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL + ENDL
            + ":Organisation a owl:Class ." + ENDL
            + ":organisationType a owl:ObjectProperty ;" + ENDL
            + "                  rdfs:domain :Organisation ;" + ENDL
            + "                  rdfs:range :OrganisationType ." + ENDL
            + ":OrganisationType a owl:Class ." + ENDL
            + ":companyModel a owl:ObjectProperty ;" + ENDL
            + "              rdfs:domain :OrganisationType ;" + ENDL
            + "              rdfs:range :CompanyModel ." + ENDL
            + ":CompanyModel a owl:Class ." + ENDL
            + ":model a owl:DatatypeProperty ;" + ENDL
            + "       rdfs:domain :CompanyModel ;" + ENDL
            + "       rdfs:range xsd:String ." + ENDL;

        // PrivateBusiness has an OrganisationType with a CompanyModel.model 'limited'
        String mergeSpec = "@prefix : <" + tgtNamespace + "> ." + ENDL
            + "@prefix src: <" + src1Namespace + "> ." + ENDL + ENDL
            + "@prefix null: <" + nullNamespace + "> ." + ENDL + ENDL
            + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
            + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
            + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
            + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL + ENDL
            + ":PrivateBusiness a owl:Class ; " + ENDL
            + "          rdfs:subClassOf src:Organisation ;" + ENDL
            + "          rdfs:subClassOf" + ENDL
            + "            [ a owl:Restriction ;" + ENDL
            + "              owl:onProperty src:organisationType ;" + ENDL
            + "              owl:hasValue" + ENDL
            + "                [  rdfs:subClassOf src:OrganisationType ;" + ENDL
            + "                   rdfs:subClassOf" + ENDL
            + "                     [ a owl:Restriction ;" + ENDL
            + "                       owl:onProperty src:companyModel ;" + ENDL
            + "                       owl:hasValue"
            + "                         [ rdfs:subClassOf src:CompanyModel ;" + ENDL
            + "                           rdfs:subClassOf" + ENDL
            + "                             [ a owl:Restriction ;" + ENDL
            + "                               owl:onProperty src:model ;" + ENDL
            + "                               owl:hasValue \"limited\"" + ENDL
            + "                             ] " + ENDL
            + "                         ] " + ENDL
            + "                     ] " + ENDL
            + "                ] " + ENDL
            + "            ] ." + ENDL;


        MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), tgtNamespace, true);
        OntModel src = ModelFactory.createOntologyModel();
        src.read(new StringReader(srcStr), null, "N3");

        merger.mergeByEquivalence(src, src1Namespace);
        System.out.println(merger.equiv);
        System.out.println(merger.subMap);
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "PrivateBusiness"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "Organisation"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "OrganisationType"));
        assertNotNull(merger.tgtModel.getOntClass(tgtNamespace + "CompanyModel"));
        //assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "PrivateBusiness__organisationType"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "OrganisationType__companyModel"));
        assertNotNull(merger.tgtModel.getOntProperty(tgtNamespace + "CompanyModel__model"));

        // name, address and organisationType should have Company and Charity as domains
        OntProperty companyModelProp = merger.tgtModel.getOntProperty(tgtNamespace + "OrganisationType__companyModel");
        Iterator i = companyModelProp.listDomain();
        while (i.hasNext()) {
            assertFalse(((OntResource) i.next()).isAnon());
        }
        i = companyModelProp.listRange();
        while (i.hasNext()) {
            assertFalse(((OntResource) i.next()).isAnon());
        }

        OntProperty modelProp = merger.tgtModel.getOntProperty(tgtNamespace + "CompanyModel__model");
        i = modelProp.listDomain();
        while (i.hasNext()) {
            assertFalse(((OntResource) i.next()).isAnon());
        }

//         OntProperty orgTypeProp = merger.tgtModel.getOntProperty(tgtNamespace + "PrivateBusiness__organisationType");
//         OntClass busCls = merger.tgtModel.getOntClass(tgtNamespace + "PrivateBusiness");
//         assertTrue(orgTypeProp.hasDomain(busCls));
//         i = orgTypeProp.listRange();
//         while (i.hasNext()) {
//             assertFalse(((OntResource) i.next()).isAnon());
//         }
    }


    // problem occurred when a class in source model has multiple restricted subclasses
    // only one had a property in the target model
//     public void testMultipleRestrictedSubclasses() throws Exception {
//          String ensemblNs = "http://www.flymine.org/model/ensembl#";
//          String genomicNs = "http://www.flymine.org/model/genomic#";

//          String mergeSpec = "@prefix : <" + genomicNs + "> ." + ENDL
//             + "@prefix e: <" + ensemblNs + "> ." + ENDL
//             + "@prefix null: <" + nullNamespace + "> ." + ENDL
//             + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
//             + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
//             + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
//             + ":BioEntity a owl:Class ." + ENDL
//              + ":CpGIsland a owl:Class ;"
//              + "  rdfs:subClassOf :BioEntity ;"
//             + "  rdfs:subClassOf e:simple_feature ;"
//             + "  rdfs:subClassOf"
//             + "  [ a owl:Restriction ;"
//             + "    owl:onProperty e:simple_feature__analysis ;"
//             + "    owl:hasValue"
//             + "      [ rdfs:subClassOf e:analysis ;"
//             + "        rdfs:subClassOf"
//             + "          [ a owl:Restriction ;"
//             + "            owl:onProperty e:analysis__program ;"
//             + "            owl:hasValue \"cpg\""
//             + "          ]"
//             + "      ]"
//             + "  ] ;"  + ENDL
//              + "    rdfs:subClassOf"
//              +"      [ a owl:Restriction ;"
//              +"        owl:maxCardinality \"1\" ;"
//              +"        owl:onProperty :CpGIsland__contig ] ."+ENDL
//             + ":TRNA a owl:Class ;"
//             + "  rdfs:subClassOf :BioEntity ;"
//             + "  rdfs:subClassOf e:simple_feature ;"
//             + "  rdfs:subClassOf"
//             + "  [ a owl:Restriction ;"
//             + "    owl:onProperty e:simple_feature__analysis ;"
//             + "    owl:hasValue"
//             + "      [ rdfs:subClassOf e:analysis ;"
//             + "        rdfs:subClassOf"
//             + "          [ a owl:Restriction ;"
//             + "            owl:onProperty e:analysis__program ;"
//             + "            owl:hasValue \"trna\""
//             + "          ]"
//             + "      ]"
//             + "  ] ;" + ENDL
//             + "    rdfs:subClassOf"
//             +"      [ a owl:Restriction ;"
//             +"        owl:maxCardinality \"1\" ;"
//             +"        owl:onProperty :TRNA__contig ] ."+ENDL
//             + ":Contig a owl:Class;"
//             + "  owl:equivalentClass e:contig." + ENDL
//             + ":TRNA__contig a owl:ObjectProperty;"
//             + "  rdfs:domain :TRNA;"
//             + "  rdfs:range :Contig."+ENDL
//              //+ "  owl:equivalentProperty e:simple_feature__contig." + ENDL
//             + ":CpGIsland__contig a owl:ObjectProperty;"
//             + "  rdfs:domain :CpGIsland;"
//             + "  rdfs:range :Contig."+ENDL
//              //+ "  owl:equivalentProperty e:simple_feature__contig." + ENDL
//             + "null:simple_feature a owl:Class ;"
//             + "  owl:equivalentClass e:simple_feature ." + ENDL
//             +":ComputationalAnalysis a owl:Class ;"
//              +"    owl:equivalentClass e:analysis ."+ENDL;

//         String srcStr = "@prefix : <" + ensemblNs + "> ." + ENDL
//             + "@prefix rdf:  <" + OntologyUtil.RDF_NAMESPACE + "> ." + ENDL
//             + "@prefix rdfs: <" + OntologyUtil.RDFS_NAMESPACE + "> ." + ENDL
//             + "@prefix owl:  <" + OntologyUtil.OWL_NAMESPACE + "> ." + ENDL
//             + "@prefix xsd:  <" + OntologyUtil.XSD_NAMESPACE + "> ." + ENDL
//             + ":simple_feature__contig a owl:ObjectProperty ;"
//             + "  rdfs:domain :simple_feature ;"
//             + "  rdfs:range :contig ." + ENDL
//             + ":simple_feature__analysis a owl:ObjectProperty ;"
//             + "  rdfs:domain :simple_feature ;"
//             + "  rdfs:range :analysis ." + ENDL
//             + ":simple_feature a owl:Class." + ENDL
//             + ":contig a owl:Class."+ ENDL
//             + ":analysis a owl:Class."+ ENDL
//             + ":analysis__program a owl:DatatypeProperty ;"
//             + "  rdfs:domain :analysis ;"
//             + "  rdfs:range xsd:string ." + ENDL;

//         Reader genomicReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("genomic.n3"));
//         MergeOwl merger = new MergeOwl(genomicReader, genomicNs, true);
//         //MergeOwl merger = new MergeOwl(new StringReader(mergeSpec), genomicNs);

//         OntModel ms = ModelFactory.createOntologyModel();
//         ms.read(new StringReader(mergeSpec), null, "N3");

//         merger.mergeByEquivalence(ms, genomicNs);
//         assertNotNull(merger.tgtModel.getOntClass(genomicNs + "CpGIsland"));
//         assertNotNull(merger.tgtModel.getOntClass(genomicNs + "TRNA"));
//         //assertNotNull(merger.tgtModel.getOntProperty(genomicNs + "TRNA__contig"));
//         //assertNotNull(merger.tgtModel.getOntProperty(genomicNs + "CpGIsland__contig"));

//         OntModel src = ModelFactory.createOntologyModel();
//         src.read(new StringReader(srcStr), null, "N3");

//         merger.mergeByEquivalence(src, ensemblNs);

//         System.out.println(merger.equiv);
//         System.out.println(merger.subMap);
//         merger.tgtModel.write(System.out, "N3");

//         assertNotNull(merger.tgtModel.getOntClass(genomicNs + "CpGIsland"));
//         assertNotNull(merger.tgtModel.getOntClass(genomicNs + "TRNA"));
//         assertNotNull(merger.tgtModel.getOntProperty(genomicNs + "TRNA__contig"));
//         assertNotNull(merger.tgtModel.getOntProperty(genomicNs + "CpGIsland__contig"));

//     }



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
            + ":Company__name a owl:DatatypeProperty ;" + ENDL
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
            + ":companyName a owl:DatatypeProperty ;" + ENDL
            + "             rdfs:domain :LtdCompany ;" + ENDL
            + "             rdfs:range rdfs:Literal ." + ENDL
            + ":vatNumber a owl:DatatypeProperty ;" + ENDL
            + "           rdfs:domain :LtdCompany ;" + ENDL
            + "           rdfs:range rdfs:Literal ." + ENDL
            + ":address a owl:ObjectProperty ;" + ENDL
            + "      rdfs:domain :LtdCompany ;" + ENDL
            + "      rdfs:range :Address ." + ENDL
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
            + "          rdfs:domain :Corporation ;" + ENDL
            + "          rdfs:range rdfs:Literal ." + ENDL
            + ":corpNumber a rdf:Property ;" + ENDL
            + "            rdfs:domain :Corporation ;" + ENDL
            + "            rdfs:range  rdfs:Literal ." + ENDL;
    }
}
