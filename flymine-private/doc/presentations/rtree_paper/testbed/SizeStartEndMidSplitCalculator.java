public class SizeStartEndMidSplitCalculator extends SplitCalculator
{
    public static final SizeStartEndMidSplitCalculator INSTANCE = new SizeStartEndMidSplitCalculator();

    private SizeStartEndMidSplitCalculator() {
    }

    protected void splitPage(SplitPage retval, Range ranges[]) {
        int lowestSize = Integer.MAX_VALUE;
        int lowestMin = Integer.MAX_VALUE;
        int lowestMax = Integer.MAX_VALUE;
        int highestSize = Integer.MIN_VALUE;
        int highestMin = Integer.MIN_VALUE;
        int highestMax = Integer.MIN_VALUE;
        for (Range r : ranges) {
            if (r != null) {
                int size = r.getMax() - r.getMin() + 1;
                lowestSize = Math.min(lowestSize, size);
                lowestMin = Math.min(lowestMin, r.getMin());
                lowestMax = Math.min(lowestMax, r.getMax());
                highestMin = Math.max(highestMin, r.getMin());
                highestMax = Math.max(highestMax, r.getMax());
            }
        }
        for (Range r : ranges) {
            if (r != null) {
                int size = r.getMax() - r.getMin() + 1;
                if ((r.getMax() == highestMax) || (r.getMin() == lowestMin)) {
                    highestSize = Math.max(highestSize, size);
                }
            }
        }
                
        if ((highestSize * 3 > (highestMax - lowestMin) * 2) && (lowestSize < (highestMax - lowestMin) / 3)) {
            int split = lowestSize + ((highestSize - lowestSize) / 4);
            for (Range r : ranges) {
                if (r != null) {
                    if (r.getMax() - r.getMin() + 1 > split) {
                        addToLeft(retval, r);
                    } else {
                        addToRight(retval, r);
                    }
                }
            }
        } else if (highestMin - lowestMin > highestMax - lowestMax) {
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

