package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;

public class DBReaderTestCase extends TestCase {
    protected Database db;

    public DBReaderTestCase(String arg) {
        super(arg);
    }

    public void testRead1() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
        Connection c = db.getConnection();
        c.setAutoCommit(true);
        Statement s = c.createStatement();
        try {
            s.execute("DROP TABLE testread");
        } catch (SQLException e) {
        }
        s.execute("CREATE TABLE testread (value int, id text)");
        for (int i = 1000; i < 2500; i++) {
            s.addBatch("INSERT INTO testread VALUES (" + i + ", '" + i + "')");
        }
        for (int i = 0; i < 1000; i++) {
            s.addBatch("INSERT INTO testread VALUES (" + i + ", '" + i + "')");
        }
        s.executeBatch();
        c.close();

        DBReader reader = getDBReader();

        Iterator iter = reader.sqlIterator("SELECT value, id FROM testread", "value");
        int v = 0;
        while (iter.hasNext()) {
            Map row = (Map) iter.next();
            assertEquals(new Integer(v), row.get("value"));
            assertEquals("" + v, row.get("id"));
            v++;
        }
        assertEquals(2500, v);

        List l = reader.execute("SELECT value, id FROM testread ORDER BY value");
        iter = l.iterator();
        v = 0;
        while (iter.hasNext()) {
            Map row = (Map) iter.next();
            assertEquals(new Integer(v), row.get("value"));
            assertEquals("" + v, row.get("id"));
            v++;
        }
        assertEquals(2500, v);
    }

    public DBReader getDBReader() {
        throw new UnsupportedOperationException();
    }
}
