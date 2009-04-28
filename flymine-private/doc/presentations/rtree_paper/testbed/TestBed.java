import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class TestBed
{
    public static final Pattern splitter = Pattern.compile(" ");
    public static final int PAGE_SIZE = 20;

    public static void main(String args[]) throws Exception {
        InputStreamReader in = new InputStreamReader(System.in);
        BufferedReader r = new BufferedReader(in);
        IndexPage index = new LeafIndexPage(PAGE_SIZE);
        PenaltyCalculator penaltyCalc = DefaultPenaltyCalculator.INSTANCE;
        SplitCalculator splitCalc = MidAverageSplitCalculator.INSTANCE;
        String line = r.readLine();
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
            line = r.readLine();
        }
        System.out.println("Index: " + index);
    }
}
