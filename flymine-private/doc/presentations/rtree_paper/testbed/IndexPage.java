public abstract class IndexPage extends Range
{
    public IndexPage() {
        super(Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    public abstract void addEntry(IndexEntry entry) throws PageNeedsSplitException;

    public abstract Image makeImage(int imageWidth, int minImage, int maxImage, int depth);

    public abstract void lookup(Range range, LookupStats stats);
}
