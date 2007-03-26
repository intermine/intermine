package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.intermine.sql.DatabaseFactory;

public class PostgresExplainResultTest extends TestCase
{
    private PostgresExplainResult er;

    public PostgresExplainResultTest(String arg1) {
        super(arg1);
    }

    public void testParseWarningString() throws Exception {
        er = new PostgresExplainResult();
        er.parseWarningString("Seq Scan on flibble  (cost=0.00..3.15 rows=2 width=4)\n"
                + "  SubPlan\n    ->  Seq Scan on flibble t  (cost=0.00..1.05 rows=1 width=4)\n");
        assertEquals(2, er.getRows());
        assertEquals(0, er.getStart());
        assertEquals(31, er.getComplete());
        assertEquals(4, er.getWidth());
        assertEquals(2, er.getEstimatedRows());
    }

    public void testParseLargeNumbers() throws Exception {
        er = new PostgresExplainResult();
        er.parseWarningString("Seq Scan on flibble  (cost=29878176587635462734.18..8976187635487623465.32 rows=4387651876534329817234 width=876197364987632987438764)\n"
                + "  SubPlan\n    ->  Seq Scan on flibble t  (cost=0.00..1.05 rows=1 width=4)\n");
        assertEquals(Long.MAX_VALUE, er.getStart());
        assertEquals(Long.MAX_VALUE, er.getComplete());
        assertEquals(Long.MAX_VALUE, er.getWidth());
        assertEquals(Long.MAX_VALUE, er.getEstimatedRows());
        assertEquals(Long.MAX_VALUE, er.getRows());
    }

    /*
    public void testConstructNullQuery() throws Exception {
        try {
            er = new PostgresExplainResult(null, DatabaseFactory.getDatabase("db.unittest").getConnection());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }*/

    public void testConstructNullConnection() throws Exception {
        try {
            er = new PostgresExplainResult(new Query(), null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    /*
    public void testParseNullString() throws Exception {
        try {
            er.parseWarningString(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }*/

    public void testParseEmptyString() throws Exception {
        er = new PostgresExplainResult();
        try {
            er.parseWarningString("");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testParseRubbishString() throws Exception {
        er = new PostgresExplainResult();
        try {
            er.parseWarningString("ljkhafjhaiue,mnz fgsdfes) haskfjsdf");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testParseCraftedString() throws Exception {
        er = new PostgresExplainResult();
        try {
            er.parseWarningString("Flibble Wotsit (cost=76222.25..3241.13 rows2kj"
                    + "hdaslkjasdf) alkjhfasfjasdfkjh");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testConstructNullPreparedStatement() throws Exception {
        er = new PostgresExplainResult();
        try {
            er = new PostgresExplainResult(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullWarningPreparedStatement() throws Exception {
        // pass in an sql statement without an EXPLAIN, should give no warnings
        Connection con = DatabaseFactory.getDatabase("db.unittest").getConnection();
        try {
            String sql = "select 1";
            PreparedStatement stmt = con.prepareStatement(sql);
            er = new PostgresExplainResult(stmt);
            fail("Expected: SQLException");
        } catch (SQLException e) {
        } catch (NullPointerException e) {
            fail("Expected SQLException but Null PointerException thrown");
        } finally {
            con.close();
        }
    }

}

