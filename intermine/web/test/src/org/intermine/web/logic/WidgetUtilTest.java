package org.intermine.web.logic;

import java.math.BigDecimal;
import java.util.HashMap;

import junit.framework.TestCase;

import org.intermine.web.logic.widget.Hypergeometric;

public class WidgetUtilTest extends TestCase
{
    private final int REFERENCE_SIZE = 5;
    private HashMap<String, BigDecimal> resultsMap = new HashMap();
    private String[] id = new String[REFERENCE_SIZE];
    private int[] taggedSample = new int[REFERENCE_SIZE];
    private int[] taggedPopulation = new int[REFERENCE_SIZE];
    private BigDecimal[] expectedResults = new BigDecimal[REFERENCE_SIZE];
    private int bagsize = 3;
    private int total = 100;
    public  WidgetUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        // these numbers are generated via this website:
        // http://www.quantitativeskills.com/sisa/distributions/hypghlp.htm

        id[1] = "overrepresented";
        taggedSample[1] = 3;
        taggedPopulation[1] = 20;
        expectedResults[1] = new BigDecimal(0.00705009276437833);

        id[2] = "notEnriched";
        taggedSample[2] = 1;
        taggedPopulation[2] = 10;
        expectedResults[2] = new BigDecimal(0.27346938775509510577421679045073688030242919921875);

        id[3] = "underrepresented";
        taggedSample[3] = 1;
        taggedPopulation[3] = 50;
        expectedResults[3] = new BigDecimal(0.8787878787878582);

        id[4] = "one";
        taggedSample[4] = 3;
        taggedPopulation[4] = 100;
        expectedResults[4] = new BigDecimal(1);

        for (int i = 1; i <= 4; i++) {

            BigDecimal p = new BigDecimal(Hypergeometric.calculateP(taggedSample[i], bagsize,
                    taggedPopulation[i], total));
            resultsMap.put(id[i], p);
        }
    }

    public void testHypergeometric() throws Exception {
        for (int i = 0; i < 4; i++) {
            assertEquals(expectedResults[i], resultsMap.get(id[i]));
        }
    }
}
