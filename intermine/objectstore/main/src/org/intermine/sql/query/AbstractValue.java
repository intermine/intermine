package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * An abstract representation of an item that can be present in the SELECT, GROUP BY,
 * or ORDER BY sections of an SQL query, as well as in a constraint.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class AbstractValue implements SQLStringable
{
    /**
     * Describes two AbstractValues as being equal.
     */
    public static final int EQUAL = 0;
    /**
     * Describes one AbstractValue as being less than another AbstractValue.
     */
    public static final int LESS = 1;
    /**
     * Describes one AbstractValue as being greater than another AbstractValue.
     */
    public static final int GREATER = 2;
    /**
     * Describes one AbstractValue as being definitely not equal to another AbstractValue.
     */
    public static final int NOT_EQUAL = 3;
    /**
     * Describes one AbstractValue as being incomparable to another AbstractValue. In other words,
     * we can't tell if they are equal or not.
     */
    public static final int INCOMPARABLE = 4;

    /**
     * Returns a String representation of this AbstractValue object, suitable for forming
     * part of an SQL query.
     *
     * @return the String representation
     */
    public abstract String getSQLString();

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if obj is equal
     */
    public abstract boolean equals(Object obj);

    /**
     * Overrides Object.hashcode().
     *
     * @return an arbitrary integer based on the contents of the object
     */
    public abstract int hashCode();
 
    /**
     * Compare the value of this AbstractValue with another.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return EQUAL, LESS, GREATER, NOT_EQUAL, or INCOMPARABLE
     */
    public abstract int compare(AbstractValue obj, Map tableMap, Map reverseTableMap);

    /**
     * Compare the value of this AbstractValue with another to see if it is less. This only really
     * makes sense with Constants. It uses the compare method of AbstractValue. Note that the
     * result being false does not imply greater than or equal - it may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return true if this is less than obj
     */
    public boolean lessThan(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        return (compare(obj, tableMap, reverseTableMap) == LESS);
    }

    /**
     * Compare this value of this AbstractValue with another to see if it is more. This only really
     * makes sense with Constants. It uses the compare method of AbstractValue. Note that the
     * result being false does not imply less than or equal - it may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return true if this is more than obj
     */
    public boolean greaterThan(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        return (compare(obj, tableMap, reverseTableMap) == GREATER);
    }

    /**
     * Compare this value of this AbstractValue with another to see if it is not equal. It uses the
     * compare method of AbstractValue. Note that the result being false does not imply EQUAL - it
     * may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return true if this is definitely not equal to obj
     */
    public boolean notEqualTo(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        int compareVal = compare(obj, tableMap, reverseTableMap);
        return (compareVal == NOT_EQUAL) || (compareVal == LESS) || (compareVal == GREATER);
    }

    /**
     * Compare this value of this AbstractValue with another to see if it is more or equal.
     * This only really
     * makes sense with Constants. It uses the compare method of AbstractValue. Note that the
     * result being false does not imply less than - it may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return true if this is more than obj
     */
    public boolean greaterOrEqual(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        int compareVal = compare(obj, tableMap, reverseTableMap);
        return (compareVal == GREATER) || (compareVal == EQUAL);
    }

    /**
     * Compare the value of this AbstractValue with another to see if it is less or equal. This
     * only really
     * makes sense with Constants. It uses the compare method of AbstractValue. Note that the
     * result being false does not imply greater than - it may be incomparable.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return true if this is less than obj
     */
    public boolean lessOrEqual(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        int compareVal = compare(obj, tableMap, reverseTableMap);
        return (compareVal == LESS) || (compareVal == EQUAL);
    }

    /**
     * Compare the value of this AbstractValue with another to see if it is equal. It uses the
     * compare method of AbstractValue.
     *
     * @param obj an AbstractValue to compare to
     * @param tableMap a mapping between tablenames of the two elements
     * @param reverseTableMap a reverse of tableMap
     * @return true if this is equal to obj
     */
    public boolean valueEquals(AbstractValue obj, Map tableMap, Map reverseTableMap) {
        int compareVal = compare(obj, tableMap, reverseTableMap);
        return (compareVal == EQUAL);
    }

    /**
     * Returns true if this value is an aggregate function.
     *
     * @return a boolean
     */
    public abstract boolean isAggregate();
}
