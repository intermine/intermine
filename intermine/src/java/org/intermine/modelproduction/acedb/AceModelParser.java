package org.intermine.modelproduction.acedb;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.Reader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.intermine.modelproduction.ModelParser;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;

/**
 * Parses the AceDB model file given, and produces a InterMine model.
 *
 * @author Matthew Wakeling
 */
public class AceModelParser implements ModelParser
{
    protected String nameSpace;
    protected String modelName, pkgName;

    /**
     * No-arg constructor for testing purposes
     */
    protected AceModelParser() {
    }

    /**
     * Constructor that takes the modelName - required because not available from wrm file
     * but necessary to name ClassDescriptors correctly
     *
     * @param modelName the name of the model to produce
     * @param pkgName the name of package in model
     * @param nameSpace the XML name space
     */
    public AceModelParser(String modelName, String pkgName, String nameSpace) {
        this.modelName = modelName;
        this.pkgName = pkgName;
        this.nameSpace = nameSpace;
    }

    /**
     * Read source model information in Ace model format and
     * construct a InterMine Model object.
     *
     * @param reader the AceDBsource model to parse
     * @return the InterMine Model created
     * @throws Exception if a problem occurs during parsing
     */
    public Model process(Reader reader) throws Exception {
        Set classes = parse(new BufferedReader(reader));
        Set classDescriptors = new LinkedHashSet();
        addBuiltinClasses(classDescriptors);
        Iterator classIter = classes.iterator();
        while (classIter.hasNext()) {
            ModelNode c = (ModelNode) classIter.next();
            classDescriptors.add(nodeClassToDescriptor(c));
        }
        return new Model(modelName, nameSpace + "/" + modelName, classDescriptors);
    }

    /**
     * Adds a predefined set of builtin classes.
     *
     * @param l a Set of ClassDescriptors to add to
     */
    protected void addBuiltinClasses(Set l) {
        Set atts = new LinkedHashSet();
        Set refs = Collections.EMPTY_SET;
        Set cols = Collections.EMPTY_SET;
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        l.add(new ClassDescriptor(qualified("Colour"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        atts.add(new AttributeDescriptor("sequence", "java.lang.String"));
        l.add(new ClassDescriptor(qualified("DNA"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.util.Date"));
        l.add(new ClassDescriptor(qualified("DateType"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.Float"));
        l.add(new ClassDescriptor(qualified("Float"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.Integer"));
        l.add(new ClassDescriptor(qualified("Int"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        refs = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        //refs.add(new ReferenceDescriptor("Quoted_in", false, "org.intermine.model.acedb.Paper",
        //            null));
        l.add(new ClassDescriptor(qualified("Keyword"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        refs = Collections.EMPTY_SET;
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        atts.add(new AttributeDescriptor("text", "java.lang.String"));
        l.add(new ClassDescriptor(qualified("LongText"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        atts.add(new AttributeDescriptor("peptide", "java.lang.String"));
        l.add(new ClassDescriptor(qualified("Peptide"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        l.add(new ClassDescriptor(qualified("Text"), null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
        l.add(new ClassDescriptor(qualified("Comment"), null, false, atts, refs, cols));
    }

    /**
     * Parses the given file.
     *
     * @param in the BufferedReader containing the file to parse
     * @return a Set of ModelNodes, each representing a class
     * @throws IOException when the file has a problem
     */
    protected Set parse(BufferedReader in) throws IOException {
        PrintStream out = System.out;
        PrintStream err = System.err;
        Stack indents = new Stack();
        Set results = new LinkedHashSet();
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
                ModelNode lastPopped = null;
                while (((ModelNode) indents.peek()).getIndent() > mn.getIndent()) {
                    lastPopped = (ModelNode) indents.pop();
                }
                ModelNode parent = null;
                if (((ModelNode) indents.peek()).getIndent() < mn.getIndent()) {
                    if ((lastPopped != null) && (lastPopped.getIndent() != mn.getIndent())) {
                        throw new IllegalArgumentException("Unmatched indentation");
                    }
                    parent = (ModelNode) indents.peek();
                    parent.setChild(mn);
                    indents.push(mn);
                } else {
                    ModelNode sibling = (ModelNode) indents.pop();
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
        //out.println("Final set of classes: " + results);
        return results;
    }

    /**
     * Prints a ModelNode object.
     *
     * @param node a ModelNode to print
     */
    protected void printModelNode(ModelNode node) {
        printModelNode(node, 0);
    }

    private void printModelNode(ModelNode node, int indent) {
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
     * Converts a ModelNode that is a class into a ClassDescriptor.
     *
     * @param node a ModelNode to convert
     * @return a ClassDescriptor for the ModelNode
     */
    protected ClassDescriptor nodeClassToDescriptor(ModelNode node) {
        if (node.getAnnotation() == ModelNode.ANN_CLASS) {
            Set atts = new LinkedHashSet();
            Set refs = new LinkedHashSet();
            Set cols = new LinkedHashSet();
            atts.add(new AttributeDescriptor("identifier", "java.lang.String"));
            nodeToSets(node.getChild(), "value", true, atts, refs, cols);
            return new ClassDescriptor(qualified(formatAceName(node.getName().substring(1))),
                                       null, false, atts, refs, cols);
        } else {
            throw new IllegalArgumentException("Not a class");
        }
    }

    /**
     * Converts a ModelNode that is not a class into a FieldDescriptor and puts it into the supplied
     * Sets.
     *
     * @param node a ModelNode to convert
     * @param parent the name of the parent, for the case where this node is actually a Reference
     * @param collection true if the parent node is not a UNIQUE keyword
     * @param atts a Set of AttributeDescriptors to add to
     * @param refs a Set of ReferenceDescriptors to add to
     * @param cols a Set of CollectionDescriptors to add to
     */
    protected void nodeToSets(ModelNode node, String parent, boolean collection, Set atts,
            Set refs, Set cols) {
        if (node.getAnnotation() == ModelNode.ANN_TAG) {
            if (node.getChild() != null) {
                nodeToSets(node.getChild(), node.getName(), true, atts, refs, cols);
            } else {
                atts.add(new AttributeDescriptor(formatAceName(node.getName()), "boolean"));
            }
            if (node.getSibling() != null) {
                nodeToSets(node.getSibling(), parent, collection, atts, refs, cols);
            }
        } else if ((node.getAnnotation() == ModelNode.ANN_KEYWORD)
                && "UNIQUE".equals(node.getName())) {
            if (node.getSibling() != null) {
                throw new IllegalArgumentException("Unsuitable node next to TAG-UNIQUE");
            }
            if (node.getChild() != null) {
                nodeToSets(node.getChild(), parent, false, atts, refs, cols);
            } else {
                throw new IllegalArgumentException("UNIQUE cannot be a leaf node");
            }
        } else if (node.getAnnotation() == ModelNode.ANN_REFERENCE) {
            nodeRefToSets(node, parent, collection, 1, atts, refs, cols);
        } else {
            throw new IllegalArgumentException("Unknown node");
        }
    }

    /**
     * Converts a ModelNode that is a reference into a FieldDescriptor and puts it into the supplied
     * Sets.
     *
     * @param node a ModelNode to convert
     * @param parent the name of the parent tag
     * @param collection true if this reference is a collection
     * @param number the field number for this parent tag name
     * @param atts a Set of AttributeDescriptors to add to
     * @param refs a Set of ReferenceDescriptors to add to
     * @param cols a Set of CollectionDescriptors to add to
     */
    protected void nodeRefToSets(ModelNode node, String parent, boolean collection,
            int number, Set atts, Set refs, Set cols) {
        if (node.getSibling() != null) {
            throw new IllegalArgumentException("Another node next to a reference");
        }
        String xref = null;
        ModelNode nextNode = node.getChild();
        if ((nextNode != null) && (nextNode.getAnnotation() == ModelNode.ANN_KEYWORD)
                && "XREF".equals(nextNode.getName())
                && (nextNode.getChild() != null)
                && (nextNode.getChild().getAnnotation() == ModelNode.ANN_XREF)) {
            xref = nextNode.getChild().getName();
            nextNode = nextNode.getChild().getChild();
        }
        if ((nextNode != null) && (nextNode.getAnnotation() == ModelNode.ANN_KEYWORD)
                && "REPEAT".equals(nextNode.getName())) {
            collection = true;
            nextNode = nextNode.getChild();
        }
        String fieldName = formatAceName(parent + (number == 1 ? "" : "_" + number));
        String type = node.getName();
        if ((type.charAt(0) == '#') || (type.charAt(0) == '?')) {
            type = type.substring(1);
        }
        if (collection) {
            cols.add(new CollectionDescriptor(fieldName, qualified(formatAceName(type)),
                                              formatAceName(xref), false));
        } else if ("Text".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, "java.lang.String"));
        } else if ("Float".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, "java.lang.Float"));
        } else if ("Int".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, "java.lang.Integer"));
        } else if ("DateType".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, "java.util.Date"));
        } else {
            refs.add(new ReferenceDescriptor(fieldName, qualified(formatAceName(type)),
                                             formatAceName(xref)));
        }
        if (nextNode != null) {
            if (nextNode.getAnnotation() == ModelNode.ANN_REFERENCE) {
                nodeRefToSets(nextNode, parent, true, number + 1, atts, refs, cols);
            } else if ((nextNode.getAnnotation() == ModelNode.ANN_KEYWORD)
                    && "UNIQUE".equals(nextNode.getName())) {
                nextNode = nextNode.getChild();
                if (nextNode == null) {
                    throw new IllegalArgumentException("UNIQUE cannot be a leaf node");
                } else if (nextNode.getAnnotation() == ModelNode.ANN_REFERENCE) {
                    nodeRefToSets(nextNode, parent, collection, number + 1, atts, refs, cols);
                } else {
                    throw new IllegalArgumentException("Invalid node type after a reference and"
                            + " UNIQUE");
                }
            } else {
                throw new IllegalArgumentException("Invalid node type after a reference");
            }
        }
    }


    /**
     * Convert Ace field/Class names into valid java names.  Some Ace names
     * begin with digits or use java keywords.
     *
     * @param name the Ace object name
     * @return a valid java name
     */
    public static String formatAceName(String name) {
        if (name == null) {
            return null;
        }

        // cannot have digits as first character of field/Class names in java
        if (Character.isDigit(name.charAt(0))) {
            return "X" + name;
        }
        if (name.equals("Class") || name.equals("Id")) {
            return "Ace" + name;
        }
        return name;
    }

    /**
     * Undo conversion from Ace names to valid java names.  Conversion follows
     * simple rules, unformatting is not fool proof but currently works.
     * @param name the java name of an ace equivalent
     * @return the original Ace name
     */
    public static String unformatAceName(String name) {
        if (name == null) {
            return null;
        }

        // strip off x for names beginning with digits
        if (name.substring(0, 1).equals("x") && Character.isDigit(name.charAt(1))) {
            return name.substring(1);
        }
        if (name.equals("AceClass")) {
            return "Class";
        }
        if (name.equals("AceId")) {
            return "Id";
        }
        return name;
    }

    private String qualified(String clsName) {
        return pkgName + "." + clsName;
    }

}
