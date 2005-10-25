package org.intermine.ontology;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * 
 * @author Thomas Riley
 */
public class OboParser
{
    private static final Logger LOG = Logger.getLogger(OboParser.class);

    private final Pattern synPattern = Pattern.compile("\\s*\"(.+?[^\\\\])\".*");
    private final Matcher synMatcher = synPattern.matcher("");
    
    /** All terms. */
    protected Map terms = new HashMap();
    /** Accumulated root terms. */
    protected Map rootTerms;
    /** Default namespace. */
    protected String defaultNS = "";

    /**
     * Parse an OBO file to produce a set of toplevel DagTerms.
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
     * Parse an OBO file to produce a set map from ontology term id to name.
     * 
     * @param in text in OBO format
     * @return a map from ontology term identifier to name
     * @throws IOException if anything goes wrong
     */
    public Map getTermIdNameMap(Reader in) throws IOException {
        readTerms(new BufferedReader(in));
        Map idNames = new HashMap();
        for (Iterator iter = terms.values().iterator(); iter.hasNext(); ) {
            DagTerm dt = (DagTerm) iter.next();
            idNames.put(dt.getId(), dt.getName());
        }
        return idNames;
    }
    
    /**
     * Read DAG input line by line to generate hierarchy of DagTerms.
     * 
     * @param in text in DAG format
     * @throws IOException if anything goes wrong
     */
    public void readTerms(BufferedReader in) throws IOException {
        String line;
        Map tagValues = new MultiHashMap();
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
                tagValues = new MultiHashMap(); // cut loose
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
        
        LOG.info("Found " + tagValuesList.size() + " root terms");
        
        // Just build all the OboTerms disconnected
        for (Iterator iter = tagValuesList.iterator(); iter.hasNext(); ) {
            Map tvs = (Map) iter.next();
            String id = (String) ((List) tvs.get("id")).get(0);
            String name = (String) ((List) tvs.get("name")).get(0);
            if (!isObselete(tvs)) {
                OboTerm term = new OboTerm(id, name);
                terms.put(term.getId(), term);
            } else {
                LOG.debug("Skipping obselete term " + id + " : " + name);
            }
        }
        
        // Copy all terms into rootTerms map - non-root terms will be removed
        rootTerms = new HashMap(terms);
        
        // Now connect them all together
        for (Iterator iter = tagValuesList.iterator(); iter.hasNext(); ) {
            Map tvs = (Map) iter.next();
            if (!isObselete(tvs)) {
                configureDagTerm(tvs);
            }
        }
    }
    
    /**
     * Configure dag terms with values from one entry.
     * 
     * @param tagValues term config
     * @return the DagTerm corresponding to the tagValues passed
     */
    protected DagTerm configureDagTerm(Map tagValues) {
        String id = (String) ((List) tagValues.get("id")).get(0);
        OboTerm term = (OboTerm) terms.get(id);
        
        List isas = (List) tagValues.get("is_a");
        if (isas != null) {
            for (Iterator iter = isas.iterator(); iter.hasNext(); ) {
                String isa = (String) iter.next();
                DagTerm pt = (DagTerm) terms.get(isa);
                if (pt == null) {
                    LOG.warn("child term (" + term + ") in OBO file refers to a non-existant "
                             + "parent (" + isa + ")");
                    continue;
                }
                LOG.debug(term + " isa " + pt);
                pt.addChild(term);
                rootTerms.remove(term.getId());
            }
        }
        
        List relationships = (List) tagValues.get("relationship");
        if (relationships != null) {
            for (Iterator iter = relationships.iterator(); iter.hasNext(); ) {
                String relationship = (String) iter.next();
                String bits[] = StringUtils.split(relationship);
                if (bits[0].equals("part_of")) {
                    DagTerm pt = (DagTerm) terms.get(bits[1]);
                    LOG.debug(term + " part_of " + pt);
                    if (pt == null) {
                        LOG.warn("child term (" + term + ") in OBO file refers to a non-existant "
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
        if (defl != null && defl.size() > 0) {
            String def = (String) defl.get(0);
            synMatcher.reset(def);
            if (synMatcher.matches()) {
                term.setDescription(unescape(synMatcher.group(1)));
            } else {
                LOG.error("Failed to parse def of term " + id + " def: " + def);
            }
        }
        
        return term;
    }
    
    /**
     * Given the tag+value map for a term, work out whether the term is marked
     * as obselete.
     * 
     * @param tagValues map of tag name to value for a single term
     * @return true if the term is marked obselete, false if not
     */
    public boolean isObselete(Map tagValues) {
        List vals = (List) tagValues.get("is_obsolete");
        if (vals != null && vals.size() > 0) {
            if (vals.size() > 1) {
                LOG.warn("Term: " + tagValues + " has more than one (" + vals.size()
                        + ") is_obselete values - just using first");
            }
            return ((String) vals.get(0)).equalsIgnoreCase("true");
        }
        return false;
    }
    
    /**
     * Add synonyms to a DagTerm.
     * 
     * @param term the DagTerm
     * @param synonyms List of synonyms (Strings)
     * @param type synonym type
     */
    protected void addSynonyms(DagTerm term, List synonyms, String type) {
        for (Iterator iter = synonyms.iterator(); iter.hasNext(); ) {
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
