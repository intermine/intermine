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

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;

import com.mockobjects.sql.MockSingleRowResultSet;
import com.mockobjects.sql.MockMultiRowResultSet;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.util.TypeUtil;
import org.flymine.xml.full.Field;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.FullRenderer;

public class ChadoConvertorTest extends TestCase {
    private Map map;
    private Model model;
    private ChadoConvertor convertor;
    private Database db;
    private MockSingleRowResultSet blank;
    private Collection items;
    
    public void setUp() throws Exception {
        map = new HashMap();
        model = Model.getInstanceByName("testmodel");
        db = DatabaseFactory.getDatabase("db.unittest");
        items = new ArrayList();
        convertor = new MockChadoConvertor(model, db, items);
        blank = new MockSingleRowResultSet();
        blank.next();
    }

    public void testAttribute() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockSingleRowResultSet mrs = new MockSingleRowResultSet();
        mrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), "DepartmentA1", null, null});

        map.put("SELECT * FROM Department ORDER BY Department_id LIMIT 1", mrs);
        map.put("SELECT * FROM Department WHERE Department_id > 12 ORDER BY Department_id LIMIT 1", blank);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("name");
        field.setValue("DepartmentA1");
        item.addField(field);

        convertor.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testReference() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockSingleRowResultSet mrs = new MockSingleRowResultSet();
        mrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), null, new Integer(14), null});

        map.put("SELECT * FROM Department ORDER BY Department_id LIMIT 1", mrs);
        map.put("SELECT * FROM Department WHERE Department_id > 12 ORDER BY Department_id LIMIT 1", blank);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("company");
        field.setValue("14");
        item.addReference(field);

        convertor.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void test1NCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockSingleRowResultSet msrs = new MockSingleRowResultSet();
        msrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Department ORDER BY Department_id LIMIT 1", msrs);
        map.put("SELECT * FROM Department WHERE Department_id > 12 ORDER BY Department_id LIMIT 1", blank);

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

        convertor.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testMNCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");

        MockSingleRowResultSet msrs = new MockSingleRowResultSet();
        msrs.addExpectedNamedValues(
                                   new String[] {"Company_id", "name", "vatNumber", "cEO_id"},
                                   new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Company ORDER BY Company_id LIMIT 1", msrs);
        map.put("SELECT * FROM Company WHERE Company_id > 12 ORDER BY Company_id LIMIT 1", blank);

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

        convertor.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testUnidirectional() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Contractor");

        MockSingleRowResultSet msrs = new MockSingleRowResultSet();
        msrs.addExpectedNamedValues(
                                    new String[] {"Contractor_id", "personalAddress_id", "businessAddress_id"},
                                    new Object[] {new Integer(12), new Integer(14), null});

        map.put("SELECT * FROM Contractor ORDER BY Contractor_id LIMIT 1", msrs);
        map.put("SELECT * FROM Contractor WHERE Contractor_id > 12 ORDER BY Contractor_id LIMIT 1", blank);
        
        map.put("SELECT Company_id FROM Contractor_Company WHERE Contractor_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Contractor");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("personalAddress");
        field.setValue("14");
        item.addReference(field);

        convertor.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }
    
    public void testMultipleInstances() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        MockMultiRowResultSet mrs = new MockMultiRowResultSet();
        mrs.setupColumnNames(new String[] {"Department_id", "name", "company_id", "manager_id"});
        mrs.setupRows(new Object[][] {
            {new Integer(12), "DepartmentA1", null, null},
            {new Integer(13), "DepartmentA2", null, null}});

        map.put("SELECT * FROM Department ORDER BY Department_id LIMIT 1", mrs);
        map.put("SELECT * FROM Department WHERE Department_id > 12 ORDER BY Department_id LIMIT 1", mrs);
        map.put("SELECT * FROM Department WHERE Department_id > 13 ORDER BY Department_id LIMIT 1", mrs);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 13", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 13", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("name");
        field.setValue("DepartmentA1");
        item.addField(field);

        Item item2 = new Item();
        item2.setClassName(model.getNameSpace() + "Department");
        item2.setIdentifier("13");
        Field field2 = new Field();
        field2.setName("name");
        field2.setValue("DepartmentA2");
        item2.addField(field2);

        convertor.processClassDescriptor(cld);
        assertEquals(Arrays.asList(new Object[] {item, item2}), items);
    }

    public void testMultipleClasses() throws Exception {
        for (Iterator iter = model.getClassNames().iterator(); iter.hasNext();) {
            String clsName = TypeUtil.unqualifiedName((String) iter.next());
            map.put("SELECT * FROM " +clsName + " ORDER BY " + clsName + "_id LIMIT 1", blank);
        }

        MockSingleRowResultSet mrs = new MockSingleRowResultSet();
        mrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), "DepartmentA1", null, null});

        map.put("SELECT * FROM Department ORDER BY Department_id LIMIT 1", mrs);
        map.put("SELECT * FROM Department WHERE Department_id > 12 ORDER BY Department_id LIMIT 1", mrs);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("name");
        field.setValue("DepartmentA1");
        item.addField(field);

        MockSingleRowResultSet mrs2 = new MockSingleRowResultSet();
        mrs2.addExpectedNamedValues(
                                   new String[] {"Company_id", "name", "vatNumber", "cEO_id"},
                                   new Object[] {new Integer(13), null, null, null});

        map.put("SELECT * FROM Company ORDER BY Company_id LIMIT 1", mrs2);
        map.put("SELECT * FROM Company WHERE Company_id > 13 ORDER BY Company_id LIMIT 1", mrs2);

        map.put("SELECT Department_id FROM Department WHERE company_id = 13", blank);
        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 13", blank);

        Item item2 = new Item();
        item2.setClassName(model.getNameSpace() + "Company");
        item2.setIdentifier("13");

        convertor.process();
        assertEquals(Arrays.asList(new Object[] {item, item2}), items); 
    }

    public void testProcessWriter() throws Exception {
        for (Iterator iter = model.getClassNames().iterator(); iter.hasNext();) {
            String clsName = TypeUtil.unqualifiedName((String) iter.next());
            map.put("SELECT * FROM " +clsName + " ORDER BY " + clsName + "_id LIMIT 1", blank);
        }

        MockSingleRowResultSet mrs = new MockSingleRowResultSet();
        mrs.addExpectedNamedValues(
                                   new String[] {"Department_id", "name", "company_id", "manager_id"},
                                   new Object[] {new Integer(12), "DepartmentA1", null, null});

        map.put("SELECT * FROM Department ORDER BY Department_id LIMIT 1", mrs);
        map.put("SELECT * FROM Department WHERE Department_id > 12 ORDER BY Department_id LIMIT 1", mrs);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier("12");
        Field field = new Field();
        field.setName("name");
        field.setValue("DepartmentA1");
        item.addField(field);

        MockSingleRowResultSet mrs2 = new MockSingleRowResultSet();
        mrs2.addExpectedNamedValues(
                                   new String[] {"Company_id", "name", "vatNumber", "cEO_id"},
                                   new Object[] {new Integer(13), null, null, null});

        map.put("SELECT * FROM Company ORDER BY Company_id LIMIT 1", mrs2);
        map.put("SELECT * FROM Company WHERE Company_id > 13 ORDER BY Company_id LIMIT 1", mrs2);

        map.put("SELECT Department_id FROM Department WHERE company_id = 13", blank);
        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 13", blank);

        Item item2 = new Item();
        item2.setClassName(model.getNameSpace() + "Company");
        item2.setIdentifier("13");

        convertor = new MockChadoConvertor(model, db, null);
        StringWriter sw = new StringWriter();
        convertor.process(sw);
        assertEquals(FullRenderer.render(Arrays.asList(new Object[] {item, item2})), sw.toString()); 
    }

    class MockChadoConvertor extends ChadoConvertor {
        protected Collection items;

        public MockChadoConvertor(Model model, Database db, Collection items) {
            super(model, db);
            this.items = items;
        }

        protected void processItem(Item item) throws IOException {
            if (items == null) {
                super.processItem(item);
            } else {
                items.add(item);
            }
        }

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
