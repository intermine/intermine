package org.flymine.modelproduction.acedb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Parses the AceDB model file given, and produces a Flymine model.
 *
 * @author Matthew Wakeling
 */
public class ParseAceModel
{
    private static final String PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n"
        + "<model name=\"acedb\">\n"
        + "<package name=\"org.flymine.model.acedb\">\n"
        + "<class name=\"Colour\">\n"
        + "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n"
        + "</class>\n"
        + "<class name=\"DNA\">\n"
        + "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n"
        + "    <attribute name=\"sequence\" type=\"String\"/>\n"
        + "</class>\n"
        + "<class name=\"DateType\">\n"
        + "    <attribute name=\"identifier\" type=\"Date\" primary-key=true/>\n"
        + "</class>\n"
        + "<class name=\"Float\">\n"
        + "    <attribute name=\"identifier\" type=\"float\" primary-key=true/>\n"
        + "</class>\n"
        + "<class name=\"Int\">\n"
        + "    <attribute name=\"identifier\" type=\"int\" primary-key=true/>\n"
        + "</class>\n"
        + "<class name=\"Keyword\">\n"
        + "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n"
        + "</class>\n"
        + "<class name=\"LongText\">\n"
        + "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n"
        + "    <attribute name=\"text\" type=\"String\"/>\n"
        + "</class>\n"
        + "<class name=\"Peptide\">\n"
        + "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n"
        + "    <attribute name=\"peptide\" type=\"String\"/>\n"
        + "</class>\n"
        + "<class name=\"Text\">\n"
        + "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n"
        + "</class>\n";
    private static final String POSTAMBLE = "</package>\n</model>\n";
    
    /**
     * Takes a single argument - the file to parse.
     *
     * @param args the command-line
     * @throws Exception sometimes
     */
    public static void main(String args[]) throws Exception {
        PrintStream out = System.out;
        PrintStream err = System.err;
        if (args.length != 1) {
            err.println("Usage: java org.flymine.modelproduction.acedb.ParseAceModel <file>");
        } else {
            BufferedReader in = new BufferedReader(new FileReader(args[0]));
            List classes = parse(in);
            out.print(PREAMBLE);
            Iterator classIter = classes.iterator();
            while (classIter.hasNext()) {
                ModelNode c = (ModelNode) classIter.next();
                //printModelNode(c);
                out.print(nodeClassToXML(c));
            }
            out.print(POSTAMBLE);
        }
    }

    /**
     * Parses the given file.
     *
     * @param in the BufferedReader containing the file to parse
     * @return a List of ModelNodes, each representing a class
     * @throws IOException when the file has a problem
     */
    public static List parse(BufferedReader in) throws IOException {
        PrintStream out = System.out;
        PrintStream err = System.err;
        Stack indents = new Stack();
        List results = new ArrayList();
        ModelTokenStream mts = new ModelTokenStream(in);
        ModelNode mn = mts.nextToken();
        while (mn != null) {
            //out.println("ModelNode - indent =  " + mn.getIndent() + ", token = "
            //        + mn.getName());
            if (mn.getIndent() == 0) {
                results.add(mn);
                indents = new Stack();
                indents.push(mn);
                mn.setAnnotation(ModelNode.ANN_CLASS);
            } else {
                while (((ModelNode) indents.peek()).getIndent() > mn.getIndent()) {
                    indents.pop();
                }
                ModelNode parent = null;
                if (((ModelNode) indents.peek()).getIndent() < mn.getIndent()) {
                    parent = (ModelNode) indents.peek();
                    parent.setChild(mn);
                    indents.push(mn);
                } else {
                    ModelNode sibling = (ModelNode) indents.pop();
                    if (sibling.getIndent() != mn.getIndent()) {
                        throw new IllegalArgumentException("Unmatched indentation");
                    }
                    parent = (ModelNode) indents.peek();
                    sibling.setSibling(mn);
                    indents.push(mn);
                }
                switch (parent.getAnnotation()) {
                    case ModelNode.ANN_CLASS:
                        if ("UNIQUE".equals(mn.getName())) {
                            mn.setAnnotation(ModelNode.ANN_KEYWORD);
                        } else {
                            mn.setAnnotation(ModelNode.ANN_TAG);
                        }
                        break;
                    case ModelNode.ANN_TAG:
                        if ("UNIQUE".equals(mn.getName())) {
                            mn.setAnnotation(ModelNode.ANN_KEYWORD);
                        } else if ("Text".equals(mn.getName()) || "Float".equals(mn.getName())
                                || "Int".equals(mn.getName()) || "DateType".equals(mn.getName())
                                || mn.getName().startsWith("?")
                                || mn.getName().startsWith("#")) {
                            mn.setAnnotation(ModelNode.ANN_REFERENCE);
                        } else {
                            mn.setAnnotation(ModelNode.ANN_TAG);
                        }
                        break;
                    case ModelNode.ANN_KEYWORD:
                        if ("XREF".equals(parent.getName())) {
                            mn.setAnnotation(ModelNode.ANN_XREF);
                        } else if ("UNIQUE".equals(parent.getName())) {
                            if ("Text".equals(mn.getName()) || "Float".equals(mn.getName())
                                    || "Int".equals(mn.getName()) || "DateType".equals(mn.getName())
                                    || mn.getName().startsWith("?")
                                    || mn.getName().startsWith("#")) {
                                mn.setAnnotation(ModelNode.ANN_REFERENCE);
                            } else {
                                mn.setAnnotation(ModelNode.ANN_TAG);
                            }
                        } else {
                            throw new IllegalArgumentException("Keyword \"" + parent.getName()
                                    + "\" before \"" + mn.getName() + "\" not recognised.");
                        }
                        break;
                    case ModelNode.ANN_REFERENCE:
                    case ModelNode.ANN_XREF:
                        if ("UNIQUE".equals(mn.getName()) || "XREF".equals(mn.getName())
                                || "REPEAT".equals(mn.getName())) {
                            mn.setAnnotation(ModelNode.ANN_KEYWORD);
                        } else {
                            mn.setAnnotation(ModelNode.ANN_REFERENCE);
                        }
                        break;
                }
            }
            mn = mts.nextToken();
        }
        //out.println("Final list of classes: " + results);
        return results;
    }

    /**
     * Prints a ModelNode object.
     *
     * @param node a ModelNode to print
     */
    public static void printModelNode(ModelNode node) {
        printModelNode(node, 0);
    }

    private static void printModelNode(ModelNode node, int indent) {
        PrintStream out = System.out;
        for (int i = 0; i < indent; i++) {
            out.print("    ");
        }
        out.println(node.getName() + ": " + ModelNode.ANN_STRINGS[node.getAnnotation()]);
        if (node.getChild() != null) {
            printModelNode(node.getChild(), indent + 1);
        }
        if (node.getSibling() != null) {
            printModelNode(node.getSibling(), indent);
        }
    }

    /**
     * Converts a ModelNode that is a class into XML.
     *
     * @param node a ModelNode to convert
     * @return an XML String
     */
    public static String nodeClassToXML(ModelNode node) {
        String retval = null;
        if (node.getAnnotation() == ModelNode.ANN_CLASS) {
            retval = "<class name=\"" + node.getName().substring(1) + "\">\n";
            retval += "    <attribute name=\"identifier\" type=\"String\" primary-key=true/>\n";
            retval += nodeToXML(node.getChild(), null, true);
            retval += "</class>\n";
        } else {
            throw new IllegalArgumentException("Not a class");
        }
        return retval;
    }

    /**
     * Converts a ModelNode that is not a class into XML.
     *
     * @param node a ModelNode to convert
     * @param parent the name of the parent, for the case where this node is actually a Reference
     * @param collection true if the parent node is not a UNIQUE keyword
     * @return an XML String
     */
    public static String nodeToXML(ModelNode node, String parent, boolean collection) {
        String retval = null;
        if (node.getAnnotation() == ModelNode.ANN_TAG) {
            if (node.getChild() != null) {
                retval = nodeToXML(node.getChild(), node.getName(), true);
            } else {
                retval = "    <attribute name=\"" + node.getName() + "\" type=\"boolean\"/>\n";
            }
            if (node.getSibling() != null) {
                retval += nodeToXML(node.getSibling(), parent, collection);
            }
        } else if ((node.getAnnotation() == ModelNode.ANN_KEYWORD)
                && "UNIQUE".equals(node.getName())) {
            if (node.getSibling() != null) {
                throw new IllegalArgumentException("Unsuitable node next to TAG-UNIQUE");
            }
            if (node.getChild() != null) {
                retval = nodeToXML(node.getChild(), parent, false);
            } else {
                throw new IllegalArgumentException("UNIQUE cannot be a leaf node");
            }
        } else if (node.getAnnotation() == ModelNode.ANN_REFERENCE) {
            retval = nodeRefToXML(node, parent, collection, 1);
        } else {
            throw new IllegalArgumentException("Unknown node");
        }
        return retval;
    }

    /**
     * Converts a ModelNode that is a reference into XML.
     *
     * @param node a ModelNode to convert
     * @param parent the name of the parent tag
     * @param collection true if this reference is a collection
     * @param number the field number for this parent tag name
     * @return an XML String
     */
    public static String nodeRefToXML(ModelNode node, String parent, boolean collection,
            int number) {
        if (node.getSibling() != null) {
            throw new IllegalArgumentException("Another node next to a reference");
        }
        String xref = "";
        ModelNode nextNode = node.getChild();
        if ((nextNode != null) && (nextNode.getAnnotation() == ModelNode.ANN_KEYWORD)
                && "XREF".equals(nextNode.getName())
                && (nextNode.getChild() != null)
                && (nextNode.getChild().getAnnotation() == ModelNode.ANN_XREF)) {
            xref = " reverse-reference=\"" + nextNode.getChild().getName() + "\"";
            nextNode = nextNode.getChild().getChild();
        }
        if ((nextNode != null) && (nextNode.getAnnotation() == ModelNode.ANN_KEYWORD)
                && "REPEAT".equals(nextNode.getName())) {
            collection = true;
            nextNode = nextNode.getChild();
        }
        String fieldName = parent + (number == 1 ? "" : "_" + number);
        String type = node.getName();
        if ((type.charAt(0) == '#') || (type.charAt(0) == '?')) {
            type = type.substring(1);
        }
        String retval = "    " + (collection ? "<collection " : "<reference ") + "name=\""
                + fieldName + "\" referenced-type=\"" + type + "\"" + xref + "/>\n";
        if (nextNode != null) {
            if (nextNode.getAnnotation() == ModelNode.ANN_REFERENCE) {
                retval += nodeRefToXML(nextNode, parent, true, number + 1);
            } else if ((nextNode.getAnnotation() == ModelNode.ANN_KEYWORD)
                    && "UNIQUE".equals(nextNode.getName())) {
                nextNode = nextNode.getChild();
                if ((nextNode != null) && (nextNode.getAnnotation() == ModelNode.ANN_REFERENCE)) {
                    retval += nodeRefToXML(nextNode, parent, collection, number + 1);
                } else {
                    throw new IllegalArgumentException("Invalid node type after a reference and"
                            + " UNIQUE");
                }
            } else {
                throw new IllegalArgumentException("Invalid node type after a reference");
            }
        }
        return retval;
    }

}
