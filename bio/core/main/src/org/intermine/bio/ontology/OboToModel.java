package org.intermine.bio.ontology;

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
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.TypeUtil;

/**
 * Processes list of root OboTerms to produce the equivalent Model
 *
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class OboToModel
{
    protected String namespace;
    protected Map<String, Set<String>> nameToSupers = new HashMap();
    protected Map<String, Set<String>> nameToPartofs = new HashMap();
    protected Model model;

    /**
     * Constructor.
     *
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public OboToModel(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Return the model.
     *
     * @return the model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Perform the conversion by iterating over the root terms.
     *
     * @param rootTerms a collection of rootTerms
     */
    public void process(Collection<OboTerm> rootTerms) {
        for (OboTerm rootTerm : rootTerms) {
            process(rootTerm, null);
        }
    }

    /**
     * Convert a (root) DagTerm to a Resource, recursing through children.
     *
     * @param term a DagTerm
     * @return true if this Term was included (part of SOFA)
     */
    public boolean process(OboTerm term, String superClassName) {
        String className = generateClassName(term);
        boolean include = false;
        for (Iterator i = term.getChildren().iterator(); i.hasNext(); ) {
            OboTerm subTerm = (OboTerm) i.next();
            include = process(subTerm, className) || include;
        }
        if (term.isObsolete()) {
            return include;
        }
        Map tagValues = term.getTagValues();
        List subsets = null;
        if (tagValues != null) {
            subsets = (List) tagValues.get("subset");
        } else {
            System.err.println("No tag values for term " + term.getName());
        }
        include = include || ((subsets != null) && subsets.contains("SOFA"));
        if (include) {
            Set<String> supers = nameToSupers.get(className);
            if (supers == null) {
                supers = new HashSet();
                nameToSupers.put(className, supers);
            }
            if (superClassName != null) {
                supers.add(superClassName);
            }
            Set partofs = nameToPartofs.get(className);
            if (partofs == null) {
                partofs = new HashSet();
                nameToPartofs.put(className, partofs);
            }
            for (OboTerm component : ((Collection<OboTerm>) term.getComponents())) {
                partofs.add(generateClassName(component));
            }
        }
        return include;
    }

    /**
     * Specifies how a class name is generated.
     *
     * @param term the relevant term
     * @return the generated class name
     */
    public String generateClassName(DagTerm term) {
        return namespace + TypeUtil.javaiseClassName(term.getName());
    }

    /**
     * Run conversion from Obo to Model format.
     *
     * @param args oboFilename, modelFilename, namespace, errorFilename
     * @throws Exception if anthing goes wrong
     */
    public static void main(String[] args) throws Exception {
        if ((args.length < 3) || (args.length > 4)) {
            throw new Exception("Usage: Dag2Owl dagfile owlfile tgt_namespace errorfile");
        }

        String oboFilename = args[0];
        String modelFilename = args[1];
        String tgtNamespace = args[2];
        String errorFilename = (args.length > 3) ? args[3] : null;

        try {
            File oboFile = new File(oboFilename);
            File modelFile = new File(modelFilename);

            System.out .println("Starting OboToModel conversion from " + oboFilename + " to "
                    + modelFilename);
            OboParser parser = new OboParser();
            Set rootTerms = parser.processForLabellingOntology(new FileReader(oboFile));

            DagValidator validator = new DagValidator();
            if (!validator.validate(rootTerms)) {
                if (errorFilename != null) {
                    BufferedWriter out =
                        new BufferedWriter(new FileWriter(new File(errorFilename)));
                    out.write(validator.getOutput());
                    out.flush();
                } else {
                    System.err.print(validator.getOutput());
                    System.err.flush();
                }
            }

            OboToModel owler = new OboToModel(tgtNamespace);
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(modelFile)));
            owler.process(rootTerms);
            Set<ClassDescriptor> clds = new HashSet();
            for (Map.Entry<String, Set<String>> entry : owler.nameToSupers.entrySet()) {
                String className = entry.getKey();
                StringBuffer supers = new StringBuffer();
                boolean needComma = false;
                for (String superClassName : entry.getValue()) {
                    if (needComma) {
                        supers.append(" ");
                    }
                    needComma = true;
                    supers.append(superClassName);
                }
                Set<AttributeDescriptor> fakeAttributes = Collections.emptySet();
                Set<ReferenceDescriptor> fakeReferences = Collections.emptySet();
                Set<CollectionDescriptor> collections = new HashSet();
                for (String partof : owler.nameToPartofs.get(className)) {
                    if (owler.nameToSupers.containsKey(partof)) {
                        collections.add(new CollectionDescriptor(TypeUtil.javaiseClassName(partof)
                                    + "s", partof, null));
                    }
                }
                clds.add(new ClassDescriptor(className,
                                             (entry.getValue().isEmpty() ?
                                              null :
                                              supers.toString()),
                                             true, fakeAttributes, fakeReferences, collections));
            }
            Model model = new Model("name", "whatever", clds);
            out.println(model);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

