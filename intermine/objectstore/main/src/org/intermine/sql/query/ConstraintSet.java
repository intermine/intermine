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

import java.util.*;

import org.intermine.util.ConsistentSet;

/**
 * A representation of a set of constraints ORed together.
 *
 * @author Matthew Wakeling
 */
public class ConstraintSet extends AbstractConstraint
{
    // TODO: Should this be a list or a set?
    protected Set cons;

    /**
     * Constructor for a ConstraintSet object.
     * Add AbstractConstraint objects to this object with the add method.
     */
    public ConstraintSet() {
        cons = new ConsistentSet();
    }

    /**
     * Add an AbstractConstraint to this ConstraintSet.
     *
     * @param obj an AbstractConstraint to add to this ConstraintSet
     * @throws IllegalArgumentException if obj contains in any way a ConstraintSet. (This is to
     *          protect us from having anything but a simple Conjunctive Normal Form expression.)
     */
    public void add(AbstractConstraint obj) {
        if (obj instanceof ConstraintSet) {
            throw (new IllegalArgumentException("A ConstraintSet cannot contain a ConstraintSet."));
        }
        if (obj instanceof NotConstraint) {
            NotConstraint objNC = (NotConstraint) obj;
            if (objNC.con instanceof ConstraintSet) {
                throw (new IllegalArgumentException("A ConstraintSet cannot contain a "
                            + "ConstraintSet, even inside a NotConstraint."));
            }
        }
        cons.add(obj);
    }

    /**
     * Returns a String representation of this ConstraintSet object, suitable for forming part of
     * an SQL query.
     *
     * @return the String representation
     */
    public String getSQLString() {
        boolean needOR = false;
        boolean needParentheses = false;
        String retval = "";
        Iterator consIter = cons.iterator();
        while (consIter.hasNext()) {
            AbstractConstraint con = (AbstractConstraint) consIter.next();
            if (needOR) {
                retval += " OR ";
                needParentheses = true;
            }
            needOR = true;
            retval = retval + con.getSQLString();
        }
        return (needOR ? (needParentheses ? "(" + retval + ")" : retval) : "FALSE");
    }

    /**
     * Compare this ConstraintSet with another AbstractConstraint, ignoring aliases in member
     * fields and tables.
     *
     * {@inheritDoc}
     */
    public int compare(AbstractConstraint obj, Map tableMap, Map reverseTableMap) {
        if (obj instanceof ConstraintSet) {
            return alterComparisonAnd(internalCompare(obj, tableMap, reverseTableMap),
                    alterComparisonSwitch(((ConstraintSet) obj).internalCompare(this,
                            reverseTableMap, tableMap)));
        } else if (obj instanceof NotConstraint) {
            NotConstraint objNC = (NotConstraint) obj;
            return alterComparisonNotObj(compare(objNC.con, tableMap, reverseTableMap));
        }
        return internalCompare(obj, tableMap, reverseTableMap);
    }

    /**
     * Compare this ConstraintSet with another AbstractConstraint, ignoring aliases in member
     * fields and tables. If obj is a ConstraintSet, then the comparison returned is loose -
     * that is, it accepts the possibility of an occurance that cannot actually happen. This is
     * a safe operation, but to get a correct result, one should combine A.internalCompare(B) with
     * B.internalCompare(A), using the AbstractConstraint.alterComparisonAnd method. Even then,
     * we have not written a proof that this method returns a completely strict comparison.
     *
     * <br>Also, note that it is possible that this method will return values other than the seven
     * recognised comparisons, if a ConstraintSet contains constraints that EXCLUDE each other or
     * compare to each other with the OR comparison. This is alright - the value that the method
     * has returned is merely more strict than the seven "useful" values are, and reflects the fact
     * that one of the ConstraintSets is always true or false. We don't think we will be
     * constructing any such ConstraintSets, so it ought to be alright.
     *
     * @param obj an AbstractConstraint to compare to
     * @param tableMap a Map from tables in this constraint to tables in obj
     * @param reverseTableMap a reverse of tableMap
     * @return INDEPENDENT, IMPLIED_BY, IMPLIES, EQUAL, OPPOSITE, EXCLUDES, or OR, depending on the
     * constraints.
     */
    protected int internalCompare(AbstractConstraint obj, Map tableMap, Map reverseTableMap) {
        int currentComp = 3; // This is a "A is false, but we don't know about B" comparison.
                             // We use it because it is the identity, with the
                             // AbstractConstraint.alterComparisonAORB operator. It happens to be
                             // a subset of "A IMPLIES B".
        Iterator consIter = cons.iterator();
        while (consIter.hasNext()) {
            AbstractConstraint con = (AbstractConstraint) consIter.next();
            currentComp = alterComparisonSwitch(alterComparisonAORB(
                        alterComparisonSwitch(currentComp),
                        alterComparisonSwitch(con.compare(obj, tableMap, reverseTableMap))));
        }
        return currentComp;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the Constraint
     */
    public int hashCode() {
        int retval = 0;
        Iterator consIter = cons.iterator();
        while (consIter.hasNext()) {
            AbstractConstraint con = (AbstractConstraint) consIter.next();
            retval += con.hashCode();
        }
        return retval;
    }

    /**
     * Returns the Set of constraints forming this object.
     *
     * @return the Set of Constraints
     */
    public Set getConstraints() {
        return cons;
    }
}
