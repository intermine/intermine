public class Range implements Comparable<Range>
{
    protected int min, max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Range(Range orig) {
        this.min = orig.min;
        this.max = orig.max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int compareTo(Range o) {
        int retval = min - o.min;
        if (retval == 0) {
            retval = o.max - max;
        }
        return retval;
    }

    public boolean overlaps(Range range) {
        return min <= range.max && max >= range.min;
    }

    public void expandToCover(Range range) {
        min = Math.min(min, range.getMin());
        max = Math.max(max, range.getMax());
    }

    public int size() {
        if (max > min) {
            return max - min + 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return min + ".." + max;
    }
}
