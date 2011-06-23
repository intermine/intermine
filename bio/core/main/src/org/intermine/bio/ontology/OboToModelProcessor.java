package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.util.StringUtil;
import org.intermine.util.Util;

/**
 * This class handles the ontologies for OboToModel.
 *
 * @author Julie Sullivan
 */
public class OboToModelProcessor
{
    private String namespace;
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

    // TODO put this in config file instead
    private static final String CHROMOSOME = "SO:0000340";
//    private static final String TRANSCRIPT = "SO:0000673";
//    private static final String EXON = "SO:0000147";

    /**
     * Constructor.
     *
     * @param termsFile file containing list of SO terms to filter on
     * @param namespace the namespace to use in generating URI-based identifiers
     */
    public OboToModelProcessor(File termsFile, String namespace) {
        this.namespace = namespace;
        processTermFile(termsFile);
    }

    /**
     * @return number of OBO terms we are filtering on
     */
    public int getTermsCount() {
        return termsToKeep.size();
    }

    /**
     * Returns name of the package, eg. org.intermine.bio.
     *
     * @return name of the package, eg. org.intermine.bio
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param childIdentifier the oboterm to get relationships for
     * @return all collections for given class
     */
    public Set<String> getPartOfs(String childIdentifier) {
        return partOfs.get(childIdentifier);
    }

    /**
     * @param childIdentifier the oboterm to get relationships for
     * @return all collections for given class
     */
    public Set<String> getReversePartOfs(String childIdentifier) {
        return reversePartOfs.get(childIdentifier);
    }

    /**
     * Test is class is in the model.  Only used after the obo terms have been processed and
     * trimmed.
     *
     * @param identifier for obo term
     * @return true if the class is in the model
     */
    public boolean classInModel(String identifier) {
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
    public boolean isManyToMany(String parent, String child) {
        if (!testManyToMany(parent, child)) {
            return testManyToMany(child, parent);
        } else {
            return true;
        }
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
    public Set<String> getParents(String childIdentifier) {
        return childToParents.get(childIdentifier);
    }

    /**
     * @param identifier for obo term
     * @return name of term, eg. sequence_feature
     */
    public String getName(String identifier) {
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
    public Set<String> getOboTermIdentifiers() {
        return validOboTerms.keySet();
    }

    /**
     * Processes obo relations from OBOEdit.  Parses each relationship and builds collections and
     * parents.  Also assigns grandchildren the collections of the grandparents and trims/flattens
     * terms if the user has provided a terms list.
     *
     * @param oboRelations List of obo relations from OBOEdit
     */
    public void processRelations(List<OboRelation> oboRelations) {
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
            if (("part_of".equals(relationshipType) || "member_of".equals(relationshipType))
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
                // TODO put this in config file
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

    private void removeRelationship(Map<String, Set<String>> map1,
            Map<String, Set<String>> map2, String child,
            String collectionName) {
        // remove collection from both ends
        removeCollection(map1, child, collectionName);
        removeCollection(map1, collectionName, child);
        // remove both ends of reference
        removeReference(map2, child, collectionName);
        removeReference(map2, collectionName, child);
    }

    private void removeCollection(Map<String, Set<String>> relationshipMap, String child,
            String collection) {
        Set<String> childCollections = relationshipMap.get(child);
        if (childCollections != null) {
            childCollections.remove(collection);
        }
    }

    private void removeReference(Map<String, Set<String>> relationshipMap, String child,
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
    public void processOboTerms(Set<OboTerm> terms) {
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
     *
     * @param oboFilename name of obo file - used for error message only
     * @param termsToKeepFileName file containing obo terms - used for error message only
     */
    public void validateTermsToKeep(String oboFilename, String termsToKeepFileName) {
        List<String> invalidTermsConfigured = new ArrayList<String>();
        for (String term : termsToKeep) {
            if (!term.contains("#") && !term.contains(".")
                    && oboNameToIdentifier.get(term) == null) {
                invalidTermsConfigured.add(term);
            }
        }
        if (!invalidTermsConfigured.isEmpty()) {
            throw new RuntimeException("The following terms specified in "
                    + termsToKeepFileName + " are not valid Sequence Ontology terms"
                    + " according to: " + oboFilename + ": "
                    + StringUtil.prettyList(invalidTermsConfigured));
        }
    }

    // move terms from (user provided) file to list
    // only these terms (and dependents) will be processed
    private void processTermFile(File filename) {
        Set<String> terms = new HashSet<String>();
        Map<String, Set<String>> manyToMany = new HashMap<String, Set<String>>();
        try {
            BufferedReader br =  new BufferedReader(new FileReader(filename));
            try {
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (StringUtils.isNotEmpty(line) && !line.startsWith("#")) {
                        if (line.contains(".")) {
                            String[] bits = line.split("\\.");
                            if (bits.length != 2) {
                                throw new RuntimeException("Invalid entry in "
                                        + filename.getPath() + ": " + line);
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
}
