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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

import com.mockobjects.sql.MockSingleRowResultSet;
import com.mockobjects.sql.MockMultiRowResultSet;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.xml.full.Field;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.ReferenceList;

public class ChadoConvertorTest extends TestCase {
    private Map map;
    private Model model;
    private ChadoConvertor convertor;
    private Database db;
    
    public void setUp() throws Exception {
        map = new HashMap();
        model = Model.getInstanceByName("testmodel");
        convertor = new MockChadoConvertor();
        db = DatabaseFactory.getDatabase("db.unittest");
    }

    public void testAttribute() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockSingleRowResultSet mrs = new MockSingleRowResultSet();
        mrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), "DepartmentA1", null, null});

        map.put("SELECT * FROM Department", mrs);

        MockSingleRowResultSet blank = new MockSingleRowResultSet();
        blank.next();

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("name");
        field.setValue("DepartmentA1");
        item.addField(field);

        assertEquals(Collections.singletonList(item), convertor.processClassDescriptor(cld));
    }

    public void testReference() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockSingleRowResultSet mrs = new MockSingleRowResultSet();
        mrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), null, new Integer(14), null});

        map.put("SELECT * FROM Department", mrs);

        MockSingleRowResultSet blank = new MockSingleRowResultSet();
        blank.next();

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("company");
        field.setValue("14");
        item.addReference(field);

        assertEquals(Collections.singletonList(item), convertor.processClassDescriptor(cld));
    }

    public void test1NCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockSingleRowResultSet msrs = new MockSingleRowResultSet();
        msrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Department", msrs);

        MockSingleRowResultSet blank = new MockSingleRowResultSet();
        blank.next();

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);

        MockMultiRowResultSet mmrs = new MockMultiRowResultSet();
        mmrs.setupColumnNames(new String[] {"Employee_id"});
        mmrs.setupRows(new Object[][] {{new Integer(1)}, {new Integer(2)}});

        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", mmrs);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        ReferenceList refs = new ReferenceList();
        refs.setName("employees");
        refs.addValue("1");
        refs.addValue("2");
        item.addCollection(refs);

        assertEquals(Collections.singletonList(item), convertor.processClassDescriptor(cld));
    }

    public void testMNCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");

        MockSingleRowResultSet msrs = new MockSingleRowResultSet();
        msrs.addExpectedNamedValues(
                                   new String[] {"Company_id", "name", "vatNumber", "cEO_id"},
                                   new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Company", msrs);

        MockSingleRowResultSet blank = new MockSingleRowResultSet();
        blank.next();

        map.put("SELECT Department_id FROM Department WHERE company_id = 12", blank);
        //map.put("SELECT Contractor_id FROM oldComs_oldContracts WHERE Company_id = 12", blank);

        MockMultiRowResultSet mmrs = new MockMultiRowResultSet();
        mmrs.setupColumnNames(new String[] {"Contractor_id"});
        mmrs.setupRows(new Object[][] {{new Integer(1)}, {new Integer(2)}});

        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 12", mmrs);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Company");
        item.setIdentifier("12");
        ReferenceList refs = new ReferenceList();
        refs.setName("contractors");
        refs.addValue("1");
        refs.addValue("2");
        item.addCollection(refs);

        assertEquals(Collections.singletonList(item), convertor.processClassDescriptor(cld));
    }

    public void testUnidirectional() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Contractor");

        MockSingleRowResultSet msrs = new MockSingleRowResultSet();
        msrs.addExpectedNamedValues(
                                    new String[] {"Contractor_id", "personalAddress_id", "businessAddress_id"},
                                    new Object[] {new Integer(12), new Integer(14), null});

        map.put("SELECT * FROM Contractor", msrs);

        MockSingleRowResultSet blank = new MockSingleRowResultSet();
        blank.next();
        
        map.put("SELECT Company_id FROM Contractor_Company WHERE Contractor_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Contractor");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("personalAddress");
        field.setValue("14");
        item.addReference(field);

        assertEquals(Collections.singletonList(item), convertor.processClassDescriptor(cld));
    }

    class MockChadoConvertor extends ChadoConvertor {
        protected ResultSet executeQuery(Connection c, String sql) throws SQLException {
            System.out.println(sql);
            ResultSet rs = (ResultSet) map.get(sql);
            if (rs == null) {
                throw new RuntimeException("SQL string not found in ResultSet map: " + sql);
            }
            return rs;
        }
        protected String findIndirectionTable(String clsName, String otherClsName) throws SQLException {
            return clsName + "_" + otherClsName;
        }
    }
}
