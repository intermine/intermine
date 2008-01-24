package org.intermine.web.logic;

import java.util.HashMap;

import org.intermine.objectstore.query.ResultsRow;

import org.intermine.web.logic.widget.Hypergeometric;

import junit.framework.TestCase;

public class WebUtilTest extends TestCase
{
    private Double maxValue = 1000.0;
    private HashMap resultsMap = new HashMap();
    private String[] id = new String[3];
    private int[] taggedSample = new int[3];
    private int[] taggedPopulation = new int[3];
    private double[] expectedResults = new double[3];
    private int bagsize = 3;
    private int total = 100;
    HashMap<String, Double> bonferroniMap = new HashMap<String, Double>();
    HashMap<String, Double> benjaminiMap = new HashMap<String, Double>();
        
    public  WebUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        
        // these numbers are generated via this website:
        // http://www.quantitativeskills.com/sisa/distributions/hypghlp.htm
       
        id[0] = "notEnriched";
        taggedSample[0] = 1;
        taggedPopulation[0] = 10;
        expectedResults[0] = 0.2734693877550951;

        id[1] = "underrepresented";
        taggedSample[1] = 1;
        taggedPopulation[1] = 50;
        expectedResults[1] = 0.8787878787878582;
        
        id[2] = "overrepresented";
        taggedSample[2] = 3;
        taggedPopulation[2] = 20;
        expectedResults[2] = 0.00705009276437833;
        
        Hypergeometric h = new Hypergeometric(total);
        for (int i = 0; i < 3; i++) {
            double p = h.calculateP(bagsize, taggedSample[i], taggedPopulation[i], total);
            resultsMap.put(id[i], new Double(p));
            bonferroniMap.put(id[i], p * bagsize);
            benjaminiMap.put(id[i], new Double(p));
        }  
    }

    public void testHypergeometric() throws Exception {
        Hypergeometric h = new Hypergeometric(total);
        for (int i = 0; i < 3; i++) {            
            double p = (Double) resultsMap.get(id[i]);
            assertEquals(expectedResults[i], p);
        }        
    }
    
    public void testBonferroni() throws Exception {
        HashMap adjustedMap = WebUtil.calcErrorCorrection("Bonferroni", maxValue, bagsize, resultsMap);
        assertEquals(bonferroniMap, adjustedMap);
    }

    public void testBenjaminiHochberg() throws Exception {
        
        // largest p-value doesn't get adjusted
        // id[1] = "underrepresented";
        
        double p = (Double) resultsMap.get(id[0]);
        p = p * (bagsize / (bagsize -1) );
        benjaminiMap.put(id[0], p);

        p = (Double) resultsMap.get(id[2]);
        p = p * (bagsize / (bagsize -2) );
        benjaminiMap.put(id[2], p);
        
        HashMap adjustedMap = WebUtil.calcErrorCorrection("BenjaminiHochberg", maxValue, bagsize, resultsMap);
        assertEquals(benjaminiMap, adjustedMap);
    }
}
