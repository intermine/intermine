package org.flymine.owlproduction;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Stack;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.File;
import java.io.FileWriter;
import java.util.StringTokenizer;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;


/**
 * Convert a file in DAG format to OWL using Jena ontology API.
 * The parsing code borrows heavily from code written by Iwei Yeh
 * of Stanford University.
 *
 * @author Richard Smith
 *
 */
public class DagToOwl
{

    protected Stack parents = new Stack();
    private OntModel m;
    private String namespace;
    private String prefix;

    private final String comment = "!";
    private final String domain = "$";
    private final String isa = "%";
    private final String partof = "<";
    private final String delimiter = " ; ";


    /**
     * Construct a DagToOwl parser with namespace and prefix for use in target document.
     * @param namespace for classes written in target model
     * @param prefix to be assigned to given namespace in target OWL file
     * @throws Exception if error occurs generating model
     */
    public DagToOwl(String namespace, String prefix) throws Exception {
        this.namespace = namespace;
        this.prefix = prefix;
        m = ModelFactory.createOntologyModel();
    }


    /**
     * Parse a DAG file to produce a Jena OWL ontology model.
     * @param input text in DAG format
     * @return a Jena OWL ontology
     * @throws Exception if anything goes wrong
     */
    public OntModel process(BufferedReader input) throws Exception {
        readTerms(input);
        return m;
    }

    /**
     * Read DAG input line by line to generate ontology classes in OntModel.
     * @param input text in DAG format
     * @throws Exception if anything goes wrong
     */
    public void readTerms(BufferedReader input) throws Exception {
        String line;
        int prevspaces = -1;
        int currspaces;

        while ((line = input.readLine()) != null) {
            OntClass ont = null;

            if (!line.startsWith(comment) && !line.equals("")) {
                int length = line.length();
                line = trimLeft(line);
                if (line.startsWith(isa) || line.startsWith(domain)) {
                    currspaces = length - line.length();
                    if (prevspaces == -1) {
                        prevspaces = currspaces;
                        ont = makeOntClass(line);
                        parents.push(ont);
                    } else if (currspaces == prevspaces) {
                        // same parent as previous term
                        parents.pop();
                        ont = makeOntClass(line);
                        parents.push(ont);
                    } else if (currspaces > prevspaces) {
                        // term is a child of previous
                        ont = makeOntClass(line);
                        parents.push(ont);
                    } else if (currspaces < prevspaces) {
                        // how far have we moved back up nesting?
                        for (int i = currspaces; i <= prevspaces; i++) {
                            parents.pop();
                        }
                        ont = makeOntClass(line);
                        parents.push(ont);
                    }
                    prevspaces = currspaces;
                }
            }
        }
    }


    /**
     * Create (or get from model) an OntClass given a line of DAG text - keeps track of indentation
     * to manage parent classes.
     * @param line a line of DAG text
     * @return an onology class representing a DAG term
     */
    protected OntClass makeOntClass(String line) {
        line = line.trim();
        String token = line.substring(0, 1);
        line = line.substring(1);
        StringTokenizer tokenizer = new StringTokenizer(line, (domain + isa + partof), true);

        String term = tokenizer.nextToken();
        OntClass ont = null;

        // at present only read isa relationships
        if (token.equals(isa) || token.equals(domain)) {

            // details of this class from first token
            ont = ontClassFromString(term);

            if (token.equals(isa)) {
                ont.addSuperClass((OntClass) parents.peek());
            }

            // other tokens are additional relations; parents or partofs
            while (tokenizer.hasMoreTokens()) {
                String relation = tokenizer.nextToken();
                if (relation.equals(isa)) {
                    relation = tokenizer.nextToken();
                    ont.addSuperClass(ontClassFromString(relation.trim()));
                }
            }
        }
        return ont;
    }


    /**
     * Create (or get from model) an OntClass given a string defining a DAG term.
     * @param details string representing a DAG term
     * @return the generated OWL class
     */
    public OntClass ontClassFromString(String details) {
        String[] elements = details.split(delimiter);
        String name = cleanNameString(elements[0]);

        OntClass ont = m.getOntClass(namespace + name);
        if (ont == null) {
            ont = m.createClass(namespace + name);
        }
        return ont;
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
     * OWL class names must be valid URIs - need to clean DAG term names of restricted
     * characters.
     * @param s string to process
     * @return a cleaned-up string
     */
    protected String cleanNameString(String s) {
        s = stripEscaped(s);
        s = stripForwardSlashes(s);
        s = spaceToUnderscore(s);
        return s;
    }


    /**
     * Replace spaces with underscores.
     * @param s string to process
     * @return a cleaned-up string
     */
    protected String spaceToUnderscore(String s) {
        return s = s.replace(' ', '_');
    }

    /**
     * Remove forward slashes from a string.
     * @param s string to process
     * @return a cleaned-up string
     */
    private String stripForwardSlashes(String s) {
        // TODO find out how to escape / correctly
        return s = s.replaceAll("/", " or ");
    }

    /**
     * Some punctuation characters are escaped in DAG files, remove backslashes.
     * @param s string to remove escaped characters from
     * @return a cleaned-up string
     */
    protected String stripEscaped(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if ('\\' != s.charAt(i) && !Character.isLetterOrDigit((char) (i + 1))) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * temporary main method to aid evaluation
     * @param args dagFile, owlFile
     */
    public static void main(String[] args) {
        String dagFilename = args[0];
        String owlFilename = args[1];

        int num = args.length;

        String namespace = null;
        if (num >= 3) {
            namespace = args[2];
        }
        if (namespace == null) {
            namespace = "http://www.flymine.org/namespace#";
        }

        String prefix = null;
        if (num >= 4) {
            prefix = args[3];
        }
        if (prefix == null) {
            prefix = "flymine";
        }

        try {
            File dagFile = new File(dagFilename);
            File owlFile = new File(owlFilename);
            DagToOwl parser = new DagToOwl(namespace, prefix);
            OntModel model = parser.process(new BufferedReader(new FileReader(dagFile)));

            Writer out = new FileWriter(owlFile);
            model.write(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}


