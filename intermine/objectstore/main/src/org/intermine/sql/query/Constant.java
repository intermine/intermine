package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.Map;

/**
 * A representation of a constant value in an SQL query.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class Constant extends AbstractValue
{
    protected String value;

    /**
     * Constructor for this Constant object.
     *
     * @param value the constant, as referenced in the SQL query, including any ' characters
     * surrounding it.
     */
    public Constant(String value) {
        if (value == null) {
            throw (new NullPointerException("Constants cannot have a null value"));
        }
        this.value = value;
    }

    /**
     * Returns a String representation of this Constant object, suitable for forming part of an
     * SQL query.
     *
     * @return the String representation
     */
    @Override
    public String getSQLString() {
        return value;
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is of the same class, and with the same value
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Constant) {
            Constant objConstant = (Constant) obj;
            return compare(objConstant, null, null) == EQUAL;
        }
        return false;
    }

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the value of the Constant
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Compare this Constant to another AbstractValue.
     * This method is capable of spotting some situations when one Constant is strictly less or
     * greater than another.
     *
     * {@inheritDoc}
     */
    @Override
    public int compare(AbstractValue obj,
            @SuppressWarnings("unused") Map<AbstractTable, AbstractTable> tableMap,
            @SuppressWarnings("unused") Map<AbstractTable, AbstractTable> reverseTableMap) {
        if (obj instanceof Constant) {
            Constant objC = (Constant) obj;
            if (value.equals(objC.value)) {
                return EQUAL;
            }
            boolean thisIsString = ((value.toUpperCase().charAt(0) == 'E')
                    && (value.charAt(1) == '\'')
                    && (value.charAt(value.length() - 1) == '\''));
            boolean objIsString = ((objC.value.toUpperCase().charAt(0) == 'E')
                    && (objC.value.charAt(1) == '\'')
                    && (objC.value.charAt(objC.value.length() - 1) == '\''));
            if (thisIsString && objIsString) {
                // Both this and obj are string constants.
                return (value.compareTo(objC.value) < 0 ? LESS : GREATER);
            }
            thisIsString = ((value.charAt(0) == '\'')
                    && (value.charAt(value.length() - 1) == '\''));
            objIsString = ((objC.value.charAt(0) == '\'')
                    && (objC.value.charAt(objC.value.length() - 1) == '\''));
            if (thisIsString && objIsString) {
                // Both this and obj are string constants.
                return (value.compareTo(objC.value) < 0 ? LESS : GREATER);
            }
            boolean thisIsNumber = false;
            BigDecimal thisNumber = null;
            boolean objIsNumber = false;
            BigDecimal objNumber = null;
            try {
                if (value.toUpperCase().endsWith("::REAL")) {
                    thisNumber = new BigDecimal(Float.valueOf(value.substring(0, value.length()
                                    - 6)).doubleValue());
                } else {
                    thisNumber = new BigDecimal(value);
                }
                thisIsNumber = true;
            } catch (NumberFormatException e) {
                // Okay
            }
            try {
                if (objC.value.toUpperCase().endsWith("::REAL")) {
                    objNumber = new BigDecimal(Float.valueOf(objC.value.substring(0,
                                    objC.value.length() - 6)).doubleValue());
                } else {
                    objNumber = new BigDecimal(objC.value);
                }
                objIsNumber = true;
            } catch (NumberFormatException e) {
                // Okay
            }

            if ((thisIsNumber && objIsString) || (objIsNumber && thisIsString)) {
                return NOT_EQUAL;
            }
            if (thisIsNumber && objIsNumber) {
                int comparison = thisNumber.compareTo(objNumber);
                if (comparison == 0) {
                    return EQUAL;
                } else if (comparison > 0) {
                    return GREATER;
                } else {
                    return LESS;
                }
            }
        }
        return INCOMPARABLE;
    }

    /**
     * Returns true if this value is an aggregate function.
     *
     * @return a boolean
     */
    @Override
    public boolean isAggregate() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value;
    }
}
