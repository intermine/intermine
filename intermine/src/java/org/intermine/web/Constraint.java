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
    protected ConstraintOp op;
    protected Object value;
    protected String description;
    protected String identifier;

    /**
     * Make a new Constraint with no description or identifier.
     * @param op the constraintOp for this constraint
     * @param value the value for this constraint
     */
    public Constraint(ConstraintOp op, Object value) {
        this.op = op;
        this.value = value;
        this.description = description;
    }

    /**
     * Make a new Constraint with a description and an identifier.
     * @param op the constraintOp for this constraint
     * @param value the value for this constraint
     * @param description the description of this constraint
     * @param identifier a label for this Constraint used for refering to this it in a
     * template. null means that this Constraint has no identifier. 
     */
    public Constraint(ConstraintOp op, Object value, String description, String identifier) {
        this.op = op;
        this.value = value;
        this.description = description;
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
     * Return the description that was passed to the constructor.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return the identifier that was passed to the constructor.
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Return value in display format. This performs conversion between SQL
     * wildcard % symbols and user wildcard * symbols.
     *
     * @param node  the path node related to this constraint
     * @return      constraint value translated for the user as a string
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
        return "<Constraint: " + op + ", " + value
            + (identifier == null ? "" : ", " + identifier)
            + (description == null ? "" : ", " + description)
            + ">";
    }

    /**
     * @see Object#equalsSet
     */
    public boolean equals(Object o) {
        org.intermine.web.LogMe.log("i", "this : " + this);
        org.intermine.web.LogMe.log("i", "other: " + o);

        if (o instanceof Constraint) {
            Constraint other = (Constraint) o;
            if (op.equals(other.op)
                && value.equals(other.value)) {
                if (description == null) {
                    if (other.description != null) {
                        return false;
                    }
                } else {
                    if (!description.equals(other.description)) {
                        return false;
                    }
                }
                if (identifier == null) {
                    if (other.identifier != null) {
                        return false;
                    }
                } else {
                    if (!identifier.equals(other.identifier)) {
                        return false;
                    }
                }
                org.intermine.web.LogMe.log("i", "equal");
                return true;
            }
        }
        org.intermine.web.LogMe.log("i", "not equal");

        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * op.hashCode()
            + 3 * value.hashCode()
            + (description == null ? 0 : 5 * description.hashCode())
            + (identifier == null ? 0 : 7 * identifier.hashCode());
    }
}
