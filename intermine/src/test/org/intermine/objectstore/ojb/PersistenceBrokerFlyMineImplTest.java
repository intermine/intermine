package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import java.lang.reflect.*;
import java.beans.*;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;

import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.objectstore.ObjectStore;

import org.flymine.model.testmodel.*;

public class PersistenceBrokerFlyMineImplTest extends TestCase
{
    public PersistenceBrokerFlyMineImplTest(String arg) {
        super(arg);
    }

    protected Database db;
    private PersistenceBroker broker;
    private Collection toStore = new ArrayList();
    protected Collection companys = new ArrayList();

    public void setUp() throws Exception {
        data();
        db = DatabaseFactory.getDatabase("db.unittest");
        broker = ObjectStoreOjbImpl.getInstance(db).getPersistenceBroker();
        try {
            broker.beginTransaction();
            Iterator i = toStore.iterator();
            while (i.hasNext()) {
                broker.store(i.next());
            }
            broker.commitTransaction();
        } catch (PersistenceBrokerException ex) {
            broker.abortTransaction();
            ex.printStackTrace();
        }
    }

    public void tearDown() throws Exception {
        Query query = new QueryByCriteria(Company.class, null);
        try {
            broker.beginTransaction();
            broker.deleteByQuery(query);
            broker.commitTransaction();
        } catch (PersistenceBrokerException ex) {
            broker.abortTransaction();
            ex.printStackTrace();
        }
    }

    public void testPBQuery() throws Exception {
        Query query = new QueryByCriteria(Company.class, null);
        Collection results = broker.getCollectionByQuery(query);
        assertEquals(2, results.size());
    }

    private void data() throws Exception {
        Company p1 = p1(), p2 = p2();
        Contractor c1 = c1(), c2 = c2();
        p1.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        p2.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        store(p1);
        store(p2);
        companys.add(p1);
        companys.add(p2);
    }

    protected Contractor c1() {
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

    protected Contractor c2() {
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
    
    protected Company p1() {
        Address a1 = new Address();
        a1.setAddress("Company Street, AVille");
        Address a2 = new Address();
        a2.setAddress("Employee Street, AVille");
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
        e1.setDepartment(d1); // bidirectional one-to-one
        d1.setEmployees(Arrays.asList(new Object[] { e2, e3 }));
        Company p = new Company();
        p.setName("CompanyA");
        p.setVatNumber(1234);
        p.setAddress(a1);
        p.setDepartments(Arrays.asList(new Object[] { d1 }));
        return p;
    }

    protected Company p2() {
        Address a1 = new Address();
        a1.setAddress("Company Street, BVille");
        Address a2 = new Address();
        a2.setAddress("Employee Street, BVille");
        Employee e1 = new Manager();
        e1.setName("EmployeeB1");
        e1.setFullTime(true);
        e1.setAddress(a2);
        e1.setAge(40);
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
        d1.setManager((Manager) e1);
        e1.setDepartment(d1); // bidirectional one-to-one
        d1.setEmployees(Arrays.asList(new Object[] { e2 }));
         Department d2 = new Department();
        d2.setName("DepartmentB2");
        d2.setManager((Manager) e3);
        e3.setDepartment(d2); // bidirectional one-to-one
        d2.setEmployees(Arrays.asList(new Object[] { e3 }));
        Company p = new Company();
        p.setName("CompanyB");
        p.setVatNumber(5678);
        p.setAddress(a1);
        p.setDepartments(Arrays.asList(new Object[] { d1, d2 }));
        return p;
    }

    private void store(Object o) throws Exception {
        if(o == null || toStore.contains(o)) {
            return;
        }
        toStore.add(o);
        PropertyDescriptor[] pd = Introspector.getBeanInfo(o.getClass()).getPropertyDescriptors();
        for(int i=0;i<pd.length;i++) {
            Method getter = pd[i].getReadMethod();
            if(!getter.getName().equals("getClass")) {
                Class returnType = getter.getReturnType();
                if(java.util.Collection.class.isAssignableFrom(returnType)) {
                    Iterator iter = ((Collection)getter.invoke(o, new Object[] {})).iterator();
                    while(iter.hasNext()) {
                        store(iter.next());
                    }
                } else if(returnType.equals(Integer.TYPE) || returnType.equals(Character.TYPE) ||
                          returnType.equals(Byte.TYPE) || returnType.equals(Short.TYPE) ||
                          returnType.equals(Long.TYPE) || returnType.equals(Boolean.TYPE) ||
                          returnType.equals(Float.TYPE) || returnType.equals(Double.TYPE) ||
                          returnType.equals(String.class)) {
                } else {
                    store(getter.invoke(o, new Object[] {}));
                }
            }
        }
    }
}

