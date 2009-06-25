import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class IncrementalLoader implements Loader
{
    public static final IncrementalLoader INSTANCE = new IncrementalLoader();

    private IncrementalLoader() {
    }

    public IndexPage load(String indexFile, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize) throws IOException {
        FileReader in = new FileReader(indexFile);
        BufferedReader r = new BufferedReader(in);
        Random rand = new Random();
        IndexPage index = new LeafIndexPage(rand.nextLong(), pageSize);
        String line = r.readLine();
        //int insert = 0;
        while (line != null) {
            String value[] = TestBed.splitter.split(line);
            if (value.length != 2) {
                throw new IllegalArgumentException("Line \"" + line + "\" has the wrong number of elements: " + value.length);
            }
            int min = Integer.parseInt(value[0]);
            int max = Integer.parseInt(value[1]);
            try {
                index.addEntry(new IndexEntry(min, max));
            } catch (PageNeedsSplitException e) {
                SplitPage splitPage = splitCalc.calc(index);
                index = new InternalIndexPage(rand.nextLong(), pageSize, penaltyCalc, splitCalc, splitPage);
            }
/*            if (insert % 2000 == 1999) {
                int frame = insert / 2000;
//                System.err.println("Outputting frame " + frame);
                index.makeImage(1550, 0, 28000000, 2).writeImage("anim/" + splitCalc.getClass().getName() + "_frame" + (frame / 1000) + ((frame / 100) % 10) + ((frame / 10) % 10) + (frame % 10) + ".pnm", 1550);
            }
            insert++;*/
            line = r.readLine();
        }
        return index;
    }

    public String layoutDescription(PenaltyCalculator penaltyCalc, SplitCalculator splitCalc) {
        return "Incremental_" + splitCalc.getClass().getName();
    }
}
