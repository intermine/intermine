package org.intermine.api.template;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;
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
    private String extraValue;
    private boolean bagConstraint = false;
    private boolean objectConstraint = false;
    private ValueType valueType;
    private SwitchOffAbility switchOffAbility;

    /**
     * Possible values for TemplateValue type.
     */
    public enum ValueType { SIMPLE_VALUE, BAG_VALUE, OBJECT_VALUE };

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
        this(constraint, op, value, valueType, null, switchOffAbility);
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
        this.constraint = constraint;
        this.op = op;
        this.value = value;
        this.valueType = valueType;
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
