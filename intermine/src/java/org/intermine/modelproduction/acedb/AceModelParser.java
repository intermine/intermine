package org.flymine.modelproduction.acedb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.flymine.modelproduction.AbstractModelParser;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.Model;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.MetaDataException;

/**
 * Parses the AceDB model file given, and produces a Flymine model.
 *
 * @author Matthew Wakeling
 */
public class AceModelParser extends AbstractModelParser
{
    private static final String PACKAGE = "org.flymine.model.acedb.";

    /**
     * Takes a single argument - the file to parse.
     *
     * @param args the command-line
     * @throws Exception sometimes
     */
    /*    public static void main(String args[]) throws Exception {
        PrintStream out = System.out;
        PrintStream err = System.err;
        if (args.length != 1) {
            err.println("Usage: java org.flymine.modelproduction.acedb.ParseAceModel <file>");
        } else {
            BufferedReader in = new BufferedReader(new FileReader(args[0]));
            Model m = readerToModel(in);
            out.print(m.toString());
        }
    }
    */
    /**
     * @see ModelParser#process
     * @throws Exception
     */
    public Model process(InputStream is) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        Model m = readerToModel(in);
        return m;
    }

    /**
     * Converts an ACEDB model file provided in the BufferedReader into a Flymine Model.
     *
     * @param in the contents of the ACEDB model file
     * @return a Flymine Model
     * @throws IOException if the BufferedReader does
     * @throws MetaDataException if the model is inconsistent
     */
    public Model readerToModel(BufferedReader in) throws IOException, MetaDataException {
        Set classes = parse(in);
        Set classDescriptors = new LinkedHashSet();
        addBuiltinClasses(classDescriptors);
        Iterator classIter = classes.iterator();
        while (classIter.hasNext()) {
            ModelNode c = (ModelNode) classIter.next();
            classDescriptors.add(nodeClassToDescriptor(c));
        }
        return new Model("acedb", classDescriptors);
    }

    /**
     * Adds a predefined set of builtin classes.
     *
     * @param l a Set of ClassDescriptors to add to
     */
    public void addBuiltinClasses(Set l) {
        Set atts = new LinkedHashSet();
        Set refs = Collections.EMPTY_SET;
        Set cols = Collections.EMPTY_SET;
        atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
        l.add(new ClassDescriptor(PACKAGE + "Colour", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
        atts.add(new AttributeDescriptor("sequence", false, "java.lang.String"));
        l.add(new ClassDescriptor(PACKAGE + "DNA", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "java.util.Date"));
        l.add(new ClassDescriptor(PACKAGE + "DateType", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "float"));
        l.add(new ClassDescriptor(PACKAGE + "Float", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "int"));
        l.add(new ClassDescriptor(PACKAGE + "Int", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
        l.add(new ClassDescriptor(PACKAGE + "Keyword", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
        atts.add(new AttributeDescriptor("text", false, "java.lang.String"));
        l.add(new ClassDescriptor(PACKAGE + "LongText", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
        atts.add(new AttributeDescriptor("peptide", false, "java.lang.String"));
        l.add(new ClassDescriptor(PACKAGE + "Peptide", null, null, false, atts, refs, cols));
        atts = new LinkedHashSet();
        atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
        l.add(new ClassDescriptor(PACKAGE + "Text", null, null, false, atts, refs, cols));
    }

    /**
     * Parses the given file.
     *
     * @param in the BufferedReader containing the file to parse
     * @return a Set of ModelNodes, each representing a class
     * @throws IOException when the file has a problem
     */
    public Set parse(BufferedReader in) throws IOException {
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
    public void printModelNode(ModelNode node) {
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
    public ClassDescriptor nodeClassToDescriptor(ModelNode node) {
        if (node.getAnnotation() == ModelNode.ANN_CLASS) {
            Set atts = new LinkedHashSet();
            Set refs = new LinkedHashSet();
            Set cols = new LinkedHashSet();
            atts.add(new AttributeDescriptor("identifier", true, "java.lang.String"));
            nodeToSets(node.getChild(), "value", true, atts, refs, cols);
            return new ClassDescriptor(PACKAGE + node.getName().substring(1),
                    null, null, false, atts, refs, cols);
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
    public void nodeToSets(ModelNode node, String parent, boolean collection, Set atts,
            Set refs, Set cols) {
        if (node.getAnnotation() == ModelNode.ANN_TAG) {
            if (node.getChild() != null) {
                nodeToSets(node.getChild(), node.getName(), true, atts, refs, cols);
            } else {
                atts.add(new AttributeDescriptor(node.getName(), false, "boolean"));
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
    public void nodeRefToSets(ModelNode node, String parent, boolean collection,
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
        String fieldName = parent + (number == 1 ? "" : "_" + number);
        String type = node.getName();
        if ((type.charAt(0) == '#') || (type.charAt(0) == '?')) {
            type = type.substring(1);
        }
        if (collection) {
            cols.add(new CollectionDescriptor(fieldName, false, PACKAGE + type, xref, false));
        } else if ("Text".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, false, "java.lang.String"));
        } else if ("Float".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, false, "float"));
        } else if ("Int".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, false, "int"));
        } else if ("DateType".equals(type)) {
            atts.add(new AttributeDescriptor(fieldName, false, "java.util.Date"));
        } else {
            refs.add(new ReferenceDescriptor(fieldName, false, PACKAGE + type, xref));
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

}
