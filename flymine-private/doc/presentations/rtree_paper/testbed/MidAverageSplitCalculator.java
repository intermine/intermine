public class MidAverageSplitCalculator extends SplitCalculator
{
    public static final MidAverageSplitCalculator INSTANCE = new MidAverageSplitCalculator();

    private MidAverageSplitCalculator() {
    }

    protected void splitPage(SplitPage retval, Range ranges[]) {
        long midPointSum = 0;
        int midPoint = 0;
        int count = 0;
        for (Range r : ranges) {
            if (r != null) {
                midPoint = r.getMin() + ((r.getMax() - r.getMin()) / 2);
                midPointSum += midPoint;
                count++;
            }
        }
        int split = (int) (midPointSum / count);
        for (Range r : ranges) {
            if (r != null) {
                midPoint = r.getMin() + ((r.getMax() - r.getMin()) / 2);
                if (midPoint < split) {
                    addToLeft(retval, r);
                } else {
                    addToRight(retval, r);
                }
            }
        }
    }
}
