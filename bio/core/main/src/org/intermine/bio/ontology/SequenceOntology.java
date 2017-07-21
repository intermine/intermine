package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.StringUtil;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;

/**
 * Helper class for SequenceOntology.  Keeps track of all obo relationships and filters on terms
 * (if provided).
 *
 * @author Julie Sullivan
 */
public class SequenceOntology
{
    private Map<String, Set<String>> childToParents, parentToChildren, partOfs;
    // SO terms to filter on, eg. sequence_feature
    private Set<String> termsToKeep = new HashSet<String>();
    // partOf relationships that are many-to-many.  default is many-to-one
    private Map<String, Set<String>> manyToManyPartOfs = new HashMap<String, Set<String>>();

    // list of classes to load into the model.  OboTerm is an object that contains the SO term
    // value eg. sequence_feature and the Java name, eg. org.intermine.bio.SequenceFeature
    private Map<String, OboTerm> validOboTerms = new HashMap<String, OboTerm>();

    // contains ALL non-obsolete terms.  key = sequence_feature, value = SO:001
    private Map<String, String> oboNameToIdentifier = new HashMap<String, String>();

    // special case for sequence_feature, we always need this term in the model
    private static final String SEQUENCE_FEATURE = "SO:0000110";

    private static final boolean DEBUG = false;

    private Map<String, Set<String>> reversePartOfs = new HashMap<String, Set<String>>();

    private static final String CHROMOSOME = "SO:0000340";

    // class descriptors for classes found in SO OBO file, after filtering
    private Set<ClassDescriptor> classes;

    private static final String NAMESPACE = "org.intermine.model.bio";
    private static final String NAME = "so";

    private Model soModel = null;

    /**
     * Constructor.
     *
     * @param termsFile file containing list of SO terms to filter on
     * @param oboFileName full path of OBO file, needed for OboParser
     */
    public SequenceOntology(String oboFileName, InputStream termsFile) {

        if (termsFile != null) {
            //  parse SO terms from config file
            processTerms(termsFile);
        }

        // parse all SO terms, filter on SO terms in config file
        process(oboFileName);
    }

    /**
     * @return set of class descriptors created from SO OBO file
     */
    public Model getModel() {
        return soModel;
    }

    /**
     * @param childIdentifier the oboterm to get relationships for
     * @return all collections for given class
     */
    private Set<String> getPartOfs(String childIdentifier) {
        return partOfs.get(childIdentifier);
    }

    /**
     * @param childIdentifier the oboterm to get relationships for
     * @return all collections for given class
     */
    private Set<String> getReversePartOfs(String childIdentifier) {
        return reversePartOfs.get(childIdentifier);
    }

    /**
     *
     * @param name name of child term, eg "exon"
     * @return list of parent names
     */
    public Set<String> getAllPartOfs(String name) {
        String identifier = getIdentifier(name);
        if (identifier == null) {
            return Collections.emptySet();
        }
        Set<String> parentIdentifiers = getPartOfs(identifier);
        if (parentIdentifiers == null) {
            return Collections.emptySet();
        }
        Set<String> parents = new HashSet<String>();
        for (String ident : parentIdentifiers) {
            String parent = getName(ident);
            if (StringUtils.isNotEmpty(parent)) {
                // add this class name
                parents.add(parent);

                // add all of this term's partOfs too
                parents.addAll(getAllPartOfs(parent));
            }
        }
        return parents;
    }

    /**
     * Test is class is in the model.  Only used after the obo terms have been processed and
     * trimmed.
     *
     * @param identifier for obo term
     * @return true if the class is in the model
     */
    private boolean classInModel(String identifier) {
        if (validOboTerms == null) {
            return false;
        }
        return validOboTerms.containsKey(identifier);
    }

    /**
     * In the OBO file, part_of relationships are processed as many-to-one relationships.  Special
     * exceptions are listed in the config file and are processed as many-to-many relationships.
     *
     * @param parent the term to test
     * @param child the term to test
     * @return TRUE if this term is listed in the config file as a many-to-many relationship.
     */
    private boolean isManyToMany(String parent, String child) {
        if (!testManyToMany(parent, child)) {
            return testManyToMany(child, parent);
        }
        return true;
    }

    private boolean testManyToMany(String parent, String child) {
        Set<String> manyToManyConfig = manyToManyPartOfs.get(getName(child));
        if (manyToManyConfig == null) {
            return false;
        }
        if (manyToManyConfig.contains(getName(parent))) {
            return true;
        }
        return false;
    }

    private String getIdentifier(String name) {
        return oboNameToIdentifier.get(name);
    }


    /**
     * Test whether a term is in the list the user provided.
     *
     * @param identifier for obo term
     * @return false if oboterm isn't in list user provided, true if term in list or list empty
     */
    private boolean validTerm(String identifier) {
        OboTerm o = validOboTerms.get(identifier);
        if (o == null) {
            return false;
        }
        String oboName = o.getName();
        if (termsToKeep.isEmpty() || termsToKeep.contains(oboName)
                || SEQUENCE_FEATURE.equals(identifier)) {
            return true;
        }
        return false;
    }

    // this term has children
    private boolean validParent(String identifier) {
        return parentToChildren.containsKey(identifier);
    }

    /**
     * @param childIdentifier identifier for obo term of interest
     * @return list of identifiers for parent obo terms
     */
    private Set<String> getParentIdentifiers(String childIdentifier) {
        return childToParents.get(childIdentifier);
    }

    /**
     * @param identifier for obo term
     * @return name of term, eg. sequence_feature
     */
    private String getName(String identifier) {
        OboTerm o = validOboTerms.get(identifier);
        if (o == null) {
            return null;
        }
        return o.getName();
    }

    /**
     * Returns list of (valid for this model) obo term identifiers.
     *
     * @return set of obo term identifiers to process, eg. SO:001
     */
    private Set<String> getOboTermIdentifiers() {
        return validOboTerms.keySet();
    }

    /**
     * Processes obo relations from OBOEdit.  Parses each relationship and builds collections and
     * parents.  Also assigns grandchildren the collections of the grandparents and trims/flattens
     * terms if the user has provided a terms list.
     *
     * @param oboRelations List of obo relations from OBOEdit
     */
    private void processRelations(List<OboRelation> oboRelations) {
        childToParents = new HashMap<String, Set<String>>();
        partOfs = new HashMap<String, Set<String>>();
        for (OboRelation r : oboRelations) {
            String child = r.childTermId;
            String parent = r.parentTermId;
            if (StringUtils.isEmpty(child) || StringUtils.isEmpty(parent) || !classInModel(child)) {
                continue;
            }
            String relationshipType = r.getRelationship().getName();
            /**
             * PART_OF
             * parent = transcript
             * child  = exon
             * Transcript has a collection of exons
             */
            if (("part_of".equals(relationshipType) || "member_of".equals(relationshipType)
                    || "variant_of".equals(relationshipType))
                    && r.direct) {
                assignPartOf(parent, child);
            } else if ("is_a".equals(relationshipType) && r.direct) {
                Util.addToSetMap(childToParents, child, parent);
           /**
           * HAS_PART (inverse of part_of)
           * parent = EST
           * child  = OverlappingESTSet
           * OverlappingESTSet has a collection of ESTs
           */
            } else if ("has_part".equals(relationshipType) && r.direct) {
                assignPartOf(child, parent);
            }
        }

        // manually add the part of relationships in config
        addPartOfsFromConfig();

        buildParentsMap();

        for (OboRelation r : oboRelations) {
            String child = r.childTermId;
            String parent = r.parentTermId;
            String relationshipType = r.getRelationship().getName();
            if ("is_a".equals(relationshipType) && r.direct) {
                assignPartOfsToChild(parent, child);
            }
        }

        if (!termsToKeep.isEmpty()) {
            trimModel();
        }

        // gene.transcripts is in part_ofs map, now set transcript.gene
        setReverseReferences();

        // remove UTR.mRNA if UTR.transcript exists
        removeRedundantCollections();

    }

    private void addPartOfsFromConfig() {
        for (Map.Entry<String, Set<String>> entry : manyToManyPartOfs.entrySet()) {
            String child = getIdentifier(entry.getKey());
            Set<String> parents = entry.getValue();
            for (String parent : parents) {
                parent = getIdentifier(parent);
                assignPartOf(parent, child);
            }
        }
    }

    // set many-to-one relationships
    private void setReverseReferences() {
        Map<String, Set<String>> partOfsCopy = new HashMap<String, Set<String>>(partOfs);
        for (Map.Entry<String, Set<String>> entry : partOfsCopy.entrySet()) {
            String oboTerm = entry.getKey();
            Set<String> parents = new HashSet<String>(entry.getValue());
            for (String parent : parents) {
                if (parent.equals(CHROMOSOME)) {
                    continue;
                }
                if (!StringUtils.isEmpty(oboTerm) && !StringUtils.isEmpty(parent)) {
                    Util.addToSetMap(reversePartOfs, parent, oboTerm);
                }
            }
        }
    }

    private void assignPartOf(String parent, String child) {
        if (!StringUtils.isEmpty(child) && !StringUtils.isEmpty(parent)) {
            Util.addToSetMap(partOfs, child, parent);
        }
    }

    private void assignPartOfsToChild(String parent, String child) {
        transferPartOfs(parent, child);
        Set<String> grandparents = childToParents.get(parent);
        if (grandparents != null && !grandparents.isEmpty()) {
            for (String grandparent : grandparents) {
                assignPartOfsToChild(grandparent, child);
            }
        }
    }

    private void transferPartOfs(String parent, String child) {
        Set<String> parentPartOfs = partOfs.get(parent);
        if (parentPartOfs != null && !parentPartOfs.isEmpty()) {
            Set<String> childPartOfs = partOfs.get(child);
            if (childPartOfs == null) {
                childPartOfs = new HashSet<String>();
                partOfs.put(child, childPartOfs);
            }
            childPartOfs.addAll(parentPartOfs);
        }
    }

    // build parent --> children map
    private void buildParentsMap() {
        parentToChildren = new HashMap<String, Set<String>>();
        for (String child : childToParents.keySet()) {
            Set<String> parents = childToParents.get(child);
            for (String parent : parents) {
                if (!StringUtils.isEmpty(child) && !StringUtils.isEmpty(parent)) {
                    Util.addToSetMap(parentToChildren, parent, child);
                }
            }
        }
    }

    private void trimModel() {

        Map<String, OboTerm> oboTermsCopy = new HashMap<String, OboTerm>(validOboTerms);

        System.out .println("Total terms: " + validOboTerms.size());

        for (String oboTermIdentifier : oboTermsCopy.keySet()) {
            prune(oboTermIdentifier);
        }

        System.out .println("Total terms, post-pruning: " + validOboTerms.size());

        oboTermsCopy = new HashMap<String, OboTerm>(validOboTerms);

        for (String oboTermIdentifier : oboTermsCopy.keySet()) {
            if (!validTerm(oboTermIdentifier)) {
                flatten(oboTermIdentifier);
            }
        }

        System.out .println("Total terms, post-flattening: " + validOboTerms.size());
    }

    /*
     * remove term if:
     *  1. not in list of desired terms
     *  2. no children
     */
    private void prune(String oboTermIdentifier) {
        // process each child term
        if (parentToChildren.get(oboTermIdentifier) != null) {
            Set<String> children = new HashSet<String>(parentToChildren.get(oboTermIdentifier));
            for (String child : children) {
                prune(child);
            }
        }

        // if this term has no children AND it's not on our list = DELETE
        if (!validParent(oboTermIdentifier) && !validTerm(oboTermIdentifier)) {
            removeTerm(oboTermIdentifier);
            debugOutput(oboTermIdentifier, "Pruning [no children, not on list]");
        }
    }

    /*
     * remove term if not on list AND:
     *  (a) term has only ONE parent and ONE child
     *  (b) term has only ONE parent and NO children
     *  (c) term has NO parents and only ONE child
     */
    private void flatten(String oboTerm) {
        Set<String> parents = childToParents.get(oboTerm);
        Set<String> kids = parentToChildren.get(oboTerm);

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
                parentToChildren.get(parent).addAll(kids);

                // add parent to new children
                for (String kid : kids) {
                    Set<String> otherParents = childToParents.get(kid);
                    otherParents.remove(oboTerm);
                    otherParents.add(parent);
                }
                removeTerm(oboTerm);
                debugOutput(oboTerm, "Flattening [term had only one parent]");
                return;
            }

            // term has only one child.  remove term and assign child to new parents.
            if (kids.size() == 1) {
                String kid = kids.toArray()[0].toString();

                // add parents to new kid
                childToParents .get(kid).addAll(parents);

                // reassign parents to new kid
                for (String parent : parents) {
                    Set<String> otherChildren = parentToChildren.get(parent);
                    otherChildren.remove(oboTerm);
                    otherChildren.add(kid);
                }
                removeTerm(oboTerm);
                debugOutput(oboTerm, "Flattening [term had only one child]");
                return;
            }

            // root term
        } else if (parents == null) {
            removeTerm(oboTerm);
            debugOutput(oboTerm, "Flattening [root term]");
        }

        // no children, delete!
        if (kids == null) {
            removeTerm(oboTerm);
            debugOutput(oboTerm, "Flattening [no children]");
        }
    }

    // make sure collection is at the highest level term
    // eg. Gene.transcripts should mean that Gene.mRNAs never happens
    private void removeRedundantCollections() {
        Map<String, Set<String>> invalidPartOfs = new HashMap<String, Set<String>>();
        for (String parent : validOboTerms.keySet()) {
            Set<String> refs = reversePartOfs.get(parent);
            if (refs != null) {
                for (String refName : refs) {
                    removeRelationshipFromChildren(reversePartOfs, partOfs, invalidPartOfs, parent,
                            refName);
                }
            }
            Set<String> collections = partOfs.get(parent);
            if (collections != null) {
                for (String coll : collections) {
                    removeRelationshipFromChildren(partOfs, reversePartOfs, invalidPartOfs, parent,
                            coll);
                }
            }
        }

        if (!invalidPartOfs.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : invalidPartOfs.entrySet()) {
                String parent = entry.getKey();
                Set<String> collections = entry.getValue();
                for (String collectionName : collections) {
                    removeRelationship(partOfs, reversePartOfs, parent, collectionName);
                    removeRelationship(reversePartOfs, partOfs, parent, collectionName);
                }
            }
        }
    }

    /*
     * remove collection from children of the specified term.  eg. remove MRNA.cDSs because that
     * collection is in a parent, transcript.
     *
     *  CDS --- > mRNA
     *      --- > transcript
     *
     *  CRM               ----> gene
     *  regulatory_region ---->
     *
     * @param parent eg. transcript
     * @collectioName eg. CDSs
     */
    private Map<String, Set<String>> removeRelationshipFromChildren(Map<String, Set<String>> map1,
            Map<String, Set<String>> map2, Map<String, Set<String>> invalidPartOfs, String parent,
            String collectionName) {
        Set<String> children = parentToChildren.get(parent);

        if (children == null) {
            return Collections.emptyMap();
        }

        for (String child : children) {

            // this relationship is in config, so we can't delete
            // remove parent relationship instead
            if (isManyToMany(collectionName, child)) {
                if (!StringUtils.isEmpty(collectionName) && !StringUtils.isEmpty(parent)) {
                    Util.addToSetMap(invalidPartOfs, parent, collectionName);
                }
                continue;
            }
            removeRelationship(map1, map2, child, collectionName);

            removeRelationshipFromChildren(map1, map2, invalidPartOfs, child, collectionName);
        }
        return invalidPartOfs;
    }

    private static void removeRelationship(Map<String, Set<String>> map1,
            Map<String, Set<String>> map2, String child,
            String collectionName) {
        // remove collection from both ends
        removeCollection(map1, child, collectionName);
        removeCollection(map1, collectionName, child);
        // remove both ends of reference
        removeReference(map2, child, collectionName);
        removeReference(map2, collectionName, child);
    }

    private static void removeCollection(Map<String, Set<String>> relationshipMap, String child,
            String collection) {
        Set<String> childCollections = relationshipMap.get(child);
        if (childCollections != null) {
            childCollections.remove(collection);
        }
    }

    private static void removeReference(Map<String, Set<String>> relationshipMap, String child,
    String collection) {
        Set<String> childRefs = relationshipMap.get(collection);
        if (childRefs != null) {
            childRefs.remove(child);
        }
    }

    private void debugOutput(String oboTerm, String err) {
        if (DEBUG) {
            System.out .println(err + " " + oboTerm
                    + " Valid terms count: " + validOboTerms.size());
        }
    }

    // remove term from every map
    private void removeTerm(String oboTerm) {
        validOboTerms.remove(oboTerm);

        childToParents.remove(oboTerm);
        parentToChildren.remove(oboTerm);
        partOfs.remove(oboTerm);
        removeCollections(oboTerm);

        // remove mention in maps
        Map<String, Set<String>> mapCopy
            = new HashMap<String, Set<String>>(parentToChildren);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            String parent = entry.getKey();
            Set<String> children = entry.getValue();

            // remove current term
            children.remove(oboTerm);

            // if parent is childless, remove
            if (children.size() == 0) {
                parentToChildren.remove(parent);
            }
        }

        mapCopy = new HashMap<String, Set<String>>(childToParents);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            String child = entry.getKey();
            Set<String> parents = entry.getValue();

            // remove current term
            parents.remove(oboTerm);

            // if child has no parents remove from p
            if (parents.size() == 0) {
                childToParents.remove(child);
            }
        }
    }

    private void removeCollections(String oboTerm) {
        Map<String, Set<String>> mapCopy = new HashMap<String, Set<String>>(partOfs);
        for (Map.Entry<String, Set<String>> entry : mapCopy.entrySet()) {
            Set<String> collections = entry.getValue();
            if (collections.contains(oboTerm)) {
                collections.remove(oboTerm);
            }
        }
    }

    /**
     * For each term in our list, add to our map if the term is not obsolete.
     *
     * @param terms set of obo terms to process
     */
    private void processOboTerms(Set<OboTerm> terms) {
        for (OboTerm term : terms) {
            if (!term.isObsolete()) {
                String identifier = term.getId().trim();
                String name = term.getName().trim();
                if (!StringUtils.isEmpty(identifier) && !StringUtils.isEmpty(name)) {
                    OboTerm c = new OboTerm(identifier, name);
                    validOboTerms.put(identifier, c);
                    oboNameToIdentifier.put(name, identifier);
                }
            }
        }
    }

    /**
     * Check that each OBO term in file provided by user is in OBO file.
     */
    private void validateTermsToKeep() {
        List<String> invalidTermsConfigured = new ArrayList<String>();
        for (String term : termsToKeep) {
            if (!term.contains("#") && !term.contains(".")
                    && oboNameToIdentifier.get(term) == null) {
                invalidTermsConfigured.add(term);
            }
        }
        if (!invalidTermsConfigured.isEmpty()) {
            throw new BuildException("The following terms specified in so_terms are not valid "
                    + "Sequence Ontology terms according to so.obo: "
                    + StringUtil.prettyList(invalidTermsConfigured));
        }
    }

    /**
     *  move terms from (user provided) file to list
     *  only these terms (and dependents) will be processed
     * @param filename terms file
     */
    private void processTerms(InputStream oboFile) {
        Set<String> terms = new HashSet<String>();
        Map<String, Set<String>> manyToMany = new HashMap<String, Set<String>>();
        try {
            BufferedReader br =  new BufferedReader(new InputStreamReader(oboFile));
            try {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (StringUtils.isNotEmpty(line) && !line.startsWith("#")) {
                        if (line.contains(".")) {
                            String[] bits = line.split("\\.");
                            if (bits.length != 2) {
                                throw new RuntimeException("Invalid entry in oboFile: " + line);
                            }
                            String child = bits[0];
                            String parent = bits[1];
                            if (!StringUtils.isEmpty(parent) && !StringUtils.isEmpty(child)) {
                                Util.addToSetMap(manyToMany, child, parent.trim());
                            }
                        } else {
                            terms.add(line);
                        }
                    }
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        termsToKeep = terms;
        manyToManyPartOfs = manyToMany;
    }

    /**
     * translates OBO file --> Model
     *
     * 1. uses OBO edit to parse OBO file
     * 2. trims unwanted SO terms using config file as guide
     * 3. finally creates class descriptors based on relationships created in previous steps
     */
    private void process(String oboFilename) {

        // parse file using OBOEdit
        OboParser parser = new OboParser();
        try {
            parser.processOntology(new FileReader(oboFilename));
            parser.processRelations(oboFilename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Couldn't find obo file", e);
        } catch (Exception e) {
            throw new RuntimeException("Parsing obo file failed", e);
        }

        // process results of parsing by OBOEdit.  flatten and trim unwanted terms
        processOboTerms(parser.getOboTerms());
        validateTermsToKeep();
        processRelations(parser.getOboRelations());

        Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>();

        // process each oboterm - add parent and collections
        for (String childIdentifier : getOboTermIdentifiers()) {
            // is_a
            String parents = processParents(childIdentifier);
            // part_of
            ClassDescriptor cd = processRefsAndColls(parents, childIdentifier);
            clds.add(cd);
        }

        classes = new TreeSet<ClassDescriptor>(getComparator());
        classes.addAll(clds);

        try {
            soModel = new Model(NAME, NAMESPACE, classes);
        } catch (MetaDataException e) {
            throw new RuntimeException("Invalid model", e);
        }
    }

    private static Comparator<ClassDescriptor> getComparator() {
        // sort classes by name for readability
        Comparator<ClassDescriptor> comparator = new Comparator<ClassDescriptor>() {
            @Override
            public int compare(ClassDescriptor o1, ClassDescriptor o2) {
                String fieldName1 = o1.getName().toLowerCase();
                String fieldName2 = o2.getName().toLowerCase();
                return fieldName1.compareTo(fieldName2);
            }
        };
        return comparator;
    }

    /**
     * using the relationship maps, build each SO term
     *
     * @param childIdentifier identifier for child SO term
     * @return space-delimited list of parents for the given term, or NULL if none
     */
    private String processParents(String childIdentifier) {
        Set<String> parents = getParentIdentifiers(childIdentifier);
        Set<String> parentsInModel = new HashSet<String>();
        if (parents != null && !parents.isEmpty()) {
            for (String parentIdentifier : parents) {
                if (classInModel(parentIdentifier)) {
                    String parentName = getName(parentIdentifier);
                    parentName = TypeUtil.generateClassName(NAMESPACE, parentName);
                    parentsInModel.add(parentName);
                }
            }
        }
        String parentList = StringUtil.join(parentsInModel, " ");
        if (StringUtils.isBlank(parentList)) {
            parentList = null;
        }
        return parentList;
    }

    private ClassDescriptor processRefsAndColls(String parents, String childIdentifier) {
        Set<AttributeDescriptor> fakeAttributes = Collections.emptySet();
        Set<ReferenceDescriptor> references = new HashSet<ReferenceDescriptor>();
        Set<CollectionDescriptor> collections = new HashSet<CollectionDescriptor>();
        Set<String> childReversePartOfs = getReversePartOfs(childIdentifier);
        Set<String> partOfIdentifiers = getPartOfs(childIdentifier);
        String childOBOName = getName(childIdentifier);

        // part ofs, reference to parent
        // can be a collection if in config, though
        if (partOfIdentifiers != null) {
            for (String parent : partOfIdentifiers) {
                if (classInModel(parent)) {
                    // reference
                    String parentName = getName(parent);
                    String fullyQualifiedClassName = TypeUtil.generateClassName(
                            NAMESPACE, parentName);
                    parentName = TypeUtil.javaiseClassName(parentName);
                    parentName = StringUtil.decapitalise(parentName);

                    // reverse reference
                    String reverseReference = generateReverseReference(parentName, childOBOName,
                            true);
                    if (isManyToMany(parent, childIdentifier)) {
                        parentName = parentName + "s";
                        CollectionDescriptor cd = new CollectionDescriptor(parentName,
                                fullyQualifiedClassName, reverseReference);
                        collections.add(cd);
                    } else {
                        ReferenceDescriptor rd = new ReferenceDescriptor(parentName,
                                fullyQualifiedClassName, reverseReference);
                        references.add(rd);
                    }
                }
            }
        }

        // other side of part_of relationship, collection of children
        // reverse reference can be a collection if in config
        if (childReversePartOfs != null) {
            for (String collection : childReversePartOfs) {
                if (classInModel(collection)) {
                    // collection
                    String collectionName = TypeUtil.javaiseClassName(
                            getName(collection));
                    String fullyQualifiedClassName = TypeUtil.generateClassName(
                            NAMESPACE, collectionName);
                    collectionName = StringUtil.decapitalise(collectionName) + "s";
                    // reverse reference
                    String reverseReference = generateReverseReference(collectionName, childOBOName,
                            isManyToMany(collection, childIdentifier));
                    // cd
                    CollectionDescriptor cd = new CollectionDescriptor(collectionName ,
                            fullyQualifiedClassName, reverseReference);
                    collections.add(cd);
                }
            }
        }

        String childName = TypeUtil.generateClassName(NAMESPACE, getName(childIdentifier));
        return new ClassDescriptor(childName, parents, true, fakeAttributes, references,
                collections);
    }

    private static String generateReverseReference(String parent, String child,
            boolean manyToMany) {
        if ("chromosome".equals(parent) || "chromosome".equals(child)) {
            return null;
        }
        String reverseReference = TypeUtil.javaiseClassName(child);
        reverseReference = StringUtil.decapitalise(reverseReference);
        if (manyToMany) {
            reverseReference = reverseReference + "s";
        }
        return reverseReference;
    }


}
