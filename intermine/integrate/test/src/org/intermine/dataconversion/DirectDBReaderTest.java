package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;

public class DirectDBReaderTest extends DBReaderTestCase
{
    public DirectDBReaderTest(String arg) {
        super(arg);
    }

    public DBReader getDBReader() throws Exception {
        return new DirectDBReader(db);
    }

    public void testRead2() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
        Connection c = db.getConnection();
        c.setAutoCommit(false);
        try {
            Statement s = c.createStatement();
            try {
                s.execute("DROP TABLE testread");
            } catch (SQLException e) {
                c.rollback();
            }
            s.execute("CREATE TABLE testread (value int, id text)");
            Batch batch = new Batch(new BatchWriterPostgresCopyImpl());
            String colNames[] = new String[] {"value", "id"};
            for (int i = 20202; i < 21500; i++) {
                batch.addRow(c, "testread", null, colNames, new Object[] {new Integer(i), "" + i});
            }
            for (int i = 0; i < 20200; i++) {
                batch.addRow(c, "testread", null, colNames, new Object[] {new Integer(i), "" + i});
            }
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 250000; i++) {
                sb.append("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
            }
            batch.addRow(c, "testread", null, colNames, new Object[] {new Integer(20200), sb.toString()});
            sb.append("abcdefghijklmnopqrstuvwxyz");
            batch.addRow(c, "testread", null, colNames, new Object[] {new Integer(20201), sb.toString()});
            sb = null;
            batch.close(c);
            c.commit();

            DBReader reader = getDBReader();

            Iterator iter = reader.sqlIterator("SELECT value, id FROM testread", "value", "testread");
            int v = 0;
            while (iter.hasNext()) {
                Map row = (Map) iter.next();
                assertEquals(new Integer(v), row.get("value"));
                if (v == 20200) {
                    assertEquals(13000000, ((String) row.get("id")).length());
                } else if (v == 20201) {
                    assertEquals(13000026, ((String) row.get("id")).length());
                } else {
                    assertEquals("" + v, row.get("id"));
                }
                int offset = ((DirectDBReader) reader).batch.getOffset();
                int size = ((DirectDBReader) reader).batch.getRows().size();
                if (v == 0) {
                    assertEquals(0, offset);
                    assertEquals(1, size);
                } else if ((v >= 1) && (v <= 20000)) {
                    assertEquals(1, offset);
                    assertEquals(20000, size);
                } else if ((v >= 20001) && (v <= 20200)) {
                    assertEquals(20001, offset);
                    assertEquals(200, size);
                } else if (v == 20201) {
                    assertEquals(20201, offset);
                    assertEquals(1, size);
                } else {
                    assertEquals(20202, offset);
                    assertEquals(1298, size);
                }
                v++;
            }
            assertEquals(21500, v);
        } finally {
            if (c != null) {
                try {
                    c.createStatement().execute("DROP TABLE testread");
                } catch (SQLException e) {
                    e.printStackTrace(System.out);
                    c.rollback();
                }
                c.close();
            }
        }
    }
}
