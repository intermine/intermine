public abstract class GeometricStartEndSplitCalculator extends SplitCalculator
{
    protected void splitPage(SplitPage retval, Range ranges[]) {
        boolean splitMax = false;
        int bestSplit = 0;
        double bestPenalty = Double.MAX_VALUE;
        for (Range pivot : ranges) {
            if (pivot != null) {
                int split = pivot.getMin();
                Range left = new Range(Integer.MAX_VALUE, Integer.MIN_VALUE);
                Range right = new Range(Integer.MAX_VALUE, Integer.MIN_VALUE);
                int leftCount = 0;
                int rightCount = 0;
                for (Range r : ranges) {
                    if (r != null) {
                        if (r.getMin() < split) {
                            leftCount++;
                            left.expandToCover(r);
                        } else {
                            rightCount++;
                            right.expandToCover(r);
                        }
                    }
                }
                double penalty = penalty(left, right, leftCount, rightCount, split, false);
                if (penalty < bestPenalty) {
                    bestPenalty = penalty;
                    bestSplit = split;
                    splitMax = false;
                }
                split = pivot.getMax();
                left = new Range(Integer.MAX_VALUE, Integer.MIN_VALUE);
                right = new Range(Integer.MAX_VALUE, Integer.MIN_VALUE);
                leftCount = 0;
                rightCount = 0;
                for (Range r : ranges) {
                    if (r != null) {
                        if (r.getMax() < split) {
                            leftCount++;
                            left.expandToCover(r);
                        } else {
                            rightCount++;
                            right.expandToCover(r);
                        }
                    }
                }
                penalty = penalty(left, right, leftCount, rightCount, split, true);
                if (penalty < bestPenalty) {
                    bestPenalty = penalty;
                    bestSplit = split;
                    splitMax = true;
                }
            }
        }

//        System.err.println("Best penalty: " + bestPenalty + ", split: " + bestSplit + (splitMax ? " max" : " min"));
        if (!splitMax) {
            for (Range r : ranges) {
                if (r != null) {
                    if (r.getMin() < bestSplit) {
                        addToLeft(retval, r);
                    } else {
                        addToRight(retval, r);
                    }
                }
            }
        } else {
            for (Range r : ranges) {
                if (r != null) {
                    if (r.getMax() < bestSplit) {
                        addToLeft(retval, r);
                    } else {
                        addToRight(retval, r);
                    }
                }
            }
        }
//        System.err.println("Splitted into " + retval.getLeft().entryCount() + " and " + retval.getRight().entryCount());
    }

    protected abstract double penalty(Range left, Range right, int leftCount, int rightCount, int split, boolean splitMax);
}
