package org.intermine.web.logic;

import java.util.HashMap;

import org.intermine.objectstore.query.ResultsRow;

import org.intermine.web.logic.widget.Hypergeometric;

import junit.framework.TestCase;

public class WebUtilTest extends TestCase
{
    private Double maxValue = 1.0;
    private HashMap resultsMap = new HashMap();
    private String[] id = new String[3];
    private int[] taggedSample = new int[3];
    private int[] taggedPopulation = new int[3];
    private double[] expectedResults = new double[3];
    private int bagsize = 10;
    private int total = 100;

    public  WebUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        id[0] = "notEnriched";
        taggedSample[0] = 1;
        taggedPopulation[0] = 10;
        expectedResults[0] = 0.6695237889132329;

        id[1] = "underrepresented";
        taggedSample[1] = 1;
        taggedPopulation[1] = 50;
        expectedResults[1] = 0.9994065803273947;

        id[2] = "overrepresented";
        taggedSample[2] = 10;
        taggedPopulation[2] = 10;
        expectedResults[2] = 5.776904234533623E-14;
    }

    public void testHypergeometric() throws Exception {

        Hypergeometric h = new Hypergeometric(total);
        HashMap<String, Double> resultsMap = new HashMap<String, Double>();

        for (int i = 0; i < 3; i++) {
            double p = h.calculateP(bagsize, taggedSample[i], taggedPopulation[i], total);
            resultsMap.put(id[i], new Double(p));

            assertEquals(expectedResults[i], p);
        }
    }

    public void testBonferroni() throws Exception {
        WebUtil.calcErrorCorrection("Bonferroni", maxValue, resultsMap);
    }

    public void testBenjaminiHochberg() throws Exception {
        WebUtil.calcErrorCorrection("BenjaminiHochberg", maxValue, resultsMap);
    }
}
