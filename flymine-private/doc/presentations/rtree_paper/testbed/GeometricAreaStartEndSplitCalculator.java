public class GeometricAreaStartEndSplitCalculator extends GeometricStartEndSplitCalculator
{
    public static final GeometricAreaStartEndSplitCalculator INSTANCE = new GeometricAreaStartEndSplitCalculator();

    private GeometricAreaStartEndSplitCalculator() {
    }

    protected double penalty(Range left, Range right, int leftCount, int rightCount, int split, boolean splitMax) {
        Range total = new Range(left);
        total.expandToCover(right);
        double areaPenalty = left.size() + right.size();
        double countPenalty = Math.pow(((double) Math.max(leftCount, rightCount) * 2.0) / (leftCount + rightCount) - 1.0, 4.0) * total.size() * 2.0;
//        System.err.println("Left: " + left + ", right: " + right + ", leftCount: " + leftCount + ", rightCount: " + rightCount + ", areaPenalty: " + areaPenalty + ", countPenalty: " + countPenalty + ", penalty: " + (areaPenalty + countPenalty) + ", split: " + split + (splitMax ? " max" : " min"));
        return areaPenalty + countPenalty;
    }
}
