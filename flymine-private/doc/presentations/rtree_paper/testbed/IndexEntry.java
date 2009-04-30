public class IndexEntry extends Range
{
    public IndexEntry(int min, int max) {
        super(min, max);
    }

    public String toString() {
        return min + ".." + max;
    }
}
