public class IndexEntry extends Range
{
    public IndexEntry(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public String toString() {
        return min + ".." + max;
    }
}
