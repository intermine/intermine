package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Node used in displaying query
 * @author Mark Woodbridge
 */
public class PathNode extends Node
{
    List constraints = new ArrayList();

    /**
     * Constructor for a root node
     * Constucting a PathNode is not usually necessary - use PathQuery#addNode
     * @param type the root type of this tree
     */
    public PathNode(String type) {
        super(type);
    }

    /**
     * Constructor for a non-root node
     * Constucting a PathNode is not usually necessary - use PathQuery#addNode
     * @param parent the parent node of this node
     * @param fieldName the name of the field that this node represents
     */
    public PathNode(Node parent, String fieldName) {
        super(parent, fieldName);
    }

    /**
     * Gets the value of constraints
     *
     * @return the value of constraints
     */
    public List getConstraints()  {
        return constraints;
    }

    /**
     * Sets the value of constraints
     *
     * @param constraints value to assign to constraints
     */
    public void setConstraints(List constraints) {
        this.constraints = constraints;
    }


    /**
     * Removes a constraint from list
     * @param constraint to remove
     */
    public void removeConstraint(Constraint constraint) {
        this.constraints.remove(constraint);
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return super.toString() + " " + constraints;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        return super.equals(o)
            && (o instanceof PathNode)
            && constraints.equals(((PathNode) o).constraints);
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * super.hashCode()
            + 3 * constraints.hashCode();
    }
}
