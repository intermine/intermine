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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.PersistenceBroker;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.objectstore.*;
import org.flymine.objectstore.query.*;
import org.flymine.model.testmodel.*;

public abstract class QueryTestCase extends TestCase
{
    protected Database db;
    protected Map queries;
    protected Map results;
    private DescriptorRepository dr;

    protected Map data = new HashMap();

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
        super.setUp();

        Database db = DatabaseFactory.getDatabase("db.unittest");
        ObjectStoreOjbImpl os = ObjectStoreOjbImpl.getInstance(db);
        PersistenceBroker broker = os.getPersistenceBroker();
        dr = broker.getDescriptorRepository();
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
        queries.put("SubQuery", subQuery());
        queries.put("WhereSimpleEquals", whereSimpleEquals());
        queries.put("WhereSimpleNotEquals", whereSimpleNotEquals());
        queries.put("WhereSimpleLike", whereSimpleLike());
        queries.put("WhereEqualsString", whereEqualString());
        queries.put("WhereAndSet", whereAndSet());
        queries.put("WhereOrSet", whereOrSet());
        queries.put("WhereNotSet", whereNotSet());
        queries.put("WhereSubQueryField", whereSubQueryField());
        queries.put("WhereSubQueryClass", whereSubQueryClass());
        queries.put("WhereNotSubQueryClass", whereNotSubQueryClass());
        queries.put("WhereNegSubQueryClass", whereNegSubQueryClass());
        queries.put("WhereClassClass", whereClassClass());
        queries.put("WhereNotClassClass", whereNotClassClass());
        queries.put("WhereNegClassClass", whereNegClassClass());
        queries.put("WhereClassObject", whereClassObject());
        queries.put("Contains11", contains11());
        queries.put("Contains1N", contains1N());
        queries.put("ContainsN1", containsN1());
        queries.put("ContainsMN", containsMN());
        queries.put("SimpleGroupBy", simpleGroupBy());
        queries.put("MultiJoin", multiJoin());
        queries.put("SelectComplex", selectComplex());
        queries.put("SelectClassAndSubClasses", selectClassAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses", selectInterfaceAndSubClasses());
    }

    /**
     * Set up any data needed
     *
     * @throws Exception if an error occurs
     */
    public void setUpData() throws Exception {
        data();
        db = DatabaseFactory.getDatabase("db.unittest");
        PersistenceBrokerFlyMineImpl broker = (PersistenceBrokerFlyMineImpl) ObjectStoreOjbImpl.getInstance(db).getPersistenceBroker();
         try {
            broker.beginTransaction();
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                broker.store(data.get(iter.next()));
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
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                broker.delete(data.get(iter.next()));
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
    public void testQueries() throws Throwable {
        Iterator i = results.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            // Does this appear in the queries map;
            if (!(queries.containsKey(type))) {
                throw new Exception(type + " does not appear in the queries map");
            }
            try {
                executeTest(type);
            } catch (Throwable t) {
                throw new Throwable("Failed on " + type, t);
            }
        }
    }

    /*
      select subquery.company.name, subquery.alias
      from (select company, 5 as alias from Company) as subquery
    */
    public Query subQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        q1.addToSelect(v1);
        Query q2 = new Query();
        q2.addFrom(q1);
        QueryField f1 = new QueryField(q1, c1, "name");
        QueryField f2 = new QueryField(q1, v1);
        q2.addToSelect(f1);
        q2.addToSelect(f2);
        return q2;
    }

    /*
      select name
      from Company
      where vatNumber = 1234
    */
    public Query whereSimpleEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where vatNumber! = 1234
    */
    public Query whereSimpleNotEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.NOT_EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f2);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name like "Company%"
    */
    public Query whereSimpleLike() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name = "CompanyA"
    */
    public Query whereEqualString() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    /*
      select name
      from Company
      where name LIKE "Company%"
      and vatNumber > 2000
    */
    public Query whereAndSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select name
      from Company
      where name LIKE "CompanyA%"
      or vatNumber > 2000
    */
    public Query whereOrSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.OR);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select name
      from Company
      where not (name LIKE "Company%"
      and vatNumber > 2000)
    */
    public Query whereNotSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        cs1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department
      from Department
      where (select name from Department) contains department.name
    */
    public Query whereSubQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, f1);
        Query q2 = new Query();
        q2.addFrom(c1);
        q2.addToSelect(c1);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select department
      from Department
      where (select company from Company where name = "CompanyA") contains department
    */
    public Query whereSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1));
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select department
      from Department
      where (select company from Company where name = "CompanyA") !contains department
    */
    public Query whereNotSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1));
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.DOES_NOT_CONTAIN, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select department
      from Department
      where not (select company from Company where name = "CompanyA") contains department
    */
    public Query whereNegSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1));
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        sqc1.setNegated(true);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where c1 = c2
    */
    public Query whereClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where c1 != c2
    */
    public Query whereNotClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.NOT_EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select c1, c2
      from Company c1, Company c2
      where not (c1 = c2)
    */
    public Query whereNegClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        cc1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select company,
      from Company
      where c1 = <company object>
    */
    public Query whereClassObject() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Company obj = new Company();
        ClassDescriptor cld = dr.getDescriptorFor(Company.class);
        FieldDescriptor fld = cld.getFieldDescriptorByName("id");
        //Integer id = (Integer) fld.getPersistentField().get(data.get("CompanyA"));
        Integer id = new Integer(2345);
        fld.getPersistentField().set(obj, id);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select department, manager
      from Department, Manager
      where department.manager contains manager
      and department.name = "DepartmentA1"
    */

      public Query contains11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select company, department
      from Company, Department
      where company contains department
      and company.name = "CompanyA"
    */
      public Query contains1N() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select department, company
      from Department, company
      where department.company = company
      and company.name = "CompanyA"
    */
      public Query containsN1() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "company");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc2, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.companys contains company
      and contractor.name = "ContractorA"
    */
      public Query containsMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "companys");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("ContractorA");
        QueryField qf1 = new QueryField(qc1, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
      }

    /*
      select company, count(*)
      from Company, Department
      where company contains department
      group by company
    */
    public Query simpleGroupBy() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS,  qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(new QueryFunction());
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.setConstraint(cc1);
        q1.addToGroupBy(qc1);
        return q1;
    }

    /*
      select company, department, manager, address
      from Company, Department, Manager, Address
      where company contains department
      and department.manager = manager
      and manager.address = address
      and manager.name = "EmployeeA1"
    */
    public Query multiJoin() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Manager.class);
        QueryClass qc4 = new QueryClass(Address.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        QueryReference qr2 = new QueryObjectReference(qc2, "manager");
        QueryReference qr3 = new QueryObjectReference(qc3, "address");
        QueryField qf1 = new QueryField(qc3, "name");
        QueryValue qv1 = new QueryValue("EmployeeA1");

        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        q1.addToSelect(qc4);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addFrom(qc4);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2));
        cs1.addConstraint(new ContainsConstraint(qr2, ContainsConstraint.CONTAINS, qc3));
        cs1.addConstraint(new ContainsConstraint(qr3, ContainsConstraint.CONTAINS, qc4));
        cs1.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, qv1));
        return q1;
    }

    /*
      select company, avg(company.vatNumber) + 20, department.name, department
      from Company, Department
    */
    public Query selectComplex() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        QueryField f3 = new QueryField(c2, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addFrom(c2);
        q1.addToSelect(c1);
        QueryExpression e1 = new QueryExpression(new QueryFunction(f2, QueryFunction.AVERAGE),
                QueryExpression.ADD, new QueryValue(new Integer(20)));
        q1.addToSelect(e1);
        q1.addToSelect(f3);
        q1.addToSelect(c2);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND ALL EMPLOYEES
      select employee
      from Employee
    */
    public Query selectClassAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employee.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS, CONTRACTORS AND ALL EMPLOYEES
      select employable
      from Employable
    */
    public Query selectInterfaceAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    private void data() throws Exception {
        Company p1 = p1(), p2 = p2();
        Contractor c1 = c1(), c2 = c2();
        p1.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        p2.setContractors(Arrays.asList(new Object[] { c1, c2 }));
        map(flatten(Arrays.asList(new Object[] { p1, p2 })));
    }

    private void map(Collection c) throws Exception {
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            Method name = null;
            try {
                name = o.getClass().getMethod("getName", new Class[] {});
            } catch (Exception e) {}
            if(name!=null) {
                System.err.println("putting "+(String)name.invoke(o, new Object[] {})+ " "+o);
                data.put((String)name.invoke(o, new Object[] {}), o);
            } else {
                System.err.println("putting "+o.hashCode()+ " "+o);
                data.put(new Integer(o.hashCode()), o);
            }
        }
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
