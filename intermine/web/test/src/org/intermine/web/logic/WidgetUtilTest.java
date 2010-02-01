package org.intermine.web.logic;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.web.logic.widget.Hypergeometric;

public class WidgetUtilTest extends TestCase
{
    private Double maxValue = 1000.0;
    private HashMap<String, BigDecimal> resultsMap = new HashMap();
    private String[] id = new String[4];
    private int[] taggedSample = new int[4];
    private int[] taggedPopulation = new int[4];
    private BigDecimal[] expectedResults = new BigDecimal[4];
    private int bagsize = 3;
    private int total = 100;
    private HashMap<String, BigDecimal> bonferroniMap = new HashMap();
    private LinkedHashMap<String, BigDecimal> benjaminiMap = new LinkedHashMap();

    public  WidgetUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        // these numbers are generated via this website:
        // http://www.quantitativeskills.com/sisa/distributions/hypghlp.htm

        id[2] = "notEnriched";
        taggedSample[2] = 1;
        taggedPopulation[2] = 10;
        expectedResults[2] = new BigDecimal(0.27346938775509510577421679045073688030242919921875);

        id[1] = "underrepresented";
        taggedSample[1] = 1;
        taggedPopulation[1] = 50;
        expectedResults[1] = new BigDecimal(0.8787878787878582);

        id[3] = "overrepresented";
        taggedSample[3] = 3;
        taggedPopulation[3] = 20;
        expectedResults[3] = new BigDecimal(0.00705009276437833);

        id[0] = "one";
        taggedSample[0] = 3;
        taggedPopulation[0] = 100;
        expectedResults[0] = new BigDecimal(1);

        BigDecimal numberOfTests = new BigDecimal(id.length);
        BigDecimal alpha = new BigDecimal(0.05);
        BigDecimal alphaPerTest = alpha.divide(numberOfTests);
        
        MathContext mc = new MathContext(10, RoundingMode.HALF_EVEN);
        
        for (int i = 0; i < 4; i++) {
            BigDecimal p = new BigDecimal(Hypergeometric.calculateP(taggedSample[i], bagsize, taggedPopulation[i], total));
            resultsMap.put(id[i], p);            
            bonferroniMap.put(id[i], p.add(alphaPerTest)); 
            
            //p-value * (n/ n - index)
            if (i == 0) {
                // biggest one isn't changed
                benjaminiMap.put(id[i], p);
            } else {
                BigDecimal divisor = numberOfTests.subtract(new BigDecimal(i));
                BigDecimal m = numberOfTests.divide(divisor, mc);
                benjaminiMap.put(id[i], p.multiply(m));
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
