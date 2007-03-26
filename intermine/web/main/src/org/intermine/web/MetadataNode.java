package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Node used in displaying metadata
 * @author Mark Woodbridge
 */
public class MetadataNode extends Node
{
    String button;
    boolean selected = false;

    /**
     * Constructor for a root node
     * @param type the root type of this tree
     */
    public MetadataNode(String type) {
        super(type);
        button = " ";
    }

    /**
     * Constructor for a non-root node
     * @param parent the parent node of this node
     * @param fieldName the name of the field that this node represents
     * @param button the button displayed next to this node's name
     */
    public MetadataNode(MetadataNode parent, String fieldName, String button) {
        super(parent, fieldName);
        this.button = button;
    }

    /**
     * Gets the value of button
     *
     * @return the value of button
     */
    public String getButton()  {
        return button;
    }

    /**
     * Get the value of selected
     * @return the value of selected as a boolean
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the value of selected
     * @param selected a boolean
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
}