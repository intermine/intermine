package org.flymine.sql.query;

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
    public String getSQLString() {
        if (con instanceof Constraint) {
            Constraint conC = (Constraint) con;
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
            }
            return conC.left.getSQLString() + op + conC.right.getSQLString();
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
     * @see AbstractConstraint#compare
     */
    public int compare(AbstractConstraint obj) {
        return alterComparisonNotThis(con.compare(obj));
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the NotConstraint
     */
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
