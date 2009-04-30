public class StartEndMidSplitCalculator extends SplitCalculator
{
    public static final StartEndMidSplitCalculator INSTANCE = new StartEndMidSplitCalculator();

    private StartEndMidSplitCalculator() {
    }

    protected void splitPage(SplitPage retval, Range ranges[]) {
        int lowestMin = Integer.MAX_VALUE;
        int lowestMax = Integer.MAX_VALUE;
        int highestMin = Integer.MIN_VALUE;
        int highestMax = Integer.MIN_VALUE;
        for (Range r : ranges) {
            if (r != null) {
                lowestMin = Math.min(lowestMin, r.getMin());
                lowestMax = Math.min(lowestMax, r.getMax());
                highestMin = Math.max(highestMin, r.getMin());
                highestMax = Math.max(highestMax, r.getMax());
            }
        }
        if (highestMin - lowestMin > highestMax - lowestMax) {
            int split = lowestMin + ((highestMin - lowestMin) / 2);
            for (Range r : ranges) {
                if (r != null) {
                    if (r.getMin() < split) {
                        addToLeft(retval, r);
                    } else {
                        addToRight(retval, r);
                    }
                }
            }
        } else {
            int split = lowestMax + ((highestMax - lowestMax) / 2);
            for (Range r : ranges) {
                if (r != null) {
                    if (r.getMax() < split) {
                        addToLeft(retval, r);
                    } else {
                        addToRight(retval, r);
                    }
                }
            }
        }
    }
}
