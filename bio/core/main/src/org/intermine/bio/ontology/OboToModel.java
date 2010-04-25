package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
    protected static String packageName;
    protected static Map<String, Set<String>> childNamesToParentNames;
    protected static Map<String, Set<String>> namesToPartOfs;
    protected static Map<String, String> oboTerms = new HashMap();
    protected Model model;

    /**
     * Constructor.
     *
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public OboToModel(String namespace) {
        this.packageName = namespace;
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
     * Specifies how a class name is generated.
     *
     * @param term the relevant term
     * @return the generated class name
     */
    private static String generateClassName(String className) {
        return packageName + "." + TypeUtil.javaiseClassName(className);
    }

    /**
     * Run conversion from Obo to Model format.
     *
     * @param args oboFilename, modelFilename, packageName (eg. org.intermine.model.bio)
     * @throws Exception if anthing goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new Exception("Usage: newModelName oboFileName modelFileName packageName");
        }

        String newModelName = args[0];
        String oboFilename = args[1];
        String modelFilename = args[2];
        String newPackageName = args[3];

        try {
            File oboFile = new File(oboFilename);
            File modelFile = new File(modelFilename);

            System.out .println("Starting OboToModel conversion from " + oboFilename + " to "
                    + modelFilename);
            OboParser parser = new OboParser();
            parser.processOntology(new FileReader(oboFile));
            parser.processRelations(oboFilename);

            OboToModel owler = new OboToModel(newPackageName);
            owler.processOboTerms(parser.getOboTerms());
            owler.processRelations(parser.getOboRelations());

            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(modelFile)));
            Set<ClassDescriptor> clds = new HashSet();

            // process each oboterm
            for (String childName : owler.oboTerms.values()) {

                // set parents
                StringBuffer parents = new StringBuffer();
                boolean needComma = false;
                Set<String> parentNames = owler.childNamesToParentNames.get(childName);
                if (parentNames != null) {
                    parents = new StringBuffer();
                    for (String parentName : parentNames) {
                        if (owler.oboTerms.containsValue(parentName)) {
                            if (needComma) {
                                parents.append(" ");
                            }
                            needComma = true;			   
			    parents.append(parentName);			   
                        }
                    }
                }
                Set<AttributeDescriptor> fakeAttributes = Collections.emptySet();
                Set<ReferenceDescriptor> fakeReferences = Collections.emptySet();
                Set<CollectionDescriptor> collections = Collections.emptySet();
                Set<String> collectionIdentifiers = owler.namesToPartOfs.get(childName);

                // add collections
                if (collectionIdentifiers != null) {
                    collections = new HashSet();
                    for (String partof : owler.namesToPartOfs.get(childName)) {			
                        // only add collections in our model
                        if (owler.oboTerms.containsValue(partof)) {
                            String[] bits = partof.split("\\.");
                            CollectionDescriptor cd = new CollectionDescriptor(
                                    TypeUtil.javaiseClassName(bits[bits.length - 1])
                                    + "s", partof, null);
                            collections.add(cd);
                        }
                    }
                }
		clds.add(new ClassDescriptor(childName,
                        (parentNames == null ? null : parents.toString()),
                        true, fakeAttributes, fakeReferences, collections));
            }
            Model model = new Model(newModelName, newPackageName, clds);
            out.println(model);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processRelations(List<OboRelation> oboRelations) {
        childNamesToParentNames = new HashMap();
        namesToPartOfs = new HashMap();
        for (OboRelation r : oboRelations) {
            String childName = oboTerms.get(r.childTermId);
            String parentName = oboTerms.get(r.parentTermId);

            if (StringUtils.isEmpty(childName) || StringUtils.isEmpty(parentName)) {
                continue;
            }

	    String relationshipType = r.getRelationship().getName();
            if (relationshipType.equals("part_of") && r.direct) {
                Set<String> partofs = namesToPartOfs.get(childName);
                if (partofs == null) {
                    partofs = new HashSet();
                    namesToPartOfs.put(childName, partofs);
                }
                partofs.add(parentName);
            } else if (relationshipType.equals("is_a") && r.direct) {
                Set<String> parents = childNamesToParentNames.get(childName);
                if (parents == null) {
                    parents = new HashSet();
                    childNamesToParentNames.put(childName, parents);
                }
                parents.add(parentName);
    
            }
        }
    }

    private static void processOboTerms(Set<OboTerm> terms) {
        for (OboTerm term : terms) {
            if (!term.isObsolete()) {
                oboTerms.put(term.getId(),  generateClassName(term.getName()));
            }
        }
    }
}

