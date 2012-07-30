package org.intermine.pathquery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;

public class PathConstraintRange extends PathConstraint
{
    /** List of valid ops for this type of constraint */
    public static final Set<ConstraintOp> VALID_OPS = new HashSet<ConstraintOp>(Arrays.asList(
                ConstraintOp.WITHIN, ConstraintOp.OUTSIDE,
                ConstraintOp.OVERLAPS, ConstraintOp.DOES_NOT_OVERLAP,
                ConstraintOp.CONTAINS, ConstraintOp.DOES_NOT_CONTAIN));
    private String value;

    /**
     * Constructs a new PathConstraintRange. The path should be a normal path expression
     * with dots separating the parts. Do not use colons to represent outer joins, and do not
     * use square brackets to represent subclass constraints. The path will be checked for
     * format, but can only be verified once inside a PathQuery object by the
     * PathQuery.verifyQuery() method. This object is used to form a constraint on an attribute.
     *
     * @param path the path that the constraint is attached to
     * @param op the type of operation
     * @param range the value to constrain to, as a String - it will be validated later when the
     * query is validated, as we do not know the attribute type at this stage.
     * @throws NullPointerException if path, op, or value are null
     * @throws IllegalArgumentException if the path contains colons or square brackets, or is
     * otherwise in a bad format, or if the op is invalid for this constraint type
     */
    public PathConstraintRange(String path, ConstraintOp op, String range) {
        super(path, op);
        if (op == null) {
            throw new NullPointerException("Cannot construct a PathConstraintRange with a null"
                    + " op");
        }
        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("PathConstraints on attributes must use an op in"
                    + " the list \"" + VALID_OPS + "\"");
        }
        if (range == null) {
            throw new NullPointerException("Cannot create a constraint on a null value");
        }
        this.value = range;
    }

    /**
     * Returns the value that the constraint is constraining to.
     *
     * @return a String
     */
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return path + " " + op + " " + value;
    }

}
