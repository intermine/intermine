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

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;

public class DBReaderTestCase extends TestCase {
    protected Database db;

    public DBReaderTestCase(String arg) {
        super(arg);
    }

    public void testRead1() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
        Connection c = db.getConnection();
        c.setAutoCommit(false);
        Statement s = c.createStatement();
        try {
            s.execute("DROP TABLE testread");
        } catch (SQLException e) {
            //e.printStackTrace(System.out);
            c.rollback();
        }
        try {
            s.execute("DROP TABLE company_contractor");
        } catch (SQLException e) {
            //e.printStackTrace(System.out);
            c.rollback();
        }
        s.execute("CREATE TABLE testread (value int, id text)");
        s.execute("CREATE TABLE company_contractor (company_id int, contractor_id int)");
        c.commit();
        Batch batch = new Batch(new BatchWriterPostgresCopyImpl());
        String colNames[] = new String[2];
        colNames[0] = "value";
        colNames[1] = "id";
        for (int i = 1000; i < 2500; i++) {
            batch.addRow(c, "testread", null, colNames, new Object[] {new Integer(i), "" + i});
        }
        batch.flush(c);
        for (int i = 0; i < 1000; i++) {
            batch.addRow(c, "testread", null, colNames, new Object[] {new Integer(i), "" + i});
        }
        batch.flush(c);
        colNames[0] = "company_id";
        colNames[1] = "contractor_id";
        for (int i = 0; i < 2500; i++) {
            Integer iInteger = new Integer(i);
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 587632413)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 876324215)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 254487695)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 257876345)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 131256998)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 963575833)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 775434512)});
            batch.addRow(c, "company_contractor", null, colNames, new Object[] {iInteger, new Integer(i * 642134545)});
        }
        batch.close(c);
        c.commit();
        try {
            s.execute("DROP TABLE company_secretary");
        } catch (SQLException e) {
            c.rollback();
        }
        s.execute("CREATE TABLE company_secretary (company_id int, secretary_id int)");
        c.commit();
        c.close();

        DBReader reader = getDBReader();

        Iterator iter = reader.sqlIterator("SELECT value, id FROM testread", "value", "Company");
        int v = 0;
        while (iter.hasNext()) {
            Map row = (Map) iter.next();
            assertEquals(new Integer(v), row.get("value"));
            assertEquals("" + v, row.get("id"));
            List l = reader.execute("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = " + v);
            Set expected = new HashSet();
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 587632413)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 876324215)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 254487695)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 257876345)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 131256998)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 963575833)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 775434512)));
            expected.add(Collections.singletonMap("contractor_id", new Integer(v * 642134545)));
            assertEquals(expected, new HashSet(l));
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
        reader.close();
    }

    public DBReader getDBReader() throws Exception {
        throw new UnsupportedOperationException();
    }
}
