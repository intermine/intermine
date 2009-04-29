public class Range implements Comparable<Range>
{
    protected int min, max;

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int compareTo(Range o) {
        return min - o.min;
    }
}
