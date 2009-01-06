package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Thomas Riley
 * @author Peter Mclaren - 5/6/05 - added some functionality to allow terms to find all their parent
 * terms.
 */
public class OboParser
{
    private static final Logger LOG = Logger.getLogger(OboParser.class);

    private final Pattern synPattern = Pattern.compile("\\s*\"(.+?[^\\\\])\".*");
    private final Matcher synMatcher = synPattern.matcher("");

    /**
     * All terms.
     */
    protected Map<String, OboTerm> terms = new HashMap<String, OboTerm>();
    /**
     * Accumulated root terms.
     */
    protected Map<String, OboTerm> rootTerms;
    /**
     * Default namespace.
     */
    protected String defaultNS = "";
    /**
     * Holds a mapping of terms to the complete distinct set of their parent terms.
     */
    private Map<String, Set> termToParentTermSetMap = null;

    /**
     * Parse an OBO file to produce a set of toplevel OboTerms.
     *
     * @param in text in OBO format
     * @return a set of DagTerms - will contain only toplevel terms
     * @throws IOException if anything goes wrong
     */
    public Set processForLabellingOntology(Reader in) throws IOException {
        readTerms(new BufferedReader(in));
        return new HashSet(rootTerms.values());
    }

    /**
     * Parse an OBO file to produce a map from ontology term id to name.
     *
     * @param in text in OBO format
     * @return a map from ontology term identifier to name
     * @throws IOException if anything goes wrong
     */
    public Map<String, String> getTermIdNameMap(Reader in) throws IOException {
        readTerms(new BufferedReader(in));
        Map<String, String> idNames = new HashMap<String, String>();
        for (OboTerm ot : terms.values()) {
            idNames.put(ot.getId(), ot.getName());
        }
        return idNames;
    }

    /**
     * Process all terms - starting from the roots and work our way down to the bottom of the graph
     * setting parents.
     *
     * @return the map of all terms mapped to a set of all their parents.
     * @throws Exception if the rootTerms have not been created yet or if anything else goes wrong.
     */
    public Map<String, Set> getTermToParentTermSetMap() throws Exception {

        if (this.termToParentTermSetMap == null) {
            termToParentTermSetMap = getTermToParentTermSetMap(rootTerms.values(), terms);
        }

        return termToParentTermSetMap;
    }


    private Map<String, Set> getTermToParentTermSetMap(Collection<OboTerm> rootTermSet,
                                          Map<String, OboTerm> termMap) throws Exception {

        if (rootTermSet == null || rootTermSet.size() == 0) {
            throw new Exception("OboParser.getTermToParentTermSetMap() - "
                                + "call OboParser.getTermIdNameMap() first!");
        }

        //Iterate over the root term elements -- assuming that we might have more than one root
        //element!
        for (OboTerm currentRootTerm : rootTermSet) {
            Stack currentBranchStack = new Stack();
            setParentTermsViaOntologyDepthSearch(currentBranchStack, currentRootTerm);
        }

        //Ok now we can do some work here to iterate over the root terms and set their children to
        //point to them...
        Map termToParentTerm = new HashMap(termMap.size());

        //loop over all the terms and build a map of their ids pointing to their parent set.
        for (OboTerm nextTerm : termMap.values()) {
            termToParentTerm.put(nextTerm.getId(), nextTerm.getAllParentIds());
        }

        return termToParentTerm;
    }

    //Does the actual work
    private void setParentTermsViaOntologyDepthSearch(Stack currentBranchStack, OboTerm nextTerm) {

        //records the current parent ids set into the next term.
        HashSet parentIdsSet = new HashSet();
        for (Iterator cbsIt = currentBranchStack.iterator(); cbsIt.hasNext();) {
            parentIdsSet.add(((OboTerm) cbsIt.next()).getId());
        }
        nextTerm.addToAllParentIds(parentIdsSet);

        //push the incoming item onto the stack.
        currentBranchStack.push(nextTerm);

        // descend into children of this term
        for (OboTerm childTerm : nextTerm.getChildren()) {
            setParentTermsViaOntologyDepthSearch(currentBranchStack, childTerm);
        }

        // descend into terms that are part_of this one
        for (OboTerm componentTerm : nextTerm.getComponents()) {
            setParentTermsViaOntologyDepthSearch(currentBranchStack, componentTerm);
        }

        // we have reached a leaf term
        currentBranchStack.pop();
    }


    /**
     * Read DAG input line by line to generate hierarchy of DagTerms.
     *
     * @param in text in DAG format
     * @throws IOException if anything goes wrong
     */
    public void readTerms(BufferedReader in) throws IOException {
        String line;
        Map tagValues = new MultiValueMap();
        List tagValuesList = new ArrayList();

        Pattern tagValuePattern = Pattern.compile("(.+?[^\\\\]):(.+)");
        Pattern stanzaHeadPattern = Pattern.compile("\\s*\\[(.+)\\]\\s*");
        Matcher tvMatcher = tagValuePattern.matcher("");
        Matcher headMatcher = stanzaHeadPattern.matcher("");

        while ((line = in.readLine()) != null) {
            // First strip off any comments
            if (line.indexOf('!') >= 0) {
                line = line.substring(0, line.indexOf('!'));
            }

            tvMatcher.reset(line);
            headMatcher.reset(line);

            if (headMatcher.matches()) {
                String stanzaType = headMatcher.group(1);
                tagValues = new MultiValueMap(); // cut loose
                if (stanzaType.equals("Term")) {
                    tagValuesList.add(tagValues);
                    LOG.debug("recorded term with " + tagValues.size() + " tag values");
                } else {
                    LOG.warn("Ignoring " + stanzaType + " stanza");
                }
                LOG.debug("matched stanza " + stanzaType);
            } else if (tvMatcher.matches()) {
                String tag = tvMatcher.group(1).trim();
                String value = tvMatcher.group(2).trim();
                tagValues.put(tag, value);
                LOG.debug("matched tag \"" + tag + "\" with value \"" + value + "\"");

                if (tag.equals("default-namespace")) {
                    defaultNS = value;
                    LOG.info("default-namespace is \"" + value + "\"");
                }
            }
        }

        in.close();

        //LOG.info("Found " + tagValuesList.size() + " root terms");

        // Just build all the OboTerms disconnected
        for (Iterator iter = tagValuesList.iterator(); iter.hasNext();) {
            Map tvs = (Map) iter.next();
            String id = (String) ((List) tvs.get("id")).get(0);
            String name = (String) ((List) tvs.get("name")).get(0);
            OboTerm term = new OboTerm(id, name);
            term.setObsolete(isObsolete(tvs));
            terms.put(term.getId(), term);
        }

        // Copy all terms into rootTerms map - non-root terms will be removed
        rootTerms = new HashMap<String, OboTerm>(terms);

        // Now connect them all together
        for (Iterator iter = tagValuesList.iterator(); iter.hasNext();) {
            Map tvs = (Map) iter.next();
            if (!isObsolete(tvs)) {
                configureDagTerm(tvs);
            }
        }
    }

    /**
     * Configure dag terms with values from one entry.
     *
     * @param tagValues term config
     */
    protected void configureDagTerm(Map tagValues) {
        String id = (String) ((List) tagValues.get("id")).get(0);
        OboTerm term = terms.get(id);

        if (term != null) {
            term.setTagValues(tagValues);

            List isas = (List) tagValues.get("is_a");
            if (isas != null) {
                for (Iterator iter = isas.iterator(); iter.hasNext();) {
                    String isa = (String) iter.next();
                    OboTerm pt = terms.get(isa);
                    if (pt == null) {
                        LOG.warn("child term (" + term + ") in OBO file refers to a non-existent "
                                + "parent (" + isa + ")");
                        System.err .println("Child term (" + term + ") in OBO file refers to a non-"
                                + "existent parent (" + isa + ")");
                        continue;
                    }
                    LOG.debug(term + " isa " + pt);
                    pt.addChild(term);
                    rootTerms.remove(term.getId());
                }
            }

            List relationships = (List) tagValues.get("relationship");
            if (relationships != null) {
                for (Iterator iter = relationships.iterator(); iter.hasNext();) {
                    String relationship = (String) iter.next();
                    String bits[] = StringUtils.split(relationship);
                    String relationshipType = bits[0];
                    if (relationshipType.equals("part_of")
                                    || relationshipType.equals("regulates")
                                    || relationshipType.equals("negatively_regulates")
                                    || relationshipType.equals("positively_regulates")) {
                        OboTerm pt = terms.get(bits[1]);
                        //LOG.debug(term + relationshipType + pt);
                        if (pt == null) {
                            LOG.warn("child term (" + term
                                     + ") in OBO file refers to a non-existant "
                                     + "parent (" + bits[1] + ")");
                            continue;
                        }
                        pt.addComponent(term);
                        rootTerms.remove(term.getId());
                    } else {
                        LOG.warn("Unhandled relationship tag value: " + relationship);
                    }
                }
            }

            List synonyms = (List) tagValues.get("synonym");
            if (synonyms != null) {
                addSynonyms(term, synonyms, "synonym");
            }
            synonyms = (List) tagValues.get("related_synonym");
            if (synonyms != null) {
                addSynonyms(term, synonyms, "related_synonym");
            }
            synonyms = (List) tagValues.get("exact_synonym");
            if (synonyms != null) {
                addSynonyms(term, synonyms, "exact_synonym");
            }
            synonyms = (List) tagValues.get("broad_synonym");
            if (synonyms != null) {
                addSynonyms(term, synonyms, "broad_synonym");
            }
            synonyms = (List) tagValues.get("narrow_synonym");
            if (synonyms != null) {
                addSynonyms(term, synonyms, "narrow_synonym");
            }

            // Set namespace
            List nsl = (List) tagValues.get("namespace");
            if (nsl != null && nsl.size() > 0) {
                term.setNamespace((String) nsl.get(0));
            } else {
                term.setNamespace(defaultNS);
            }

            // Set description
            List defl = (List) tagValues.get("def");
            String def = null;
            if (defl != null && defl.size() > 0) {
                def = (String) defl.get(0);
                synMatcher.reset(def);
                if (synMatcher.matches()) {
                    term.setDescription(unescape(synMatcher.group(1)));
                }
            } else {
                LOG.warn("Failed to parse def of term " + id + " def: " + def);
            }

        } else {
            LOG.warn("OboParser.configureDagTerm() - no term found for id:" + id);
        }
    }

    /**
     * Given the tag+value map for a term, work out whether the term is marked
     * as obsolete.
     *
     * @param tagValues map of tag name to value for a single term
     * @return true if the term is marked obsolete, false if not
     */
    public static boolean isObsolete(Map tagValues) {
        List vals = (List) tagValues.get("is_obsolete");
        if (vals != null && vals.size() > 0) {
            if (vals.size() > 1) {
                LOG.warn("Term: " + tagValues + " has more than one (" + vals.size()
                        + ") is_obsolete values - just using first");
            }
            return ((String) vals.get(0)).equalsIgnoreCase("true");
        }
        return false;
    }

    /**
     * Add synonyms to a DagTerm.
     *
     * @param term     the DagTerm
     * @param synonyms List of synonyms (Strings)
     * @param type     synonym type
     */
    protected void addSynonyms(OboTerm term, List synonyms, String type) {
        for (Iterator iter = synonyms.iterator(); iter.hasNext();) {
            String line = (String) iter.next();
            synMatcher.reset(line);
            if (synMatcher.matches()) {
                term.addSynonym(new OboTermSynonym(unescape(synMatcher.group(1)), type));
            } else {
                LOG.error("Could not match synonym value from: " + line);
            }
        }
    }

    /**
     * Perform OBO unescaping.
     *
     * @param string the escaped string
     * @return the corresponding unescaped string
     */
    protected String unescape(String string) {
        int sz = string.length();
        StringBuffer out = new StringBuffer(sz);
        boolean hadSlash = false;

        for (int i = 0; i < sz; i++) {
            char ch = string.charAt(i);

            if (hadSlash) {
                switch (ch) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'W':
                        out.append(' ');
                        break;
                    default:
                        out.append(ch);
                        break;
                }
                hadSlash = false;
            } else if (ch == '\\') {
                hadSlash = true;
            } else {
                out.append(ch);
            }
        }

        return out.toString();
    }
}
