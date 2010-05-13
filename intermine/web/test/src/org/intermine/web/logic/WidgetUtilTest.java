package org.intermine.web.logic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.web.logic.widget.Hypergeometric;
import org.intermine.web.logic.widget.WidgetUtil;

public class WidgetUtilTest extends TestCase
{
    private Double maxValue = new Double(1.0);
    private HashMap<String, BigDecimal> resultsMap = new HashMap();
    private String[] id = new String[4];
    private int[] taggedSample = new int[4];
    private int[] taggedPopulation = new int[4];
    private BigDecimal[] expectedResults = new BigDecimal[4];
    private int bagsize = 3;
    private int total = 100;
    private HashMap<String, BigDecimal> bonferroniMap = new HashMap();
    private LinkedHashMap<String, BigDecimal> benjaminiMap = new LinkedHashMap();
    private BigDecimal ONE = new BigDecimal(1);
    public  WidgetUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        // these numbers are generated via this website:
        // http://www.quantitativeskills.com/sisa/distributions/hypghlp.htm

        id[1] = "notEnriched";
        taggedSample[1] = 1;
        taggedPopulation[1] = 10;
        expectedResults[1] = new BigDecimal(0.27346938775509510577421679045073688030242919921875);

        id[2] = "underrepresented";
        taggedSample[2] = 1;
        taggedPopulation[2] = 50;
        expectedResults[2] = new BigDecimal(0.8787878787878582);

        id[0] = "overrepresented";
        taggedSample[0] = 3;
        taggedPopulation[0] = 20;
        expectedResults[0] = new BigDecimal(0.00705009276437833);

        id[3] = "one";
        taggedSample[3] = 3;
        taggedPopulation[3] = 100;
        expectedResults[3] = new BigDecimal(1);

        BigDecimal numberOfTests = new BigDecimal(id.length);
        BigDecimal alpha = new BigDecimal(0.05);
        BigDecimal alphaPerTest = alpha.divide(numberOfTests, MathContext.DECIMAL32);

        for (int i = 3; i >= 0; i--) {
            BigDecimal p = new BigDecimal(Hypergeometric.calculateP(taggedSample[i], bagsize, taggedPopulation[i], total));
            resultsMap.put(id[i], p);

            BigDecimal bonferroniP =  p.add(alphaPerTest);
            if (bonferroniP.compareTo(ONE) >= 0) {
                bonferroniP = ONE;
            }
            bonferroniMap.put(id[i], bonferroniP);

            //p-value * (n/ n - index)
            if (i == 3) {
                if (p.compareTo(ONE) >= 0) {
                    p = ONE;
                }

                // biggest one isn't changed
                benjaminiMap.put(id[i], p);

            } else {
                BigDecimal divisor = numberOfTests.subtract(new BigDecimal(i + 1));
                BigDecimal m = numberOfTests.divide(divisor, MathContext.DECIMAL128);
                BigDecimal adjustedP = p.multiply(m, MathContext.DECIMAL128);
                if (adjustedP.compareTo(ONE) >= 0) {
                    adjustedP = ONE;
                }
                benjaminiMap.put(id[i], adjustedP);
            }
        }
    }

    public void testHypergeometric() throws Exception {
        for (int i = 0; i < 4; i++) {
            assertEquals(expectedResults[i], resultsMap.get(id[i]));
        }
    }

    public void testBonferroni() throws Exception {
        Map<String, BigDecimal> adjustedMap = WidgetUtil.calcErrorCorrection("Bonferroni", maxValue, resultsMap);
        for (String label : bonferroniMap.keySet()) {
            assertEquals(bonferroniMap.get(label), adjustedMap.get(label));
        }
    }

    public void testBenjaminiHochberg() throws Exception {
       Map<String, BigDecimal> adjustedMap = WidgetUtil.calcErrorCorrection("BenjaminiHochberg", maxValue, resultsMap);
        for (String label : benjaminiMap.keySet()) {
            assertEquals(benjaminiMap.get(label), adjustedMap.get(label));
        }
    }
}
