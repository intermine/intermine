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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.ObjectProperty;
import org.flymine.util.StringUtil;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;

/**
 * Processes list of root DagTerms to produce the equivalent OWL OntModel
 *
 * @author Mark Woodbridge
 */
public class Dag2Owl
{
    protected String namespace;
    protected OntModel ontModel;
    
    /**
     * Constructor
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public Dag2Owl(String namespace) {
        if (namespace.indexOf("#") != -1) {
            throw new IllegalArgumentException("namespace shouldn't contain '#'");
        }
        this.namespace = namespace;
        ontModel = ModelFactory.createOntologyModel();
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
    }

    /**
     * Convert a (root) DagTerm to a OntClass, recursing through children
     * @param term a DagTerm
     * @return the corresponding OntClass
     */
    public OntClass process(DagTerm term) {
        OntClass cls = ontModel.getOntClass(generateClassName(term));
        if (cls == null) {
            cls = ontModel.createClass(generateClassName(term));
            cls.setLabel(term.getName(), null);
            // set synonyms
            if (term.getSynonyms().size() > 0) {
                cls.addComment("synonyms=" + StringUtil.join(term.getSynonyms(), ", "), null);
            }
            // create partof property
            for (Iterator i = term.getComponents().iterator(); i.hasNext(); ) {
                DagTerm component = (DagTerm) i.next();
                ObjectProperty prop = ontModel.createObjectProperty(generatePropertyName(
                                                                                         component,
                                                                                         term));
                prop.setDomain(process(component));
                prop.setRange(cls);
            }
            // set subclasses
            for (Iterator i = term.getChildren().iterator(); i.hasNext(); ) {
                cls.addSubClass(process((DagTerm) i.next()));
            }
        }
        return cls;
    }

    /**
     * Specifies how a class name is generated
     * @param term the relevant term
     * @return the generated class name
     */
    public String generateClassName(DagTerm term) {
        return namespace + "#" + filter(term.getId());
    }

    /**
     * Specifies how a property name is generated
     * @param domain the domain term
     * @param range the range term
     * @return the generated property name
     */
    public String generatePropertyName(DagTerm domain, DagTerm range) {
        return namespace + "#" + filter(domain.getId() + "$" + range.getId());
    }

    /**
     * Filter a URI fragment to remove illegal characters
     * @param s the relevant string
     * @return the filtered string
     */
    public String filter(String s) {
        return s.replaceAll(":", "_");
    }

    /**
     * Run conversion from DAG to OWL format, still produces OWL if validation fails,
     * details of problems written to error file.
     * @param args dagFilename, owlFilename, errorFilename
     * @throws Exception if anthing goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("Usage: Dag2Owl dagfile owlfile errorfile");
        }

        String dagFilename = args[0];
        String owlFilename = args[1];
        String errorFilename = args[2];

        try {
            File dagFile = new File(dagFilename);
            File owlFile = new File(owlFilename);
            File errorFile = new File(errorFilename);

            DagParser parser = new DagParser();
            Set rootTerms = parser.process(new BufferedReader(new FileReader(dagFile)));

            DagValidator validator = new DagValidator();
            if (!validator.validate(rootTerms)) {
                 BufferedWriter out = new BufferedWriter(new FileWriter(errorFile));
                 out.write(validator.getOutput());
            }

            Dag2Owl owler = new Dag2Owl("http://www.flymine.org/namespace");
            BufferedWriter out = new BufferedWriter(new FileWriter(owlFile));
            owler.process(rootTerms);
            owler.getOntModel().write(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

