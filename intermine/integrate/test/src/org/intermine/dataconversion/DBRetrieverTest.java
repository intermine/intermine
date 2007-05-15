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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import junit.framework.TestCase;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

public class DBRetrieverTest extends TestCase {
    private Model model;
    private MockItemWriter itemWriter;
    private DBRetriever converter;
    private ArrayList blank;
    private Map map;
    private DBReader reader;
    private ItemFactory itemFactory = new ItemFactory();
    private String excludeList = "class1 Class2";

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
        Database db = DatabaseFactory.getDatabase("db.unittest");
        itemWriter = new MockItemWriter(new HashMap());
        reader = new MockDBReader();
        converter = new MockDBRetriever(model, db, reader, itemWriter, excludeList);
        blank = new ArrayList();
        map = new HashMap();
    }

    public void tearDown() throws Exception {
        reader.close();
    }

    public void testExcludeList() throws Exception {
        Set expected = new HashSet(Arrays.asList(new Object[] {"class1", "class2"}));
        assertEquals(expected, converter.excluded);
        assertTrue(converter.isClassExcluded("Class1"));
        assertFalse(converter.isClassExcluded("Class3"));

    }

    public void testAttribute() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");

        List rows = rowify(
                          new String[] {"Department_id", "name", "company_id", "manager_id"},
                          new Object[] {new Integer(12), "DepartmentA1", null, null});

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = itemFactory.makeItem();
        item.setIdentifier(converter.alias("Department") + "_12");
        item.setClassName(model.getNameSpace() + "Department");
        Attribute attr = new Attribute();
        attr.setName("name");
        attr.setValue("DepartmentA1");
        item.addAttribute(attr);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singleton(item), itemWriter.getItems());
    }

    public void testReference() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[] {new Integer(12), null, new Integer(14), null});

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = itemFactory.makeItem();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Reference ref = new Reference();
        ref.setName("company");
        ref.setRefId(converter.alias("Company") + "_14");
        item.addReference(ref);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singleton(item), itemWriter.getItems());
    }

    public void testReferenceZeroId() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[] {new Integer(12), null, new Integer(0), null});

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", blank);

        Item item = itemFactory.makeItem();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singleton(item), itemWriter.getItems());
    }

/*
 * This test is not relevant - 1N collections are ignored by the dataloader, so the DBRetriever doesn't have to bother with them.
    public void test1NCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 12", blank);

        List rows2 = rowify(
                            new String[] {"Employee_id"},
                            new Object[][] {{new Integer(1)}, {new Integer(2)}});

        map.put("SELECT Employee_id FROM Employee WHERE department_id = 12", rows2);

        Item item = itemFactory.makeItem();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        ReferenceList refs = new ReferenceList();
        refs.setName("employees");
        refs.addRefId(converter.alias("Employee") + "_1");
        refs.addRefId(converter.alias("Employee") + "_2");
        item.addCollection(refs);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singleton(item), itemWriter.getItems());
    }
    */

    public void testMNCollection() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");

        List rows = rowify(
                           new String[] {"Company_id", "name", "vatNumber", "CEO_id"},
                           new Object[] {new Integer(12), null, null, null});

        map.put("SELECT * FROM Company", rows);

        map.put("SELECT Department_id FROM Department WHERE company_id = 12", blank);
        //map.put("SELECT Contractor_id FROM oldComs_oldContracts WHERE Company_id = 12", blank);

        List rows2 = rowify(
                            new String[] {"Contractor_id"},
                            new Object[][] {{new Integer(1)}, {new Integer(2)}});

        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 12", rows2);

        Item item = itemFactory.makeItem();
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
        assertEquals(Collections.singleton(item), itemWriter.getItems());
    }

    public void testUnidirectional() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Contractor");

        List rows = rowify(
                           new String[] {"Contractor_id", "personalAddress_id", "businessAddress_id"},
                           new Object[] {new Integer(12), new Integer(14), null});

        map.put("SELECT * FROM Contractor", rows);

        map.put("SELECT Company_id FROM Contractor_Company WHERE Contractor_id = 12", blank);

        Item item = itemFactory.makeItem();
        item.setClassName(model.getNameSpace() + "Contractor");
        item.setIdentifier(converter.alias("Contractor") + "_12");
        Reference ref = new Reference();
        ref.setName("personalAddress");
        ref.setRefId(converter.alias("Address") + "_14");
        item.addReference(ref);

        converter.processClassDescriptor(cld);
        assertEquals(Collections.singleton(item), itemWriter.getItems());
    }

    public void testMultipleInstances() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");

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

        Item item = itemFactory.makeItem();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Attribute attr = new Attribute();
        attr.setName("name");
        attr.setValue("DepartmentA1");
        item.addAttribute(attr);

        Item item2 = itemFactory.makeItem();
        item2.setClassName(model.getNameSpace() + "Department");
        item2.setIdentifier(converter.alias("Department") + "_13");
        Attribute attr2 = new Attribute();
        attr2.setName("name");
        attr2.setValue("DepartmentA2");
        item2.addAttribute(attr2);

        converter.processClassDescriptor(cld);
        Set expected = new HashSet();
        expected.add(item);
        expected.add(item2);
        assertEquals(expected, itemWriter.getItems());
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

        Item item = itemFactory.makeItem();
        item.setClassName(model.getNameSpace() + "Department");
        item.setIdentifier(converter.alias("Department") + "_12");
        Attribute attribute = new Attribute();
        attribute.setName("name");
        attribute.setValue("DepartmentA1");
        item.addAttribute(attribute);

        List rows2 = rowify(
                            new String[] {"Company_id", "name", "vatNumber", "CEO_id"},
                            new Object[] {new Integer(13), null, null, null});

        map.put("SELECT * FROM Company", rows2);

        map.put("SELECT Department_id FROM Department WHERE company_id = 13", blank);
        map.put("SELECT Contractor_id FROM Company_Contractor WHERE Company_id = 13", blank);

        Item item2 = itemFactory.makeItem();
        item2.setClassName(model.getNameSpace() + "Company");
        item2.setIdentifier(converter.alias("Company") + "_13");

        converter.process();
        Set expected = new HashSet();
        expected .add(item);
        expected.add(item2);
        assertEquals(expected, itemWriter.getItems());
    }

    public void testBuildUniqueIdMap() throws Exception {
        List rows = rowify( new String[] {"Company_id"},
                            new Object[][] { new Object[] {"1"},
                                             new Object[] {"1"},
                                             new Object[] {"2"},
                                             new Object[] {"2"}});

        map.put("SELECT Company_id FROM Company", rows);

        Map expected = new HashMap();
        Stack s1 = new Stack();
        s1.push("0_3");
        s1.push("0_4");
        expected.put("0_1", s1);
        Stack s2 = new Stack();
        s2.push("0_5");
        s2.push("0_6");
        expected.put("0_2", s2);

        converter.buildUniqueIdMap("Company");
        assertEquals(expected, converter.uniqueIdMap);
    }


    public void testProcessNonUniqueIds() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Department");

        List rows = rowify(
                           new String[] {"Department_id", "name", "company_id", "manager_id"},
                           new Object[][] {new Object[]
                                           {new Integer(1), null, new Integer(14), null},
                                           {new Integer(1), null, new Integer(14), null}});


        map.put("SELECT * FROM Department", rows);

        map.put("SELECT Employee_id FROM Employee WHERE departmentThatRejectedMe_id = 1", blank);
        map.put("SELECT Employee_id FROM Employee WHERE department_id = 1", blank);

        Map idMap = new HashMap();
        Stack s1 = new Stack();
        s1.push("1_3");
        s1.push("1_4");
        idMap.put("1_1", s1);
        converter.uniqueIdMap = idMap;
        ((MockDBRetriever) converter).setIsUnique(false);

        Reference ref = new Reference();
        ref.setName("company");
        ref.setRefId(converter.alias("Company") + "_14");
        Item item1 = itemFactory.makeItem();
        item1.setClassName(model.getNameSpace() + "Department");
        item1.setIdentifier(converter.alias("Department") + "_3");
        item1.addReference(ref);
        Attribute att1 = new Attribute();
        att1.setName("nonUniqueId");
        att1.setValue("1_1");
        item1.addAttribute(att1);
        Item item2 = itemFactory.makeItem();
        item2.setClassName(model.getNameSpace() + "Department");
        item2.setIdentifier(converter.alias("Department") + "_4");
        item2.addReference(ref);
        Attribute att2 = new Attribute();
        att2.setName("nonUniqueId");
        att2.setValue("1_1");
        item2.addAttribute(att2);

        converter.processClassDescriptor(cld);
        assertEquals(new HashSet(Arrays.asList(new Object[] {item1, item2})), itemWriter.getItems());

    }

    protected List rowify(String[] names, Object[] values) {
        Map rowifyMap = new HashMap();
        for (int i=0; i < names.length; i++) {
            rowifyMap.put(names[i], values[i]);
        }
        return Collections.singletonList(rowifyMap);
    }

    protected List rowify(String[] names, Object[][] values) {
        List rows = new ArrayList();
        for (int i = 0; i < values.length; i++) {
            rows.add(rowify(names, values[i]).get(0));
        }
        return rows;
    }

    class MockDBRetriever extends DBRetriever {
        private int identifier = 2;
        private boolean isUnique = true;
        //private String excludeList = null;

        public MockDBRetriever(Model model, Database db, DBReader reader, ItemWriter writer, String excludeList) {
            super(model, db, reader, writer, excludeList);
        }

        protected void setIsUnique(boolean b) {
            isUnique = b;
        }

        //we provide ids, return true here to save us having to create mock resultset metadata to prove it
        protected boolean idsProvided(ClassDescriptor cld) throws SQLException {
            return true;
        }

        protected boolean idIsUnique(ClassDescriptor cld) throws SQLException {
            return isUnique;
        }

        protected String findIndirectionTable(String clsName, String otherClsName) throws SQLException {
            return clsName + "_" + otherClsName;
        }

        protected String getNextTableId(String clsName) {
            return "" + (++identifier);
        }
    }

    class MockDBReader implements DBReader {
        public Iterator sqlIterator(String sql, String idField, String tableName) {
            try {
                return execute(sql).iterator();
            } catch (SQLException e) {}
            return null;
        }
        public List execute(String sql) throws SQLException {
            return (List) map.get(sql);
        }
        public void close() {
        }
    }
}
