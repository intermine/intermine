public class LookupStats
{
    private int pages = 0;
    private int comparisons = 0;
    private int results = 0;

    public LookupStats() {
    }

    public void addStats(int comparisons) {
        pages++;
        this.comparisons += comparisons;
    }

    public void addStats(int comparisons, int results) {
        addStats(comparisons);
        this.results += results;
    }

    public String toString() {
        return "Pages touched: " + pages + ", comparisons: " + comparisons + ", results " + results;
    }
}
