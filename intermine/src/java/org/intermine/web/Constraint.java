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

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;

/**
 * A simple constraint representation
 * @author Mark Woodbridge
 */
public class Constraint
{
    protected ConstraintOp op;
    protected Object value;

    /**
     * Constructor
     * @param op the constraintOp for this constraint
     * @param value the value for this constraint
     */
    public Constraint(ConstraintOp op, Object value) {
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
     * Return value in display format. This performs conversion between SQL
     * wildcard % symbols and user wildcard * symbols.
     *
     * @return  constraint value translated for the user as a string
     */
    public String getDisplayValue(PathNode node) {
        if (op == ConstraintOp.MATCHES || op == ConstraintOp.DOES_NOT_MATCH) {
            return MainForm.wildcardSqlToUser(getValue().toString());
        //} else if (!node.isAttribute() && !BagConstraint.VALID_OPS.contains(getOp())) {
        //    return MainForm.dotPathToNicePath("" + getValue());
        } else {
            return "" + getValue();
        }
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