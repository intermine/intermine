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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.XmlUtil;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Processes list of root DagTerms to produce the equivalent OWL OntModel
 *
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class Dag2Owl
{
    protected boolean inversePartOf; //if true, generate inverse partOf relationships

    protected String namespace;
    protected OntModel ontModel;
    protected Map nameToResource = new HashMap();
    protected List statements = new ArrayList();

    protected static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
    protected static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    protected static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";

    protected Resource owlClass;
    protected Property owlInverseOf;
    protected Resource owlObjectProperty;
    protected Property rdfType;
    protected Property rdfsComment;
    protected Property rdfsDomain;
    protected Property rdfsLabel;
    protected Property rdfsRange;
    protected Property rdfsSubClassOf;

    /**
     * Constructor
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public Dag2Owl(String namespace) {
      this(namespace, true);
    }

    /**
     * Constructor
     * @param namespace the namespace to use in generating URI-based identifiers
     * @param inversePartOf if ture, generates the inverse partOf relationship
     */
    public Dag2Owl(String namespace, boolean inversePartOf) {
        this.namespace = XmlUtil.correctNamespace(namespace);
        this.inversePartOf = inversePartOf;
        ontModel = ModelFactory.createOntologyModel();
        owlClass = ontModel.createResource(OWL_NS + "Class");
        owlInverseOf = ontModel.createProperty(OWL_NS + "inverseOf");
        owlObjectProperty = ontModel.createResource(OWL_NS + "ObjectProperty");
        rdfType = ontModel.createProperty(RDF_NS + "type");
        rdfsComment = ontModel.createProperty(RDFS_NS + "comment");
        rdfsDomain = ontModel.createProperty(RDFS_NS + "domain");
        rdfsLabel = ontModel.createProperty(RDFS_NS + "label");
        rdfsRange = ontModel.createProperty(RDFS_NS + "range");
        rdfsSubClassOf = ontModel.createProperty(RDFS_NS + "subClassOf");
    }

    /**
     * Return the model
     * @return the model
     */
    public OntModel getOntModel() {
        return ontModel;
    }

    /**
     * Perform the conversion by iterating over the root terms
     * @param rootTerms a collection of rootTerms
     */
    public void process(Collection rootTerms) {
        for (Iterator i = rootTerms.iterator(); i.hasNext(); ) {
            process((DagTerm) i.next());
        }
        ontModel.add(statements);
    }

    /**
     * Convert a (root) DagTerm to a Resource, recursing through children
     * @param term a DagTerm
     * @return the corresponding OntClass
     */
    public Resource process(DagTerm term) {
        System .out.println("Processing term " + term.getName() + ": ");
        Resource cls = (Resource) nameToResource.get(term.getName());
        if (cls == null) {
            long start = System.currentTimeMillis();
            cls = ontModel.createResource(generateClassName(term));
            System .out.println("createClass: " + (System.currentTimeMillis() - start) + " ms");
            statements.add(ontModel.createStatement(cls, rdfType, owlClass));
            statements.add(ontModel.createStatement(cls, rdfsLabel, term.getName()));
            // set synonyms
            if (term.getSynonyms().size() > 0) {
                statements.add(ontModel.createStatement(cls, rdfsComment,
                            "synonyms=" + StringUtil.join(term.getSynonyms(), ", ")));
            }
            // create partof / inverse PartOf property
            for (Iterator i = term.getComponents().iterator(); i.hasNext(); ) {
                DagTerm component = (DagTerm) i.next();
                Resource range = process(component);
                Resource prop = ontModel.createResource(generatePropertyName(term, component));
                statements.add(ontModel.createStatement(prop, rdfType, owlObjectProperty));
                statements.add(ontModel.createStatement(prop, rdfsDomain, cls));
                statements.add(ontModel.createStatement(prop, rdfsRange, range));
                if (inversePartOf) {
                    Resource revProp =
                        ontModel.createResource(generatePropertyName(component, term));
                    statements.add(ontModel.createStatement(revProp, rdfType, owlObjectProperty));
                    statements.add(ontModel.createStatement(revProp, owlInverseOf, prop));
                }
            }
            // set subclasses
            for (Iterator i = term.getChildren().iterator(); i.hasNext(); ) {
                DagTerm subTerm = (DagTerm) i.next();
                Resource subCls = process(subTerm);
                statements.add(ontModel.createStatement(subCls, rdfsSubClassOf, cls));
            }
            nameToResource.put(term.getName(), cls);
        } else {
            System .out.println("already present");
        }
        return cls;
    }

    /**
     * Specifies how a class name is generated
     * @param term the relevant term
     * @return the generated class name
     */
    public String generateClassName(DagTerm term) {
        return namespace + TypeUtil.javaiseClassName(term.getName());
    }

    /**
     * Specifies how a property name is generated
     * @param domain the domain term
     * @param range the range term
     * @return the generated property name
     */
    public String generatePropertyName(DagTerm domain, DagTerm range) {
        String propName = TypeUtil.javaiseClassName(range.getName()) + "s";  // pluralise
        if (Character.isLowerCase(propName.charAt(1))) {
            propName = StringUtil.decapitalise(propName);
        }
        return OntologyUtil.generatePropertyName(namespace,
                TypeUtil.javaiseClassName(domain.getName()), propName);
    }

    /**
     * Run conversion from DAG to OWL format, still produces OWL if validation fails,
     * details of problems written to error file.
     * @param args dagFilename, owlFilename, errorFilename
     * @throws Exception if anthing goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new Exception("Usage: Dag2Owl dagfile owlfile tgt_namespace errorfile");
        }

        String dagFilename = args[0];
        String owlFilename = args[1];
        String tgtNamespace = args[2];
        String errorFilename = (args.length > 3) ? args[3] : "";

        try {
            File dagFile = new File(dagFilename);
            File owlFile = new File(owlFilename);

            System.out .println("Starting Dag2Owl conversion from " + dagFilename + " to "
                    + owlFilename);
            DagParser parser = new DagParser();
            Set rootTerms = parser.processForClassHeirarchy(new FileReader(dagFile));

            DagValidator validator = new DagValidator();
            if (!validator.validate(rootTerms) && !errorFilename.equals("")) {
                BufferedWriter out = new BufferedWriter(new FileWriter(new File(errorFilename)));
                out.write(validator.getOutput());
                out.flush();
            }

            Dag2Owl owler = new Dag2Owl(tgtNamespace);
            BufferedWriter out = new BufferedWriter(new FileWriter(owlFile));
            owler.process(rootTerms);
            owler.getOntModel().write(out, "N3");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

