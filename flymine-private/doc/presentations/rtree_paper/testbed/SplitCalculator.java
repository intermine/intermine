public abstract class SplitCalculator
{
    public SplitPage calc(IndexPage page) {
        SplitPage retval;
        if (page instanceof InternalIndexPage) {
            InternalIndexPage iPage = (InternalIndexPage) page;
            InternalIndexPage left = new InternalIndexPage(iPage.getMaxPages(), iPage.getPenaltyCalc(), iPage.getSplitCalc());
            InternalIndexPage right = new InternalIndexPage(iPage.getMaxPages(), iPage.getPenaltyCalc(), iPage.getSplitCalc());
            retval = new SplitPage(left, right);
            splitPage(retval, iPage.getPages());
        } else {
            LeafIndexPage lPage = (LeafIndexPage) page;
            LeafIndexPage left = new LeafIndexPage(lPage.getMaxEntries());
            LeafIndexPage right = new LeafIndexPage(lPage.getMaxEntries());
            retval = new SplitPage(left, right);
            splitPage(retval, lPage.getEntries());
        }
        return retval;
    }

    protected abstract void splitPage(SplitPage retval, Range ranges[]);

    protected void addToLeft(SplitPage retval, Range range) {
        IndexPage page = retval.getLeft();
        if (page instanceof InternalIndexPage) {
            ((InternalIndexPage) page).addPage((IndexPage) range);
        } else {
            ((LeafIndexPage) page).addEntryWithoutCheck((IndexEntry) range);
        }
    }

    protected void addToRight(SplitPage retval, Range range) {
        IndexPage page = retval.getRight();
        if (page instanceof InternalIndexPage) {
            ((InternalIndexPage) page).addPage((IndexPage) range);
        } else {
            ((LeafIndexPage) page).addEntryWithoutCheck((IndexEntry) range);
        }
    }
}
