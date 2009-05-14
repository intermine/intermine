public abstract class IndexPage extends Range
{
    protected long colour;

    public IndexPage(long colour) {
        super(Integer.MAX_VALUE, Integer.MIN_VALUE);
        this.colour = colour;
    }

    public abstract void addEntry(IndexEntry entry) throws PageNeedsSplitException;

    public abstract Image makeImage(int imageWidth, int minImage, int maxImage, int depth);

    public abstract void lookup(Range range, LookupStats stats);

    public abstract int entryCount();

    public long getColour() {
        return colour;
    }
}
