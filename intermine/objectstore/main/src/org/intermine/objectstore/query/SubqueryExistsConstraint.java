package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.List;

/**
 * Check if there are any rows in the subquery
 *
 * @author Tom Reilly
 * @author Matthew Wakeling
 */
public class SubqueryExistsConstraint extends Constraint
{
    protected Query subquery;  

    /**
     * Construct a SubqueryExistsConstraint
     *
     * @param op required op of constraint
     * @param query the subquery in question
     */
    public SubqueryExistsConstraint(ConstraintOp op, Query query) {
        if (query == null) {
            throw new NullPointerException("query cannot be null");
        }
  
        if (op == null) {
            throw new NullPointerException("op cannot be null");
        }

        if (!VALID_OPS.contains(op)) {
            throw new IllegalArgumentException("op cannot be " + op);
        }

        this.subquery = query;
        this.op = op;
    }

    /**
     * Get the query.
     *
     * @return the subquery of the constraint
     */
    public Query getQuery() {
        return subquery;
    }

    /**
     * Test whether two SubqueryConstraints are equal, overrides Object.equals()
     *
     * @param obj the object to compare with
     * @return true if objects are equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof SubqueryConstraint) {
            SubqueryConstraint sc = (SubqueryConstraint) obj;
            return subquery.equals(sc.subquery)
                && op == sc.op;
        }
        return false;
    }

    /**
     * Get the hashCode for this object overrides Object.hashCode()
     *
     * @return the hashCode
     */
    public int hashCode() {
        return subquery.hashCode()
            + 3 * op.hashCode();
    }

    //-------------------------------------------------------------------------
    
    protected static final List VALID_OPS = Arrays.asList(new ConstraintOp[] {ConstraintOp.EXISTS,
        ConstraintOp.DOES_NOT_EXIST});
}
