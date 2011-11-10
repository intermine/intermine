package org.intermine.web.logic;

import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.commons.math.distribution.HypergeometricDistributionImpl;

import junit.framework.TestCase;

public class WidgetUtilTest extends TestCase
{
    private final int REFERENCE_SIZE = 5;
    private HashMap<String, BigDecimal> resultsMap = new HashMap();
    private String[] id = new String[REFERENCE_SIZE];
    private int[] taggedSample = new int[REFERENCE_SIZE];
    private int[] taggedPopulation = new int[REFERENCE_SIZE];
    private BigDecimal[] expectedResults = new BigDecimal[REFERENCE_SIZE];
    private int bagsize = 100;
    private int total = 1000;
    public  WidgetUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        id[1] = "half";
        taggedSample[1] = 50;
        taggedPopulation[1] = 500;
        expectedResults[1] = new BigDecimal(0.54194604604639440292856988889980129897594451904296875);

        id[2] = "notEnriched";
        taggedSample[2] = 1;
        taggedPopulation[2] = 10;
        expectedResults[2] = new BigDecimal(0.6530722852079744455977561301551759243011474609375);

        id[3] = "enriched";
        taggedSample[3] = 25;
        taggedPopulation[3] = 100;
        expectedResults[3] = new BigDecimal(0.0000031931122567314319696078761534607792782480828464031219482421875);

        id[4] = "one";
        taggedSample[4] = 3;
        taggedPopulation[4] = 100;
        expectedResults[4] = new BigDecimal(1);

        HypergeometricDistributionImpl h = new HypergeometricDistributionImpl(total,bagsize,bagsize);

        for (int i = 1; i <= 4; i++) {
            h.setNumberOfSuccesses(taggedPopulation[i]);
            BigDecimal p = new BigDecimal(h.upperCumulativeProbability(taggedSample[i]));
            resultsMap.put(id[i], p);
        }
    }

    public void testHypergeometric() throws Exception {
        for (int i = 0; i < 4; i++) {
            assertEquals(expectedResults[i], resultsMap.get(id[i]));
        }
    }
}
