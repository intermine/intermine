package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
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

public class DirectDBReaderTest extends DBReaderTestCase
{
    public DirectDBReaderTest(String arg) {
        super(arg);
    }

    public DBReader getDBReader() {
        return new DirectDBReader(db);
    }

    public void testRead2() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
        Connection c = db.getConnection();
        c.setAutoCommit(true);
        Statement s = c.createStatement();
        try {
            s.execute("DROP TABLE testread");
        } catch (SQLException e) {
        }
        s.execute("CREATE TABLE testread (value int, id text)");
        for (int i = 1202; i < 2500; i++) {
            s.addBatch("INSERT INTO testread VALUES (" + i + ", '" + i + "')");
        }
        for (int i = 0; i < 1200; i++) {
            s.addBatch("INSERT INTO testread VALUES (" + i + ", '" + i + "')");
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 250000; i++) {
            sb.append("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
        }
        s.addBatch("INSERT INTO testread VALUES (1200, '" + sb.toString() + "')");
        sb.append("abcdefghijklmnopqrstuvwxyz");
        s.addBatch("INSERT INTO testread VALUES (1201, '" + sb.toString() + "')");
        sb = null;
        s.executeBatch();
        c.close();

        DBReader reader = getDBReader();

        Iterator iter = reader.sqlIterator("SELECT value, id FROM testread", "value");
        int v = 0;
        while (iter.hasNext()) {
            Map row = (Map) iter.next();
            assertEquals(new Integer(v), row.get("value"));
            if (v == 1200) {
                assertEquals(13000000, ((String) row.get("id")).length());
            } else if (v == 1201) {
                assertEquals(13000026, ((String) row.get("id")).length());
            } else {
                assertEquals("" + v, row.get("id"));
            }
            int offset = ((DirectDBReader) reader).batch.getOffset();
            int size = ((DirectDBReader) reader).batch.getRows().size();
            if (v == 0) {
                assertEquals(0, offset);
                assertEquals(1, size);
            } else if ((v >= 1) && (v <= 1000)) {
                assertEquals(1, offset);
                assertEquals(1000, size);
            } else if ((v >= 1001) && (v <= 1200)) {
                assertEquals(1001, offset);
                assertEquals(200, size);
            } else if (v == 1201) {
                assertEquals(1201, offset);
                assertEquals(1, size);
            } else if ((v >= 1202) && (v <= 2201)) {
                assertEquals(1202, offset);
                assertEquals(1000, size);
            } else {
                assertEquals(2202, offset);
                assertEquals(298, size);
            }
            v++;
        }
        assertEquals(2500, v);
    }
}
