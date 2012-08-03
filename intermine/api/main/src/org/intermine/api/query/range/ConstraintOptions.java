package org.intermine.api.query.range;

import org.intermine.objectstore.query.ConstraintOp;
import static org.intermine.objectstore.query.ConstraintOp.AND;
import static org.intermine.objectstore.query.ConstraintOp.OR;

/**
 * A bundle of options consumed by the AbstractHelper when generating constraint
 * sets for a set of ranges. A class inheriting from AbstractHelper need only provide
 * a definition for how to build a WITHIN constraint for a set of ranges as a set of
 * constraint options, and the other constraint option sets can be derived.
 * @author Alex Kalderimis
 *
 */
public class ConstraintOptions {

    private final ConstraintOp mainSetOp;
    private final ConstraintOp rangeSetOp;
    private final ConstraintOp leftOp;
    private final ConstraintOp rightOp;
    private String leftField = "start";
    private String rightField = "end";

    /**
     * Construct a new set of constraint options.
     *
     * @param mainOp The logical operator to use to combine constraints for ranges together. Expected to be AND or OR
     * @param rangeOp The logical operator to use to combine left and right constraints together for each range.
     * @param leftOp The operator to use in the left side constraint.
     * @param rightOp The operator to use in the right side constraint.
     */
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
    
    /**
     * Construct a new set of constraint options.
     *
     * @param mainOp The logical operator to use to combine constraints for ranges together. Expected to be AND or OR
     * @param rangeOp The logical operator to use to combine left and right constraints together for each range.
     * @param leftOp The operator to use in the left side constraint.
     * @param rightOp The operator to use in the right side constraint.
     * @param leftField The field of the QueryNode to evaluate in the left side constraint.
     * @param rightField The field of the QueryNode to evaluate in the right side constraint.
     */
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

    /**
     * @return The logical operator to use to combine constraints for ranges together.
     */
    public ConstraintOp getMainSetOp() {
        return mainSetOp;
    }

    /**
     * @return The logical operator to use to combine left and right constraints together for each range.
     */
    public ConstraintOp getRangeSetOp() {
        return rangeSetOp;
    }

    /**
     * @return The operator to use in the left side constraint.
     */
    public ConstraintOp getLeftOp() {
        return leftOp;
    }

    /**
     * @return The operator to use in the right side constraint.
     */
    public ConstraintOp getRightOp() {
        return rightOp;
    }

    /**
     * Defaults to "start".
     * @return The field of the QueryNode to evaluate in the left side constraint.
     */
    public String getLeftField() {
        return leftField;
    }

    /**
     * Defaults to "end".
     * @return The field of the QueryNode to evaluate in the right side constraint.
     */
    public String getRightField() {
        return rightField;
    }
    
    /**
     * Return a new constraint set with each ConstraintOp negated.
     * @return A negated version of this constraint set.
     */
    public ConstraintOptions negate() {
        ConstraintOp newMain = (mainSetOp == AND) ? OR : AND;
        ConstraintOp newRange = (rangeSetOp == AND) ? OR : AND;
        ConstraintOp newLeft = leftOp.negate();
        ConstraintOp newRight = rightOp.negate();
        return new ConstraintOptions(
            newMain, newRange, newLeft, newRight, leftField, rightField
        );
    }

    /**
     * Return a new constraint set with each ConstraintOp negated, and the left and right fields swapped.
     * @return A negated and swapped version of this constraint set.
     */
    public ConstraintOptions negateAndSwap() {
        ConstraintOptions neg = negate();
        return new ConstraintOptions(
            neg.getMainSetOp(), neg.getRangeSetOp(), neg.getLeftOp(), neg.getRightOp(),
            neg.getRightField(), neg.getLeftField()
        );
    }
}
