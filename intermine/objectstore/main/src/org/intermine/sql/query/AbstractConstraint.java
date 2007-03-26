package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.IdentityMap;
import java.util.Map;

/**
 * An abstract representation of an item that can be present in the WHERE or HAVING
 * sections of an SQL query.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public abstract class AbstractConstraint implements SQLStringable
{
    /**
     * Describes two constraints as being independent.
     * That is, knowing the result of one constraint tells one nothing about the result of another
     * constraint.
     * <br>For example, "a &gt; 5" and "b &lt; 4".
     */
    public static final int INDEPENDENT = 15;
    /**
     * Describes one constraint as being implied by another constraint.
     * That is, if the second constraint is true, then the first constraint must also be true.
     * This can also be thought of as the first constraint being less restrictive than the second.
     * <br>For example, "a &gt; 5" and "a = 8".
     */
    public static final int IMPLIED_BY = 13;
    /**
     * Describes one constraint as implying another constraint.
     * That is, if the first constraint is true, then the second constraint must also be true.
     * This can also be thought of as the first constraint being more restrictive than the second.
     * <br>For example, "a &gt; 5" and "a &gt; 2".
     */
    public static final int IMPLIES = 11;
    /**
     * Describes one constraint as being equal to another constraint.
     * That is, the result of the first constraint is always the same as the result of the second
     * constraint.
     */
    public static final int EQUAL = 9;
    /**
     * Describes two constraints as being opposite to each other.
     * That is, the result of the first constraint is always the opposite of the result of the
     * second constraint.
     * <br>For example, "a &gt; 5" and "a &le; 5".
     */
    public static final int OPPOSITE = 6;
    /**
     * Describes two constraints as being mutually exclusive.
     * That is, the results of both constraints cannot be true at the same time.
     * <br>For example, "a &gt; 5" and "a &lt; 5".
     */
    public static final int EXCLUDES = 7;
    /**
     * Describes two constraints as following "constraint 1 OR constraint 2 IS TRUE".
     * That is, at least one of the results of the two constraints must be true.
     * <br>For example, "a &gt; 5" and "a &lt; 8".
     */
    public static final int OR = 14;
    /**
     * Describes two constraints as being both true.
     * This is a subset of EQUAL.
     */
    public static final int BOTH_TRUE = 8;
    /**
     * Describes two constraints as being both false.
     * This is a subset of EQUAL.
     */
    public static final int BOTH_FALSE = 1;
    /**
     * Describes two constraints, where the left is always true, and the right is always false.
     * This is the opposite of IMPLIES.
     */
    public static final int LEFT_TRUE_RIGHT_FALSE = 4;
    /**
     * Describes two constraints, where the left is always false, and the right is always true.
     * This is the opposite of IMPLIED_BY.
     */
    public static final int LEFT_FALSE_RIGHT_TRUE = 2;
    /**
     * Describes two constraints, where the left is always true.
     */
    public static final int LEFT_TRUE = 12;
    /**
     * Describes two constraints, where the left is always false.
     */
    public static final int LEFT_FALSE = 3;
    /**
     * Describes two constraints, where the right is always true.
     */
    public static final int RIGHT_TRUE = 10;
    /**
     * Describes two constraints, where the right is always false.
     */
    public static final int RIGHT_FALSE = 5;
    
    /**
     * Returns a String representation of this AbstractConstraint object, suitable for forming
     * part of an SQL query.
     *
     * @return the String representation
     */
    public abstract String getSQLString();

    /**
     * Compare this AbstractConstraint with another, ignoring aliases in member fields and tables.
     *
     * @param obj an AbstractConstraint to compare to
     * @return INDEPENDENT, IMPLIED_BY, IMPLIES, EQUAL, OPPOSITE, EXCLUDES, or OR, depending on the
     * constraints.
     */
    public int compare(AbstractConstraint obj) {
        return compare(obj, IdentityMap.INSTANCE, IdentityMap.INSTANCE);
    }

    /**
     * Compare this AbstractConstraint with another, ignoring aliases in member fields and tables.
     *
     * @param obj an AbstractConstraint to compare to
     * @param tableMap a Map from tables in this constraint to tables in obj
     * @param reverseTableMap a reverse of tableMap
     * @return INDEPENDENT, IMPLIED_BY, IMPLIES, EQUAL, OPPOSITE, EXCLUDES, or OR, depending on the
     * constraints.
     */
    public abstract int compare(AbstractConstraint obj, Map tableMap, Map reverseTableMap);

    /**
     * Overrides Object.equals();
     *
     * @param obj the Object to compare to
     * @return true if obj is the same
     */
    public boolean equals(Object obj) {
        if (obj instanceof AbstractConstraint) {
            AbstractConstraint objC = (AbstractConstraint) obj;
            int compareVal = compare(objC);
            return checkComparisonEquals(compareVal);
        }
        return false;
    }

    /**
     * Overrides Object.hashcode();
     *
     * @return an arbitrary integer based on the contents of the AbstractConstraint
     */
    public abstract int hashCode();
    
    /**
     * Take an integer as if it is a value returned by compare, and return the value that compare
     * would have returned if first constraint (this) had been NOTed.
     * For example, if one passes in the value EQUAL, this method will return OPPOSITE.
     *
     * @param comp a previous comparison value returned by compare
     * @return an alternative comparison value, for if this had been NOTed
     */
    protected static int alterComparisonNotThis(int comp) {
        boolean nThisNObj = ((comp & 1) == 1);
        boolean nThisObj = ((comp & 2) == 2);
        boolean thisNObj = ((comp & 4) == 4);
        boolean thisObj = ((comp & 8) == 8);

        return (nThisNObj ? 4 : 0) + (nThisObj ? 8 : 0) + (thisNObj ? 1 : 0) + (thisObj ? 2 : 0);
    }

    /**
     * Take an integer as if it is a value returned by compare, and return the value that compare
     * would have returned if second constraint (obj) had been NOTed.
     * For example, if one passes in the value IMPLIES, this method will return EXCLUSIVE (as it
     * would now mean that the first implies not the second).
     *
     * @param comp a previous comparison value returned by compare
     * @return an alternative comparison value, for if this had been NOTed
     */
    protected static int alterComparisonNotObj(int comp) {
        boolean nThisNObj = ((comp & 1) == 1);
        boolean nThisObj = ((comp & 2) == 2);
        boolean thisNObj = ((comp & 4) == 4);
        boolean thisObj = ((comp & 8) == 8);

        return (nThisNObj ? 2 : 0) + (nThisObj ? 1 : 0) + (thisNObj ? 8 : 0) + (thisObj ? 4 : 0);
    }

    /**
     * Take an integer as if it is a value returned by compare, and return the value that compare
     * would have returned if this and obj had been switched.
     * For example, IMPLIES gets translated to IMPLIED_BY.
     *
     * @param comp a previous comparison value returned by compare
     * @return an alternative comparison value, for if this and obj had been switched
     */
    protected static int alterComparisonSwitch(int comp) {
        boolean nThisNObj = ((comp & 1) == 1);
        boolean nThisObj = ((comp & 2) == 2);
        boolean thisNObj = ((comp & 4) == 4);
        boolean thisObj = ((comp & 8) == 8);

        return (nThisNObj ? 1 : 0) + (nThisObj ? 4 : 0) + (thisNObj ? 2 : 0) + (thisObj ? 8 : 0);
    }

    /**
     * Take a couple of integers as if they are values returned by compare, for this compared to A,
     * and this compared to B, and return the value that compare should return for this compared
     * to (A OR B).
     *
     * @param compA a previous comparison returned by compare for this.compare(A)
     * @param compB a previous comparison returned by compare for this.compare(B)
     * @return an alternative comparison, for this.compare(A OR B)
     */
    protected static int alterComparisonAORB(int compA, int compB) {
        boolean nThisNA = ((compA & 1) == 1);
        boolean nThisA = ((compA & 2) == 2);
        boolean thisNA = ((compA & 4) == 4);
        boolean thisA = ((compA & 8) == 8);

        boolean nThisNB = ((compB & 1) == 1);
        boolean nThisB = ((compB & 2) == 2);
        boolean thisNB = ((compB & 4) == 4);
        boolean thisB = ((compB & 8) == 8);

        return (nThisNA && nThisNB ? 1 : 0) + (nThisA || nThisB ? 2 : 0) 
            + (thisNA && thisNB ? 4 : 0) + (thisA || thisB ? 8 : 0);
    }

    /**
     * Take a couple of integers as if they are values returned by compare, and assume that they
     * are two loose (and possibly different) descriptions of the relation between the same two
     * AbstractConstraints, and return a value that is a more strict description.
     * For example, passing this method IMPLIES and IMPLIED_BY will return EQUAL.
     *
     * @param compA a previous comparison returned by compare.
     * @param compB another previous comparison returned by compare for the same set of
     *      AbstractConstraints.
     * @return an alternative comparison which is the strictest comparison that can be infered from
     *      from the two input comparisons.
     */
    protected static int alterComparisonAnd(int compA, int compB) {
        boolean nThisNA = ((compA & 1) == 1);
        boolean nThisA = ((compA & 2) == 2);
        boolean thisNA = ((compA & 4) == 4);
        boolean thisA = ((compA & 8) == 8);

        boolean nThisNB = ((compB & 1) == 1);
        boolean nThisB = ((compB & 2) == 2);
        boolean thisNB = ((compB & 4) == 4);
        boolean thisB = ((compB & 8) == 8);

        return (nThisNA && nThisNB ? 1 : 0) + (nThisA && nThisB ? 2 : 0) 
            + (thisNA && thisNB ? 4 : 0) + (thisA && thisB ? 8 : 0);
    }

    /**
     * Take an integer as if it was created by compare, and return true if this implies that the
     * left constraint IMPLIES the right constraint, in a loose sense.
     * For example, a EQUALS b implies that a IMPLIES b.
     *
     * @param comparison the comparison to test
     * @return true if this is a subset of IMPLIES
     */
    public static boolean checkComparisonImplies(int comparison) {
        return ((comparison & LEFT_TRUE_RIGHT_FALSE) == 0);
    }

    /**
     * Take an integer as if it was created by compare, and return true if this implies that the
     * left constraint is EQUAL to the right constraint, in a loose sense.
     * For example the BOTH_TRUE comparison implies a EQUALS comparison.
     *
     * @param comparison the comparison to test
     * @return true if this is a subset of EQUAL
     */
    public static boolean checkComparisonEquals(int comparison) {
        return ((comparison == EQUAL) || (comparison == BOTH_TRUE) || (comparison == BOTH_FALSE));
    }
}
