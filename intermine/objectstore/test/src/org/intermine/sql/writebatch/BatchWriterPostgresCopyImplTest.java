package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * Test for doing tests on the BatchWriterPostgresCopyImpl.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterPostgresCopyImplTest extends BatchWriterTestCase
{
    public BatchWriterPostgresCopyImplTest(String arg) {
        super(arg);
    }

    public BatchWriter getWriter() {
        BatchWriterPostgresCopyImpl bw = new BatchWriterPostgresCopyImpl();
        bw.setThreshold(getThreshold());
        return bw;
    }

    /*
     * This test no longer works because we throttle analyses to once every ten minutes at most.
     *
    public void testAnalyseLargeTable() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        try {
            Statement s = con.createStatement();
            try {
                s.execute("DROP TABLE table1");
            } catch (SQLException e) {
                con.rollback();
            }
            s.addBatch("CREATE TABLE table1(key int, int4 int)");
            s.executeBatch();
            con.commit();
            s = null;
            BatchWriter writer = getWriter();
            Batch batch = new Batch(writer);
            String[] colNames = new String[] {"key", "int4"};
            for (int i = 0; i < 1010000; i++) {
                batch.addRow(con, "table1", new Integer(i), colNames, new Object[] {new Integer(i), new Integer(765234 * i)});
                if ((i == 490000) || (i == 240000) || (i == 115000)) {
                    batch.flush(con);
                }
            }
            batch.close(con);
            con.commit();
            s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT reltuples FROM pg_class WHERE relname = 'table1'");
            assertTrue(r.next());
            assertTrue("Expected rows to be > 1000000 and < 1020000 - was " + r.getFloat(1),
                    r.getFloat(1) > 1000000.0F && r.getFloat(1) < 1020000.0F);
            assertFalse(r.next());
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            while (e != null) {
                e.printStackTrace(pw);
                e = e.getNextException();
            }
            pw.flush();
            throw new Exception(sw.toString());
        } finally {
            try {
                Statement s = con.createStatement();
                s.execute("DROP TABLE table1");
                con.commit();
                con.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Exception e) {
            }
        }
    }
    */
}
