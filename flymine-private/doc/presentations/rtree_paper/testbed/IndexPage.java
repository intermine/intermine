public abstract class IndexPage extends Range
{
    public abstract void addEntry(IndexEntry entry) throws PageNeedsSplitException;
}
