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
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import com.mockobjects.sql.MockSingleRowResultSet;
import com.mockobjects.sql.MockMultiRowResultSet;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.util.TypeUtil;
import org.flymine.xml.full.ItemHelper;

public class DBConverterTest extends TestCase {
    private Model model;
    private DBConverter converter;
    private ArrayList blank = new ArrayList();
    private Collection items = new ArrayList();
    private Map map = new HashMap();

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
        Database db = DatabaseFactory.getDatabase("db.unittest");
        converter = new MockDBConverter(model, db, new MockDBReader(), new MockItemWriter());
    }

    public void testAttribute() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        List rows = rowify(
                          new String[] {"Department_id", "name", "company_id", "manager_id"},
                          new Object[] {new Integer(12), "DepartmentA1", null, null});
        List blank = new ArrayList();

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Attribute attr = new Attribute();
        attr.setName("name");
        attr.setValue("DepartmentA1");
        item.addAttribute(attr);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testReference() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");
        
        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[] {new Integer(12), null, new Integer(14), null});
        
        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Reference ref = new Reference();
        ref.setName("company");
        ref.setRefId(converter.alias("Company") + "_14");
        item.addReference(ref);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void test1NCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[] {new Integer(12), null, null, null});
        
        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);

        List rows2 = rowify(
                            new String[] {"Employee_id"},
                            new Object[][] {{new Integer(1)}, {new Integer(2)}});

        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", rows2);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        ReferenceList refs = new ReferenceList();
        refs.setName("employees");
        refs.addRefId(converter.alias("Employee") + "_1");
        refs.addRefId(converter.alias("Employee") + "_2");
        item.addCollection(refs);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testMNCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");

        List rows = rowify(
                           new String[] {"Company_id", "name", "vatNumber", "cEO_id"},
                           new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Company", rows);

        map.put("SELECT Department_id FROM Department WHERE company_id = 12", blank);
        //map.put("SELECT Contractor_id FROM oldComs_oldContracts WHERE Company_id = 12", blank);

        List rows2 = rowify(
                            new String[] {"Contractor_id"},
                            new Object[][] {{new Integer(1)}, {new Integer(2)}});
        
        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 12", rows2);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Company");
        item.setIdentifier(converter.alias("Company") + "_12");
        ReferenceList refs = new ReferenceList();
        refs.setName("contractors");
        refs.addRefId(converter.alias("Contractor") + "_1");
        refs.addRefId(converter.alias("Contractor") + "_2");
        item.addCollection(refs);
        // this is a bit dodgy - problem is that DBConvertor doesn't handle named collections
        refs = new ReferenceList();
        refs.setName("oldContracts");
        refs.addRefId(converter.alias("Contractor") + "_1");
        refs.addRefId(converter.alias("Contractor") + "_2");
        item.addCollection(refs);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testUnidirectional() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Contractor");

        List rows = rowify(
                           new String[] {"Contractor_id", "personalAddress_id", "businessAddress_id"},
                           new Object[] {new Integer(12), new Integer(14), null});

        map.put("SELECT * FROM Contractor", rows);

        map.put("SELECT Company_id FROM Contractor_Company WHERE Contractor_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Contractor");
        item.setIdentifier(converter.alias("Contractor") + "_12");
        Reference ref = new Reference();
        ref.setName("personalAddress");
        ref.setRefId(converter.alias("Address") + "_14");
        item.addReference(ref);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singletonList(item), items);
    }

    public void testMultipleInstances() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Department");

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[][] {
                               {new Integer(12), "DepartmentA1", null, null},
                               {new Integer(13), "DepartmentA2", null, null}});

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 13", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 13", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Attribute attr = new Attribute();
        attr.setName("name");
        attr.setValue("DepartmentA1");
        item.addAttribute(attr);

        Item item2 = new Item();
        item2.setClassName(model.getNameSpace() + "Department");
        item2.setIdentifier(converter.alias("Department") + "_13");
        Attribute attr2 = new Attribute();
        attr2.setName("name");
        attr2.setValue("DepartmentA2");
        item2.addAttribute(attr2);

        converter.processClassDescriptor(cld);
        assertEquals(Arrays.asList(new Object[] {item, item2}), items);
    }

    public void testMultipleClasses() throws Exception {
        for (Iterator iter = model.getClassNames().iterator(); iter.hasNext();) {
            String clsName = TypeUtil.unqualifiedName((String) iter.next());
            map.put("SELECT * FROM " +clsName, blank);
        }

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[] {new Integer(12), "DepartmentA1", null, null});

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = new Item();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Attribute attribute = new Attribute();
        attribute.setName("name");
        attribute.setValue("DepartmentA1");
        item.addAttribute(attribute);

        List rows2 = rowify(
                            new String[] {"Company_id", "name", "vatNumber", "cEO_id"},
                            new Object[] {new Integer(13), null, null, null});

        map.put("SELECT * FROM Company", rows2);

        map.put("SELECT Department_id FROM Department WHERE company_id = 13", blank);
        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 13", blank);

        Item item2 = new Item();
        item2.setClassName(model.getNameSpace() + "Company");
        item2.setIdentifier(converter.alias("Company") + "_13");

        converter.process();
        assertEquals(Arrays.asList(new Object[] {item, item2}), items);
    }

    protected List rowify(String[] names, Object[] values) {
        Map map = new HashMap();
        for (int i=0; i < names.length; i++) {
            map.put(names[i], values[i]);
        }
        return Collections.singletonList(map);
    }

    protected List rowify(String[] names, Object[][] values) {
        List rows = new ArrayList();
        for (int i = 0; i < values.length; i++) {
            rows.add(rowify(names, values[i]).get(0));
        }
        return rows;
    }

    class MockItemWriter implements ItemWriter {
        public void store(org.flymine.model.fulldata.Item item) throws ObjectStoreException {
            items.add(ItemHelper.convert(item));
        }
        public void storeAll(Collection items) throws ObjectStoreException {}
        public void close() throws ObjectStoreException {}
    }

    class MockDBConverter extends DBConverter {
        public MockDBConverter(Model model, Database db, DBReader reader, ItemWriter writer) {
            super(model, db, reader, writer);
        }

        //we provide ids, return true here to save us having to create mock resultset metadata to prove it
        protected boolean idsProvided(ClassDescriptor cld) throws SQLException {
            return true;
        }

        protected String findIndirectionTable(String clsName, String otherClsName) throws SQLException {
            return clsName + "_" + otherClsName;
        }
    }

    class MockDBReader implements DBReader {
        public Iterator sqlIterator(String sql, String idField) {
            try {
                return execute(sql).iterator();
            } catch (SQLException e) {}
            return null;
        }
        public List execute(String sql) throws SQLException {
            return (List) map.get(sql);
        }
    }
}
