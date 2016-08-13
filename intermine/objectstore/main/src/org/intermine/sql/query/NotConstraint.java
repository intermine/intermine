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

import java.util.Map;

/**
 * A representation of a constraint that is represented here as the inverse of another Constraint.
 *
 * @author Matthew Wakeling
 */
public class NotConstraint extends AbstractConstraint
{
    protected AbstractConstraint con;

    /**
     * Constructor for a NotConstraint object.
     *
     * @param con the AbstractConstraint that this NotConstraint is to be the inverse of
     */
    public NotConstraint(AbstractConstraint con) {
        this.con = con;
    }

    /**
     * Returns a String representation of this Constraint object, suitable for forming part of an
     * SQL query.
     *
     * @return the String representation
     */
    @Override
    public String getSQLString() {
        if (con instanceof Constraint) {
            Constraint conC = (Constraint) con;
            if ("null".equals(conC.right.getSQLString())) {
                return conC.getLeft().getSQLString() + " IS NOT NULL";
            } else {
                String op = null;
                switch (conC.operation) {
                    case Constraint.EQ:
                        op = " != ";
                        break;
                    case Constraint.LT:
                        op = " >= ";
                        break;
                    case Constraint.LIKE:
                        op = " NOT LIKE ";
                        break;
                    default:
                        throw new Error("Unrecognised operation " + conC.operation);
                }
                return conC.left.getSQLString() + op + conC.right.getSQLString();
            }
        } else if (con instanceof SubQueryConstraint) {
            SubQueryConstraint conC = (SubQueryConstraint) con;
            if (conC.right.getSelect().size() != 1) {
                throw (new IllegalStateException("Right must have one result column only"));
            }
            return conC.left.getSQLString() + " NOT IN (" + conC.right.getSQLString() + ")";
        }
        return "NOT (" + con.getSQLString() + ")";
    }

    /**
     * Compare this NotConstraint with another AbstractConstraint, ignoring aliases in member
     * fields and tables.
     *
     * {@inheritDoc}
     */
    @Override
    public int compare(AbstractConstraint obj, Map<AbstractTable, AbstractTable> tableMap,
            Map<AbstractTable, AbstractTable> reverseTableMap) {
        return alterComparisonNotThis(con.compare(obj, tableMap, reverseTableMap));
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the NotConstraint
     */
    @Override
    public int hashCode() {
        return -con.hashCode();
    }

    /**
     * Returns the contained constraint.
     *
     * @return the contained constraint
     */
    public AbstractConstraint getConstraint() {
        return con;
    }
}
