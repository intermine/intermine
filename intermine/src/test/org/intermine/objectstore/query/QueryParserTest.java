package org.flymine.objectstore.query;

import junit.framework.Test;

import org.flymine.testing.OneTimeTestCase;
import org.flymine.objectstore.ObjectStoreQueriesTestCase;

/**
 * Test for testing the parser on the flymine query object.
 *
 * @author Matthew Wakeling
 */
public class QueryParserTest extends ObjectStoreQueriesTestCase
{
    public QueryParserTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(QueryParserTest.class);
    }
    
    public static void oneTimeSetUp() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeSetUp();

        setUpResults();
    }

    /**
     * Set up all the results expected for a given subset of queries
     */
    public static void setUpResults() {
        results.put("SelectSimpleObject", "select a1_ from Company as a1_");
        results.put("SubQuery", "select a1_.a1_.name AS a2_, a1_.a2_ AS a3_ from (select a1_, 5 as a2_ from Company as a1_) as a1_");
        results.put("WhereSimpleEquals", "select a1_.name as a2_ from Company as a1_ where a1_.vatNumber = 1234");
        results.put("WhereSimpleNotEquals", "select a1_.name as a2_ from Company as a1_ where a1_.vatNumber != 1234");
        results.put("WhereSimpleLike", "select a1_.name as a2_ from Company as a1_ where a1_.name like 'Company%'");
        results.put("WhereEqualsString", "select a1_.name as a2_ from Company as a1_ where a1_.name = 'CompanyA'");
        results.put("WhereAndSet", "select a1_.name as a2_ from Company as a1_ where a1_.name like 'Company%' and a1_.vatNumber > 2000");
        results.put("WhereOrSet", "select a1_.name as a2_ from Company as a1_ where a1_.name like 'CompanyA%' or a1_.vatNumber > 2000");
        results.put("WhereNotSet", "select a1_.name as a2_ from Company as a1_ where not (a1_.name like 'Company%' and a1_.vatNumber > 2000)");
        results.put("WhereSubQueryField", "select a1_ from Department as a1_ where a1_.name in (select a1_.name as a2_ from Department as a1_) order by a1_.name");
        results.put("WhereSubQueryClass", "select a1_ from Company as a1_ where a1_ in (select a1_ from Company as a1_ where a1_.name = 'CompanyA')");
        results.put("WhereNotSubQueryClass", "select a1_ from Company as a1_ where a1_ not in (select a1_ from Company as a1_ where a1_.name = 'CompanyA')");
        results.put("WhereNegSubQueryClass", "select a1_ from Company as a1_ where not (a1_ in (select a1_ from Company as a1_ where a1_.name = 'CompanyA'))");
        results.put("WhereClassClass", "select a1_, a2_ from Company as a1_, Company as a2_ where a1_ = a2_");
        results.put("WhereNotClassClass", "select a1_, a2_ from Company as a1_, Company as a2_ where a1_ != a2_");
        results.put("WhereNegClassClass", "select a1_, a2_ from Company as a1_, Company as a2_ where not a1_ = a2_");
        results.put("Contains11", "select a1_, a2_ from Department as a1_, Manager as a2_ where a1_.manager contains a2_ and a1_.name = 'DepartmentA1'");
        results.put("ContainsNot11", "select a1_, a2_ from Department as a1_, Manager as a2_ where a1_.manager does not contain a2_ and a1_.name = 'DepartmentA1'");
        results.put("ContainsNeg11", "select a1_, a2_ from Department as a1_, Manager as a2_ where not a1_.manager contains a2_ and a1_.name = 'DepartmentA1'");
        results.put("Contains1N", "select a1_, a2_ from Company as a1_, Department as a2_ where a1_.departments contains a2_ and a1_.name = 'CompanyA'");
        results.put("ContainsN1", "select a1_, a2_ from Department as a1_, Company as a2_ where a1_.company contains a2_ and a2_.name = 'CompanyA'");
        results.put("ContainsMN", "select a1_, a2_ from Contractor as a1_, Company as a2_ where a1_.companys contains a2_ and a1_.name = 'ContractorA'");
        results.put("ContainsDuplicatesMN", "select a1_, a2_ from Contractor as a1_, Company as a2_ where a1_.oldComs contains a2_");
        results.put("SimpleGroupBy", "select a1_, count(*) as a2_ from Company as a1_, Department as a3_ where a1_.departments contains a3_ group by a1_");
        results.put("MultiJoin", "select a1_, a2_, a3_, a4_ from Company as a1_, Department as a2_, Manager as a3_, Address as a4_ where a1_.departments contains a2_ and a2_.manager contains a3_ and a3_.address contains a4_ and a3_.name = 'EmployeeA1'");
        results.put("SelectComplex", "select avg(a1_.vatNumber) + 20 as a3_, a2_.name as a4_, a2_ from Company as a1_, Department as a2_ group by a2_");
        results.put("SelectClassAndSubClasses", "select a1_ from Employee as a1_ order by a1_.name");
        results.put("SelectInterfaceAndSubClasses", "select a1_ from Employable as a1_");
        results.put("SelectInterfaceAndSubClasses2", "select a1_ from RandomInterface as a1_");
        results.put("SelectInterfaceAndSubClasses3", "select a1_ from ImportantPerson as a1_");
        results.put("OrderByAnomaly", "select 5 as a2_, a1_.name as a3_ from Company as a1_");
    }

    public void executeTest(String type) throws Exception {
        String fql = (String) results.get(type);
        Query parsed = new Query(fql, "org.flymine.model.testmodel");
        assertEquals(type + " has failed", (Query) queries.get(type), parsed);
    }

    public void testConstants() throws Exception {
        Query q = new Query("select 1 as b, false as c, true as d, 1.2 as e, 'hello' as f, '2003-04-30 14:12:30.333' as g from Company", "org.flymine.model.testmodel");
        assertEquals("SELECT 1 AS b, false AS c, true AS d, 1.2 AS e, hello AS f, Wed Apr 30 14:12:30 BST 2003 AS g FROM org.flymine.model.testmodel.Company AS Company", q.toString());
    }

    public void testValidPathExpressions() throws Exception {
        Query q = new Query("select subquery.c_.name as a, subquery.b as b from (select c_, c_.name as b from Company as c_) as subquery", "org.flymine.model.testmodel");
        assertEquals("SELECT subquery.c_name AS a, subquery.b AS b FROM (SELECT c_, c_.name AS b FROM org.flymine.model.testmodel.Company AS c_) AS subquery", q.toString());
    }

    public void testInvalidPathExpressions() throws Exception {
        try {
            Query q = new Query("select Company.nonExistentField from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because that field does not exist in a Company object");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.NoSuchFieldException: Field nonExistentField not found in class org.flymine.model.testmodel.Company", e.getMessage());
        }
        try {
            Query q = new Query("select Company.name.something from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression cannot extend beyond a field");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression Company.name.something extends beyond a field", e.getMessage());
        }
        try {
            Query q = new Query("select c from (select Company from Company) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression cannot end at a subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression c cannot end at a subquery", e.getMessage());
        }
        try {
            Query q = new Query("select c.Company from (select Company from Company) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because we cannot reference classes inside subqueries");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot reference classes inside subqueries - only QueryEvaluables, and fields inside classes inside subqueries, for path expression c.Company", e.getMessage());
        }
        try {
            Query q = new Query("select c.Company.nonExistentField from (select Company from Company) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because that field does not exist in a Company object");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.NoSuchFieldException: Field nonExistentField not found in class org.flymine.model.testmodel.Company", e.getMessage());
        }
        try {
            Query q = new Query("select c.Company.name.something from (select Company from Company) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression cannot extend beyond a field");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression c.Company.name.something extends beyond a field", e.getMessage());
        }
        try {
            Query q = new Query("select c.name.something from (select Company.name as name from Company) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression cannot extend beyond a field");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression c.name.something extends beyond a field", e.getMessage());
        }
        try {
            Query q = new Query("select c.subquery from (select subquery.Company.name as name from (select Company from Company) as subquery) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression references a subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot reference subquery subquery inside subquery c", e.getMessage());
        }
        try {
            Query q = new Query("select c.something from (select Company from Company) as c", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because there is no object c.something");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object something found in subquery c", e.getMessage());
        }
        try {
            Query q = new Query("select c from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because there is no object c");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object c", e.getMessage());
        }
        try {
            Query q = new Query("select a.Company.name as a from (select Company.name as a from Company) as a", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because Company is not in the SELECT list of the subquery a");
        } catch (IllegalArgumentException e) {
            assertEquals("a.Company.name is not available, because Company is not in the SELECT list of subquery a", e.getMessage());
        }
    }

    public void testNormalExpressions() throws Exception {
        Query q = new Query("select 1 + Company.vatNumber as a, 3 - 4 as b, 5 * 6 as c, 7 / 8 as d from Company", "org.flymine.model.testmodel");
        assertEquals("SELECT 1 + Company.vatNumber AS a, 3 - 4 AS b, 5 * 6 AS c, 7 / 8 AS d FROM org.flymine.model.testmodel.Company AS Company", q.toString());
    }

    public void testInvalidNormalExpressions() throws Exception {
        try {
            Query q = new Query("select Company + 3 as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because a class cannot appear in an expression");
        } catch (IllegalArgumentException e) {
            assertEquals("Expressions cannot contain classes as arguments", e.getMessage());
        }
        try {
            Query q = new Query("select 1 + 2 + 3 as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because an expression may only have two arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception: line 1:14: expecting \"as\", found '+'", e.getMessage());
        }
        try {
            Query q = new Query("select 'flibble' + 3 as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because an expression must type-match");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid arguments for specified operation", e.getMessage());
        }
        try {
            Query q = new Query("select Company.name + 3 as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because an expression must type-match");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid arguments for specified operation", e.getMessage());
        }
    }

    public void testSafeFunctions() throws Exception {
        Query q = new Query("select count(*) as a, sum(Company.vatNumber + 3) as b, avg(Company.vatNumber) as c, min(Company.vatNumber) as d, substr('flibble', 3, max(Company.vatNumber)) as e from Company", "org.flymine.model.testmodel");
        assertEquals("SELECT COUNT(*) AS a, SUM(Company.vatNumber + 3) AS b, AVG(Company.vatNumber) AS c, MIN(Company.vatNumber) AS d, SUBSTR(flibble, 3, MAX(Company.vatNumber)) AS e FROM org.flymine.model.testmodel.Company AS Company", q.toString());
    }

    public void testInvalidSafeFunctions() throws Exception {
        try {
            Query q = new Query("select count(5) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because count does not take an argument");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception: line 1:14: expecting ASTERISK, found '5'", e.getMessage());
        }
        try {
            Query q = new Query("select sum(5, 3) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because sum only takes one argument");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception: line 1:13: expecting CLOSE_PAREN, found ','", e.getMessage());
        }
        try {
            Query q = new Query("select substr('fdsafds', 3, 4, 5) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because substr only takes three arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception: line 1:30: expecting CLOSE_PAREN, found ','", e.getMessage());
        }
        try {
            Query q = new Query("select max(Company) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because functions cannot have classes as arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("Functions cannot contain classes as arguments", e.getMessage());
        }
        try {
            Query q = new Query("select substr('fdsafds', 3) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because substr takes three arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception: line 1:27: expecting COMMA, found ')'", e.getMessage());
        }
        try {
            Query q = new Query("select min() as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because min takes an argument");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception: line 1:8: unexpected token: min", e.getMessage());
        }
        try {
            Query q = new Query("select min(4) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because min's argument must be a field or expression");
        } catch (IllegalArgumentException e) {
            assertEquals("Arguments to aggregate functions may be fields or expressions only", e.getMessage());
        }
        try {
            Query q = new Query("select min(Company.name) as a from Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because min's argument must be numerical");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid argument type for specified operation", e.getMessage());
        }
    }

    public void testGroupOrder() throws Exception {
        Query q = new Query("select Company from Company group by 2 order by Company", "org.flymine.model.testmodel");
        assertEquals("SELECT Company FROM org.flymine.model.testmodel.Company AS Company GROUP BY 2 ORDER BY Company", q.toString());
    }

    public void testValidConstraints() throws Exception {
        Query q = new Query("select c_, d_, e_ from Company as c_, Department as d_, CEO as e_ where c_.departments does not contain d_ and c_.CEO contains e_ and (c_.vatNumber < 5 or c_.name like 'fish%') and e_.salary is not null and c_.vatNumber > e_.age and c_.name in (select Company.name as name from Company)", "org.flymine.model.testmodel");
        assertEquals("SELECT c_, d_, e_ FROM org.flymine.model.testmodel.Company AS c_, org.flymine.model.testmodel.Department AS d_, org.flymine.model.testmodel.CEO AS e_ WHERE (c_.departments DOES NOT CONTAIN d_ AND c_.CEO CONTAINS e_ AND (c_.vatNumber < 5 OR c_.name LIKE fish%) AND e_.salary IS NOT NULL AND c_.vatNumber > e_.age AND c_.name IN (SELECT Company.name AS name FROM org.flymine.model.testmodel.Company AS Company))", q.toString());
    }

    public void testInvalidConstraint() throws Exception {
        try {
            Query q = new Query("select Company from Company where Company in (select Company, Company.name as name from Company)", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because there are too many columns in the subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Subquery must have only one item in select list.", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from Company, Department where Company.departments.flibble contains Department", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression is too long");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression Company.departments.flibble extends beyond a collection or object reference", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from Company, Department where Company.name contains Department", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because Company.name is not a collection or object reference");
        } catch (IllegalArgumentException e) {
            assertEquals("Object Company.name is not a collection or object reference", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from Company, Department where Company.jhsfd contains Department", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because jhsfd does not exist");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object Company.jhsfd", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from Company, Department where Company.departments contains Department.name", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because Department.name is not a class");
        } catch (IllegalArgumentException e) {
            assertEquals("Collection or object reference Company.departments cannot contain anything but a QueryClass", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from Company, Department where Company contains Department", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the path expression is too short");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression for collection cannot end on a QueryClass", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from (select Company from Company) as Company, Department where Company.Company.departments contains Department", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because one cannot access a collection in a subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot access a collection or object reference inside subquery Company", e.getMessage());
        }
        try {
            Query q = new Query("select Company, Department from Company, Department where fkjsfd contains Department", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because there is no such object fkjsfd");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object fkjsfd while looking for a collection or object reference", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company is null", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because one cannot compare a class to null");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a class to null", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company > Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because > is not a valid comparison for classes");
        } catch (IllegalArgumentException e) {
            assertEquals("Operation is not valid for comparing two classes", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company > Company.name", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because one cannot compare a class to a value");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a class to a value", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company.name > Company", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because one cannot compare a value to a class");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a value to a class", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company.name = Company.vatNumber", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because the two types do not match");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid pair of arguments: class java.lang.String, class java.lang.Integer", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company.departments = Company.vatNumber", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because departments is a collection");
        } catch (IllegalArgumentException e) {
            assertEquals("Field departments is a collection type", e.getMessage());
        }
        try {
            Query q = new Query("select Company from Company where Company.CEO = Company.vatNumber", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException, because CEO is an object reference");
        } catch (IllegalArgumentException e) {
            assertEquals("Field CEO is an object reference", e.getMessage());
        }
    }
}
