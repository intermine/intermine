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
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
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
    private OntModel mergeModel;
    protected OntModel tgtModel = null;
    private final String tgtNamespace;

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
        mergeModel = m.process(mergeSpec);
        tgtModel = mergeModel;
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
     * namespace in an OntModel) add all resources to target model according to the merge
     * specification.  If a resource is found to be equivalent to a resource already in
     * target model nothing needs to be done, otherwise transfers all resources directly
     * to new namespace in target model.
     * @param source a Jena OntModel representing the source OWL
     * @param srcNamespace the namespace of the source OWL ontology
     */
    protected void mergeByEquivalence(OntModel source, String srcNamespace) {
        // map of reosource's local name in srcNamespace to Resource object in target namespace
        Map equiv = new HashMap(); // set of resource names in tgt space

        // find names of classes/properties in source namespace that are equivalent to a
        // classes/properties already in target namespace (from merge spec)
        Iterator stmtIter = tgtModel.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getPredicate().getLocalName().equals("equivalentClass")
                || stmt.getPredicate().getLocalName().equals("equivalentProperty")) {
                Resource res = stmt.getResource();
                if (res.getNameSpace().equals(srcNamespace)) {
                    //equiv.add(res.getLocalName());
                    equiv.put(res.getLocalName(), stmt.getSubject());
                }
            }
        }

        // merge classes
        Set equivNames = equiv.keySet();
        Iterator srcIter = source.listClasses();
        while (srcIter.hasNext()) {
            OntClass ont = (OntClass) srcIter.next();
            if (!equivNames.contains(ont.getLocalName())) {
                OntClass tgtOnt = tgtModel.createClass(tgtNamespace + ont.getLocalName());
                tgtOnt.addEquivalentClass(ont);
                if (ont.listLabels(null).hasNext()) {
                    tgtOnt.setLabel(ont.getLabel(null), null);
                }
            }
        }

        // merge properties
        srcIter = source.listOntProperties();
        while (srcIter.hasNext()) {
            OntProperty srcProp = (OntProperty) srcIter.next();
            if (srcProp.getNameSpace().equals(srcNamespace)
                && !equivNames.contains(srcProp.getLocalName())) {
                OntProperty tgtProp = tgtModel
                    .createOntProperty(tgtNamespace + srcProp.getLocalName());
                tgtProp.addEquivalentProperty(srcProp);
                if (srcProp.listLabels(null).hasNext()) {
                    tgtProp.setLabel(srcProp.getLabel(null), null);
                }

                Iterator domainIter = srcProp.listDomain();
                while (domainIter.hasNext()) {
                    // domain might map to a different name in target
                    Resource domain = (Resource) domainIter.next();
                    if (equiv.containsKey(domain.getLocalName())) {
                        tgtProp.addDomain((Resource) equiv.get(domain.getLocalName()));
                    } else {
                        tgtProp.addDomain(tgtModel.getOntClass(tgtNamespace
                                                               + domain.getLocalName()));
                    }
                }
            }
        }
    }
}

