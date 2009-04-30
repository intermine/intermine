import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class TestBed
{
    public static final Pattern splitter = Pattern.compile(" ");
    public static final int PAGE_SIZE = 130;

    public static void main(String args[]) throws Exception {
        InputStreamReader in = new InputStreamReader(System.in);
        BufferedReader r = new BufferedReader(in);
        IndexPage index = new LeafIndexPage(PAGE_SIZE);
        PenaltyCalculator penaltyCalc = DefaultPenaltyCalculator.INSTANCE;
        SplitCalculator splitCalc = StartEndMidSplitCalculator.INSTANCE;
        String line = r.readLine();
//        int insert = 0;
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
                index = new InternalIndexPage(PAGE_SIZE, penaltyCalc, splitCalc, splitPage);
            }
//            if (insert % 2000 == 1999) {
//                int frame = insert / 2000;
//                System.err.println("Outputting frame " + frame);
//                index.makeImage(1550, 0, 28000000, 2).writeImage("anim/frame" + (frame / 1000) + ((frame / 100) % 10) + ((frame / 10) % 10) + (frame % 10) + ".pnm", 1550);
//            }
//            insert++;
            line = r.readLine();
        }
        //System.out.println("Index: " + index);
//        index.makeImage(1550, 0, 28000000, 2).writeImage("output.pnm", 1550);
/*        if (index instanceof InternalIndexPage) {
            IndexPage pages[] = ((InternalIndexPage) index).getPages();
            for (IndexPage page : pages) {
                if (page != null) {
                    page.makeImage(1550, page.getMin(), page.getMax(), 2).writeImage("output_" + intToString(page.getMin(), 9) + ".." + intToString(page.getMax(), 9) + ".pnm", 1550);
                }
            }
        }*/
        LookupStats stats = new LookupStats();
        index.lookup(new Range(10, 1000), stats);
        System.out.println("Test lookup: " + stats); 
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
