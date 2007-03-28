package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
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
     * Get Constraint by index.
     * @param index index
     * @return Constraint
     */
    public Constraint getConstraint(int index) {
        return (Constraint) constraints.get(index);
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
     * Change a constraint value.
     * 
     * @param constraint an existing constrain under this node
     * @param value constraint value
     * @return the new Constraint object
     */
    public Constraint setConstraintValue(Constraint constraint, Object value) {
        if (!constraints.contains(constraint)) {
            throw new IllegalArgumentException("constraint not present on node");
        }
        int index = constraints.indexOf(constraint);
        constraints.set(index, new Constraint(constraint.getOp(), value, constraint.isEditable(),
                        constraint.getDescription(), constraint.getCode(),
                        constraint.getIdentifier()));
        return (Constraint) constraints.get(index);
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
