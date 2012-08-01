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

public abstract class AbstractHelper implements RangeHelper {

	
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
			mainSet.addConstraint(makeRangeConstraint(range, options, qfl, qfr));
		}
		return mainSet;
	}
	
	protected Constraint makeRangeConstraint(Range range, ConstraintOptions options, QueryField left, QueryField right) {
		ConstraintSet conSet = new ConstraintSet(options.getRangeSetOp());
		conSet.addConstraint(
			new SimpleConstraint(left, options.getLeftOp(), new QueryValue(range.getStart()))
		);
		conSet.addConstraint(
			new SimpleConstraint(right, options.getRightOp(), new QueryValue(range.getEnd()))
		);
		return conSet;
	}
	
	abstract protected ConstraintOptions getWithinOptions();
	
	protected ConstraintOptions getOutsideOptions() {
		ConstraintOptions withinOpts = getWithinOptions();
		return new ConstraintOptions(
			ConstraintOp.AND,
			ConstraintOp.OR,
			withinOpts.getLeftOp().negate(),
			withinOpts.getRightOp().negate(),
			withinOpts.getRightField(),
			withinOpts.getLeftField()
		);
	}
	
	protected ConstraintOptions getOverlapsOptions() {
		ConstraintOptions withinOpts = getWithinOptions();
		// by default the same as within, with right and left swapped.
		return new ConstraintOptions(
			ConstraintOp.OR,
			ConstraintOp.AND,
			withinOpts.getLeftOp(),
			withinOpts.getRightOp(),
			withinOpts.getRightField(),
			withinOpts.getLeftField()
		);
	}
	
	protected ConstraintOptions getDoesntOverlapOptions() {
		ConstraintOptions opts = getOverlapsOptions();
		return new ConstraintOptions(
			ConstraintOp.AND,
			ConstraintOp.OR,
			opts.getLeftOp().negate(),
			opts.getRightOp().negate(),
			opts.getLeftField(),
			opts.getRightField()
		);
	}
	
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
	
	protected ConstraintOptions getDoesntContainOptions() {
		return getContainsOptions().negate();
	}

	abstract protected Range parseRange(String range);

}
