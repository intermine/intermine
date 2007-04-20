package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Date;
import java.math.BigDecimal;

import junit.framework.AssertionFailedError;

import org.intermine.SummaryAssertionFailedError;
import org.intermine.SummaryException;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.*;
import org.intermine.objectstore.query.*;
import org.intermine.util.DynamicUtil;

/**
 * TestCase for testing InterMine Queries
 * To check results:
 * add results to the results map
 * override executeTest to run query and assert that the result is what is expected
 */

public abstract class ObjectStoreQueriesTestCase extends QueryTestCase
{
    public static final Object NO_RESULT = new Object() {
        public String toString() { return "NO RESULT"; } };

    protected static Map queries = new HashMap();
    protected static Map results = new LinkedHashMap();
    protected boolean strictTestQueries = true;

    /**
     * Constructor
     */
    public ObjectStoreQueriesTestCase(String arg) {
        super(arg);
    }

    /**
     * Set up the test
     *
     * @throws Exception if an error occurs
     */
    public static void oneTimeSetUp() throws Exception {
        QueryTestCase.oneTimeSetUp();
        setUpQueries();
        setUpResults();
    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public static void setUpResults() throws Exception {
    }

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * those expected.
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
        StringWriter errorMessage = new StringWriter();
        PrintWriter writer = new PrintWriter(errorMessage);
        int failureCount = 0;
        int errorCount = 0;
        Iterator i = results.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            // Does this appear in the queries map;
            if (!(queries.containsKey(type))) {
                writer.println("\n" + type + " does not appear in the queries map");
                failureCount++;
            } else {
                Object result = results.get(type);
                if (result != NO_RESULT) {
                    long startTime = System.currentTimeMillis();
                    try {
                        executeTest(type);
                    } catch (AssertionFailedError e) {
                        writer.println("\n" + type + " has failed: " + e.getMessage());
                        //e.printStackTrace(writer);
                        failureCount++;
                    } catch (Throwable t) {
                        writer.println("\n" + type + " produced an error:");
                        t.printStackTrace(writer);
                        errorCount++;
                    } finally {
                        System.out.println("Test " + type + " took " + (System.currentTimeMillis() - startTime) + " ms");
                    }
                }
            }
        }
        i = queries.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            Object result = results.get(type);
            if (result == null) {
                if (strictTestQueries) {
                    writer.println("\n" + type + " does not appear in the results map");
                    failureCount++;
                }
            }
        }
        writer.flush();
        errorMessage.flush();

        if (errorCount > 0) {
            if (failureCount > 0) {
                throw new SummaryException(errorMessage.toString(), errorCount + " errors and " + failureCount + " failures present");
            } else {
                throw new SummaryException(errorMessage.toString(), errorCount + " errors present");
            }
        } else if (failureCount > 0) {
            throw new SummaryAssertionFailedError(errorMessage.toString(), failureCount + " failures present");
        }
    }

    /**
     * Set up the set of queries we are testing
     *
     * @throws Exception if an error occurs
     */
    public static void setUpQueries() throws Exception {
        queries.put("SelectSimpleObject", selectSimpleObject());
        queries.put("SubQuery", subQuery());
        queries.put("WhereSimpleEquals", whereSimpleEquals());
        queries.put("WhereSimpleNotEquals", whereSimpleNotEquals());
        queries.put("WhereSimpleNegEquals", whereSimpleNegEquals());
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
        queries.put("Contains11", contains11());
        queries.put("ContainsNot11", containsNot11());
        queries.put("ContainsNeg11", containsNeg11());
        queries.put("Contains1N", contains1N());
        queries.put("ContainsNot1N", containsNot1N());
        queries.put("ContainsN1", containsN1());
        queries.put("ContainsMN", containsMN());
        queries.put("ContainsNotMN", containsNotMN());
        queries.put("ContainsDuplicatesMN", containsDuplicatesMN());
        queries.put("SimpleGroupBy", simpleGroupBy());
        queries.put("MultiJoin", multiJoin());
        queries.put("SelectComplex", selectComplex());
        queries.put("SelectClassAndSubClasses", selectClassAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses", selectInterfaceAndSubClasses());
        queries.put("SelectInterfaceAndSubClasses2", selectInterfaceAndSubClasses2());
        queries.put("SelectInterfaceAndSubClasses3", selectInterfaceAndSubClasses3());
        //queries.put("SelectClassFromSubQuery", selectClassFromSubQuery());
        queries.put("OrderByAnomaly", orderByAnomaly());
        queries.put("SelectUnidirectionalCollection", selectUnidirectionalCollection());
        queries.put("EmptyAndConstraintSet", emptyAndConstraintSet());
        queries.put("EmptyOrConstraintSet", emptyOrConstraintSet());
        queries.put("EmptyNandConstraintSet", emptyNandConstraintSet());
        queries.put("EmptyNorConstraintSet", emptyNorConstraintSet());
        queries.put("BagConstraint", bagConstraint());
        queries.put("InterfaceField", interfaceField());
        queries.put("DynamicInterfacesAttribute", dynamicInterfacesAttribute());
        queries.put("DynamicClassInterface", dynamicClassInterface());
        queries.put("DynamicClassRef1", dynamicClassRef1());
        queries.put("DynamicClassRef2", dynamicClassRef2());
        queries.put("DynamicClassRef3", dynamicClassRef3());
        queries.put("DynamicClassRef4", dynamicClassRef4());
        queries.put("DynamicClassConstraint", dynamicClassConstraint());
        queries.put("ContainsConstraintNull", containsConstraintNull());
        queries.put("ContainsConstraintNotNull", containsConstraintNotNull());
        queries.put("SimpleConstraintNull", simpleConstraintNull());
        queries.put("SimpleConstraintNotNull", simpleConstraintNotNull());
        queries.put("TypeCast", typeCast());
        queries.put("IndexOf", indexOf());
        queries.put("Substring", substring());
        queries.put("Substring2", substring2());
        queries.put("OrderByReference", orderByReference());
        queries.put("FailDistinctOrder", failDistinctOrder());
        queries.put("NegativeNumbers", negativeNumbers());
        queries.put("Lower", lower());
        queries.put("Upper", upper());

        // test 'foo' IN bag
        queries.put("LargeBagConstraint", largeBagConstraint(false));
        // test 'foo' NOT IN bag
        queries.put("LargeBagNotConstraint", largeBagConstraint(true));

        // tests using a temporary table for the bag - 'foo' IN bag
        queries.put("LargeBagConstraintUsingTable", largeBagConstraint(false));

        // tests using a temporary table for the bag - 'foo' NOT IN bag
        queries.put("LargeBagNotConstraintUsingTable", largeBagConstraint(true));
        queries.put("SubqueryExistsConstraint", subqueryExistsConstraint());
        queries.put("NotSubqueryExistsConstraint", notSubqueryExistsConstraint());
        queries.put("SubqueryExistsConstraintNeg", subqueryExistsConstraintNeg());
        queries.put("ObjectPathExpression", objectPathExpression());
        queries.put("FieldPathExpression", fieldPathExpression());
        queries.put("ForeignKey", foreignKey());
        queries.put("ForeignKey2", foreignKey2());
        queries.put("OrSubquery", orSubquery());
        queries.put("ScientificNumber", scientificNumber());
        queries.put("LowerBag", lowerBag());
        queries.put("FetchBag", fetchBag());
        queries.put("ObjectStoreBag", objectStoreBag());
        queries.put("ObjectStoreBagQueryClass", objectStoreBagQueryClass());
        queries.put("OrderDescending", orderDescending());
        queries.put("ObjectStoreBagCombination", objectStoreBagCombination());
    }

    /*
      select Alias
      from Company AS Alias
      NOT DISTINCT
    */
    public static Query selectSimpleObject() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.setDistinct(false);
        q1.alias(c1, "Alias");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        return q1;
    }

    /*
      select All.Array.name, All.alias as Alias
      from (select Array, 5 as Alias from Company AS Array) as All
    */
    public static Query subQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        Query q1 = new Query();
        q1.alias(c1, "Array");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        q1.alias(v1, "Alias");
        q1.addToSelect(v1);
        Query q2 = new Query();
        q2.alias(q1, "All");
        q2.addFrom(q1);
        QueryField f1 = new QueryField(q1, c1, "name");
        QueryField f2 = new QueryField(q1, v1);
        q2.addToSelect(f1);
        q2.alias(f2, "Alias");
        q2.addToSelect(f2);
        return q2;
    }

    /*
      select name
      from Company
      where vatNumber = 1234
    */
    public static Query whereSimpleEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
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
    public static Query whereSimpleNotEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.NOT_EQUALS, v1);
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
    public static Query whereSimpleNegEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(1234));
        QueryField f1 = new QueryField(c1, "vatNumber");
        QueryField f2 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        sc1.negate();
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
    public static Query whereSimpleLike() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
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
    public static Query whereEqualString() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
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
    public static Query whereAndSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
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
    public static Query whereOrSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("CompanyA%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
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
    public static Query whereNotSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("Company%");
        QueryValue v2 = new QueryValue(new Integer(2000));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        cs1.negate();
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select department
      from Department
      where department.name IN (select name from Department)
      order by Department.name
    */
    public static Query whereSubQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f2 = new QueryField(c2, "name");
        SubqueryConstraint sqc1 = new SubqueryConstraint(f2, ConstraintOp.IN, q1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        q2.addToOrderBy(f2);
        return q2;
    }

    /*
      select company
      from Company
      where company IN (select company from Company where name = "CompanyA")
    */
    public static Query whereSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, ConstraintOp.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(c2, ConstraintOp.IN, q1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select company
      from Company
      where company NOT IN (select company from Company where name = "CompanyA")
    */
    public static Query whereNotSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, ConstraintOp.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(c2, ConstraintOp.NOT_IN, q1);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    /*
      select company
      from Company
      where not company IN (select company from Company where name = "CompanyA")
    */
    public static Query whereNegSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("CompanyA");
        q1.setConstraint(new SimpleConstraint(f1, ConstraintOp.EQUALS, v1));
        QueryClass c2 = new QueryClass(Company.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(c2, ConstraintOp.IN, q1);
        sqc1.negate();
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
    public static Query whereClassClass() throws Exception {
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
      select c1, c2
      from Company c1, Company c2
      where c1 != c2
    */
    public static Query whereNotClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.NOT_EQUALS, qc2);
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
    public static Query whereNegClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2);
        cc1.negate();
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select department, manager
      from Department, Manager
      where department.manager CONTAINS manager
      and department.name = "DepartmentA1"
    */

      public static Query contains11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
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
      select department, manager
      from Department, Manager
      where department.manager DOES NOT CONTAIN manager
      and department.name = "DepartmentA1"
    */

      public static Query containsNot11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.DOES_NOT_CONTAIN, qc2);
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
      select department, manager
      from Department, Manager
      where (not department.manager CONTAINS manager)
      and department.name = "DepartmentA1"
    */

      public static Query containsNeg11() throws Exception {
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryReference qr1 = new QueryObjectReference(qc1, "manager");
        QueryValue v1 = new QueryValue("DepartmentA1");
        QueryField qf1 = new QueryField(qc1, "name");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        cc1.negate();
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
      select company, department
      from Company, Department
      where company.departments contains department
      and company.name = "CompanyA"
    */
      public static Query contains1N() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc1, "name");
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
      select company, department
      from Company, Department
      where company.departments DOES NOT contain department
      and company.name = "CompanyA"
    */
      public static Query containsNot1N() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.DOES_NOT_CONTAIN, qc2);
        QueryValue v1 = new QueryValue("CompanyA");
        QueryField qf1 = new QueryField(qc1, "name");
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
      select department, company
      from Department, company
      where department.company CONTAINS company
      and company.name = "CompanyA"
    */
      public static Query containsN1() throws Exception {
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
      select contractor, company
      from Contractor, Company
      where contractor.companys contains company
      and contractor.name = "ContractorA"
    */
    public static Query containsMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "companys");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2);
        QueryValue v1 = new QueryValue("ContractorA");
        QueryField qf1 = new QueryField(qc1, "name");
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
      select contractor, company
      from Contractor, Company
      where contractor.oldComs CONTAINS company
    */
    public static Query containsDuplicatesMN() throws Exception {
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
      select contractor, company
      from Contractor, Company
      where contractor.companys DOES NOT CONTAIN company
      and contractor.name = "ContractorA"
    */
    public static Query containsNotMN() throws Exception {
        QueryClass qc1 = new QueryClass(Contractor.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryReference qr1 = new QueryCollectionReference(qc1, "companys");
        ContainsConstraint cc1 = new ContainsConstraint(qr1, ConstraintOp.DOES_NOT_CONTAIN, qc2);
        QueryValue v1 = new QueryValue("ContractorA");
        QueryField qf1 = new QueryField(qc1, "name");
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
      select company, count(*)
      from Company, Department
      where company contains department
      group by company
    */
    public static Query simpleGroupBy() throws Exception {
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
      select company, department, manager, address
      from Company, Department, Manager, Address
      where company contains department
      and department.manager CONTAINS manager
      and manager.address CONTAINS address
      and manager.name = "EmployeeA1"
    */
    public static Query multiJoin() throws Exception {
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
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qc2));
        cs1.addConstraint(new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qc3));
        cs1.addConstraint(new ContainsConstraint(qr3, ConstraintOp.CONTAINS, qc4));
        cs1.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, qv1));
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select avg(company.vatNumber) + 20, stddev(company.vatNumber), department.name, department
      from Company, Department
      group by department
    */
    public static Query selectComplex() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        QueryField f3 = new QueryField(c2, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addFrom(c2);
        QueryExpression e1 = new QueryExpression(new QueryFunction(f2, QueryFunction.AVERAGE),
                QueryExpression.ADD, new QueryValue(new Integer(20)));
        QueryFunction e2 = new QueryFunction(f2, QueryFunction.STDDEV);
        q1.addToSelect(e1);
        q1.addToSelect(e2);
        q1.addToSelect(f3);
        q1.addToSelect(c2);
        q1.addToGroupBy(c2);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND ALL EMPLOYEES
      select employee
      from Employee
      order by employee.name
    */
    public static Query selectClassAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employee.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        QueryField f1 = new QueryField(qc1, "name");
        q1.addToOrderBy(f1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS, CONTRACTORS AND ALL EMPLOYEES
      select employable
      from Employable
    */
    public static Query selectInterfaceAndSubClasses() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE DEPARTMENTS AND COMPANIES
      select randominterface
      from RandomInterface
    */
    public static Query selectInterfaceAndSubClasses2() throws Exception {
        QueryClass qc1 = new QueryClass(RandomInterface.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS AND CONTRACTORS
      select ImportantPerson
      from ImportantPerson
    */
    public static Query selectInterfaceAndSubClasses3() throws Exception {
        QueryClass qc1 = new QueryClass(ImportantPerson.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        return q1;
    }

    /*
      select company
      from (select company from Company) as subquery, Company
      where company = subquery.company
    */
    /*
     * TODO: this currently cannot be done.
    public static Query selectClassFromSubQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryClass c2 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Query q2 = new Query();
        q2.addFrom(q1);
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(new ClassConstraint(c1, ConstraintOp.EQUALS, c2));
        return q2;
    }
    */

    /*
     * select 5 as a2_, Company.name as a3_ from Company
     */
    public static Query orderByAnomaly() throws Exception {
        QueryClass c = new QueryClass(Company.class);
        Query q = new Query();
        q.addFrom(c);
        q.addToSelect(new QueryValue(new Integer(5)));
        q.addToSelect(new QueryField(c, "name"));
        return q;
    }

    /*
     * select Secretary from Company, Secretary where Company.name = 'CompanyA' AND Company.secretarys CONTAINS Secretary
     */
    public static Query selectUnidirectionalCollection() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Secretary.class);
        Query q = new Query();
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc2);
        ConstraintSet qs = new ConstraintSet(ConstraintOp.AND);
        qs.addConstraint(new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("CompanyA")));
        qs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "secretarys"), ConstraintOp.CONTAINS, qc2));
        q.setConstraint(qs);
        return q;
    }

    public static Query emptyAndConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.AND));
        return q;
    }

    public static Query emptyOrConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.OR));
        return q;
    }

    public static Query emptyNandConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.NAND));
        return q;
    }

    public static Query emptyNorConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new ConstraintSet(ConstraintOp.NOR));
        return q;
    }

    /*
      select Company
      from Company
      where Company.name in ("hello", "goodbye")
    */
    public static Query bagConstraint() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.alias(c1, "Company");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        HashSet set = new LinkedHashSet();
        set.add("hello");
        set.add("goodbye");
        set.add("CompanyA");
        set.add(new Integer(5));
        q1.setConstraint(new BagConstraint(new QueryField(c1, "name"), ConstraintOp.IN, set));
        return q1;
    }

    /*
      SHOULD PICK UP THE MANAGERS, CONTRACTORS AND ALL EMPLOYEES
      select employable
      from Employable where Employable.name = "EmployeeA1"
    */
    public static Query interfaceField() throws Exception {
        QueryClass qc1 = new QueryClass(Employable.class);
        Query q1 = new Query();
        q1.addToSelect(qc1);
        q1.addFrom(qc1);
        q1.setConstraint(new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("EmployeeA1")));
        return q1;
    }

    /*
      select a1_, a1_.debt, a1_.age from (Broke, Employee) as a1_
      where a1_.debt > 0 and a1_.age > 0;

      Checks Attributes, and that they are sourced from the correct table
      Checks that two Interfaces can be combined
    */
    public static Query dynamicInterfacesAttribute() throws Exception {
        Set classes = new HashSet();
        classes.add(Broke.class);
        classes.add(Employee.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryField f1 = new QueryField(qc1, "debt");
        QueryField f2 = new QueryField(qc1, "age");
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.addToSelect(f1);
        q1.addToSelect(f2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new SimpleConstraint(f1, ConstraintOp.GREATER_THAN, new QueryValue(new Integer(0))));
        cs.addConstraint(new SimpleConstraint(f2, ConstraintOp.GREATER_THAN, new QueryValue(new Integer(0))));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_ from (Broke, Employable);

      Checks that a Class can be combined with an Interface
    */
    public static Query dynamicClassInterface() throws Exception {
        Set classes = new HashSet();
        classes.add(Broke.class);
        classes.add(Employable.class);
        QueryClass qc1 = new QueryClass(classes);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        return q1;
    }

    /*
      select a1_, a2_, a3_ from (Department, Broke) as a1_, Company as a2_, Bank as a3_
      where a2_.departments contains a1_ and a3_.debtors contains a1_
    */
    public static Query dynamicClassRef1() throws Exception {
        Set classes = new HashSet();
        classes.add(Department.class);
        classes.add(Broke.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryClass qc3 = new QueryClass(Bank.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc2, "departments"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc3, "debtors"), ConstraintOp.CONTAINS, qc1));
        q1.setConstraint(cs);
        return q1;
    }
    
    /*
      select a1_, a2_, a3_ from (Department, Broke) as a1_, Company as a2_, Bank as a3_
      where a1_.company contains a2_ and a1_.bank contains a3_
    */
    public static Query dynamicClassRef2() throws Exception {
        Set classes = new HashSet();
        classes.add(Department.class);
        classes.add(Broke.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryClass qc3 = new QueryClass(Bank.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "company"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "bank"), ConstraintOp.CONTAINS, qc3));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_, a2_, a3_ from (Company, Bank) as a1_, Department as a2_, Broke as a3_
      where a1_.departments contains a2_ and a1_.debtors contains a3_
    */
    public static Query dynamicClassRef3() throws Exception {
        Set classes = new HashSet();
        classes.add(Company.class);
        classes.add(Bank.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Broke.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "departments"), ConstraintOp.CONTAINS, qc2));
        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "debtors"), ConstraintOp.CONTAINS, qc3));
        q1.setConstraint(cs);
        return q1;
    }
 
    /*
      select a1_, a2_, a3_ from (Company, Bank) as a1_, Department as a2_, Broke as a3_
      where a2_.company contains a1_ and a3_.bank contains a1_
    */
    public static Query dynamicClassRef4() throws Exception {
        Set classes = new HashSet();
        classes.add(Company.class);
        classes.add(Bank.class);
        QueryClass qc1 = new QueryClass(classes);
        QueryClass qc2 = new QueryClass(Department.class);
        QueryClass qc3 = new QueryClass(Broke.class);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addFrom(qc3);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.addToSelect(qc3);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc2, "company"), ConstraintOp.CONTAINS, qc1));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc3, "bank"), ConstraintOp.CONTAINS, qc1));
        q1.setConstraint(cs);
        return q1;
    }

    /*
      select a1_ from (Broke, Employable) as a1_, (Broke, HasAddress) as a2_ where a1_ = a2_;
    */
    public static Query dynamicClassConstraint() throws Exception {
        Set classes = new HashSet();
        classes.add(Broke.class);
        classes.add(Employable.class);
        QueryClass qc1 = new QueryClass(classes);
        classes = new HashSet();
        classes.add(Broke.class);
        classes.add(HasAddress.class);
        QueryClass qc2 = new QueryClass(classes);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.setConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2));
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.address IS NULL;
     */
    public static Query containsConstraintNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(new QueryObjectReference(qc, "address"),
                ConstraintOp.IS_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.address IS NOT NULL;
     */
    public static Query containsConstraintNotNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        ContainsConstraint c = new ContainsConstraint(new QueryObjectReference(qc, "address"),
                ConstraintOp.IS_NOT_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Manager AS a1_ WHERE a1_.title IS NULL;
     */
    public static Query simpleConstraintNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Manager.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        SimpleConstraint c = new SimpleConstraint(new QueryField(qc, "title"), ConstraintOp.IS_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_ FROM Manager AS a1_ WHERE a1_.title IS NOT NULL;
     */
    public static Query simpleConstraintNotNull() throws Exception {
        Query q1 = new Query();
        QueryClass qc = new QueryClass(Manager.class);
        q1.addFrom(qc);
        q1.addToSelect(qc);
        SimpleConstraint c = new SimpleConstraint(new QueryField(qc, "title"), ConstraintOp.IS_NOT_NULL);
        q1.setConstraint(c);
        return q1;
    }

    /*
     * SELECT a1_.age::String from Employee AS a1_;
     */
    public static Query typeCast() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "age");
        QueryCast c = new QueryCast(f, String.class);
        q.addToSelect(c);
        return q;
    }

    /*
     * SELECT indexof(a1_.name, 'oy') from Employee AS a1_;
     */
    public static Query indexOf() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(f, QueryExpression.INDEX_OF, new QueryValue("oy"));
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT substr(a1_.name, 2, 2) AS a2_ FROM Employee AS a1_;
     */
    public static Query substring() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(f, new QueryValue(new Integer(2)), new QueryValue(new Integer(2)));
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT substr(a1_.name, 2) AS a2_ FROM Employee AS a1_;
     */
    public static Query substring2() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(f, QueryExpression.SUBSTRING, new QueryValue(new Integer(2)));
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT lower(a1_.name) AS a2_ FROM Employee AS a1_;
     */
    public static Query lower() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(QueryExpression.LOWER, f);
        q.addToSelect(e);
        return q;
    }

    /*
     * SELECT upper(a1_.name) AS a2_ FROM Employee AS a1_;
     */
    public static Query upper() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        QueryField f = new QueryField(qc, "name");
        QueryExpression e = new QueryExpression(QueryExpression.UPPER, f);
        q.addToSelect(e);
        return q;
    }

    /*
     * select a1_ FROM Employee AS a1_ ORDER BY a1_.department;
     */
    public static Query orderByReference() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToOrderBy(new QueryObjectReference(qc, "department"));
        return q;
    }

    /*
     * SELECT DISTINCT a1_.name FROM Employee AS a1_ ORDER BY a1_.age;
     */
    public static Query failDistinctOrder() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "name"));
        q.addToOrderBy(new QueryField(qc, "age"));
        q.setDistinct(true);
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS a1_ WHERE a1_.name IN (...)
     */
    public static Query largeBagConstraint(boolean makeNotConstraint) throws Exception {
        ConstraintOp constraintOp;

        if (makeNotConstraint) {
            constraintOp = ConstraintOp.NOT_IN;
        } else {
            constraintOp = ConstraintOp.IN;
        }

        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Set bag = new HashSet();
        bag.add("EmployeeA1");
        bag.add("EmployeeB2");
        for (int i = 0; i < 20000; i++) {
            bag.add("a" + i);
        }
        bag.add("a string with quotes: '\"");

        bag.add(new Short((short) 1000));
        bag.add(new Short((short) 1000));
        bag.add(new Integer(2000));
        bag.add(new Long(3000L));
        bag.add(new Long(3100L));
        bag.add(new Float(4000.0));
        bag.add(new BigDecimal(10000.0));
        bag.add(new Date(999999));
        bag.add(new Date(100));
        bag.add(new Boolean(true));
        bag.add(new Boolean(false));
        Employee employee = (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee.setId(new Integer(5000));
        bag.add(employee);
        Manager manager = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        manager.setId(new Integer(5001));
        bag.add(manager);
        Company company = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setId(new Integer(6000));
        bag.add(company);
        q.setConstraint(new BagConstraint(new QueryField(qc, "name"), constraintOp, bag));
        return q;
    }

    /*
     * SELECT a1_ FROM Employee AS 1_ WHERE a1_.age > -50
     */
    public static Query negativeNumbers() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "age"), ConstraintOp.GREATER_THAN, new QueryValue(new Integer(-51))));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE EXISTS (SELECT a1_ FROM Company AS a1_)
     */
    public static Query subqueryExistsConstraint() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(new QueryValue("hello"));
        Query q2 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q2.addFrom(qc);
        q2.addToSelect(qc);
        q2.setDistinct(false);
        q1.setConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, q2));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT 'hello' AS a1_ WHERE DOES NOT EXIST (SELECT a1_ FROM Company AS a1_)
     */
    public static Query notSubqueryExistsConstraint() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(new QueryValue("hello"));
        Query q2 = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q2.addFrom(qc);
        q2.addToSelect(qc);
        q2.setDistinct(false);
        q1.setConstraint(new SubqueryExistsConstraint(ConstraintOp.DOES_NOT_EXIST, q2));
        q1.setDistinct(false);
        return q1;
    }
    
    /*
     * SELECT 'hello' AS a1_ WHERE EXISTS (SELECT a1_ FROM Bank AS a1_)
     */
    public static Query subqueryExistsConstraintNeg() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(new QueryValue("hello"));
        Query q2 = new Query();
        QueryClass qc = new QueryClass(Bank.class);
        q2.addFrom(qc);
        q2.addToSelect(qc);
        q2.setDistinct(false);
        q1.setConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, q2));
        q1.setDistinct(false);
        return q1;
    }

    /*
     * SELECT a1_, a1_.department AS a2_ FROM Employee AS a1_
     */
    public static Query objectPathExpression() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryObjectPathExpression(qc, "department"));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.CEO.name(DEF: 'fred') AS a2_ FROM Company AS a1_
     */
    public static Query fieldPathExpression() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryFieldPathExpression(qc, "CEO", "name", "3fred"));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.CEO.id(DEF: null) AS a2_ FROM Company AS a1_
     */
    public static Query foreignKey() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryFieldPathExpression(qc, "CEO", "id", null));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_, a1_.CEO.id(DEF: 3) AS a2_ FROM Company AS a1_
     */
    public static Query foreignKey2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToSelect(new QueryFieldPathExpression(qc, "CEO", "id", new Integer(3)));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT a1_ FROM InterMineObject AS a1_ WHERE a1_ IN (SELECT a2_ FROM Company AS a2_) OR a1_ IN (SELECT a3_ FROM Broke AS a3_)
     */
    public static Query orSubquery() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
        q.setConstraint(cs);
        Query q2 = new Query();
        q2.setDistinct(false);
        QueryClass qc2 = new QueryClass(Company.class);
        q2.addFrom(qc2);
        q2.addToSelect(qc2);
        cs.addConstraint(new SubqueryConstraint(qc, ConstraintOp.IN, q2));
        Query q3 = new Query();
        q3.setDistinct(false);
        QueryClass qc3 = new QueryClass(Broke.class);
        q3.addFrom(qc3);
        q3.addToSelect(qc3);
        cs.addConstraint(new SubqueryConstraint(qc, ConstraintOp.IN, q3));
        return q;
    }

    /*
     * SELECT a1_ FROM Types AS a1_ WHERE a1_.doubleType < 1.3432E+4
     */
    public static Query scientificNumber() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Types.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "doubleType"), ConstraintOp.LESS_THAN, new QueryValue(new Double(1.3432E+24))));
        cs.addConstraint(new SimpleConstraint(new QueryField(qc, "floatType"), ConstraintOp.GREATER_THAN, new QueryValue(new Float(-8.56E-32))));
        return q;
    }

    /*
     * SELECT a1_ FROM Employee WHERE LOWER(a1_.name) IN ?
     */
    public static Query lowerBag() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        q.setConstraint(new BagConstraint(new QueryExpression(QueryExpression.LOWER, new QueryField(qc, "name")), ConstraintOp.IN, Arrays.asList(new String[] {"employeea1", "employeea2", "employeeb1"})));
        return q;
    }

    /*
     * SELECT BAG(5)
     */
    public static Query fetchBag() throws Exception {
        ObjectStoreBag osb = new ObjectStoreBag(5);
        Query q = new Query();
        q.addToSelect(osb);
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT Employee FROM Employee WHERE Employee IN BAG(5)
     */
    public static Query objectStoreBag() throws Exception {
        ObjectStoreBag osb = new ObjectStoreBag(5);
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, osb));
        return q;
    }

    /*
     * SELECT Department.id, Employee FROM Employee, BAG(5)::Department WHERE Department.employees CONTAINS Employee
     */
    public static Query objectStoreBagQueryClass() throws Exception {
        ObjectStoreBag osb = new ObjectStoreBag(5);
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        QueryClassBag qcb = new QueryClassBag(Department.class, osb);
        q.addFrom(qc);
        q.addFrom(qcb);
        q.addToSelect(new QueryField(qcb));
        q.addToSelect(qc);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb, "employees"), ConstraintOp.CONTAINS, qc));
        q.setDistinct(false);
        return q;
    }

    /*
     * SELECT Employee FROM Employee ORDER BY Employee DESC
     */
    public static Query orderDescending() throws Exception {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.addToOrderBy(new OrderDescending(qc));
        return q;
    }

    /*
     * SELECT BAG(5) UNION BAG(6)
     */
    public static Query objectStoreBagCombination() throws Exception {
        Query q = new Query();
        ObjectStoreBag osb1 = new ObjectStoreBag(5);
        ObjectStoreBag osb2 = new ObjectStoreBag(6);
        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(ObjectStoreBagCombination.UNION);
        osbc.addBag(osb1);
        osbc.addBag(osb2);
        q.addToSelect(osbc);
        q.setDistinct(false);
        return q;
    }
}
