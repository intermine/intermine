package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.ObjectUtils;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.WebUtil;

/**
 * A simple constraint representation
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class Constraint
{
    protected ConstraintOp op;
    protected Object value;
    protected boolean editable;
    protected String description = null;
    protected String identifier = null;
    protected String code = null;

    /**
     * Make a new Constraint with no description or identifier, and which has the editable flag set
     * to false.
     * @param op the constraintOp for this constraint
     * @param value the value for this constraint
     */
    public Constraint(ConstraintOp op, Object value) {
        this.op = op;
        this.value = value;
        this.editable = false;
    }

    /**
     * Make a new Constraint with a description and an identifier.
     * @param op the constraintOp for this constraint
     * @param value the value for this constraint
     * @param editable set if this constraint should be editable in a template
     * @param description the description of this constraint
     * @param code the constraint code
     * @param identifier a label for this Constraint used for refering to this it in a
     * template. null means that this Constraint has no identifier. 
     */
    public Constraint(ConstraintOp op, Object value,
                      boolean editable, String description, String code, String identifier) {
        this.op = op;
        this.value = value;
        this.editable = editable;
        this.description = description;
        this.identifier = identifier;
        this.code = code;
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
     * Return true if and only if this constraint should be editable in a template. 
     * @return the editable flag
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Return the description that was passed to the constructor.
     * @return the description or null if unset
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the code for this constraint.
     * @return code for this constraint
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Return the identifier that was passed to the constructor.
     * @return the identifier or null if unset
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Return value in display format. This performs conversion between SQL
     * wildcard % symbols and user wildcard * symbols.
     * @return  constraint value translated for the user as a string
     */
    public String getDisplayValue() {
        if (op == ConstraintOp.MATCHES || op == ConstraintOp.DOES_NOT_MATCH) {
            return WebUtil.wildcardSqlToUser(getValue().toString());
        } else if (op == ConstraintOp.IS_NOT_NULL || op == ConstraintOp.IS_NULL) {
            return "";
        } else {
            return "" + getValue();
        }
    }
    
    /**
     * Return true if this constraint can be presented as editable in a
     * template query. This method assumes that the constraint is applied to an
     * attribute.
     * @return true if constraint can be edited in a template query
     */
    public boolean isEditableInTemplate() {
        return (op != ConstraintOp.IS_NOT_NULL
                && op != ConstraintOp.IS_NULL
                && op != ConstraintOp.IN
                && op != ConstraintOp.NOT_IN);
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
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof Constraint) {
            Constraint other = (Constraint) o;
            if (op.equals(other.op)
                && ObjectUtils.equals(value, other.value)) {
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
                return true;
            }
        }

        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * op.hashCode()
            + 3 * (value != null ? value.hashCode() : 0)
            + (description == null ? 0 : 5 * description.hashCode())
            + (identifier == null ? 0 : 7 * identifier.hashCode());
    }
}
