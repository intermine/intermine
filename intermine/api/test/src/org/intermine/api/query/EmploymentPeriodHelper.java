package org.intermine.api.query;

import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintRange;

public class EmploymentPeriodHelper implements RangeHelper {

	@Override
	public Constraint createConstraint(QueryNode node, PathConstraintRange con) {
		
		String[] dates = con.getValue().split("\\.\\.");
		if (dates.length != 2) {
			throw new IllegalArgumentException("Illegal range: " + con.getValue());
		}
		
		QueryField qfa = new QueryField((QueryClass) node, "start");
		QueryField qfb = new QueryField((QueryClass) node, "end");
		
		if (con.getOp() == ConstraintOp.WITHIN) {
			ConstraintSet conset = new ConstraintSet(ConstraintOp.AND);
			PathConstraintAttribute pca =
					new PathConstraintAttribute(con.getPath() + ".start", ConstraintOp.GREATER_THAN_EQUALS, dates[0].trim());
			PathConstraintAttribute pcb =
					new PathConstraintAttribute(con.getPath() + ".end", ConstraintOp.LESS_THAN_EQUALS, dates[1].trim());
			conset.addConstraint(MainHelper.makeQueryDateConstraint(qfa, pca));
			conset.addConstraint(MainHelper.makeQueryDateConstraint(qfb, pcb));
			return conset;
		} else if (con.getOp() == ConstraintOp.OUTSIDE) {
			ConstraintSet conset = new ConstraintSet(ConstraintOp.OR);
			PathConstraintAttribute pca =
					new PathConstraintAttribute(con.getPath() + ".start", ConstraintOp.LESS_THAN, dates[0].trim());
			PathConstraintAttribute pcb =
					new PathConstraintAttribute(con.getPath() + ".end", ConstraintOp.GREATER_THAN_EQUALS, dates[1].trim());
			conset.addConstraint(MainHelper.makeQueryDateConstraint(qfa, pca));
			conset.addConstraint(MainHelper.makeQueryDateConstraint(qfb, pcb));
			return conset;
		}
		throw new RuntimeException("Unimplemented behaviour");
	}

}
