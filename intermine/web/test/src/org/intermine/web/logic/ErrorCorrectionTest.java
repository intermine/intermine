package org.intermine.web.logic;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.intermine.web.logic.widget.ErrorCorrection;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Julie Sullivan
 *
 */
public class ErrorCorrectionTest
{
    private final static int REFERENCE_SIZE = 5000;
    private static final Double max = new Double(1.0);

    private static Map<String, BigDecimal> pvalues;
    private static Map<String, String> bonferroni, benjamini, bonferroniHolm;

    @Before
    public void setUpMaps() {
        pvalues = new LinkedHashMap<String, BigDecimal>();
        bonferroni = new LinkedHashMap<String, String>();
        benjamini = new LinkedHashMap<String, String>();
        bonferroniHolm = new LinkedHashMap<String, String>();

        pvalues.put("A", new BigDecimal(0.0000000000000000000099));
        pvalues.put("B", new BigDecimal(0.00000000001));
        pvalues.put("C", new BigDecimal(0.000001));
        pvalues.put("D", new BigDecimal(1));

        bonferroni.put("A", "0.00000000000000004950000000000000322838637474085944");
        bonferroni.put("B", "0.00000004999999999999999697484846409699055");
        bonferroni.put("C", "0.004999999999999999773740559129431293");
        bonferroni.put("D", "1");

        bonferroniHolm.put("A", "0.00000000000000004950000000000000322838637474085944");
        bonferroniHolm.put("B", "0.00000004998999999999999697545349440417115");
        bonferroniHolm.put("C", "0.004997999999999999773831062905779521");
        bonferroniHolm.put("D", "1");

        benjamini.put("A", "0.00000000000000004950000000000000322838637474085944");
        benjamini.put("B", "0.00000002499999999999999848742423204849527");
        benjamini.put("C", "0.001666666666666666591246853043143765");
        benjamini.put("D", "1");
    }

    @Test
    public void testBonferroni() throws Exception {
        Map<String, BigDecimal> actual = ErrorCorrection.adjustPValues(
                ErrorCorrection.Strategy.BONFERRONI, pvalues, max, REFERENCE_SIZE);
        checkValues(actual, bonferroni);
    }

    @Test
    public void testBenjaminiHochberg() throws Exception {
        Map<String, BigDecimal> actual = ErrorCorrection.adjustPValues(
                ErrorCorrection.Strategy.BENJAMINI_HOCHBERG, pvalues, max, REFERENCE_SIZE);
        checkValues(actual, benjamini);
    }

    @Test
    public void testBonferroniHolm() throws Exception {
        Map<String, BigDecimal> actual = ErrorCorrection.adjustPValues(
                ErrorCorrection.Strategy.HOLM_BONFERRONI, pvalues, max, REFERENCE_SIZE);
        checkValues(actual, bonferroniHolm);
    }

    private void checkValues(Map<String, BigDecimal> actual, Map<String, String> expected) {
        for (Map.Entry<String, BigDecimal> entry : actual.entrySet()) {
            String adjustedPvalue = entry.getValue().toPlainString();
            String key = entry.getKey();
            assertEquals(expected.get(key), adjustedPvalue);
        }
    }

}
