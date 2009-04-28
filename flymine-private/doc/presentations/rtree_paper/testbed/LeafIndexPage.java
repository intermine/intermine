import java.util.Arrays;

public class LeafIndexPage extends IndexPage
{
    private int maxEntries, entryCount;
    private IndexEntry[] entries;

    public LeafIndexPage(int maxEntries) {
        this.maxEntries = maxEntries;
        entries = new IndexEntry[maxEntries + 1];
        entryCount = 0;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
    }

    public void addEntry(IndexEntry entry) throws PageNeedsSplitException {
        entries[entryCount++] = entry;
        min = Math.min(min, entry.getMin());
        max = Math.max(max, entry.getMax());
        if (entryCount > maxEntries) {
            throw new PageNeedsSplitException();
        }
    }

    public void addEntryWithoutCheck(IndexEntry entry) {
        entries[entryCount++] = entry;
        min = Math.min(min, entry.getMin());
        max = Math.max(max, entry.getMax());
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public IndexEntry[] getEntries() {
        return entries;
    }

    public String toString() {
        return "Leaf page (" + min + ".." + max + "): " + Arrays.asList(entries);
    }
}
