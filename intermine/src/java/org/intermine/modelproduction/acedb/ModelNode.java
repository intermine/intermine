package org.intermine.modelproduction.acedb;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An object to represent a node in an acedb model file.
 * It has an indentation level, for use while building.
 *
 * @author Matthew Wakeling
 */
public class ModelNode
{
    private int indent;
    private ModelNode child;
    private ModelNode sibling;
    private String name;
    private int annotation;

    /** No annotation done. */
    public static final int ANN_NONE = 0;
    /** This node is a class. */
    public static final int ANN_CLASS = 1;
    /** This node is a naming tag. */
    public static final int ANN_TAG = 2;
    /** This node is a keyword - UNIQUE, REPEAT, or XREF. */
    public static final int ANN_KEYWORD = 3;
    /** This node is a reference or collection. */
    public static final int ANN_REFERENCE = 4;
    /** This node is a cross-reference name. */
    public static final int ANN_XREF = 5;
    /** An array of natural-language equivalents of the ANN_ constants. */
    public static final String ANN_STRINGS[] = {null, "CLASS", "TAG", "KEYWORD", "REFERENCE",
        "XREF"};

    /**
     * Constructor.
     *
     * @param indent the number of characters of indent
     * @param name the name of the tag at this position
     */
    public ModelNode(int indent, String name) {
        this.indent = indent;
        this.name = name;
        this.child = null;
        this.sibling = null;
        this.annotation = ANN_NONE;
    }

    /**
     * Setter for child.
     *
     * @param child the new child ModelNode
     */
    public void setChild(ModelNode child) {
        this.child = child;
    }

    /**
     * Setter for sibling.
     *
     * @param sibling the new sibling ModelNode
     */
    public void setSibling(ModelNode sibling) {
        this.sibling = sibling;
    }
    
    /**
     * Setter for annotation.
     *
     * @param annotation an annotation type for this node
     */
    public void setAnnotation(int annotation) {
        this.annotation = annotation;
    }
    
    /**
     * Getter for indent.
     *
     * @return indent
     */
    public int getIndent() {
        return indent;
    }

    /**
     * Getter for name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the child.
     *
     * @return child
     */
    public ModelNode getChild() {
        return child;
    }

    /**
     * Getter for sibling.
     *
     * @return sibling
     */
    public ModelNode getSibling() {
        return sibling;
    }

    /**
     * Getter for annotation.
     *
     * @return annotation
     */
    public int getAnnotation() {
        return annotation;
    }

    /**
     * Overrides Object.
     *
     * @return name
     */
    public String toString() {
        return name;
    }
}
