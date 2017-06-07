package org.intermine.sql;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.util.DynamicUtil;

public class DatabaseUtilTest extends TestCase
{
    private Connection con;
    private Database db;

    public DatabaseUtilTest(String arg1) {
        super(arg1);
    }

    @Override
    public void setUp() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
        con = db.getConnection();
        con.setAutoCommit(true);
    }

    @Override
    public void tearDown() throws Exception {
        con.close();
    }

    protected void createTable() throws Exception {
        try {
            dropTable();
        } catch (SQLException e) {
        }
        con.createStatement().execute("CREATE TABLE table1(col1 int)");
    }

    protected void dropTable() throws Exception {
        con.createStatement().execute("DROP TABLE table1");
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
            } catch (SQLException e) {
            }
            createTable();
            assertTrue(!(DatabaseUtil.tableExists(con, "table2")));
            dropTable();
        }
    }

    public void testGetTableNameOne() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model1 = new Model("test1", "package.name", new HashSet(Arrays.asList(new Object[] {cld})));

        assertEquals("Class1", DatabaseUtil.getTableName(cld));
    }

    public void testGetTableNameTwo() throws Exception {
        ClassDescriptor cld = new ClassDescriptor("package.name.Array", null, false, new HashSet(), new HashSet(), new HashSet());

        Model model1 = new Model("test1", "package.name", new HashSet(Arrays.asList(new Object[] {cld})));

        assertEquals("intermine_Array", DatabaseUtil.getTableName(cld));
    }

    public void testGetColumnName() throws Exception {
        FieldDescriptor attr = new AttributeDescriptor("attr1", "int");

        assertEquals(DatabaseUtil.generateSqlCompatibleName("attr1"), DatabaseUtil.getColumnName(attr));
    }

    public void testGetIndirectionTableNameRef() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "package.name.Class2", "ref1");
        Set cols = new HashSet(Arrays.asList(new Object[] {col1}));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), cols);

        ReferenceDescriptor ref1 = new ReferenceDescriptor("ref1", "package.name.Class1", null);
        Set refs = new HashSet(Arrays.asList(new Object[] {ref1}));
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), refs, new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("test", "package.name", clds);

        try {
            DatabaseUtil.getIndirectionTableName(col1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetIndirectionTableNameNull() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "package.name.Class2", null);
        Set cols = new HashSet(Arrays.asList(new Object[] {col1}));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), cols);

        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), new HashSet());

        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("test", "package.name", clds);

        assertEquals("Class1Col1", DatabaseUtil.getIndirectionTableName(col1));
        assertEquals("Col1", DatabaseUtil.getInwardIndirectionColumnName(col1, 0));
        assertEquals("Class1", DatabaseUtil.getOutwardIndirectionColumnName(col1, 0));
        assertEquals("Class1", DatabaseUtil.getInwardIndirectionColumnName(col1, 1));
        assertEquals("Col1", DatabaseUtil.getOutwardIndirectionColumnName(col1, 1));
    }

    public void testGetIndirectionTableNameCol() throws Exception {
        CollectionDescriptor col1 = new CollectionDescriptor("col1", "package.name.Class2", "col2");
        Set cols = new HashSet(Arrays.asList(new Object[] {col1}));
        ClassDescriptor cld1 = new ClassDescriptor("package.name.Class1", null, false, new HashSet(), new HashSet(), cols);

        CollectionDescriptor col2 = new CollectionDescriptor("col2", "package.name.Class1", "col1");
        cols = new HashSet(Arrays.asList(new Object[] {col2}));
        ClassDescriptor cld2 = new ClassDescriptor("package.name.Class2", null, false, new HashSet(), new HashSet(), cols);

        Set clds = new HashSet(Arrays.asList(new Object[] {cld1, cld2}));
        Model model = new Model("test", "package.name", clds);

        assertEquals("Col1Col2", DatabaseUtil.getIndirectionTableName(col1));
        assertEquals("Col1", DatabaseUtil.getInwardIndirectionColumnName(col1, 0));
        assertEquals("Col2", DatabaseUtil.getOutwardIndirectionColumnName(col1, 0));
        assertEquals("Col2", DatabaseUtil.getInwardIndirectionColumnName(col1, 1));
        assertEquals("Col1", DatabaseUtil.getOutwardIndirectionColumnName(col1, 1));
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

    public void testCreateBagTable() throws Exception {
        Collection bag = new HashSet();

        bag.add(new Integer(-10000));
        bag.add(new Integer(0));
        bag.add(new Integer(10000));
        bag.add(new Long(-10000));
        bag.add(new Long(0));
        bag.add(new Long(10000));
        bag.add(new Short((short) -10000));
        bag.add(new Short((short) 0));
        bag.add(new Short((short) 10000));
        bag.add(new BigDecimal(-10000.0));
        bag.add(new BigDecimal(0.0));
        bag.add(new BigDecimal(10000.0));
        bag.add(new Float(-10000.0));
        bag.add(new Float(0.0));
        bag.add(new Float(10000.0));
        bag.add(new Double(-10000.0));
        bag.add(new Double(0.0));
        bag.add(new Double(10000.0));
        bag.add(new String());
        bag.add(new String("a String with spaces"));
        bag.add(new String("123456"));
        bag.add(new String("123456.7"));
        bag.add(new Boolean(true));
        bag.add(new Boolean(false));
        bag.add(new Date(999999));
        bag.add(new Date(100));
        Employee employee = (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee.setId(new Integer(5000));
        bag.add(employee);
        Manager manager = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        manager.setId(new Integer(5001));
        bag.add(manager);
        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(new Integer(6000));
        bag.add(company);

        // this shouldn't appear in any table
        bag.add(BigInteger.ONE);

        DatabaseUtil.createBagTable(db, con, "integer_table", bag, Integer.class);
        DatabaseUtil.createBagTable(db, con, "long_table", bag, Long.class);
        DatabaseUtil.createBagTable(db, con, "short_table", bag, Short.class);
        DatabaseUtil.createBagTable(db, con, "bigdecimal_table", bag, BigDecimal.class);
        DatabaseUtil.createBagTable(db, con, "float_table", bag, Float.class);
        DatabaseUtil.createBagTable(db, con, "double_table", bag, Double.class);
        DatabaseUtil.createBagTable(db, con, "string_table", bag, String.class);
        DatabaseUtil.createBagTable(db, con, "boolean_table", bag, Boolean.class);
        DatabaseUtil.createBagTable(db, con, "date_table", bag, Date.class);
        DatabaseUtil.createBagTable(db, con, "intermineobject_table", bag, InterMineObject.class);
        DatabaseUtil.createBagTable(db, con, "employee_table", bag, Employee.class);


        Statement s = con.createStatement();
        ResultSet r = s.executeQuery("SELECT value FROM integer_table");
        Set result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        Set expected = new HashSet();
        expected.add(new Integer(-10000));
        expected.add(new Integer(0));
        expected.add(new Integer(10000));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM long_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new Long(-10000));
        expected.add(new Long(0));
        expected.add(new Long(10000));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM short_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new Integer((short) -10000));
        expected.add(new Integer((short) 0));
        expected.add(new Integer((short) 10000));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM double_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new Double(-10000.0));
        expected.add(new Double(0.));
        expected.add(new Double(10000.0));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM float_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new Float(-10000.0));
        expected.add(new Float(0.));
        expected.add(new Float(10000.0));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM string_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new String());
        expected.add(new String("a String with spaces"));
        expected.add(new String("123456"));
        expected.add(new String("123456.7"));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM boolean_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new Boolean(true));
        expected.add(new Boolean(false));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM date_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(new Long(999999));
        expected.add(new Long(100));

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM intermineobject_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(employee.getId());
        expected.add(manager.getId());
        expected.add(company.getId());

        assertEquals(expected, result);

        r = s.executeQuery("SELECT value FROM employee_table");
        result = new HashSet();
        while (r.next()) {
            result.add(r.getObject(1));
        }

        expected = new HashSet();
        expected.add(employee.getId());
        expected.add(manager.getId());

        assertEquals(expected, result);
    }

    public void testColumnNameLegality() {

        assertFalse(DatabaseUtil.isLegalColumnName(null));
        assertFalse(DatabaseUtil.isLegalColumnName(""));
        assertFalse(DatabaseUtil.isLegalColumnName("FOO"));
        assertFalse(DatabaseUtil.isLegalColumnName("foo!"));
        assertFalse(DatabaseUtil.isLegalColumnName("foo; drop table userprofile;"));

        assertTrue(DatabaseUtil.isLegalColumnName("foo"));
        assertTrue(DatabaseUtil.isLegalColumnName("foo123"));
        assertTrue(DatabaseUtil.isLegalColumnName("foo_bar"));
        assertTrue(DatabaseUtil.isLegalColumnName("foo_bar_123"));
    }

    public void testAddColumn() throws Exception {
        createTable();

        try {
            DatabaseUtil.addColumn(con, "FOO", "bar", DatabaseUtil.Type.integer);
            fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            //
        }

        try {
            DatabaseUtil.addColumn(con, "table1", "BAR", DatabaseUtil.Type.integer);
            fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            //
        }

        DatabaseUtil.addColumn(con, "table1", "col1", DatabaseUtil.Type.integer);
        assertTrue(DatabaseUtil.columnExists(con, "table1", "col1"));

        assertFalse(DatabaseUtil.columnExists(con, "table1", "col2"));
        DatabaseUtil.addColumn(con, "table1", "col2", DatabaseUtil.Type.integer);
        assertTrue(DatabaseUtil.columnExists(con, "table1", "col2"));
        DatabaseUtil.updateColumnValue(db, "table1", "col2", 2);

        DatabaseUtil.addColumn(con, "table1", "col3", DatabaseUtil.Type.text);
        DatabaseUtil.updateColumnValue(db, "table1", "col3", "bar");

        dropTable();
    }


}
