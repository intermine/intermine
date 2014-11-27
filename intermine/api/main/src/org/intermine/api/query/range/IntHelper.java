package org.intermine.api.query.range;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.query.RangeHelper;
import org.intermine.objectstore.query.Constraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Queryable;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;

import static org.intermine.metadata.ConstraintOp.AND;
import static org.intermine.metadata.ConstraintOp.OR;
import static org.intermine.metadata.ConstraintOp.WITHIN;
import static org.intermine.metadata.ConstraintOp.OUTSIDE;
import static org.intermine.metadata.ConstraintOp.OVERLAPS;
import static org.intermine.metadata.ConstraintOp.DOES_NOT_OVERLAP;

/**
 * @author Alex
 */
public class IntHelper implements RangeHelper
{

    private static final ConstraintOp GTE = ConstraintOp.GREATER_THAN_EQUALS;
    private static final ConstraintOp LTE = ConstraintOp.LESS_THAN_EQUALS;

    @Override
    public Constraint createConstraint(Queryable q, QueryNode node, PathConstraintRange con) {
        QueryField qf = (QueryField) node;
        ConstraintOp mainSetOp = OR;
        ConstraintOp rangeSetOp = AND;
        ConstraintOp leftOp = GTE;
        ConstraintOp rightOp = LTE;
        ConstraintOp op = con.getOp();

        if (!(op == WITHIN || op == OVERLAPS || op == OUTSIDE || op == DOES_NOT_OVERLAP)) {
            throw new RuntimeException("Unimplemented behaviour: " + op);
        }

        ConstraintSet mainSet = new ConstraintSet(mainSetOp);
        for (String range: con.getValues()) {
            ConstraintSet conSet = new ConstraintSet(rangeSetOp);
            IntRange ir = new IntRange(range);
            conSet.addConstraint(new SimpleConstraint(qf, leftOp, new QueryValue(ir.start)));
            conSet.addConstraint(new SimpleConstraint(qf, rightOp, new QueryValue(ir.end)));
            mainSet.addConstraint(conSet);
        }
        if (op == OUTSIDE || op == DOES_NOT_OVERLAP) {
            mainSet.negate();
        }
        return mainSet;
    }

    private class IntRange
    {
        final Integer start;
        final Integer end;

        IntRange(String range) {
            if (range == null) {
                throw new NullPointerException("range may not be null");
            }
            String[] parts = range.split("\\.\\.");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Illegal range (" + range
                        + "). Must be in the format 'x .. y'");
            }
            this.start = Integer.valueOf(parts[0].trim());
            this.end = Integer.valueOf(parts[1].trim());
        }
    }

}

