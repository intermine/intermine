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

import org.intermine.objectstore.query.ConstraintOp;

/**
 * A simple constraint representation
 * @author Mark Woodbridge
 */
public class Constraint
{
    ConstraintOp op;
    Object value;

    /**
     * Constructor
     * @param op the constraintOp for this constraint
     * @param value the value for this constraint
     */
    Constraint(ConstraintOp op, Object value) {
        this.op = op;
        this.value = value;
    }

    /**
     * Gets the value of op
     *
     * @return the value of op
     */
    public ConstraintOp getOp()  {
        return op;
    }

    /**
     * Gets the value of value
     *
     * @return the value of value
     */
    public Object getValue()  {
        return value;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return op + " " + value;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        return (o instanceof Constraint)
            && op.equals(((Constraint) o).op)
            && value.equals(((Constraint) o).value);
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * op.hashCode()
            + 3 * value.hashCode();
    }
}