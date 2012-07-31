package org.intermine.api.query.range;

import org.intermine.objectstore.query.ConstraintOp;
import static org.intermine.objectstore.query.ConstraintOp.AND;
import static org.intermine.objectstore.query.ConstraintOp.OR;

public class ConstraintOptions {

	private ConstraintOp mainSetOp;
	private ConstraintOp rangeSetOp;
	private ConstraintOp leftOp;
	private ConstraintOp rightOp;
	private String leftField = "start";
	private String rightField = "end";
	
	public ConstraintOptions(
		ConstraintOp mainOp,
		ConstraintOp rangeOp,
		ConstraintOp leftOp,
		ConstraintOp rightOp) {
		this.mainSetOp = mainOp;
		this.rangeSetOp = rangeOp;
		this.leftOp = leftOp;
		this.rightOp = rightOp;
	}
	
	public ConstraintOptions(
			ConstraintOp mainOp,
			ConstraintOp rangeOp,
			ConstraintOp leftOp,
			ConstraintOp rightOp,
			String leftField,
			String rightField) {
		this(mainOp, rangeOp, leftOp, rightOp);
		this.leftField = leftField;
		this.rightField = rightField;
	}

	public ConstraintOp getMainSetOp() {
		return mainSetOp;
	}

	public ConstraintOp getRangeSetOp() {
		return rangeSetOp;
	}

	public ConstraintOp getLeftOp() {
		return leftOp;
	}

	public ConstraintOp getRightOp() {
		return rightOp;
	}

	public String getLeftField() {
		return leftField;
	}

	public String getRightField() {
		return rightField;
	}
	
	public ConstraintOptions negate() {
		ConstraintOp newMain = (mainSetOp == AND) ? OR : AND;
		ConstraintOp newRange = (rangeSetOp == AND) ? OR : AND;
		ConstraintOp newLeft = leftOp.negate();
		ConstraintOp newRight = leftOp.negate();
		return new ConstraintOptions(
			newMain, newRange, newLeft, newRight, leftField, rightField
		);
	}
}
