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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A representation of a constraint where an AbstractValue is IN a list of constants.
 *
 * @author Matthew Wakeling
 */
public class InListConstraint extends AbstractConstraint
{
    protected AbstractValue left;
    protected Set<Constant> right;

    /**
     * Constructor for InListConstraint object.
     *
     * @param left the AbstractValue on the left of the constraint
     */
    public InListConstraint(AbstractValue left) {
        if (left == null) {
            throw (new NullPointerException("left cannot be null"));
        }
        this.left = left;
        this.right = new HashSet<Constant>();
    }

    /**
     * Add constants to the list.
     *
     * @param c a Constant
     */
    public void add(Constant c) {
        right.add(c);
    }

    /**
     * Adds a Collection of Constants to the list.
     *
     * @param c a Collection of Constants
     */
    public void addAll(Collection<? extends Constant> c) {
        for (Constant con : c) {
            right.add(con);
        }
    }

    /**
     * Returns the left AbstractValue.
     *
     * @return an AbstractValue
     */
    public AbstractValue getLeft() {
        return left;
    }

    /**
     * Returns the right Set of Constants.
     *
     * @return a Set
     */
    public Set<Constant> getRight() {
        return Collections.unmodifiableSet(right);
    }

    /**
     * Returns a String representation of this InListConstraint object, suitable for forming part
     * of an SQL Query.
     *
     * @return the String representation
     */
    @Override
    public String getSQLString() {
        StringBuffer retval = new StringBuffer(left.getSQLString()).append(" IN (");
        boolean needComma = false;
        for (Constant con : right) {
            if (needComma) {
                retval.append(", ");
            }
            needComma = true;
            retval.append(con.getSQLString());
        }
        retval.append(")");
        return retval.toString();
    }

    /**
     * Compare this InListConstraint with another AbstractConstraint, ignoring aliases in member
     * fields and tables.
     *
     * {@inheritDoc}
     */
    @Override
    public int compare(AbstractConstraint obj, Map<AbstractTable, AbstractTable> tableMap,
            Map<AbstractTable, AbstractTable> reverseTableMap) {
        if (obj instanceof InListConstraint) {
            InListConstraint objC = (InListConstraint) obj;
            return (left.valueEquals(objC.left, tableMap, reverseTableMap)
                    && right.equals(objC.right) ? EQUAL : INDEPENDENT);
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
    @Override
    public int hashCode() {
        return (3 * left.hashCode()) + (5 * right.hashCode());
    }
}
