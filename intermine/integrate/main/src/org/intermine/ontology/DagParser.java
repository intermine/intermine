package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;

/**
 * Parse a file in DAG format into a tree of DagTerms.
 * This code borrows heavily from code written by Iwei Yeh of Stanford University.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 * @author Andrew Varley
 *
 */
public class DagParser
{

    protected Stack parents = new Stack();
    protected HashSet rootTerms = new HashSet();
    protected HashMap seenTerms = new HashMap();

    private static final String COMMENT = "!";
    private static final String DOMAIN = "$";
    private static final String ISA = "%";
    private static final String PARTOF = "<";
    private static final String DELIMITER = " ; ";

    /**
     * Parse a DAG file to produce a set of toplevel DagTerms.
     * @param in text in DAG format
     * @return a set of DagTerms - will contain only toplevel (domain) terms
     * @throws IOException if anything goes wrong
     */
    public Set processForLabellingOntology(Reader in) throws IOException {
        readTerms(new BufferedReader(replaceRelationStrings(in)));
        return rootTerms;
    }

    /**
     * Parse a DAG file to produce a set of toplevel DagTerms, having fixed orphans.
     * @param in text in DAG format
     * @return a set of DagTerms - will contain only toplevel (domain) terms
     * @throws Exception if anything goes wrong
     */
    public Set processForClassHeirarchy(Reader in) throws Exception {
        readTerms(new BufferedReader(replaceRelationStrings(in)));
        fixOrphans();
        return rootTerms;
    }

    /**
     * Parse a DAG file to produce a set map from ontology term id to name.
     * @param in text in DAG format
     * @return a map from ontology term identifier to name
     * @throws Exception if anything goes wrong
     */
    public Map getTermIdNameMap(Reader in) throws Exception {
        readTerms(new BufferedReader(replaceRelationStrings(in)));

        Map termIdNameMap = new HashMap();
        Iterator rootIter = rootTerms.iterator();
        while (rootIter.hasNext()) {
            createTermIdNameMap(termIdNameMap, (DagTerm) rootIter.next());
        }
        return termIdNameMap;
    }

    /**
     * Read DAG input line by line to generate hierarchy of DagTerms.
     * @param in text in DAG format
     * @throws IOException if anything goes wrong
     */
    public void readTerms(BufferedReader in) throws IOException {
        String line;
        int prevspaces = -1;
        int currspaces;

        while ((line = in.readLine()) != null) {
            DagTerm term = null;

            if (!line.startsWith(COMMENT) && !line.equals("")) {
                int length = line.length();
                line = trimLeft(line);
                currspaces = length - line.length();
                if (prevspaces == -1) {
                    prevspaces = currspaces;
                    term = makeDagTerm(line);
                    parents.push(term);
                } else if (currspaces == prevspaces) {
                    // same parent as previous term
                    parents.pop();
                    term = makeDagTerm(line);
                    parents.push(term);
                } else if (currspaces > prevspaces) {
                    // term is a child of previous
                    term = makeDagTerm(line);
                    parents.push(term);
                } else if (currspaces < prevspaces) {
                    // how far have we moved back up nesting?
                    for (int i = currspaces; i <= prevspaces; i++) {
                        parents.pop();
                    }
                    term = makeDagTerm(line);
                    parents.push(term);
                }
                prevspaces = currspaces;
            }
        }
        in.close();
    }


    /**
     * Create (or get from already seen terms) a DagTerm given a line of DAG text.
     * Keeps track of indentation to manage hierachical relationships.
     * @param line a line of DAG text
     * @return a DagTerm create from line of text
     * @throws IOException if anything goes wrong
     */
    protected DagTerm makeDagTerm(String line) throws IOException {
        line = line.trim();
        String token = line.substring(0, 1);
        line = line.substring(1);
        StringTokenizer tokenizer = new StringTokenizer(line, (DOMAIN + ISA + PARTOF), true);


        String termStr = tokenizer.nextToken();
        DagTerm term = null;

        // details of this class from first token
        term = dagTermFromString(termStr);

        if (token.equals(DOMAIN)) {
            rootTerms.add(term);
        } else if (token.equals(ISA)) {
            DagTerm parent = (DagTerm) parents.peek();
            parent.addChild(term);
        } else if (token.equals(PARTOF)) {
            DagTerm whole = (DagTerm) parents.peek();
            whole.addComponent(term);
        }

        // other tokens are additional relations; parents or partofs
        while (tokenizer.hasMoreTokens()) {
            String relation = tokenizer.nextToken();
            if (relation.equals(ISA)) {
                relation = tokenizer.nextToken();
                DagTerm parent = dagTermFromString(relation.trim());
                parent.addChild(term);
            }  else if (relation.equals(PARTOF)) {
                relation = tokenizer.nextToken();
                DagTerm whole = dagTermFromString(relation);
                whole.addComponent(term);
            }
        }
        return term;
    }


    /**
     * Create (or get from already seen terms) a DagTerm given a string defining the term.
     * @param details string representing a DAG term
     * @return the generated DagTerm
     * @throws IOException if cannot find a name and id in string
     */
    public DagTerm dagTermFromString(String details) throws IOException {
        details = details.trim();
        String[] elements = details.split(DELIMITER);
        String name = stripEscaped(elements[0]);
        if (elements.length < 2) {
            throw new IllegalArgumentException("term does not have an id: " + details);
        }
        String id = stripList(elements[1]);

        // TODO check that 0 and 1 are name and id, handle broken terms better

        Identifier identifier = new Identifier(id, name);
        DagTerm term = (DagTerm) seenTerms.get(identifier);
        if (term == null) {
            term = new DagTerm(id, name);
            seenTerms.put(identifier, term);
        }

        // zero or more synonyms follow name and id
        for (int i = 2; i < elements.length; i++) {
            if (elements[i].startsWith("synonym:")) {
                term.addSynonym(new DagTermSynonym(
                        elements[i].substring(elements[i].indexOf(":") + 1)));
            }
        }
        return term;
    }

    /**
     * Number of spaces at start of line determines depth of term, String.trim() might
     * cause problems with trailing whitespace.
     * @param s string to process
     * @return a left-trimmed string
     **/
    protected String trimLeft(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return s.substring(i);
            }
        }
        return s;
    }


    /**
     * Some punctuation characters are escaped in DAG files, remove backslashes.
     * @param s string to remove escaped characters from
     * @return a cleaned-up string
     */
    protected String stripEscaped(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if ('\\' != s.charAt(i)) {
                sb.append(s.charAt(i));
            }
            //if ('\\' != s.charAt(i) && !Character.isLetterOrDigit((char) (i + 1))) {
                //sb.append(s.charAt(i));
            //} else {
            //    ab.append(s.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Return first item in a comma-seperated list
     * @param s string to process
     * @return a cleaned-up string
     */
    protected String stripList(String s) {
        int index = s.indexOf(",");
        if (index > -1) {
            return s.substring(0, index);
        }
        return s;
    }

    /**
     * Replace common alternative isa and partof strings with tokens (% and &lt;).
     * @param in a reader for the DAG text
     * @return a reader for the altered DAG text
     * @throws IOException if anything goes wrong
     */
    protected Reader replaceRelationStrings(Reader in) throws IOException {
        StringWriter writer = new StringWriter();
        BufferedReader buf = new BufferedReader(in);
        String line;
        while ((line = buf.readLine()) != null) {
            line = line.replaceAll("@ISA@|@isa@|@is_a@|@IS_A@", ISA);
            line = line.replaceAll("@PARTOF@|@partof@|@part_of@|@PART_OF@", PARTOF);
            // for the moment we want derived_from relationships to look like part_ofs
            line = line
             .replaceAll("@DERIVEDFROM@|@derivedfrom@|@derived_from@|@DERIVED_FROM@", PARTOF);
            writer.write(line + "\n");
        }
        writer.close();
        return new StringReader(writer.toString());
    }


    private void fixOrphans() {
        Iterator iter = rootTerms.iterator();
        while (iter.hasNext()) {
            DagTerm domainTerm = (DagTerm) iter.next();
            Set isas = new HashSet();
            Set partofs = new HashSet();
            readTree(domainTerm, isas, partofs);
            partofs.removeAll(isas);
            Iterator partofIter = partofs.iterator();
            while (partofIter.hasNext()) {
                domainTerm.addChild((DagTerm) partofIter.next());
            }
        }
    }

    private void readTree(DagTerm term, Set isas, Set partofs) {
        isas.addAll(term.getChildren());
        Iterator iter = term.getChildren().iterator();
        while (iter.hasNext()) {
            readTree((DagTerm) iter.next(), isas, partofs);
        }

        partofs.addAll(term.getComponents());
        iter = term.getComponents().iterator();
        while (iter.hasNext()) {
            readTree((DagTerm) iter.next(), isas, partofs);
        }
    }

    private void createTermIdNameMap(Map termIdNameMap, DagTerm term) {
        if (!termIdNameMap.containsKey(term.getId())) {
            Iterator iter = term.getChildren().iterator();
            while (iter.hasNext()) {
                createTermIdNameMap(termIdNameMap, (DagTerm) iter.next());
            }

            iter = term.getComponents().iterator();
            while (iter.hasNext()) {
                createTermIdNameMap(termIdNameMap, (DagTerm) iter.next());
            }
            termIdNameMap.put(term.getId(), term.getName());
        }
    }


    /**
     * Inner class to identify a DagTerm by its name and id.  If the same id has two different
     * names or vice versa both will be included in hieracrchy.
     */
    class Identifier
    {
        protected String id;
        protected String name;

        /**
         * Construct with an id and name
         * @param id a term id
         * @param name a term name
         */
        public Identifier(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * Test identifier for equality, Identifiers are equal if id and name are the same
         * @param o Obejct to test for equality with
         * @return true if equal
         */
        public boolean equals(Object o) {
            if (o instanceof Identifier) {
                Identifier i = (Identifier) o;
                return name.equals(i.name)
                    && id.equals(i.id);
            }
            return false;
        }

        /**
         * Generate a hashCode.
         * @return the hashCode
         */
        public int hashCode() {
            return 3 * name.hashCode() + 5 * id.hashCode();
        }
    }
}


