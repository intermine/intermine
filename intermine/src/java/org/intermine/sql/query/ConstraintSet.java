package org.flymine.sql.query;

import java.util.*;

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
        cons = new HashSet();
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
     * @see AbstractConstraint#compare
     */
    public int compare(AbstractConstraint obj) {
        return INDEPENDENT;
    }
}
