package org.flymine.sql.query;

import junit.framework.*;

public class PostgresExplainResultTest extends TestCase
{
    private PostgresExplainResult er;
    
    public PostgresExplainResultTest(String arg1) {
        super(arg1);
    }
    
    public void setUp() {
        er = new PostgresExplainResult();
    }

    public void testParseWarningString() throws Exception {
        er.parseWarningString("Seq Scan on flibble  (cost=0.00..3.15 rows=2 width=4)\n"
                + "  SubPlan\n    ->  Seq Scan on flibble t  (cost=0.00..1.05 rows=1 width=4)\n");
        assertEquals(2, er.getRows());
        assertEquals(0, er.getStart());
        assertEquals(315, er.getComplete());
        assertEquals(4, er.getWidth());
        assertEquals(2, er.getEstimatedRows());
    }

    public void testParseNullString() throws Exception {
        try {
            er.parseWarningString(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testParseEmptyString() throws Exception {
        try {
            er.parseWarningString("");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testParseRubbishString() throws Exception {
        try {
            er.parseWarningString("ljkhafjhaiue,mnz fgsdfes) haskfjsdf");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testParseCraftedString() throws Exception {
        try {
            er.parseWarningString("Flibble Wotsit (cost=76222.25..3241.13 rows2kj"
                    + "hdaslkjasdf) alkjhfasfjasdfkjh");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }
}
