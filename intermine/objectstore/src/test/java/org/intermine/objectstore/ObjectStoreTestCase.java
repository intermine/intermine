package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.testmodel.*;
import org.intermine.objectstore.proxy.Lazy;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.iql.IqlQueryParser;
import org.intermine.util.DynamicUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

public class ObjectStoreTestCase {

    private static ObjectStore osForOsTests;
    protected static ObjectStoreWriter storeDataWriter;

    protected static Map<String, Query> queries = new HashMap<String, Query>();
    protected static Map data;

    public static void oneTimeSetUp(
            ObjectStore os, String osWriterName, String modelName, String itemsXmlFilename) throws Exception {
        osForOsTests = os;
        storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter(osWriterName);

        data = ObjectStoreTestUtils.getTestData(modelName, itemsXmlFilename);
        ObjectStoreTestUtils.storeData(storeDataWriter, data);

        queries.put("ContainsDuplicatesMN", generateContainsDuplicatesMNQuery());
        queries.put("ContainsN1", generateContainsN1Query());
        queries.put("SelectSimpleObject", generateSelectSimpleObjectQuery());
        queries.put("SimpleGroupBy", generateSimpleGroupByQuery());
        queries.put("WhereClassClass", generateWhereClassClassQuery());
        queries.put("WhereClassObject", generateWhereClassObjectQuery());
    }

    @AfterClass
    public static void oneTimeShutdown() throws Exception {
        ObjectStoreTestUtils.deleteAllObjectsInStore(storeDataWriter);
        storeDataWriter.close();
    }

    /*
      select contractor, company
      from Contractor, Company
      where contractor.oldComs CONTAINS company
    */
    public static Query generateContainsDuplicatesMNQuery() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "oldComs");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select department, company
      from Department, company
      where department.company CONTAINS company
      and company.name = "CompanyA"
    */
    public static Query generateContainsN1Query() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "company");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc2, "name");
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, v1);
        cs1.addConstraint(cc1);
        cs1.addConstraint(c1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select Alias
      from Company AS Alias
      NOT DISTINCT
    */
    public static Query generateSelectSimpleObjectQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.setDistinct(false);
        q1.alias(c1, "Alias");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        return q1;
    }

    /*
      select company, count(*)
      from Company, Department
      where company contains department
      group by company
    */
    public static Query generateSimpleGroupByQuery() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS,  qc2);
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
      select c1, c2
      from Company c1, Company c2
      where c1 = c2
    */
    public static Query generateWhereClassClassQuery() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2);
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
    public static Query generateWhereClassObjectQuery() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Company obj = (Company) data.get("CompanyA");
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }

    @Test
    public void testResults() throws Exception {
        Object[][] r = new Object[][] { { data.get("CompanyA") },
                { data.get("CompanyB") } };
        List res = osForOsTests.execute((Query) queries.get("SelectSimpleObject"));
        Assert.assertEquals(ObjectStoreTestUtils.toList(r).size(), res.size());
        Assert.assertEquals(ObjectStoreTestUtils.toList(r), res);
    }

    // estimate tests

    @Test
    public void testEstimateQueryNotNull() throws Exception {
        ResultsInfo er = osForOsTests.estimate((Query)queries.get("WhereClassClass"));
        if (er == null) {
            Assert.fail("a null ResultsInfo was returned");
        }
    }

    // reference and collection proxy tests

    @Test
    public void testCEOWhenSearchingForManager() throws Exception {
        // select manager where manager.name="EmployeeB1" (actually a CEO)
        QueryClass c1 = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("EmployeeB1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        q1.setConstraint(sc1);
        List l1 = osForOsTests.execute(q1);
        Assert.assertEquals(1, l1.size());
        CEO ceo = (CEO) (((ResultsRow) l1.get(0)).get(0));
        Assert.assertEquals(ceo.toString(), 45000, ceo.getSalary());
    }

    @Test
    public void testLazyCollection() throws Exception {
        List r = osForOsTests.execute((Query) queries.get("ContainsN1"));
        Department d = (Department) ((ResultsRow) r.get(0)).get(0);
        Assert.assertTrue("Expected " + d.getEmployees().getClass() + " to be a Lazy object", d.getEmployees() instanceof Lazy);

        Set expected = new HashSet();
        expected.add(data.get("EmployeeA1"));
        expected.add(data.get("EmployeeA2"));
        expected.add(data.get("EmployeeA3"));
        Assert.assertEquals(expected, new HashSet(d.getEmployees()));
    }

    @Test
    public void testLazyCollectionMtoN() throws Exception {
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        q1.setConstraint(sc1);
        Results r  = osForOsTests.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Company c = (Company) rr.get(0);
        Assert.assertTrue("Expected " + c.getContractors().getClass() + " to be a Lazy object", c.getContractors() instanceof Lazy);
        Set contractors = new HashSet(c.getContractors());
        Set expected1 = new HashSet();
        expected1.add(data.get("ContractorA"));
        expected1.add(data.get("ContractorB"));
        Assert.assertEquals(expected1, contractors);

        Contractor contractor1 = (Contractor) contractors.iterator().next();
        Assert.assertTrue("Expected " + contractor1.getCompanys().getClass() + " to be a Lazy object", contractor1.getCompanys() instanceof Lazy);
        Set expected2 = new HashSet();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        Assert.assertEquals(expected2, new HashSet(contractor1.getCompanys()));
    }

    // setDistinct tests

    @Test
    public void testCountNoGroupByNotDistinct() throws Exception {
        Query q = QueryCloner.cloneQuery((Query) queries.get("ContainsDuplicatesMN"));
        q.setDistinct(false);
        int count = osForOsTests.count(q, ObjectStore.SEQUENCE_IGNORE);
        Assert.assertEquals(4, count);
        Assert.assertEquals(4, osForOsTests.execute(q).size());
    }

    @Test
    public void testCountNoGroupByDistinct() throws Exception {
        Query q = (Query) queries.get("ContainsDuplicatesMN");
        int count = osForOsTests.count(q, ObjectStore.SEQUENCE_IGNORE);
        Assert.assertEquals(4, count);
        Assert.assertEquals(4, osForOsTests.execute(q).size());
    }

    @Test
    public void testCountGroupByNotDistinct() throws Exception {
        Query q = QueryCloner.cloneQuery((Query) queries.get("SimpleGroupBy"));
        q.setDistinct(false);
        int count = osForOsTests.count(q, ObjectStore.SEQUENCE_IGNORE);
        Assert.assertEquals(2, count);
        Assert.assertEquals(2, osForOsTests.execute(q).size());
    }

    @Test
    public void testCountGroupByDistinct() throws Exception {
        // distinct doesn't actually do anything to group by reuslt
        Query q = (Query) queries.get("SimpleGroupBy");
        int count = osForOsTests.count(q, ObjectStore.SEQUENCE_IGNORE);
        Assert.assertEquals(2, count);
        Assert.assertEquals(2, osForOsTests.execute(q).size());
    }

    // getObjectByExample tests

    @Test
    public void testGetObjectByExampleNull() throws Exception {
        try {
            osForOsTests.getObjectByExample(null, Collections.singleton("name"));
            Assert.fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testGetObjectByExampleNonExistent() throws Exception {
        Address a = new Address();
        a.setAddress("10 Downing Street");
        Assert.assertNull(osForOsTests.getObjectByExample(a, Collections.singleton("address")));
    }

    @Test
    public void testGetObjectByExampleAttribute() throws Exception {
        Address a1 = ((Employee) data.get("EmployeeA1")).getAddress();
        Address a = new Address();
        a.setAddress(a1.getAddress());
        Assert.assertEquals(a1, osForOsTests.getObjectByExample(a, Collections.singleton("address")));
    }

    @Test
    public void testGetObjectByExampleFields() throws Exception {
        Employee e1 = (Employee) data.get("EmployeeA1");
        Employee e = new Employee();
        e.setName(e1.getName());
        e.setAge(e1.getAge());
        e.setAddress(e1.getAddress());
        Assert.assertEquals(e1, osForOsTests.getObjectByExample(e, new HashSet(Arrays.asList(new String[] {"name", "age", "address"}))));
    }

    @Test
    public void testDataTypes() throws Exception {
        Types d1 = (Types) data.get("Types1");
        //Types d2 = new Types();
        //d2.setName(d1.getName());
        Query q = new Query();
        QueryClass c = new QueryClass(Types.class);
        q.addFrom(c);
        q.addToSelect(c);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryField name = new QueryField(c, "name");
        QueryField booleanType = new QueryField(c, "booleanType");
        QueryField floatType = new QueryField(c, "floatType");
        QueryField doubleType = new QueryField(c, "doubleType");
        QueryField shortType = new QueryField(c, "shortType");
        QueryField intType = new QueryField(c, "intType");
        QueryField longType = new QueryField(c, "longType");
        QueryField booleanObjType = new QueryField(c, "booleanObjType");
        QueryField floatObjType = new QueryField(c, "floatObjType");
        QueryField doubleObjType = new QueryField(c, "doubleObjType");
        QueryField shortObjType = new QueryField(c, "shortObjType");
        QueryField intObjType = new QueryField(c, "intObjType");
        QueryField longObjType = new QueryField(c, "longObjType");
        QueryField bigDecimalObjType = new QueryField(c, "bigDecimalObjType");
        QueryField stringObjType = new QueryField(c, "stringObjType");
        QueryField dateObjType = new QueryField(c, "dateObjType");
        q.addToSelect(name);
        q.addToSelect(booleanType);
        q.addToSelect(floatType);
        q.addToSelect(doubleType);
        q.addToSelect(shortType);
        q.addToSelect(intType);
        q.addToSelect(longType);
        q.addToSelect(booleanObjType);
        q.addToSelect(floatObjType);
        q.addToSelect(doubleObjType);
        q.addToSelect(shortObjType);
        q.addToSelect(intObjType);
        q.addToSelect(longObjType);
        q.addToSelect(bigDecimalObjType);
        q.addToSelect(stringObjType);
        q.addToSelect(dateObjType);
        cs.addConstraint(new SimpleConstraint(name, ConstraintOp.EQUALS, new QueryValue("Types1")));
        cs.addConstraint(new SimpleConstraint(booleanType, ConstraintOp.EQUALS, new QueryValue(Boolean.TRUE)));
        cs.addConstraint(new SimpleConstraint(floatType, ConstraintOp.EQUALS, new QueryValue(new Float(0.6F))));
        cs.addConstraint(new SimpleConstraint(doubleType, ConstraintOp.EQUALS, new QueryValue(new Double(0.88D))));
        cs.addConstraint(new SimpleConstraint(shortType, ConstraintOp.EQUALS, new QueryValue(new Short((short) 675))));
        cs.addConstraint(new SimpleConstraint(intType, ConstraintOp.EQUALS, new QueryValue(new Integer(267))));
        cs.addConstraint(new SimpleConstraint(longType, ConstraintOp.EQUALS, new QueryValue(new Long(98729353495843l))));
        cs.addConstraint(new SimpleConstraint(booleanObjType, ConstraintOp.EQUALS, new QueryValue(Boolean.TRUE)));
        cs.addConstraint(new SimpleConstraint(floatObjType, ConstraintOp.EQUALS, new QueryValue(new Float(1.6F))));
        cs.addConstraint(new SimpleConstraint(doubleObjType, ConstraintOp.EQUALS, new QueryValue(new Double(1.88D))));
        cs.addConstraint(new SimpleConstraint(shortObjType, ConstraintOp.EQUALS, new QueryValue(new Short((short) 1982))));
        cs.addConstraint(new SimpleConstraint(intObjType, ConstraintOp.EQUALS, new QueryValue(new Integer(369))));
        cs.addConstraint(new SimpleConstraint(longObjType, ConstraintOp.EQUALS, new QueryValue(new Long(38762874323212l))));
        cs.addConstraint(new SimpleConstraint(bigDecimalObjType, ConstraintOp.EQUALS, new QueryValue(new BigDecimal("876323428764587621764532432.8768173432887324123645"))));
        cs.addConstraint(new SimpleConstraint(stringObjType, ConstraintOp.EQUALS, new QueryValue("A test String")));
        cs.addConstraint(new SimpleConstraint(dateObjType, ConstraintOp.EQUALS, new QueryValue(new Date(7777777l))));

        q.setConstraint(cs);
        Results res = osForOsTests.execute(q);
        List row1 = (List) res.get(0);
        Types d = (Types) row1.get(0);

        //Types d = (Types) (osForOsTests.getObjectByExample(d2));

        // Go through each attribute to check that it has been set correctly
        Assert.assertEquals(d1.getName(), d.getName());
        Assert.assertEquals(d1.getName(), row1.get(1));
        Assert.assertEquals(d1.getBooleanType(), d.getBooleanType());
        Assert.assertEquals(Boolean.class, row1.get(2).getClass());
        Assert.assertEquals(d1.getBooleanType(), ((Boolean) row1.get(2)).booleanValue());
        Assert.assertEquals(d1.getFloatType(), d.getFloatType(), 0.0);
        Assert.assertEquals(Float.class, row1.get(3).getClass());
        Assert.assertEquals(d1.getFloatType(), ((Float) row1.get(3)).floatValue(), 0.0);
        Assert.assertEquals(d1.getDoubleType(), d.getDoubleType(), 0.0);
        Assert.assertEquals(Double.class, row1.get(4).getClass());
        Assert.assertEquals(d1.getDoubleType(), ((Double) row1.get(4)).doubleValue(), 0.0);
        Assert.assertEquals(d1.getShortType(), d.getShortType());
        Assert.assertEquals(Short.class, row1.get(5).getClass());
        Assert.assertEquals(d1.getShortType(), ((Short) row1.get(5)).shortValue());
        Assert.assertEquals(d1.getIntType(), d.getIntType());
        Assert.assertEquals(Integer.class, row1.get(6).getClass());
        Assert.assertEquals(d1.getIntType(), ((Integer) row1.get(6)).intValue());
        Assert.assertEquals(d1.getLongType(), d.getLongType());
        Assert.assertEquals(Long.class, row1.get(7).getClass());
        Assert.assertEquals(d1.getLongType(), ((Long) row1.get(7)).longValue());
        Assert.assertEquals(d1.getBooleanObjType(), d.getBooleanObjType());
        Assert.assertEquals(d1.getBooleanObjType(), row1.get(8));
        Assert.assertEquals(d1.getFloatObjType(), d.getFloatObjType());
        Assert.assertEquals(d1.getFloatObjType(), row1.get(9));
        Assert.assertEquals(d1.getDoubleObjType(), d.getDoubleObjType());
        Assert.assertEquals(d1.getDoubleObjType(), row1.get(10));
        Assert.assertEquals(d1.getShortObjType(), d.getShortObjType());
        Assert.assertEquals(d1.getShortObjType(), row1.get(11));
        Assert.assertEquals(d1.getIntObjType(), d.getIntObjType());
        Assert.assertEquals(d1.getIntObjType(), row1.get(12));
        Assert.assertEquals(d1.getLongObjType(), d.getLongObjType());
        Assert.assertEquals(d1.getLongObjType(), row1.get(13));
        Assert.assertEquals(d1.getBigDecimalObjType(), d.getBigDecimalObjType());
        Assert.assertEquals(d1.getBigDecimalObjType(), row1.get(14));
        Assert.assertEquals(d1.getStringObjType(), d.getStringObjType());
        Assert.assertEquals(d1.getStringObjType(), row1.get(15));
        Assert.assertEquals(d1.getDateObjType(), d.getDateObjType());
        Assert.assertEquals(Date.class, row1.get(16).getClass());
        Assert.assertEquals(d1.getDateObjType(), row1.get(16));
    }

    @Test
    public void testGetObjectByNullId() throws Exception {
        try {
            osForOsTests.getObjectById(null);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testGetObjectById() throws Exception {
        Integer id = ((Employee) data.get("EmployeeA1")).getId();
        Employee e = (Employee) osForOsTests.getObjectById(id, Employee.class);
        Assert.assertEquals(data.get("EmployeeA1"), e);
        Assert.assertTrue(e == osForOsTests.getObjectById(id, Employee.class));
    }

    @Test
    public void testGetObjectMultipleTimes() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select Secretary from Secretary where Secretary.name = 'Secretary1'", "org.intermine.model.testmodel"));
        Secretary a = (Secretary) ((List) osForOsTests.execute(q).get(0)).get(0);

        Secretary b = (Secretary) osForOsTests.getObjectById(a.getId(), Secretary.class);
        Secretary c = (Secretary) osForOsTests.getObjectById(a.getId(), Secretary.class);
        Assert.assertEquals(b, c);
        Assert.assertTrue(b == c);
        Assert.assertEquals(a, b);
        Assert.assertTrue(a == b);
    }

    @Test
    public void testIndirectionTableMultipleCopies() throws Exception {
        Contractor c1 = new Contractor();
        c1.setName("Clippy");
        Company c2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c2.setName("?bersoft");
        c2.addContractors(c1);
        c1.addCompanys(c2);

        storeDataWriter.store(c1);
        storeDataWriter.store(c2);

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("Clippy"));
        ContainsConstraint cc = new ContainsConstraint(new QueryCollectionReference(qc1, "companys"), ConstraintOp.CONTAINS, qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(sc);
        cs.addConstraint(cc);
        q1.setConstraint(cs);
        q1.setDistinct(false);

        try {
            Results r1 = osForOsTests.execute(q1);
            Assert.assertEquals(1, r1.size());

            storeDataWriter.store(c1);
            Results r2 = osForOsTests.execute(q1);
            Assert.assertEquals(1, r1.size());
        } finally {
            storeDataWriter.delete(c1);
            storeDataWriter.delete(c2);
        }
    }

    @Test
    public void testSimpleObjects() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(SimpleObject.class);
        QueryField qf = new QueryField(qc, "name");
        q.addFrom(qc);
        q.addToSelect(qc);
        Constraint con = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("Albert"));
        q.setConstraint(con);

        // Clean up any leftover objects in case of previous test run failures
        deleteObjects(qc, con, q);

        SimpleObject so = new SimpleObject();
        so.setName("Albert");
        storeDataWriter.store(so);
        SingletonResults res = osForOsTests.executeSingleton(q);
        Assert.assertEquals(1, res.size());
        SimpleObject got = (SimpleObject)res.get(0);
        Assert.assertEquals("Albert", got.getName());
        Assert.assertNull(got.getEmployee());

        deleteObjects(qc, con, q);
    }

    private void deleteObjects(QueryClass qc, Constraint con, Query checkQuery) throws Exception {
        storeDataWriter.delete(qc, con);
        SingletonResults res = osForOsTests.executeSingleton(checkQuery);
        Assert.assertEquals(0, res.size());
    }

    @Test
    public void testNullFields() throws Exception {
        Employee ex = new Employee();
        ex.setName("EmployeeB1");
        Employee e = (Employee) osForOsTests.getObjectByExample(ex, Collections.singleton("name"));
        Assert.assertNull(e.proxGetAddress());
        Assert.assertNull(e.getAddress());
    }

    @Test
    public void testEmptySelect() throws Exception {
        try {
            Query q = new Query();
            QueryClass qc = new QueryClass(Employee.class);
            q.addFrom(qc);
            q.setDistinct(false);
            osForOsTests.execute(q, 0, 100, false, false, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected exception");
        } catch (ObjectStoreException e) {
            Assert.assertEquals("SELECT list is empty in Query", e.getMessage());
        }
    }
}
