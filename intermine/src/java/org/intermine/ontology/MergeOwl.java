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

import java.io.Reader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.*;


/**
 * Class to merge OWL ontologies according to an OWL format merge specification.  Construct
 * with a Reader for the merge spec and a namespace for the target model.  The merge specificaion
 * becomes the target model and other OWL sources are added by addToTargetOwl() method.
 * mergeByEquivalence() checks each toplevel node in the source to see if it is an equivalentClass
 * to a class already in target model.  If so, do nothing, otherwise perform direct transfer
 * of resources to the new namesapce.
 *
 * @author Richard Smith
 */
public class MergeOwl
{
    protected OntModel tgtModel = null;
    private final String tgtNamespace;
    private final String owlNamespace = "http://www.w3.org/2002/07/owl#";
    protected Map equiv;

    /**
     * Construct with a Reader for the merge spec (which becomes the initial target model) and
     * a namespace URI for the target model.
     * @param mergeSpec a Reader pointing to and OWL file defining the OWL merge
     * @param tgtNamespace a String representing the URI of the target model namespace
     * @throws IOException if problem with Reader occurs
     */
    public MergeOwl(Reader mergeSpec, String tgtNamespace) throws IOException {
        this.tgtNamespace = tgtNamespace;
        MergeSpec2Owl m = new MergeSpec2Owl();
        tgtModel = m.process(mergeSpec);
    }

    /**
     * Get the merged target model and a Jena OntModel.
     * @return the merged ontology model
     */
    public OntModel getTargetModel() {
        return this.tgtModel;
    }

    /**
     * Add a source OWL ontology to the target OWL ontology.
     * @param sourceOwl a Reader pointing to the source OWL document
     * @param srcNamespace string represencting the URI namespace of source OWL document
     */
    protected void addToTargetOwl(Reader sourceOwl, String srcNamespace) {
        OntModel source = ModelFactory.createOntologyModel();
        source.read(sourceOwl, null, "RDF/XML");
        mergeByEquivalence(source, srcNamespace);
    }

    /**
     * Given a source OWL model and the desired source namespace (there may be more than one
     * namespace in an OntModel) transfer all statements to target model according to the merge
     * specification.  If a resource is found to be equivalent to a resource already in
     * target model nothing needs to be done, otherwise transfers all statements directly
     * to new namespace in target model.
     * @param srcModel a Jena OntModel representing the source OWL
     * @param srcNamespace the namespace of the source OWL ontology
     */
    protected void mergeByEquivalence(OntModel srcModel, String srcNamespace) {
        // map of URIs (i.e. names of resources) in srcNamespace to equivalent Resources in target
        equiv = new HashMap();

        // find classes/properties/individuals in source namespace that are equivalent to
        // classes/properties/individuals already in target namespace (from merge spec)
        Iterator stmtIter = tgtModel.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getPredicate().getLocalName().equals("equivalentClass")
                || stmt.getPredicate().getLocalName().equals("equivalentProperty")
                || stmt.getPredicate().getLocalName().equals("sameAs")) {
                Resource res = stmt.getResource();
                if (res.getNameSpace().equals(srcNamespace)) {
                    equiv.put(res.getURI(), stmt.getSubject());
                }
            }
        }

        // adding statements to Jena model by batch method supposedly faster
        List statements = new ArrayList();

        // transfer statements to target namespace
        stmtIter = srcModel.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            Resource subject = stmt.getSubject();
            if (subject.getNameSpace().equals(srcNamespace)) {
                if (stmt.getPredicate().getLocalName().equals("type")) {
                    if (!equiv.containsKey(subject.getURI())) {
                        Resource target = tgtModel.createResource(tgtNamespace
                                                                  + subject.getLocalName());
                        Resource object = getTargetResource(stmt.getResource(), srcNamespace);
                        statements.add(tgtModel.createStatement(target,
                                                                stmt.getPredicate(), object));
                        addEquivalenceStatement(target, object, subject, statements);
                    }
                } else {
                    subject = getTargetResource(subject, srcNamespace);
                    RDFNode object = stmt.getObject();
                    if (object instanceof Resource) {
                        object = getTargetResource((Resource) object, srcNamespace);
                    }
                    statements.add(tgtModel.createStatement(subject, stmt.getPredicate(), object));
                }
            }
        }
        tgtModel.add(statements);
    }

    /**
     * Given a resource and the namespace of the source OWL find the equivalent resource
     * already in the target or create a new resource with this name in the target namespace.
     * @param res the resource to find or create
     * @param srcNamespace namespace of the source OWL
     * @return the equivalent or newly create Resource in the target namespace
     */
    protected Resource getTargetResource(Resource res, String srcNamespace) {
        if (equiv.containsKey(res.getURI())) {
            return (Resource) equiv.get(res.getURI());
        } else if (res.getNameSpace().equals(srcNamespace)) {
            return tgtModel.createResource(tgtNamespace + res.getLocalName());
        }
        return res;
    }

    /**
     * Add to statements list the appropriate equivalence statement between a new resource
     * in the target namespace and the name in the source namespace.  owl:equivalentClass,
     * owl:equivalentProperty or owl:sameAs selected depending on type of object.
     * @param target resource created in target namespace
     * @param object object of the triple in source OWL - i.e. what the rdf:type actually is
     * @param original resource in source namespace to point equivalence statement at
     * @param statements the list of statements generated for target model
     */
    protected void addEquivalenceStatement(Resource target, Resource object, Resource original,
                                           List statements) {
        if (object.getLocalName().equals("Class")) {
            statements.add(tgtModel.createStatement(target,
                                                    tgtModel.createProperty(owlNamespace,
                                                                            "equivalentClass"),
                                                    original));
        } else if (object.getLocalName().equals("Property")) {
            statements.add(tgtModel.createStatement(target,
                                                    tgtModel.createProperty(owlNamespace,
                                                                            "equivalentProperty"),
                                                    original));
        } else if (object.getLocalName().equals("Individual")) {
            statements.add(tgtModel.createStatement(target,
                                                    tgtModel.createProperty(owlNamespace,
                                                                            "sameAs"),
                                                    original));
        }
    }
}

