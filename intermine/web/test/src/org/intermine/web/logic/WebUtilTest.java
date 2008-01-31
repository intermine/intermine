package org.intermine.web.logic;

import java.util.HashMap;
import java.util.Map;

import org.intermine.web.logic.widget.Hypergeometric;

import java.math.BigDecimal;

import junit.framework.TestCase;

public class WebUtilTest extends TestCase
{
    private Double maxValue = 1000.0;
    private HashMap<String, BigDecimal> resultsMap = new HashMap();
    private String[] id = new String[3];
    private int[] taggedSample = new int[3];
    private int[] taggedPopulation = new int[3];
    private BigDecimal[] expectedResults = new BigDecimal[3];
    private int bagsize = 3;
    private int total = 100;
    HashMap<String, BigDecimal> bonferroniMap = new HashMap();
    HashMap<String, BigDecimal> benjaminiMap = new HashMap();
        
    public  WebUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        
        // these numbers are generated via this website:
        // http://www.quantitativeskills.com/sisa/distributions/hypghlp.htm
       
        id[0] = "notEnriched";
        taggedSample[0] = 1;
        taggedPopulation[0] = 10;
        expectedResults[0] = new BigDecimal(0.27346938775509510577421679045073688030242919921875);

        id[1] = "underrepresented";
        taggedSample[1] = 1;
        taggedPopulation[1] = 50;
        expectedResults[1] = new BigDecimal(0.8787878787878582);
        
        id[2] = "overrepresented";
        taggedSample[2] = 3;
        taggedPopulation[2] = 20;
        expectedResults[2] = new BigDecimal(0.00705009276437833);
        
        Hypergeometric h = new Hypergeometric(total);
        for (int i = 0; i < 3; i++) {
            double p = h.calculateP(bagsize, taggedSample[i], taggedPopulation[i], total);
            resultsMap.put(id[i], new BigDecimal(p));
            bonferroniMap.put(id[i], new BigDecimal(p * bagsize));
            benjaminiMap.put(id[i], new BigDecimal(p));
        }  
    }

    public void testHypergeometric() throws Exception {
        Hypergeometric h = new Hypergeometric(total);
        for (int i = 0; i < 3; i++) {            
            assertEquals(expectedResults[i], resultsMap.get(id[i]));
        }        
    }
    
    public void testBonferroni() throws Exception {
        Map<String, BigDecimal> adjustedMap = WebUtil.calcErrorCorrection("Bonferroni", maxValue, resultsMap);
       
        // rounding issue
        for (String label : bonferroniMap.keySet()) {
            BigDecimal expected = bonferroniMap.get(label);
            BigDecimal actual = adjustedMap.get(label);            
            assert(expected.compareTo(actual) == 0);
        }      
        
    }

    public void testBenjaminiHochberg() throws Exception {
        
        // largest p-value doesn't get adjusted
        // id[1] = "underrepresented";
        
        double p = resultsMap.get(id[0]).doubleValue();
        p = p * (bagsize / (bagsize -1) );
        benjaminiMap.put(id[0], new BigDecimal(p));

        p = resultsMap.get(id[2]).doubleValue();
        p = p * (bagsize / (bagsize -2) );
        benjaminiMap.put(id[2], new BigDecimal(p));
        
       Map<String, BigDecimal> adjustedMap = WebUtil.calcErrorCorrection("BenjaminiHochberg", maxValue, resultsMap);
        
        // rounding issue
        for (String label : benjaminiMap.keySet()) {
            BigDecimal expected = benjaminiMap.get(label);
            BigDecimal actual = adjustedMap.get(label);            
            assert(expected.compareTo(actual) == 0);
        }        
    }
}
