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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import org.intermine.metadata.MetaDataException;
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
    protected static Map<String, Set<String>> parentNamesToChildNames;
    protected static Map<String, Set<String>> namesToPartOfs;
    // identifier TO fully qualified intermine name, eg. `org.intermine.bio.Gene`
    protected static Map<String, String> identifierToFullName = new HashMap<String, String>();
    // fully qualified name to identifier
    protected static Map<String, String> fullNameToIdentifier = new HashMap<String, String>();
    protected Model model;

    /**
     * Constructor.
     *
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public OboToModel(String namespace) {
        OboToModel.packageName = namespace;
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
     * @throws Exception if invalid arguments supplied or file can't be found
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("Usage: newModelName oboFileName modelFileName"
                    + " packageName filename");
        }

        String newModelName = args[0];
        String oboFilename = args[1];
        String modelFilename = args[2];
        String newPackageName = args[3];
        String termsFileName = null;
        if (args.length > 4) {
            termsFileName = args[4];
            System.out .println("Filtering by SO terms in " + termsFileName);
        }

        File oboFile = new File(oboFilename);
        File modelFile = new File(modelFilename);

        System.out .println("Starting OboToModel conversion from " + oboFilename + " to "
                + modelFilename);
        OboParser parser = new OboParser();
        parser.processOntology(new FileReader(oboFile));
        parser.processRelations(oboFilename);

        Set<String> termsToKeep = processTermFile(termsFileName);

        OboToModel.createAndWriteModel(parser, termsToKeep, modelFile, newModelName,
                newPackageName);
    }

    public static void createAndWriteModel(OboParser parser, Set<String> termsToKeep,
            File modelFile, String newModelName, String newPackageName)
    throws IOException, MetaDataException {
        OboToModel.processOboTerms(parser.getOboTerms());
        OboToModel.processRelations(parser.getOboRelations(), termsToKeep);

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(modelFile)));
        Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>();

        // process each oboterm
        for (String childName : OboToModel.identifierToFullName.values()) {

            // set parents
            StringBuffer parents = new StringBuffer();
            boolean needComma = false;
            Set<String> parentNames = OboToModel.childNamesToParentNames.get(childName);
            if (parentNames != null) {
                for (String parentName : parentNames) {
                    if (OboToModel.identifierToFullName.containsValue(parentName)) {
                        if (needComma) {
                            parents.append(" ");
                        }
                        needComma = true;
                        parents.append(parentName);
                    }
                }
                String parentString = parents.toString();
                parentString = parentString.trim();
                if (StringUtils.isEmpty(parentString)) {
                    parentNames = null;
                }
            }
            Set<AttributeDescriptor> fakeAttributes = Collections.emptySet();
            Set<ReferenceDescriptor> fakeReferences = Collections.emptySet();
            Set<CollectionDescriptor> collections = Collections.emptySet();
            Set<String> collectionIdentifiers = OboToModel.namesToPartOfs.get(childName);

            // add collections
            if (collectionIdentifiers != null) {
                collections = new HashSet<CollectionDescriptor>();
                for (String partof : OboToModel.namesToPartOfs.get(childName)) {
                    // only add collections in our model
                    if (OboToModel.identifierToFullName.containsValue(partof)) {
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
        System.out .println("Wrote " + modelFile.getCanonicalPath());
    }

    private static void processRelations(List<OboRelation> oboRelations, Set<String> termsToKeep) {
        childNamesToParentNames = new HashMap<String, Set<String>>();
        namesToPartOfs = new HashMap<String, Set<String>>();
        for (OboRelation r : oboRelations) {
            String childName = identifierToFullName.get(r.childTermId);
            String parentName = identifierToFullName.get(r.parentTermId);

            if (StringUtils.isEmpty(childName) || StringUtils.isEmpty(parentName)) {
                continue;
            }

            String relationshipType = r.getRelationship().getName();
            System.out .println("relationship type: " + relationshipType + " parent: "
                    + parentName + " child: " + childName);
            if (relationshipType.equals("part_of") && r.direct) {
                Set<String> partofs = namesToPartOfs.get(childName);
                if (partofs == null) {
                    partofs = new HashSet<String>();
                    namesToPartOfs.put(childName, partofs);
                }
                partofs.add(parentName);
            } else if (relationshipType.equals("is_a") && r.direct) {
                Set<String> parents = childNamesToParentNames.get(childName);
                if (parents == null) {
                    parents = new HashSet<String>();
                    childNamesToParentNames.put(childName, parents);
                }
                parents.add(parentName);
            }
        }

        buildParentsMap();

        assignPartOfsToGrandchildren(oboRelations, namesToPartOfs);

        if (!termsToKeep.isEmpty()) {
            trimModel(termsToKeep);
        }
    }

    private static void assignPartOfsToGrandchildren(List<OboRelation> oboRelations,
    		Map<String, Set<String>> namesToPartOfs) {
        childNamesToParentNames = new HashMap<String, Set<String>>();
        namesToPartOfs = new HashMap<String, Set<String>>();
        for (OboRelation r : oboRelations) {
            String childName = identifierToFullName.get(r.childTermId);
            String parentName = identifierToFullName.get(r.parentTermId);

            if (StringUtils.isEmpty(childName) || StringUtils.isEmpty(parentName)) {
                continue;
            }

            String relationshipType = r.getRelationship().getName();
            if (relationshipType.equals("part_of") && r.direct) {
                Set<String> grandchildren = parentNamesToChildNames.get(childName);
                if (grandchildren == null || grandchildren.isEmpty()) {
                    continue;
                }
                for (String grandchild : grandchildren) {
                    Set<String> partofs = namesToPartOfs.get(grandchild);
                    if (partofs == null) {
                        partofs = new HashSet<String>();
                        namesToPartOfs.put(grandchild, partofs);
                    }
                    partofs.add(parentName);
                }
            }
        }
    }


    // build parent --> children map
    private static void buildParentsMap() {
        parentNamesToChildNames = new HashMap<String, Set<String>>();
        for (String child : childNamesToParentNames.keySet()) {
            Set<String> parents = childNamesToParentNames.get(child);
            for (String parent : parents) {
                Set<String> kids = parentNamesToChildNames.get(parent);
                if (kids == null) {
                    kids = new HashSet<String>();
                    parentNamesToChildNames.put(parent, kids);
                }
                kids.add(child);
            }
        }
    }

    private static void trimModel(Set<String> termsToKeep) {
    	
        Map<String, String> oboTermsCopy = new HashMap<String, String>(identifierToFullName);

        System.out .println("Total terms: " + identifierToFullName.size());

        for (String oboTerm : oboTermsCopy.values()) {
            if (!termsToKeep.contains(oboTerm)) {
                prune(oboTerm);
            }
        }

        System.out .println("Total terms, post-pruning: " + identifierToFullName.size());

        oboTermsCopy = new HashMap<String, String>(identifierToFullName);

        for (String oboTerm : oboTermsCopy.values()) {
            if (!termsToKeep.contains(oboTerm)) {
                flatten(oboTerm);
            }
        }

        System.out .println("Total terms, post-flattening: " + identifierToFullName.size());
    }

    /*
     * remove term if:
     *  1. not in list of desired terms
     *  2. no children
     */
    private static void prune(String oboTermName) {
        // process each child term
        if (parentNamesToChildNames.get(oboTermName) != null) {
            Set<String> children = new HashSet<String>(parentNamesToChildNames.get(oboTermName));
            for (String childName : children) {
                prune(childName);
            }
        }

        // if this term has no children AND it's not on our list = DELETE
        if (!parentNamesToChildNames.containsKey(oboTermName)) {
            removeTerm(oboTermName);
        }
    }

    /*
     * remove term if not on list AND:
     *  (a) term has only ONE parent and ONE child
     *  (b) term has only ONE parent and NO children
     *  (c) term has NO parents and only ONE child
     */
    private static void flatten(String oboTerm) {

        // process children of this term first
        // can't do this, `Exception in thread "main" java.lang.StackOverflowError`
        //        if (parentNamesToChildNames.get(oboTerm) != null) {
        //            Set<String> children = new HashSet(parentNamesToChildNames.get(oboTerm));
        //            for (String child : children) {
        //                flatten(child);
        //            }
        //        }

        Set<String> parents = childNamesToParentNames.get(oboTerm);
        Set<String> kids = parentNamesToChildNames.get(oboTerm);

        // has both parents and children
        if (parents != null && kids != null) {

            // multiple parents and children.  can't flatten.
            if (parents.size() > 1 && kids.size() > 1) {
                return;
            }

            // term only has one parent.  remove term and assign this terms parents and children
            // to each other
            if (parents.size() == 1) {
                String parent = parents.toArray()[0].toString();

                // add children to new parent
                parentNamesToChildNames.get(parent).addAll(kids);

                // add parent to new children
                for (String kid : kids) {
                    Set<String> otherParents = childNamesToParentNames.get(kid);
                    transferPartOfs(oboTerm, kid);
                    otherParents.remove(oboTerm);
                    otherParents.add(parent);
                }
                System.out .println("Flattening: " + oboTerm);
                removeTerm(oboTerm);
                return;
            }

            // term has only one child.  remove term and assign child to new parents.
            if (kids.size() == 1) {
                String kid = kids.toArray()[0].toString();

                // add parents to new kid
                childNamesToParentNames .get(kid).addAll(parents);

                // pass down any relationships before parent term is removed from tree
                transferPartOfs(oboTerm, kid);

                // reassign parents to new kid
                for (String parent : parents) {
                    Set<String> otherChildren = parentNamesToChildNames.get(parent);
                    otherChildren.remove(oboTerm);
                    otherChildren.add(kid);
                }
                System.out .println("Flattening: " + oboTerm);
                removeTerm(oboTerm);
                return;
            }

            // root term
        } else if (parents == null) {
            // leave roots
        }

        // no children, delete!
        if (kids == null) {
            System.out .println("Flattening: " + oboTerm);
            removeTerm(oboTerm);
        }
    }

    // pass down any relationships before parent term is removed from tree
    // parent will be removed from partOf map in removeTerm() - only after all children have
    // been processed
    private static void transferPartOfs(String parentOboTerm, String childOboTerm) {
        Set<String> partOfs = new HashSet<String>(namesToPartOfs.get(parentOboTerm));
        namesToPartOfs.put(childOboTerm, partOfs);
    }

    // remove term from every map
    private static void removeTerm(String oboTerm) {

    	identifierToFullName.remove(fullNameToIdentifier.get(oboTerm));
    	childNamesToParentNames.remove(oboTerm);
    	parentNamesToChildNames.remove(oboTerm);
    	namesToPartOfs.remove(oboTerm);
    	removeCollections(oboTerm);

    	// remove mention in maps
    	Map<String, Set<String>> mapCopy
    	= new HashMap<String, Set<String>>(parentNamesToChildNames);
    	for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
    		String parent = entry.getKey();
    		Set<String> children = entry.getValue();

    		// remove current term
    		children.remove(oboTerm);

    		// if parent is childless, remove
    		if (children.size() == 0) {
    			parentNamesToChildNames.remove(parent);
    		}
    	}

    	mapCopy = new HashMap<String, Set<String>>(childNamesToParentNames);
    	for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
    		String child = entry.getKey();
    		Set<String> parents = entry.getValue();

    		// remove current term
    		parents.remove(oboTerm);

    		// if child has no parents remove from p
    		if (parents.size() == 0) {
    			childNamesToParentNames.remove(child);
    		}
    	}
    }

    private static void removeCollections(String oboTerm) {
        Map<String, Set<String>> mapCopy = new HashMap<String, Set<String>>(namesToPartOfs);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            Set<String> partOfs = entry.getValue();
            if (partOfs.contains(oboTerm)) {
                partOfs.remove(oboTerm);
            }
        }
    }

    private static void processOboTerms(Set<OboTerm> terms) {
        for (OboTerm term : terms) {
            if (!term.isObsolete()) {
                String identifier = term.getId().trim();
                String name = term.getName().trim();
                if (!StringUtils.isEmpty(identifier) && !StringUtils.isEmpty(name)) {
                    identifierToFullName.put(identifier,  generateClassName(name));
                    fullNameToIdentifier.put(generateClassName(name), identifier);
                }
            }
        }
    }

    // only these terms (and dependents) will be processed
    private static Set<String> processTermFile(String filename) {
        Set<String> terms = new HashSet<String>();
        try {
            BufferedReader br =  new BufferedReader(new FileReader(filename));
            try {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (StringUtils.isNotEmpty(line)) {
                        terms.add(generateClassName(line));
                    }
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return terms;
    }
}
