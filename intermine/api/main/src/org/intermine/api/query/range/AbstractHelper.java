package org.intermine.api.query.range;

import org.intermine.api.query.RangeHelper;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;

/**
 * A class that can be extended to easily create RangeHelpers for a particular type.
 *
 * To create a valid range helper any extending class need only provide a definition
 * for what it means to be with a set of ranges, and a mechanism for parsing individual
 * ranges.
 *
 * @author Alex Kalderimis
 *
 */
public abstract class AbstractHelper implements RangeHelper {

    /**
     * Extending classes may with to override this method if they need to adjust the
     * main constraint set in some way, such as adding an extra constraint.
     */
    @Override
    public Constraint createConstraint(QueryNode node, PathConstraintRange con) {
        ConstraintOptions options;
        
        if (con.getOp() == ConstraintOp.WITHIN) {
            options = getWithinOptions();
        } else if (con.getOp() == ConstraintOp.OUTSIDE) {
            options = getOutsideOptions();
        } else if (con.getOp() == ConstraintOp.CONTAINS) {
            options = getContainsOptions();
        } else if (con.getOp() == ConstraintOp.DOES_NOT_CONTAIN) {
            options = getDoesntContainOptions();
        } else if (con.getOp() == ConstraintOp.OVERLAPS) {
            options = getOverlapsOptions();
        } else if (con.getOp() == ConstraintOp.DOES_NOT_OVERLAP) {
            options = getDoesntOverlapOptions();
        } else {
            throw new IllegalStateException("Unexpected operator:" + con.getOp());
        }

        ConstraintSet mainSet = new ConstraintSet(options.getMainSetOp());
        QueryField qfl = new QueryField((QueryClass) node, options.getLeftField());
        QueryField qfr = new QueryField((QueryClass) node, options.getRightField());

        for (String rangeString: con.getValues()) {
            Range range = parseRange(rangeString);
            mainSet.addConstraint(makeRangeConstraint(con.getOp(), range, options, qfl, qfr));
        }
        return mainSet;
    }
    
    /**
     * Construct a constraint for an individual range, to be combined as per the ConstraintOptions.
     *
     * Override this method if you need finer grain control over the content of each constraint, in
     * order, for example to add a further constraint to the constructed constraint set.
     *
     * @param op The operator we are creating constraints for (eg. WITHIN, OUTSIDE, OVERLAPS...). 
     * @param range The parsed range we are constructing a constraint for.
     * @param options The generated bundle of configured values.
     * @param left The field to be constrained in the left side constraint.
     * @param right The field to be constrained in the right side constraint.
     * @return A constraint.
     */
    protected Constraint makeRangeConstraint(ConstraintOp op, Range range, ConstraintOptions options, QueryField left, QueryField right) {
        ConstraintSet conSet = new ConstraintSet(options.getRangeSetOp());
        conSet.addConstraint(
            new SimpleConstraint(left, options.getLeftOp(), new QueryValue(range.getStart()))
        );
        conSet.addConstraint(
            new SimpleConstraint(right, options.getRightOp(), new QueryValue(range.getEnd()))
        );
        return conSet;
    }
    
    /**
     * @return The options that define what it means for the constrained object to be WITHIN a given set of ranges.
     */
    abstract protected ConstraintOptions getWithinOptions();
    
    /**
     * By default the OUTSIDE options are the negation of the WITHIN options, with the right and left fields swapped.
     * @return The options that define what it means for the constrained object to be OUTSIDE a given set of ranges.
     */
    protected ConstraintOptions getOutsideOptions() {
        return getWithinOptions().negateAndSwap();
    }

    /**
     * By default the OVERLAPS options are the same as the WITHIN options with the right and left fields swapped.
     * @return The options that define what it means for the constrained object to OVERLAP a given set of ranges.
     */
    protected ConstraintOptions getOverlapsOptions() {
        ConstraintOptions withinOpts = getWithinOptions();
        // by default the same as within, with right and left swapped.
        return new ConstraintOptions(
            withinOpts.getMainSetOp(),
            withinOpts.getRangeSetOp(),
            withinOpts.getLeftOp(),
            withinOpts.getRightOp(),
            withinOpts.getRightField(),
            withinOpts.getLeftField()
        );
    }

    /**
     * By default the DOES NOT OVERLAP options are negation of the OVERLAPS options.
     * @return The options that define what it means for the constrained object to NOT OVERLAP a given set of ranges.
     */
    protected ConstraintOptions getDoesntOverlapOptions() {
        return getOverlapsOptions().negate();
    }

    /**
     * By default the CONTAINS options are same as the WITHIN options with the left and right operators swapped.
     * @return The options that define what it means for the constrained object to CONTAIN a given set of ranges.
     */
    protected ConstraintOptions getContainsOptions() {
        ConstraintOptions withinOpts = getWithinOptions();
        // by default the same as within, with right and left operations swapped.
        return new ConstraintOptions(
            withinOpts.getMainSetOp(),
            withinOpts.getRangeSetOp(),
            withinOpts.getLeftOp().negate(),
            withinOpts.getRightOp().negate(),
            withinOpts.getLeftField(),
            withinOpts.getRightField()
        );
    }

    /**
     * By default the DOES NOT CONTAINS options are negation of the CONTAINS options.
     * @return The options that define what it means for the constrained object to NOT CONTAIN a given set of ranges.
     */
    protected ConstraintOptions getDoesntContainOptions() {
        return getContainsOptions().negate();
    }

    /**
     * Construct a Range object from a string defining one of the ranges this constraint operates on.
     * @param range A string describing a range (eg: "1 .. 10").
     * @return A parsed range object.
     */
    abstract protected Range parseRange(String range);

}
