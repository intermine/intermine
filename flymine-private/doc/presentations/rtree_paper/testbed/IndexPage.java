public abstract class IndexPage extends Range
{
    public abstract void addEntry(IndexEntry entry) throws PageNeedsSplitException;


    public abstract Image makeImage(int imageWidth, int minImage, int maxImage, int depth);
}
