package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * Bean to represent one row in the display of a tree
 * @author Kim Rutherford
 * @author Mark Woodbridge
 */
public class TreeNode
{
    boolean selected, leaf, open;
    int indentation;
    Object o;
    String text;
    List<String> structure;

    /**
     * Constructor
     * @param o the Object
     * @param text extra text describing this node
     * @param indentation the indentation
     * @param selected whether the node has been selected
     * @param leaf whether this is a leaf node
     * @param open whether this node is 'open' ie. expanded
     * @param structure a List of Strings - for definition, see getStructure()
     */
    public TreeNode(Object o, String text, int indentation, boolean selected, boolean leaf,
            boolean open, List<String> structure) {
        this.o = o;
        this.text = text;
        this.indentation = indentation;
        this.selected = selected;
        this.leaf = leaf;
        this.open = open;
        this.structure = structure;
    }

    /**
     * Get the Object
     * @return the Object
     */
    public Object getObject() {
        return o;
    }

    /**
     * Get the text that was passed to the constructor
     * @return the test
     */
    public String getText() {
        return text;
    }

    /**
     * Get the indentation
     * @return the indentation
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Is this node selected?
     * @return true if this node is selected, and false otherwise
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Is this node a leaf?
     * @return true if this node is a leaf, and false otherwise
     */
    public boolean isLeaf() {
        return leaf;
    }

    /**
     * Is this node expanded?
     * @return true if this node is expanded, and false otherwise
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Returns the structure of the tree. This is an array of Strings, which are one of four
     * possible strings, in order to draw a tree structure:
     * <UL><LI><B>blank</B> - Do not draw any lines.</LI>
     *     <LI><B>straight</B> - Draw a straight vertical line.</LI>
     *     <LI><B>ell</B> - Draw an L-shaped line.</LI>
     *     <LI><B>tee</B> - Draw a T-junction, with lines going vertically and to the right.</LI>
     * </UL>
     * If the sequence of these is correctly drawn in front of each tree node, then a tree structure
     * will be drawn correctly.
     *
     * @return a List of Strings
     */
    public List<String> getStructure() {
        return structure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TreeNode)) {
            return false;
        }
        TreeNode n = (TreeNode) obj;
        return n.o.equals(o)
            && n.indentation == indentation
            && n.selected == selected
            && n.leaf == leaf
            && n.open == open;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return o.hashCode()
            + 2 * indentation
            + 3 * (selected ? 0 : 1)
            + 5 * (leaf ? 0 : 1)
            + 7 * (open ? 0 : 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return o + " " + text + " " + indentation + " " + selected + " " + leaf + " " + open;
    }
}

