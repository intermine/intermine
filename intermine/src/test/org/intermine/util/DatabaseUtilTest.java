package org.intermine.util;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import org.intermine.sql.DatabaseFactory;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;

public class DatabaseUtilTest extends TestCase
{
    private Connection con;
    private String uri = "http://www.intermine.org/model/testmodel";

    public DatabaseUtilTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        con = DatabaseFactory.getDatabase("db.unittest").getConnection();
    }

    public void tearDown() throws Exception {
        con.close();
    }

    protected void createTable() throws Exception {
        try {
            dropTable();
        } catch (SQLException e) {
            con.rollback();
        }
        con.createStatement().execute("CREATE TABLE table1(col1 int)");
        con.commit();
    }

    protected void dropTable() throws Exception {
        con.createStatement().execute("DROP TABLE table1");
        con.commit();
    }

    public void testTableExistsNullConnection() throws Exception {
        try {
            DatabaseUtil.tableExists(null, "table1");
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testTableExistsNullTable() throws Exception {
        try {
            DatabaseUtil.tableExists(con, null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testTableExists() throws Exception {
        synchronized (con) {
            createTable();
            assertTrue(DatabaseUtil.tableExists(con, "table1"));
            dropTable();
        }
    }

    public void testTableNotExists() throws Exception {
        synchronized (con) {
            try {
                con.createStatement().execute("DROP TABLE table2");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
            }
            createTable();
            assertTrue(!(DatabaseUtil.tableExists(con, "table2")));
            dropTable();
        }
    }

    public void testGetTableNameOne() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model1 = new Model("test1", uri, new HashSet(Arrays.asList(new Object[] {cld})));

        assertEquals("Class1", DatabaseUtil.getTableName(cld));
    }

    public void testGetTableNameTwo() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("Array", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model1 = new Model("test1", uri, new HashSet(Arrays.asList(new Object[] {cld})));

        assertEquals("intermine_Array", DatabaseUtil.getTableName(cld));
    }

    public void testGetColumnName() throws Exception {
        FieldDescriptor attr = new AttributeDescriptor("attr1", "int");

        assertEquals(DatabaseUtil.generateSqlCompatibleName("attr1"), DatabaseUtil.getColumnName(attr));
    }

    public void testGetIndirectionTableNameRef() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "Class2", "ref1", false);
        Set cols = new HashSet(Arrays.asList(new Object[] {col1}));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), cols);

        ReferenceDescriptor ref1 = new ReferenceDescriptor("ref1", "Class1", null);
        Set refs = new HashSet(Arrays.asList(new Object[] {ref1}));
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), refs, new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("test", uri, clds);

        try {
            DatabaseUtil.getIndirectionTableName(col1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetIndirectionTableNameNull() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "Class2", null, false);
        Set cols = new HashSet(Arrays.asList(new Object[] {col1}));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), cols);

        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("test", uri, clds);

        assertEquals("Class1Col1", DatabaseUtil.getIndirectionTableName(col1));
        assertEquals("Col1", DatabaseUtil.getInwardIndirectionColumnName(col1));
        assertEquals("Class1", DatabaseUtil.getOutwardIndirectionColumnName(col1));
    }

    public void testGetIndirectionTableNameCol() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "Class2", "col2", false);
        Set cols = new HashSet(Arrays.asList(new Object[] {col1}));
        ClassDescriptor cld1 = new ClassDescriptor("Class1", null, false, new HashSet(), new HashSet(), cols);

        CollectionDescriptor col2 = new CollectionDescriptor("col2", "Class1", "col1", false);
        cols = new HashSet(Arrays.asList(new Object[] {col2}));
        ClassDescriptor cld2 = new ClassDescriptor("Class2", null, false, new HashSet(), new HashSet(), cols);

        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("test", uri, clds);

        assertEquals("Col1Col2", DatabaseUtil.getIndirectionTableName(col1));
        assertEquals("Col1", DatabaseUtil.getInwardIndirectionColumnName(col1));
        assertEquals("Col2", DatabaseUtil.getOutwardIndirectionColumnName(col1));
    }

    public void testGenerateSqlCompatibleName() throws Exception {
        assertEquals("intermine_end", DatabaseUtil.generateSqlCompatibleName("end"));
        assertEquals("intermine_intermine_end", DatabaseUtil.generateSqlCompatibleName("intermine_end"));
        assertEquals("id", DatabaseUtil.generateSqlCompatibleName("id"));
        assertEquals("index", DatabaseUtil.generateSqlCompatibleName("index"));
        assertEquals("intermine_order", DatabaseUtil.generateSqlCompatibleName("order"));
        assertEquals("intermine_full", DatabaseUtil.generateSqlCompatibleName("full"));
        assertEquals("intermine_offset", DatabaseUtil.generateSqlCompatibleName("offset"));
        assertEquals("some_string", DatabaseUtil.generateSqlCompatibleName("some_string"));

        try {
            DatabaseUtil.generateSqlCompatibleName(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
