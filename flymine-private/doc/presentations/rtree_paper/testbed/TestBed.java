import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Pattern;

public class TestBed
{
    public static final Pattern splitter = Pattern.compile(" ");
    public static final int PAGE_SIZE = 130;

    public static void main(String args[]) throws Exception {
        PenaltyCalculator penaltyCalc = DefaultPenaltyCalculator.INSTANCE;
        SplitCalculator splitCalc = GeometricSizeSplitCalculator.INSTANCE;
        if (args.length == 2) {
            executeTest(args[0], args[1], penaltyCalc, MidAverageSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], penaltyCalc, StartEndMidSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], penaltyCalc, SizeStartEndMidSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], penaltyCalc, GeometricAreaStartEndSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], penaltyCalc, GeometricSizeSplitCalculator.INSTANCE, PAGE_SIZE);
        } else {
            executeTest(args[0], null, penaltyCalc, splitCalc, PAGE_SIZE);
        }
    }

    public static void executeTest(String indexFile, String lookupFile, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize) throws IOException {
        long startTime = System.currentTimeMillis();
        FileReader in = new FileReader(indexFile);
        BufferedReader r = new BufferedReader(in);
        Random rand = new Random();
        IndexPage index = new LeafIndexPage(rand.nextLong(), PAGE_SIZE);
        String line = r.readLine();
        int insert = 0;
        while (line != null) {
            String value[] = splitter.split(line);
            if (value.length != 2) {
                throw new IllegalArgumentException("Line \"" + line + "\" has the wrong number of elements: " + value.length);
            }
            int min = Integer.parseInt(value[0]);
            int max = Integer.parseInt(value[1]);
            //System.out.println(min + " " + max);
            try {
                index.addEntry(new IndexEntry(min, max));
            } catch (PageNeedsSplitException e) {
                SplitPage splitPage = splitCalc.calc(index);
                index = new InternalIndexPage(rand.nextLong(), PAGE_SIZE, penaltyCalc, splitCalc, splitPage);
            }
/*            if (insert % 2000 == 1999) {
                int frame = insert / 2000;
//                System.err.println("Outputting frame " + frame);
                index.makeImage(1550, 0, 28000000, 2).writeImage("anim/" + splitCalc.getClass().getName() + "_frame" + (frame / 1000) + ((frame / 100) % 10) + ((frame / 10) % 10) + (frame % 10) + ".pnm", 1550);
            }
            insert++;*/
            line = r.readLine();
        }
        //System.out.println("Index: " + index);
        index.makeImage(1550, 0, 28000000, 2).writeImage(splitCalc.getClass().getName() + ".pnm", 1550);
        /*if (index instanceof InternalIndexPage) {
            IndexPage pages[] = ((InternalIndexPage) index).getPages();
            for (IndexPage page : pages) {
                if (page != null) {
                    page.makeImage(1550, page.getMin(), page.getMax(), 2).writeImage(splitCalc.getClass().getName() + "_" + intToString(page.getMin(), 9) + ".." + intToString(page.getMax(), 9) + ".pnm", 1550);
                }
            }
        }*/
        long builtTime = System.currentTimeMillis();
        if (lookupFile != null) {
            LookupStats stats = new LookupStats();
            in = new FileReader(lookupFile);
            r = new BufferedReader(in);
            line = r.readLine();
            while (line != null) {
                String value[] = splitter.split(line);
                if (value.length != 2) {
                    throw new IllegalArgumentException("Line \"" + line + "\" has the wrong number of elements: " + value.length);
                }
                int min = Integer.parseInt(value[0]);
                int max = Integer.parseInt(value[1]);
                index.lookup(new Range(min, max), stats);
                line = r.readLine();
            }
            System.out.println(splitCalc.getClass().getName() + ": " + stats + " took " + (builtTime - startTime) + " ms to build, and " + (System.currentTimeMillis() - builtTime) + " ms to query");
        }
    }

    public static String intToString(int value, int digits) {
        String retval = "";
        while ((value != 0) || (digits > 0)) {
            retval = value % 10 + retval;
            value = value / 10;
            digits--;
        }
        return retval;
    }
}
