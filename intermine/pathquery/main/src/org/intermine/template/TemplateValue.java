package org.intermine.template;

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

import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.PathConstraint;


/**
 * A TemplateValue represents the value for an editable constraint of a template query and details
 * about what is being constrained.
 *
 * @author Richard Smith
 **/
public class TemplateValue
{
    private ConstraintOp op;
    private PathConstraint constraint;
    private String value;
    private List<String> values;
    private String extraValue;
    private boolean bagConstraint = false;
    private boolean objectConstraint = false;
    private ValueType valueType;
    private SwitchOffAbility switchOffAbility;

    /**
     * Possible values for TemplateValue type.
     */
    public enum ValueType {
        /**
         * a simple value, like a string
         */
        SIMPLE_VALUE,
        /**
         * e.g. a list
         */
        BAG_VALUE,
        /**
         * e.g. an object
         */
        OBJECT_VALUE
    }

    /**
     * Constructor.Construct with details of what we are constraining, but without the value.
     *
     * @param constraint The constraint this value refers to.
     * @param op The operation this constraint should have.
     * @param valueType One of SIMPLE_VALUE, BAG_VALUE, or OBJECT_VALUE.
     * @param switchOffAbility One of LOCKED, ON, OFF.
     */
    public TemplateValue(PathConstraint constraint, ConstraintOp op,
            ValueType valueType, SwitchOffAbility switchOffAbility) {
        this(constraint, op, null, valueType, null, null, switchOffAbility);
    }

    /**
     * Construct with the details of what we are constraining and the value.  The value may be a
     * user entered text, a bag name or an InterMineObject.  The extraValue is only included for
     * some constraint types.
     *
     * @param constraint the constraint
     * @param op constraint operation
     * @param value value of the constraint
     * @param valueType the type of this constraint: simple value, bag or object
     * @param switchOffAbility the required/optional status of the constraint
     */
    public TemplateValue(PathConstraint constraint, ConstraintOp op, String value,
            ValueType valueType, SwitchOffAbility switchOffAbility) {
        this(constraint, op, value, valueType, null, null, switchOffAbility);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }

    /**
     * Construct with the details of what we are constraining and the value.  The value may be a
     * user entered text, a bag name or an InterMineObject.  The extraValue is only included for
     * some constraint types.
     *
     * @param constraint the constraint
     * @param op constraint operation
     * @param value value of the constraint
     * @param valueType the type of this constraint: simple value, bag or object
     * @param extraValue extra value
     * @param switchOffAbility the required/optional status of the constraint
     */
    public TemplateValue(PathConstraint constraint, ConstraintOp op, String value,
            ValueType valueType, String extraValue, SwitchOffAbility switchOffAbility) {
        this(constraint, op, value, valueType, extraValue, null, switchOffAbility);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }

    /**
     * Construct with the details of what we are constraining and values.  The value may be a
     * user entered text, a bag name or an InterMineObject.  The extraValue is only included for
     * some constraint types.
     *
     * @param constraint the constraint
     * @param op constraint operation
     * @param values Multiple values
     * @param valueType the type of this constraint: simple value, bag or object
     * @param switchOffAbility the required/optional status of the constraint
     */
    public TemplateValue(PathConstraint constraint, ConstraintOp op,
            ValueType valueType, List<String> values, SwitchOffAbility switchOffAbility) {
        this(constraint, op, null, valueType, null, values, switchOffAbility);
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
    }

    /**
     * Private contructor called by all other constructors. This is private as it
     * does not make sense to provide values for all properties.
     *
     * @param constraint the constraint
     * @param op constraint operation
     * @param value value of the constraint
     * @param valueType the type of this constraint: simple value, bag or object
     * @param extraValue extra value
     * @param values The multi-values
     * @param switchOffAbility the required/optional status of the constraint
     */
    private TemplateValue(PathConstraint constraint, ConstraintOp op, String value,
            ValueType valueType, String extraValue,
            List<String> values, SwitchOffAbility switchOffAbility) {
        if (value != null && values != null) {
            throw new IllegalArgumentException("Cannot have both value and values");
        }
        this.constraint = constraint;
        this.op = op;
        this.value = value;
        this.valueType = valueType;
        this.values = values;
        this.extraValue = extraValue;
        this.switchOffAbility = switchOffAbility;
    }

    /**
     * @return extra value
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * @return Whether this represents a new multi-value template parameter
     */
    public boolean isMultipleValue() {
        return (values != null);
    }

    /**
     * @return multiple values
     */
    public List<String> getValues() {
        return values;
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
    public String getValue() {
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
     * Returns the constraint that this object was constructed with.
     *
     * @return a PathConstraint object
     */
    public PathConstraint getConstraint() {
        return this.constraint;
    }

    /**
     * Returns the SwitchOffAbility.
     *
     * @return the SwitchOffAbility
     */
    public SwitchOffAbility getSwitchOffAbility() {
        return switchOffAbility;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return constraint.getPath() + " " + op + " " + value + " (" + extraValue + ", "
            + objectConstraint + ", " + bagConstraint + ") - " + switchOffAbility;
    }

}
