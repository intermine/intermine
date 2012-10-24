package org.intermine.api.query;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.query.MainHelper;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryExistsConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.DynamicUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NullRefs {

    private static ObjectStoreWriter osw;
    private static final String ALIAS = "osw.unittest";
    
    private static final Logger LOG = Logger.getLogger(NullRefs.class);
    private static Set<InterMineObject> madeThings = new HashSet<InterMineObject>();

    @BeforeClass
    public static void build() {
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(ALIAS);
        } catch (ObjectStoreException e) {
            LOG.error("Could not initialise object-store", e);
            return;
        }
        if (osw != null) {
            try {
                osw.beginTransaction();
                for (char c: new char[] {'a', 'b', 'c', 'd'}) {
                    Department d = new Department();
                    d.setName("temp-department-" + c);
                    osw.store(d);
                    madeThings.add(d);
                    if ((int) c % 2 == 0) { // Half the departments have no employees.
                        for (int i = 0; i < 10; i++) {
                            Employee e = new Employee();
                            e.setName(String.format("temp-emp-%s%s", c, i));
                            e.setDepartment(d);
                            osw.store(e);
                            madeThings.add(e);
                        }
                    }
                    if ((int) c % 2 == 1) { // Half the departments have no managers.
                        Company comp = DynamicUtil.simpleCreateObject(Company.class);
                        comp.setName(String.format("temp-manager-%s", c));
                        d.setCompany(comp);
                        osw.store(comp);
                        osw.store(d);
                        madeThings.add(comp);
                    }
                }
                osw.commitTransaction();
                LOG.info("Made " + madeThings.size() + " things");
            } catch (Exception e) {
                if (osw != null) {
                    try {
                        osw.abortTransaction();
                    } catch (ObjectStoreException e1) {
                        LOG.error("While aborting transaction", e1);
                    }
                    LOG.error("Could not load test fixture", e);
                }
            } finally {
                if (osw != null) {
                    try {
                        osw.close();
                    } catch (ObjectStoreException e) {
                        LOG.error("While closing connection to object store", e);
                    }
                }
            }
        }
    }

    @Before
    public void setup() {
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(ALIAS);
        } catch (ObjectStoreException e) {
            LOG.error("Could not initialise object-store", e);
            return;
        } 
    }

    @After
    public void teardown() {
        if (osw != null) {
            try {
                osw.close();
            } catch (ObjectStoreException e) {
                LOG.error("While closing connection to object store", e);
            }
        }
    }

    private void doTest(Query q, int rows, int expected) {
        Results res = osw.execute(q, 50000, true, false, true);
        assertEquals(rows, res.size());
        int empsAndCompanies = 0;
        for (Object o: res) {
            Department d = (Department) ((List) o).get(0);
            if (d.getEmployees() != null) {
                empsAndCompanies += d.getEmployees().size();
            }
            if (d.getCompany() != null) {
                empsAndCompanies++;
            }
        }
        assertEquals(expected, empsAndCompanies);
    }

    private Query makeQuery(PathQuery pq) {
        Query q;
        try {
            q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());
        } catch (ObjectStoreException e) {
            LOG.error(e);
            fail(e.getMessage());
            return null;
        }
        return q;
    }

    @Test
    public void testSanity() {
        Query q = new Query();
        QueryClass cls = new QueryClass(Department.class);
        q.addFrom(cls);
        q.addToSelect(cls);
        q.setConstraint(new SimpleConstraint(new QueryField(cls, "name"), ConstraintOp.MATCHES, new QueryValue("temp-%")));
        doSanityTest(q);
    }

    @Test
    public void testPQSanity() {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.name");
        pq.addConstraint(Constraints.eq("Department.name", "temp-*"));
        Query q = makeQuery(pq);
        doSanityTest(q);
    }
    
    private void doSanityTest(Query q) {
        doTest(q, 4, 22);
    }
    
    @Test
    public void nullRefs() {
        Query q = new Query();
        QueryClass cls = new QueryClass(Department.class);
        q.addFrom(cls);
        q.addToSelect(cls);
        ConstraintSet cons = new ConstraintSet(ConstraintOp.AND);
        cons.addConstraint(new SimpleConstraint(new QueryField(cls, "name"), ConstraintOp.MATCHES, new QueryValue("temp-%")));
        cons.addConstraint(new ContainsConstraint(new QueryObjectReference(cls, "company"), ConstraintOp.IS_NULL));
        q.setConstraint(cons);
        doNullRefTests(q);
    }
    
    @Test
    public void pathQueryNullRefs() {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.name");
        pq.addConstraint(Constraints.eq("Department.name", "temp-*"));
        pq.addConstraint(Constraints.isNull("Department.company"));
        Query q = makeQuery(pq);
        doNullRefTests(q);
    }

    private void doNullRefTests(Query q) {
        doTest(q, 2, 20);
    }

    @Test
    public void nonNullRefs() {
        Query q = new Query();
        QueryClass cls = new QueryClass(Department.class);
        q.addFrom(cls);
        q.addToSelect(cls);
        ConstraintSet cons = new ConstraintSet(ConstraintOp.AND);
        cons.addConstraint(new SimpleConstraint(new QueryField(cls, "name"), ConstraintOp.MATCHES, new QueryValue("temp-%")));
        cons.addConstraint(new ContainsConstraint(new QueryObjectReference(cls, "company"), ConstraintOp.IS_NOT_NULL));
        q.setConstraint(cons);
        doNonNullRefTests(q);
    }

    @Test
    public void pathQueryNonNullRefs() {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.name");
        pq.addConstraint(Constraints.eq("Department.name", "temp-*"));
        pq.addConstraint(Constraints.isNotNull("Department.company"));
        Query q = makeQuery(pq);
        doNonNullRefTests(q);
    }

    public void doNonNullRefTests(Query q) {
        doTest(q, 2, 2);
    }

    /**
     * This is possible if the following sql can be generated:
     * <pre>
     *   select id from foo as f where not exists (select 1 from bar as b where b.foo = f.id);
     * </pre>
     * This seems to rather involve wrestling with the ghost of mrw....
     */
    @Test
    public void nonNullCollections() {
        QueryClass dep = new QueryClass(Department.class);
        QueryClass emp = new QueryClass(Employee.class);
        
        Query q = new Query();
        q.addFrom(dep);
        q.addToSelect(dep);
        ConstraintSet cons = new ConstraintSet(ConstraintOp.AND);
        QueryField nameF = new QueryField(dep, "name");
        cons.addConstraint(new SimpleConstraint(nameF, ConstraintOp.MATCHES, new QueryValue("temp-%")));
        q.setConstraint(cons);
        
        Query subQ = new Query();
        subQ.alias(dep, q.getAliases().get(dep)); // W. T. F. 
        subQ.setDistinct(false);
        subQ.addFrom(emp);
        subQ.addToSelect(new QueryValue(1));
        ConstraintSet subset = new ConstraintSet(ConstraintOp.AND);
        
        //subset.addConstraint(new ContainsConstraint(new QueryObjectReference(emp, "department"), ConstraintOp.CONTAINS, dep));
        subset.addConstraint(new ContainsConstraint(new QueryCollectionReference(dep, "employees"), ConstraintOp.CONTAINS, emp));
        subQ.setConstraint(subset);
        //
        
        cons.addConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, subQ));
        
        doNonNullCollectionTests(q);
    }
    
    @Test
    public void nonNullPathQueryCollections() {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.name");
        pq.addConstraint(Constraints.eq("Department.name", "temp-*"));
        pq.addConstraint(Constraints.isNotNull("Department.employees"));
        Query q = makeQuery(pq);
        System.out.println(q.toString());
        doNonNullCollectionTests(q);
    }

    @Test
    public void nonNullPathQueryCollectionsUnmarshall() {
        String xml = "<query model=\"testmodel\" view=\"Department.name\">"
            + "<constraint path=\"Department.name\" op=\"=\" value=\"temp-*\"/>"
            + "<constraint path=\"Department.employees\" op=\"IS NOT NULL\"/>"
            + "</query>";
        PathQuery pq = PathQueryBinding.unmarshalPathQuery(
            new StringReader(xml), PathQuery.USERPROFILE_VERSION);

        Query q = makeQuery(pq);
        doNonNullCollectionTests(q);
    }

    @Test
    public void nonNullPathQueryCollectionsSynonymUnmarshall() {
        String xml = "<query model=\"testmodel\" view=\"Department.name\">"
            + "<constraint path=\"Department.name\" op=\"=\" value=\"temp-*\"/>"
            + "<constraint path=\"Department.employees\" op=\"IS NOT EMPTY\"/>"
            + "</query>";
        PathQuery pq = PathQueryBinding.unmarshalPathQuery(
            new StringReader(xml), PathQuery.USERPROFILE_VERSION);

        Query q = makeQuery(pq);
        doNonNullCollectionTests(q);
    }
    
    private void doNonNullCollectionTests(Query q) {
        Results res = osw.execute(q, 50000, true, false, true);
        for (Object o: res) {
            Department d = (Department) ((List) o).get(0);
            System.out.println(d.getName() + ":\n-------");
            for (Employee e: d.getEmployees()) {
                System.out.println(e);
            }
            System.out.println();
        }
        doTest(q, 2, 20);
    }
    
    /**
     * This is possible if the following sql can be generated:
     * <pre>
     *   select id from foo as f where not exists (select 1 from bar as b where b.foo = f.id);
     * </pre>
     * This seems to rather involve wrestling with the ghost of mrw....
     */
    @Test
    public void nullCollections() {
        QueryClass dep = new QueryClass(Department.class);
        QueryClass emp = new QueryClass(Employee.class);

        Query q = new Query();
        q.addFrom(dep);
        q.addToSelect(dep);
        ConstraintSet cons = new ConstraintSet(ConstraintOp.AND);
        QueryField nameF = new QueryField(dep, "name");
        cons.addConstraint(
            new SimpleConstraint(nameF, ConstraintOp.MATCHES, new QueryValue("temp-%")));
        q.setConstraint(cons);

        Query subQ = new Query();
        subQ.alias(dep, q.getAliases().get(dep)); // W. T. F. 
        subQ.setDistinct(false);
        subQ.addFrom(emp);
        subQ.addToSelect(new QueryValue(1));
        ConstraintSet subset = new ConstraintSet(ConstraintOp.AND);
        
        //subset.addConstraint(new ContainsConstraint(new QueryObjectReference(emp, "department"), ConstraintOp.CONTAINS, dep));
        subset.addConstraint(
            new ContainsConstraint(
                new QueryCollectionReference(dep, "employees"), ConstraintOp.CONTAINS, emp));
        subQ.setConstraint(subset);
        //
        
        cons.addConstraint(new SubqueryExistsConstraint(ConstraintOp.DOES_NOT_EXIST, subQ));
        
        doNullCollectionTests(q);
    }

    @Test
    public void nullPathQueryCollections() {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.name");
        pq.addConstraint(Constraints.eq("Department.name", "temp-*"));
        pq.addConstraint(Constraints.isNull("Department.employees"));
        Query q = makeQuery(pq);
        System.out.println(q.toString());
        doNullCollectionTests(q);
    }

    @Test
    public void nullPathQueryCollectionsSynonyms() {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.name");
        pq.addConstraint(Constraints.eq("Department.name", "temp-*"));
        pq.addConstraint(new PathConstraintNull("Department.employees", ConstraintOp.IS_EMPTY));
        Query q = makeQuery(pq);
        System.out.println(q.toString());
        doNullCollectionTests(q);
    }

    @Test
    public void nullPathQueryCollectionsSynonymUnmarshall() {
        String xml = "<query model=\"testmodel\" view=\"Department.name\">"
            + "<constraint path=\"Department.name\" op=\"=\" value=\"temp-*\"/>"
            + "<constraint path=\"Department.employees\" op=\"IS EMPTY\"/>"
            + "</query>";
        PathQuery pq = PathQueryBinding.unmarshalPathQuery(
            new StringReader(xml), PathQuery.USERPROFILE_VERSION);

        Query q = makeQuery(pq);
        doNullCollectionTests(q);
    }

    private void doNullCollectionTests(Query q) {
        Results res = osw.execute(q, 50000, true, false, true);
        for (Object o: res) {
            Department d = (Department) ((List) o).get(0);
            System.out.println(d.getName() + ":\n-------");
            for (Employee e: d.getEmployees()) {
                System.out.println(e);
            }
            System.out.println();
        }
        doTest(q, 2, 2);
    }

    @AfterClass
    public static void tearDown() {
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(ALIAS);
        } catch (ObjectStoreException e) {
            LOG.error("Could not initialise object-store", e);
            return;
        }
        if (osw != null) {
            try {
                osw.beginTransaction();
                for (InterMineObject o: madeThings) {
                    osw.delete(o);
                }
                osw.commitTransaction();
                LOG.info("Deleted " + madeThings.size() + " things");
                madeThings.clear();
            } catch (ObjectStoreException e) {
                LOG.error("While unloading fixture", e);
            } finally {
                if (osw != null) {
                    try {
                        osw.close();
                    } catch (ObjectStoreException e) {
                        LOG.error("Closing object store", e);
                    }
                }
            }
        }
    }

}
