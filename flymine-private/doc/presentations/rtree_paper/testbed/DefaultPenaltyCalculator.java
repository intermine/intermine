public class DefaultPenaltyCalculator implements PenaltyCalculator
{
    public static final DefaultPenaltyCalculator INSTANCE = new DefaultPenaltyCalculator();

    private DefaultPenaltyCalculator() {
    }

    public double calc(IndexPage page, IndexEntry entry) {
        // The penalty is the amount that the page would have to grow.
        int min = Math.min(entry.getMin(), page.getMin());
        int max = Math.max(entry.getMax(), page.getMax());
        double penalty = max - min - page.getMax() + page.getMin();
        // If the penalty is zero, then tie-break by using the size of the page.
        if (penalty == 0) {
            penalty = ((double) (max - min)) / 2147483648.0 - 1.0;
        }
        return penalty;
    }
}
