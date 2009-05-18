import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class BulkLoader implements Loader
{
    public static final BulkLoader INSTANCE = new BulkLoader();

    private BulkLoader() {
    }

    public IndexPage load(String indexFile, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize) throws IOException {
        FileReader in = new FileReader(indexFile);
        BufferedReader r = new BufferedReader(in);
        ArrayList<IndexEntry> all = new ArrayList<IndexEntry>();
        String line = r.readLine();
        while (line != null) {
            String value[] = TestBed.splitter.split(line);
            if (value.length != 2) {
                throw new IllegalArgumentException("Line \"" + line + "\" has the wrong number of elements: " + value.length);
            }
            int min = Integer.parseInt(value[0]);
            int max = Integer.parseInt(value[1]);
            all.add(new IndexEntry(min, max));
            line = r.readLine();
        }
        return bulkLoad(all.toArray(new IndexEntry[all.size()]), penaltyCalc, splitCalc, pageSize, 0, "");
    }

    // Warning - this may not generate a strict tree with all leaves at the same depth!
    public static IndexPage bulkLoad(IndexEntry entries[], PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize, int pageCount, String history) {
        Random rand = new Random();
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
            // Leave some wiggle-room in the pages.
            int wigglePageSize = (int) (pageSize * 0.9);
            if (pages > wigglePageSize) {
                pages = wigglePageSize;
            }
            if (pageCount > 0) {
                pages = pageCount;
            }
            Range all = new Range(Integer.MAX_VALUE, Integer.MIN_VALUE);
            for (IndexEntry entry : entries) {
                all.expandToCover(entry);
            }
            history = history + all + ": ";
            int sizePivot = (int) (((double) all.size()) / pages * 0.5);
            Arrays.sort(entries, new Comparator<Range>() {
                    public int compare(Range o1, Range o2) {
                        return o1.size() - o2.size();
                    }});
            int bigCount = 0;
            for (IndexEntry entry : entries) {
                if (entry.size() > sizePivot) {
                    bigCount++;
                }
            }
            //if ((bigCount < entries.length / pages - 1) && (bigCount > 0)) {
            //    bigCount = entries.length / pages - 1;
            //}
            //if ((bigCount < wigglePageSize) && (bigCount > 0)) {
            //    bigCount = wigglePageSize;
            //}
            int smallCount = entries.length - bigCount;
            //System.err.println(history + "SizePivot: " + sizePivot + ", small: " + smallCount + ", big: " + bigCount + ", page count: " + pages);
            Arrays.sort(entries, 0, smallCount);
            int lastPageNo = 0;
            ArrayList<IndexEntry> page = new ArrayList<IndexEntry>();
            InternalIndexPage retval = new InternalIndexPage(rand.nextLong(), pageSize, penaltyCalc, splitCalc);
            for (int i = 0; i < smallCount; i++) {
                IndexEntry entry = entries[i];
                page.add(entry);
                int pageNo = (i * pages) / smallCount;
                if (pageNo > lastPageNo) {
                    retval.addPage(bulkLoad(page.toArray(new IndexEntry[page.size()]), penaltyCalc, splitCalc, pageSize, 0, history));
                    page = new ArrayList<IndexEntry>();
                    lastPageNo = pageNo;
                }
            }
            if (!page.isEmpty()) {
                retval.addPage(bulkLoad(page.toArray(new IndexEntry[page.size()]), penaltyCalc, splitCalc, pageSize, 0, history));
            }
            if (bigCount > 0) {
                IndexEntry bigs[] = new IndexEntry[entries.length - smallCount];
                for (int i = smallCount; i < entries.length; i++) {
                    bigs[i - smallCount] = entries[i];
                }
                if ((bigCount <= pageSize) || (bigCount <= entries.length / pages)) {
                    retval.addPage(bulkLoad(bigs, penaltyCalc, splitCalc, pageSize, 0, history + "singlebig: "));
                    //System.err.println(history + "Just one big page for " + bigCount + " entries (" + (entries.length / pages) + ") average page size");
                } else {
                    InternalIndexPage bigPages = (InternalIndexPage) bulkLoad(bigs, penaltyCalc, splitCalc, pageSize, (pages * bigCount) / entries.length + 1, history + "multibig: ");
                    int bigPageCount = 0;
                    for (IndexPage bigPage : bigPages.getPages()) {
                        if (bigPage != null) {
                            retval.addPage(bigPage);
                            bigPageCount++;
                        }
                    }
                    //System.err.println(history + "Asked for " + ((pages * bigCount) / entries.length + 1) + " big pages, got " + bigPageCount + " for " + bigCount + " entries (" + (entries.length / pages) + ") average page size");
                }
            }
            return retval;
        }
    }
}
