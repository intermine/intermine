import java.util.Arrays;

public class InternalIndexPage extends IndexPage
{
    private int maxPages, pageCount;
    private IndexPage[] pages;
    private PenaltyCalculator penaltyCalc;
    private SplitCalculator splitCalc;

    public InternalIndexPage(int maxPages, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc) {
        this.maxPages = maxPages;
        this.penaltyCalc = penaltyCalc;
        this.splitCalc = splitCalc;
        pages = new IndexPage[maxPages + 1];
        pageCount = 0;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
    }

    public InternalIndexPage(int maxPages, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc,
            SplitPage splitPage) {
        this(maxPages, penaltyCalc, splitCalc);
        addPage(splitPage.getLeft());
        addPage(splitPage.getRight());
    }

    public void addEntry(IndexEntry entry) throws PageNeedsSplitException {
        double bestPenalty = Double.MAX_VALUE;
        int bestPage = -1;
        for (int i = 0; i < pageCount; i++) {
            double penalty = penaltyCalc.calc(pages[i], entry);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestPage = i;
            }
        }
        min = Math.min(min, entry.getMin());
        max = Math.max(max, entry.getMax());
        try {
            pages[bestPage].addEntry(entry);
        } catch (PageNeedsSplitException e) {
            SplitPage splitPage = splitCalc.calc(pages[bestPage]);
            pages[bestPage] = splitPage.getLeft();
            pages[pageCount++] = splitPage.getRight();
            if (pageCount > maxPages) {
                throw new PageNeedsSplitException();
            }
        }
    }

    protected void addPage(IndexPage page) {
        pages[pageCount++] = page;
        min = Math.min(min, page.getMin());
        max = Math.max(max, page.getMax());
    }

    public int getMaxPages() {
        return maxPages;
    }

    public PenaltyCalculator getPenaltyCalc() {
        return penaltyCalc;
    }

    public SplitCalculator getSplitCalc() {
        return splitCalc;
    }

    public IndexPage[] getPages() {
        return pages;
    }

    public String toString() {
        return "Internal page (" + min + ".." + max + "): " + Arrays.asList(pages);
    }
}
