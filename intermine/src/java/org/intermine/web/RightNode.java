package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.Model;

/**
 * Node used in displaying query
 * @author Mark Woodbridge
 */
public class RightNode extends Node
{
    List constraints = new ArrayList();
    
    /**
     * Constructor for a root node
     * @param type the root type of this tree
     */
    public RightNode(String type) {
        super(type);
    }

    /**
     * Constructor for a non-root node
     * @param parent the parent node of this node
     * @param fieldName the name of the field that this node represents
     * @param model the model used to resolve paths
     */
    public RightNode(Node parent, String fieldName, Model model) {
        super(parent, fieldName, model);
    }

    /**
     * Return the list of constraints on this field
     * @return the constraints
     */
    public List getConstraints()  {
        return constraints;
    }
}