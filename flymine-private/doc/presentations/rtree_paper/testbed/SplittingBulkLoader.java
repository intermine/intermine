import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;

public class SplittingBulkLoader extends BulkLoader
{
    public static final SplittingBulkLoader INSTANCE = new SplittingBulkLoader();

    private SplittingBulkLoader() {
    }

    // Warning - this may not generate a strict tree with all leaves at the same depth!
    public IndexPage bulkLoad(IndexEntry entries[], PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize, int pageCount, String history) {
        Random rand = new Random();
        System.err.println("Bulkloading " + entries.length + " entries with pageSize " + pageSize);
        if (entries.length <= pageSize) {
            LeafIndexPage retval = new LeafIndexPage(rand.nextLong(), pageSize);
            for (IndexEntry entry : entries) {
                try {
                    retval.addEntry(entry);
                } catch (PageNeedsSplitException e) {
                    throw new RuntimeException(e);
                }
            }
            return retval;
        } else {
            int pages = (int) Math.sqrt(entries.length);
            if (pages > pageSize) {
                pages = pageSize;
            }
            LeafIndexPage toSplit = new LeafIndexPage(rand.nextLong(), entries.length);
            for (IndexEntry entry : entries) {
                try {
                    toSplit.addEntry(entry);
                } catch (PageNeedsSplitException e) {
                    throw new RuntimeException(e);
                }
            }
            TreeSet<LeafIndexPage> splitted = new TreeSet<LeafIndexPage>(new Comparator<LeafIndexPage>() {
                    public int compare(LeafIndexPage o1, LeafIndexPage o2) {
                        int retval = o2.entryCount() - o1.entryCount();
                        if (retval == 0) {
                            retval = o1.hashCode() - o2.hashCode();
                        }
                        return retval;
                    }});
            splitted.add(toSplit);
            while (splitted.size() < pages) {
                toSplit = splitted.pollFirst();
                SplitPage splitPage = splitCalc.calc(toSplit);
                splitted.add((LeafIndexPage) splitPage.getLeft());
                splitted.add((LeafIndexPage) splitPage.getRight());
                System.err.println("Split " + toSplit.entryCount() + " entries into " + splitPage.getLeft().entryCount() + " and " + splitPage.getRight().entryCount());
            }
            StringBuilder errMessage = new StringBuilder("Recursing with group sizes ");
            boolean needComma = false;
            for (LeafIndexPage page : splitted) {
                if (needComma) {
                    errMessage.append(", ");
                }
                needComma = true;
                errMessage.append("" + page.entryCount());
            }
            System.err.println(errMessage.toString());
            InternalIndexPage retval = new InternalIndexPage(rand.nextLong(), pageSize, penaltyCalc, splitCalc);
            for (LeafIndexPage page : splitted) {
                IndexEntry subEntries[] = new IndexEntry[page.entryCount()];
                for (int i = 0; i < page.entryCount(); i++) {
                    subEntries[i] = page.getEntries()[i];
                }
                retval.addPage(bulkLoad(subEntries, penaltyCalc, splitCalc, pageSize, 0, history));
            }
            return retval;
        }
    }

    public String layoutDescription(PenaltyCalculator penaltyCalc, SplitCalculator splitCalc) {
        return "SplittingBulkLoader_" + splitCalc.getClass().getName();
    }
}
