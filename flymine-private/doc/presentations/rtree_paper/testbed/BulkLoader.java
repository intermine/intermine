import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public abstract class BulkLoader implements Loader
{
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

    public abstract IndexPage bulkLoad(IndexEntry entries[], PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize, int pageCount, String history);
}
