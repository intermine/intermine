import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class TestBed
{
    public static final Pattern splitter = Pattern.compile(" ");
    public static final int PAGE_SIZE = 130;

    public static void main(String args[]) throws Exception {
        PenaltyCalculator penaltyCalc = DefaultPenaltyCalculator.INSTANCE;
        SplitCalculator splitCalc = GeometricSizeSplitCalculator.INSTANCE;
        if (args.length == 2) {
            executeTest(args[0], args[1], IncrementalLoader.INSTANCE, penaltyCalc, MidAverageSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], IncrementalLoader.INSTANCE, penaltyCalc, StartEndMidSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], IncrementalLoader.INSTANCE, penaltyCalc, SizeStartEndMidSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], IncrementalLoader.INSTANCE, penaltyCalc, GeometricAreaStartEndSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], IncrementalLoader.INSTANCE, penaltyCalc, GeometricSizeSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], STRBulkLoader.INSTANCE, penaltyCalc, GeometricSizeSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], SplittingBulkLoader.INSTANCE, penaltyCalc, MidAverageSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], SplittingBulkLoader.INSTANCE, penaltyCalc, StartEndMidSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], SplittingBulkLoader.INSTANCE, penaltyCalc, SizeStartEndMidSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], SplittingBulkLoader.INSTANCE, penaltyCalc, GeometricAreaStartEndSplitCalculator.INSTANCE, PAGE_SIZE);
            executeTest(args[0], args[1], SplittingBulkLoader.INSTANCE, penaltyCalc, GeometricSizeSplitCalculator.INSTANCE, PAGE_SIZE);
        } else {
            executeTest(args[0], null, IncrementalLoader.INSTANCE, penaltyCalc, splitCalc, PAGE_SIZE);
        }
    }

    public static void executeTest(String indexFile, String lookupFile, Loader loader, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize) throws IOException {
        long startTime = System.currentTimeMillis();
        IndexPage index = loader.load(indexFile, penaltyCalc, splitCalc, pageSize);
        long builtTime = System.currentTimeMillis();
        //System.out.println("Index: " + index);
        //index.makeImage(15500, 0, 28000000, -1).writeImage(loader.layoutDescription(penaltyCalc, splitCalc) + ".pnm", 15500);
/*        if (index instanceof InternalIndexPage) {
            IndexPage pages[] = ((InternalIndexPage) index).getPages();
            for (IndexPage page : pages) {
                if (page != null) {
                    page.makeImage(1550, page.getMin(), page.getMax(), 2).writeImage(loader.layoutDescription(penaltyCalc, splitCalc) + "_" + intToString(page.getMin(), 9) + ".." + intToString(page.getMax(), 9) + ".pnm", 1550);
                }
            }
        }*/
        if (lookupFile != null) {
            long startLookupTime = System.currentTimeMillis();
            LookupStats stats = new LookupStats();
            FileReader in = new FileReader(lookupFile);
            BufferedReader r = new BufferedReader(in);
            String line = r.readLine();
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
            System.out.println(loader.layoutDescription(penaltyCalc, splitCalc) + ": " + stats + " took " + (builtTime - startTime) + " ms to build, and " + (System.currentTimeMillis() - startLookupTime) + " ms to query. Root page contains " + index.entryCount() + " entries.");
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
