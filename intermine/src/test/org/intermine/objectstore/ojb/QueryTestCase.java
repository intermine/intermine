package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.*;
import java.beans.*;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.objectstore.*;
import org.flymine.objectstore.ojb.*;
import org.flymine.objectstore.query.*;
import org.flymine.model.testmodel.*;

public abstract class QueryTestCase extends TestCase
{
    protected Database db;
    protected Map queries;
    protected Map results;
    protected List companys = new ArrayList();

    /**
     * Constructor
     */
    public QueryTestCase(String arg) {
        super(arg);
    }

    /**
     * Set up the test
     *
     * @throws Exception if an error occurs
     */
    public void setUp() throws Exception {
        queries = new HashMap();
        results = new HashMap();
        setUpQueries();
        setUpResults();
    }

    /**
     * Set up the set of queries we are testing
     *
     * @throws Exception if an error occurs
     */
    public void setUpQueries() throws Exception {
    }

    /**
     * Set up any data needed
     *
     * @throws Exception if an error occurs
     */
    public void setUpData() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
        PersistenceBrokerFlyMineImpl broker = (PersistenceBrokerFlyMineImpl) ObjectStoreOjbImpl.getInstance(db).getPersistenceBroker();
         try {
            broker.beginTransaction();
            Iterator i = data().iterator();
            while (i.hasNext()) {
                broker.store(i.next());
            }
            broker.commitTransaction();
        } catch (Exception e) {
            broker.abortTransaction();
            throw new Exception(e);
        }
    }

    public void tearDownData() throws Exception {
        PersistenceBrokerFlyMineImpl broker = (PersistenceBrokerFlyMineImpl) ObjectStoreOjbImpl.getInstance(db).getPersistenceBroker();
         try {
            broker.beginTransaction();
            Iterator i = data().iterator();
            while (i.hasNext()) {
                broker.delete(i.next());
            }
            broker.commitTransaction();
        } catch (Exception e) {
            broker.abortTransaction();
            throw new Exception(e);
        }
    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public abstract void setUpResults() throws Exception;

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * thos expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public abstract void executeTest(String type) throws Exception;

    /**
     * Test the queries produce the appropriate result
     *
     * @throws Exception if an error occurs
     */
    public void testQueries() throws Exception {
        Iterator i = results.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            // Does this appear in the queries map;
            if (!(queries.containsKey(type))) {
                throw new Exception(type + " does not appear in the queries map");
            }
            executeTest(type);
        }
    }

    private Collection data() throws Exception {
        Company p1 = p1(), p2 = p2();
        Contractor c1 = c1(), c2 = c2();
        p1.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        p2.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        companys.add(p1);
        companys.add(p2);
        return flatten(companys);
    }

    private Collection flatten(Collection c) throws Exception {
        List toStore = new ArrayList();
        Iterator i = c.iterator();
        while(i.hasNext()) {
            flatten_(i.next(), toStore);
        }
        return toStore;
    }

    private void flatten_(Object o, Collection c) throws Exception {
        if(o == null || c.contains(o)) {
            return;
        }
        c.add(o);
        PropertyDescriptor[] pd = Introspector.getBeanInfo(o.getClass()).getPropertyDescriptors();
        for(int i=0;i<pd.length;i++) {
            Method getter = pd[i].getReadMethod();
            if(!getter.getName().equals("getClass")) {
                Class returnType = getter.getReturnType();
                if(java.util.Collection.class.isAssignableFrom(returnType)) {
                    Iterator iter = ((Collection)getter.invoke(o, new Object[] {})).iterator();
                    while(iter.hasNext()) {
                        flatten_(iter.next(), c);
                    }
                } else if(returnType.getName().startsWith("org.flymine.model")) {
                    flatten_(getter.invoke(o, new Object[] {}), c);
                }
            }
        }
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
}
