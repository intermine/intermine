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

import java.util.Map;

/**
 * A representation of a constraint where an AbstractValue is IN the results of a Query.
 *
 * @author Matthew Wakeling
 */
public class SubQueryConstraint extends AbstractConstraint
{
    protected AbstractValue left;
    protected Query right;

    /**
     * Constructor for SubQueryConstraint object.
     *
     * @param left the AbstractValue on the left of the constraint
     * @param right the Query containing the results. The query should only have one column in its
     * results.
     */
    public SubQueryConstraint(AbstractValue left, Query right) {
        if (left == null) {
            throw (new NullPointerException("left cannot be null"));
        }
        if (right == null) {
            throw (new NullPointerException("right cannot be null"));
        }
        if (right.getSelect().size() != 1) {
            throw (new IllegalArgumentException("right must have one result column only"));
        }
        this.left = left;
        this.right = right;
    }

    /**
     * Returns a String representation of this SubQueryConstraint object, suitable for forming part
     * of an SQL Query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        if (right.getSelect().size() != 1) {
            throw (new IllegalStateException("Right must have one result column only"));
        }
        return left.getSQLString() + " IN (" + right.getSQLString() + ")";
    }

    /**
     * Compare this SubQueryConstraint with another AbstractConstraint, ignoring aliases in member
     * fields and tables.
     *
     * @see AbstractConstraint#compare
     */
    public int compare(AbstractConstraint obj, Map tableMap, Map reverseTableMap) {
        if (obj instanceof SubQueryConstraint) {
            SubQueryConstraint objC = (SubQueryConstraint) obj;
            return (left.valueEquals(objC.left, tableMap, reverseTableMap)
                    && right.equals(objC.right) ? EQUAL : INDEPENDENT);
            // TODO: Implement this a bit better maybe? Two unequal queries may actually have the
            // same set of results. Also, a Query with less Constraints should result in a
            // SubQueryConstraint that is less restrictive. Complicated.
        } else if (obj instanceof NotConstraint) {
            NotConstraint objNC = (NotConstraint) obj;
            return alterComparisonNotObj(compare(objNC.con, tableMap, reverseTableMap));
        }
        return INDEPENDENT;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the Constraint
     */
    public int hashCode() {
        return (3 * left.hashCode()) + (5 * right.hashCode());
    }
}
