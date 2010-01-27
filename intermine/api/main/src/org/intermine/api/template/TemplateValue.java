package org.intermine.api.template;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;


/**
 * A TemplateValue represents the value for an editable constraint of a template query and details
 * about what is being constrained.
 *
 * @author Richard Smith
 **/
public class TemplateValue
{
    private ConstraintOp op;
    private Object value;
    private Object extraValue;
    private String path;
    private String code;
    private boolean bagConstraint = false;
    private boolean objectConstraint = false;
    private ValueType valueType;

    /**
     * Possible values for TemplateValue type.
     */
    public enum ValueType { SIMPLE_VALUE, BAG_VALUE, OBJECT_VALUE };

    /**
     * Construct with the details of what we are constraining and the value.  The value may be a
     * user entered text, a bag name or an InterMineObject.  The extraValue is only included for
     * some constraint types.
     * @param path the path that this is constraining
     * @param op constraint operation
     * @param value value of the constraint
     * @param code the constraint code - needed as there may be more than one constraint on a path
     * @param valueType the type of this constraint: simple value, bag or object
     */
    public TemplateValue(String path, ConstraintOp op, Object value, ValueType valueType,
            String code) {
        this(path, op, value, valueType, code, null);
    }

    /**
     * Construct with the details of what we are constraining and the value.  The value may be a
     * user entered text, a bag name or an InterMineObject.  The extraValue is only included for
     * some constraint types.
     * @param path the path that this is constraining
     * @param op constraint operation
     * @param value value of the constraint
     * @param code the constraint code - needed as there may be more than one constraint on a path
     * @param valueType the type of this constraint: simple value, bag or object
     * @param extraValue extra value
     */
    public TemplateValue(String path, ConstraintOp op, Object value, ValueType valueType,
            String code, Object extraValue) {
        this.path = path;
        this.op = op;
        this.value = value;
        this.code = code;
        this.valueType = valueType;
        this.extraValue = extraValue;
    }

    /**
     * Return the code of this constraint in the query.
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the path being constrained in the query.
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return extra value
     */
    public Object getExtraValue() {
        return extraValue;
    }

    /**
     * @return operation
     */
    public ConstraintOp getOperation() {
        return op;
    }

    /**
     * Returns value.
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Return true if this constraint value is a bag.
     * @return true if constraint value is a bag
     */
    public boolean isBagConstraint() {
        return valueType == ValueType.BAG_VALUE;
    }

    /**
     * Return true if this constraint value is on InterMineObject
     * @return true if constraint value is an InterMineObject
     */
    public boolean isObjectConstraint() {
        return valueType == ValueType.OBJECT_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return code + " - " + path + " " + op + " " + value + " (" + extraValue + ", "
            + objectConstraint + ", " + bagConstraint + ")";
    }

}
