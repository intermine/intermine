package org.intermine.api.query.range;

import static org.intermine.objectstore.query.ConstraintOp.AND;
import static org.intermine.objectstore.query.ConstraintOp.DOES_NOT_OVERLAP;
import static org.intermine.objectstore.query.ConstraintOp.OR;
import static org.intermine.objectstore.query.ConstraintOp.OUTSIDE;
import static org.intermine.objectstore.query.ConstraintOp.OVERLAPS;
import static org.intermine.objectstore.query.ConstraintOp.WITHIN;

import org.intermine.api.query.RangeHelper;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;

public class StringHelper implements RangeHelper {
	
	private static final ConstraintOp GTE = ConstraintOp.GREATER_THAN_EQUALS;
	private static final ConstraintOp LTE = ConstraintOp.LESS_THAN_EQUALS;
	private static final ConstraintOp LT = ConstraintOp.LESS_THAN;
	private static final ConstraintOp GT = ConstraintOp.GREATER_THAN_EQUALS;
	
	@Override
	public Constraint createConstraint(QueryNode node, PathConstraintRange con) {
		QueryField qf = (QueryField) node;
		ConstraintOp mainSetOp;
		ConstraintOp rangeSetOp;
		ConstraintOp leftOp;
		ConstraintOp rightOp;
		
		if (con.getOp() == WITHIN || con.getOp() == OVERLAPS) {
			// The number is within at least one of the ranges.
			// eg: azzz WITHIN a .. b => true
			//     a    WITHIN a .. b => true
			//     b    WITHIN a .. b => false
			mainSetOp = OR;
			rangeSetOp = AND;
			leftOp = GTE;
			rightOp = LT;
		} else if (con.getOp() == OUTSIDE || con.getOp() == DOES_NOT_OVERLAP) {
			// The number lies outside all the given ranges.
			mainSetOp = AND;
			rangeSetOp = OR;
			leftOp = LT;
			rightOp = GTE;
		} else {
			throw new RuntimeException("Unimplemented behaviour: " + con.getOp());
		}
		
		ConstraintSet mainSet = new ConstraintSet(mainSetOp);
		for (String range: con.getValues()) {
			ConstraintSet conSet = new ConstraintSet(rangeSetOp);
			StringRange r = new StringRange(range);
			conSet.addConstraint(new SimpleConstraint(qf, leftOp, new QueryValue(r.start)));
			conSet.addConstraint(new SimpleConstraint(qf, rightOp, new QueryValue(r.end)));
			mainSet.addConstraint(conSet);
		}
		return mainSet;
	}
	
	private class StringRange
	{
		final String start;
		final String end;
		
		StringRange(String range) {
			if (range == null) {
				throw new NullPointerException("range may not be null");
			}
			String[] parts = range.split("\\.\\.");
			if (parts.length != 2) {
				throw new IllegalArgumentException(
					"Illegal range (" + range + "): should be in the format 'x .. y'"
				);
			}
			start = parts[0].trim();
			end = parts[1].trim();
		}
	}

}
