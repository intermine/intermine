package org.flymine.codegen;

import junit.framework.TestCase;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

import org.flymine.model.testmodel.*;
import org.flymine.util.*;

public class CastorModelOutputFunctionalTest extends TestCase
{
   protected static final org.apache.log4j.Logger LOG
        = org.apache.log4j.Logger.getLogger(CastorModelOutputTest.class);

    private Mapping map;
    private Company p1, p2;
    private Contractor c1, c2;
    private int fakeId = 0;
    private File file;

    public CastorModelOutputFunctionalTest(String name) {
        super(name);
    }


    public void setUp() throws Exception {
        URL mapFile = getClass().getClassLoader().getResource("castor_xml_testmodel.xml");

        map = new Mapping();
        map.loadMapping(mapFile);

        p1 = p1(); p2 = p2();
        c1 = c1(); c2 = c2();
        p1.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        p2.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        p1.setOldContracts(Arrays.asList(new Object[] {c1, c1, c2, c2}));
        p2.setOldContracts(Arrays.asList(new Object[] {c1, c1, c2, c2}));
        c1.setCompanys(Arrays.asList(new Object[] {p1, p2}));
        c2.setCompanys(Arrays.asList(new Object[] {p1, p2}));
        c1.setOldComs(Arrays.asList(new Object[] {p1, p1, p2, p2}));
        c2.setOldComs(Arrays.asList(new Object[] {p1, p1, p2, p2}));

    }


    public void tearDown() {
        file.delete();
    }

    public void testTestData() throws Exception {

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(p1);
        List flatList = TypeUtil.flatten(list);
        setIds(flatList);

        ListBean bean = new ListBean();
        bean.setItems(flatList);

        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flatList);

        Company com = (Company)result.get(0);
        assertEquals(com, p1);
    }


    public void testSimpleObject() throws Exception {
        Employee e1 = new Employee();
        e1.setName("e1");
        e1.setFullTime(true);
        e1.setAge(25);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(e1);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);

        //LOG.warn("testSimpleObject..." + bean.getItems().toString());
        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Employee e2 = (Employee) result.get(0);
        assertEquals(e1, e2);

    }


    public void testSimpleRelation() throws Exception {
        Employee e1 = new Employee();
        e1.setName("e1");
        e1.setFullTime(true);
        e1.setAge(25);

        Address a1 = new Address();
        a1.setAddress("a1");
        e1.setAddress(a1);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(e1);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);

        //LOG.warn("testSimpleRelation..." + bean.getItems().toString());
        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Employee e2 = (Employee) result.get(0);
        assertEquals(e1, e2);

        Address a2 = (Address) result.get(1);
        assertEquals(a1, a2);

        assertEquals(a1, e2.getAddress());

    }

    public void testOneToOne() throws Exception {
        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        CEO ceo1 = new CEO();
        ceo1.setName("ceo1");
        ceo1.setAge(40);
        ceo1.setFullTime(false);
        ceo1.setTitle("Dr.");
        ceo1.setSalary(10000);
        c1.setCEO(ceo1);
        ceo1.setCompany(c1);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(c1);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);

        //LOG.warn("testOneToOne..." + bean.getItems().toString());
        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Company c2 = (Company) result.get(0);
        assertEquals(c1, c2);

        CEO ceo2 = (CEO) result.get(1);
        assertEquals(ceo1, ceo2);

        CEO ceo3 = c2.getCEO();
        assertEquals(ceo1, ceo3);

        Company c3 = ceo2.getCompany();
        assertEquals(c1, c3);

    }

    public void testUnidirectional() throws Exception {
        Department d1 = new Department();
        d1.setName("d1");
        Manager m1 = new Manager();
        m1.setName("m1");
        m1.setAge(40);
        m1.setFullTime(false);
        m1.setTitle("Dr.");
        d1.setManager(m1);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(d1);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);

        //LOG.warn("testUnidirectional..." + bean.getItems().toString());
        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Department d2 = (Department) result.get(0);
        assertEquals(d1, d2);

        Manager m2 = (Manager) result.get(1);
        assertEquals(m1, m2);

        Manager m3 = d2.getManager();
        assertEquals(m1, m3);
    }

    public void testCollection() throws Exception {
        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        Department d1 = new Department();
        d1.setName("d1");
        Department d2 = new Department();
        d2.setName("d2");
        Department d3 = new Department();
        d3.setName("d3");
        List depts = new ArrayList();
        depts.addAll(Arrays.asList(new Object[] {d1, d2, d3}));
        c1.setDepartments(depts);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(c1);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();

        bean.setItems(flat);

        //LOG.warn("testCollection..." + bean.getItems().toString());

        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Company c2 = (Company) result.get(0);
        assertEquals(c1, c2);

        Department dd1 = (Department) result.get(1);
        assertEquals(d1, dd1);
        Department dd2 = (Department) result.get(2);
        assertEquals(d2, dd2);
        Department dd3 = (Department) result.get(3);
        assertEquals(d3, dd3);

        assertEquals(depts, c2.getDepartments());

    }

    public void testManyToMany() throws Exception {
        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        Company c2 = new Company();
        c2.setName("c2");
        c2.setVatNumber(202);

        Contractor t1 = new Contractor();
        t1.setName("t1");
        Contractor t2 = new Contractor();
        t2.setName("t2");

        List ts1 = new ArrayList();
        ts1.add(t1);
        List ts2 = new ArrayList();
        ts2.addAll(Arrays.asList(new Object[] {t1, t2}));

        List cs1 = new ArrayList();
        cs1.add(c1);
        List cs2 = new ArrayList();
        cs2.addAll(Arrays.asList(new Object[] {c1, c2}));

        c1.setContractors(ts1);
        c2.setContractors(ts2);
        t1.setCompanys(cs1);
        t2.setCompanys(cs2);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(c1);
        list.add(c2);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);

        //LOG.warn("testManyToMany..." + bean.getItems().toString());
        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Company cc1 = (Company) result.get(0);
        assertEquals(c1, cc1);
        List tts1 = cc1.getContractors();
        assertEquals(ts1, tts1);

        Company cc2 = (Company) result.get(2);
        assertEquals(c2, cc2);
        List tts2 = cc2.getContractors();
        assertEquals(ts2, tts2);

        Contractor tt1 = (Contractor) tts1.get(0);
        assertEquals(cs1, tt1.getCompanys());
        Contractor tt2 = (Contractor) tts2.get(1);
        assertEquals(cs2, tt2.getCompanys());


    }


    public void testInheritatnce() throws Exception {
        Department d1 = new Department();
        d1.setName("d1");
        Employee e1 = new Employee();
        e1.setName("e1");
        e1.setFullTime(true);
        e1.setAge(25);
        Manager e2 = new Manager();
        e2.setName("e2_m");
        e2.setFullTime(true);
        e2.setAge(35);
        e2.setTitle("Dr.");
        CEO e3 = new CEO();
        e3.setName("e3_c");
        e3.setFullTime(true);
        e3.setAge(45);
        e3.setSalary(10000);
        List emps = new ArrayList();
        emps.addAll(Arrays.asList(new Object[] {e1, e2, e3}));
        d1.setEmployees(emps);
        d1.setManager((Manager)e2);
        e1.setDepartment(d1);
        e2.setDepartment(d1);
        e3.setDepartment(d1);

        file = File.createTempFile("temp", "xml");
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List list = new ArrayList();
        list.add(d1);
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);

        //LOG.warn("testInheritance..." + bean.getItems().toString());
        marshaller.marshal(bean);

        Reader reader = new FileReader(file);
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);

        stripIds(flat);

        Department dd1 = (Department) result.get(0);
        assertEquals(d1, dd1);
        Employee ee1 = (Employee) result.get(1);
        assertEquals(e1, ee1);
        Employee ee2 = (Employee) result.get(2);
        assertEquals(e2, ee2);
        Employee ee3 = (Employee) result.get(3);
        assertEquals(e3, ee3);

        assertEquals(d1, ee1.getDepartment());
        assertEquals(e2, dd1.getManager());

    }

    // set fake ids for a collection of business objects
     void setIds(Collection c) throws Exception {
         Iterator iter = c.iterator();
         while (iter.hasNext()) {
             fakeId++;
             Object obj = iter.next();
             Class cls = obj.getClass();
             Field f = getIdField(cls);
             if (f != null) {
                 f.setAccessible(true);
                 f.set(obj, new Integer(fakeId));
             }
         }
     }

    // remove fake ids for a collection of business objects
     void stripIds(Collection c) throws Exception {
         Iterator iter = c.iterator();
         while (iter.hasNext()) {
             fakeId++;
             Object obj = iter.next();
             Class cls = obj.getClass();
             Field f = getIdField(cls);
             if (f != null) {
                 f.setAccessible(true);
                 f.set(obj, null);
             }
         }
     }

    // find the id field of an object, search superclasses if not found
    Field getIdField(Class cls) throws Exception {
        Field[] fields = cls.getDeclaredFields();
        for(int i=0; i<fields.length; i++) {
            if (fields[i].getName() == "id") {
                return fields[i];
            }
        }
        Class sup = cls.getSuperclass();
        if (sup != null && !sup.isInterface()) {
            return getIdField(sup);
        }
        return null;
    }

    Contractor c1() {
        Address a1 = new Address();
        a1.setAddress("Contractor Personal Street, AVille");
        Address a2 = new Address();
        a2.setAddress("Contractor Business Street, AVille");
        Contractor c = new Contractor();
        c.setName("ContractorA");
        c.setPersonalAddress(a1);
        c.setBusinessAddress(a2);
        return c;
    }

    Contractor c2() {
        Address a1 = new Address();
        a1.setAddress("Contractor Personal Street, BVille");
        Address a2 = new Address();
        a2.setAddress("Contractor Business Street, BVille");
        Contractor c = new Contractor();
        c.setName("ContractorB");
        c.setPersonalAddress(a1);
        c.setBusinessAddress(a2);
        return c;
    }

    Company p1() {
        Address a1 = new Address();
        a1.setAddress("Company Street, AVille");
        Address a2 = new Address();
        a2.setAddress("Employee Street, AVille");
        Company p = new Company();
        p.setName("CompanyA");
        p.setVatNumber(1234);
        p.setAddress(a1);
        Employee e1 = new Manager();
        e1.setName("EmployeeA1");
        e1.setFullTime(true);
        e1.setAddress(a2);
        e1.setAge(10);
        Employee e2 = new Employee();
        e2.setName("EmployeeA2");
        e2.setFullTime(true);
        e2.setAddress(a2);
        e2.setAge(20);
        Employee e3 = new Employee();
        e3.setName("EmployeeA3");
        e3.setFullTime(false);
        e3.setAddress(a2);
        e3.setAge(30);
        Department d1 = new Department();
        d1.setName("DepartmentA1");
        d1.setManager((Manager) e1);
        d1.setEmployees(Arrays.asList(new Object[] { e1, e2, e3 }));
        d1.setCompany(p);
        p.setDepartments(Arrays.asList(new Object[] { d1 }));
        return p;
    }

    Company p2() {
        Address a1 = new Address();
        a1.setAddress("Company Street, BVille");
        Address a2 = new Address();
        a2.setAddress("Employee Street, BVille");
        Company p = new Company();
        p.setName("CompanyB");
        p.setVatNumber(5678);
        p.setAddress(a1);
        CEO e1 = new CEO();
        e1.setName("EmployeeB1");
        e1.setFullTime(true);
        e1.setAddress(a2);
        e1.setAge(40);
        e1.setTitle("Mr.");
        e1.setSalary(45000);
        Employee e2 = new Employee();
        e2.setName("EmployeeB2");
        e2.setFullTime(true);
        e2.setAddress(a2);
        e2.setAge(50);
        Employee e3 = new Manager();
        e3.setName("EmployeeB3");
        e3.setFullTime(true);
        e3.setAddress(a2);
        e3.setAge(60);
        Department d1 = new Department();
        d1.setName("DepartmentB1");
        d1.setManager(e1);
        d1.setEmployees(Arrays.asList(new Object[] { e1, e2 }));
        d1.setCompany(p);
         Department d2 = new Department();
        d2.setName("DepartmentB2");
        d2.setManager((Manager) e3);
        d2.setEmployees(Arrays.asList(new Object[] { e3 }));
        d2.setCompany(p);
        p.setDepartments(Arrays.asList(new Object[] { d1, d2 }));
        return p;
    }

}
