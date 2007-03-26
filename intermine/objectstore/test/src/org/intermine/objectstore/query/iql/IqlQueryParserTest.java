package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.intermine.objectstore.query.Query;
import org.intermine.testing.OneTimeTestCase;

/**
 * Test for testing the parser on the InterMine query object.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class IqlQueryParserTest extends IqlQueryTestCase
{
    public IqlQueryParserTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(IqlQueryParserTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        IqlQueryTestCase.oneTimeSetUp();

        results.put("WhereSimpleNegEquals", new IqlQuery("SELECT DISTINCT a1_.name AS a2_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE NOT (a1_.vatNumber = 1234)", null));
        results.put("WhereNegSubQueryClass", new IqlQuery("SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE NOT (a1_ IN (SELECT DISTINCT a1_ FROM org.intermine.model.testmodel.Company AS a1_ WHERE a1_.name = 'CompanyA'))", null));
        results.put("WhereNegClassClass", new IqlQuery("SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Company AS a2_ WHERE NOT a1_ = a2_", null));
        results.put("ContainsNeg11", new IqlQuery("SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a2_ WHERE NOT a1_.manager CONTAINS a2_ AND a1_.name = 'DepartmentA1'", null));
        results.put("EmptyNandConstraintSet", NO_RESULT);
        results.put("EmptyNorConstraintSet", NO_RESULT);
        results.put("QueryClassBagNotViaNand", NO_RESULT); // Has trouble with "1 = 1" - both are UnknownTypeValues
        results.put("QueryClassBagNotViaNor", NO_RESULT);
    }

    public void executeTest(String type) throws Exception {
        Object res = results.get(type);
        if (res instanceof IqlQuery) {
            IqlQuery fq = (IqlQuery) res;
            Query parsed = IqlQueryParser.parse(fq);
            if (type.equals("SubQuery") || type.equals("OrderByAnomaly")) {
                // These two queries CANNOT be generated properly by IQL (as they contain 5 in the SELECT list).
                // Therefore, we must merely check that they are regenerated back into IQL.
                IqlQuery fqNew = new IqlQuery(parsed);
                assertEquals(type + " has failed", fq, fqNew);
            } else {
                assertEquals(type + " has failed", (Query) queries.get(type), parsed);
            }
        } else {
            Iterator resIter = ((Collection) res).iterator();
            while (resIter.hasNext()) {
                IqlQuery fq = (IqlQuery) resIter.next();
                Query parsed = IqlQueryParser.parse(fq);
                assertEquals(type + " has failed: " + fq.toString(), (Query) queries.get(type), parsed);
            }
        }
    }

    public void testNotEnoughParameters() throws Exception {
        IqlQuery fq = new IqlQuery("select a1_, a2_ from Company as a1_, Department as a2_ where a1_ = ? and a2_ = ?", "org.intermine.model.testmodel");
        fq.setParameters(Collections.singletonList(data.get("CompanyA")));
        try {
            IqlQueryParser.parse(fq);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Not enough parameters in IqlQuery object", e.getMessage());
        }
    }

    public void testConstants() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select 1 as b, false as c, true as d, 1.2 as e, 'hello' as f, '2003-04-30 14:12:30.333' as g from Company", "org.intermine.model.testmodel"));
        assertEquals("SELECT 1 AS b, false AS c, true AS d, 1.2 AS e, 'hello' AS f, '2003-04-30 14:12:30.333' AS g FROM org.intermine.model.testmodel.Company AS Company", q.toString());
    }

    public void testValidPathExpressions() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select subquery.c_.name as a, subquery.b as b from (select c_, c_.name as b from Company as c_) as subquery", "org.intermine.model.testmodel"));
        assertEquals("SELECT subquery.c_.name AS a, subquery.b AS b FROM (SELECT c_, c_.name AS b FROM org.intermine.model.testmodel.Company AS c_) AS subquery", q.toString());
    }

    public void testInvalidPathExpressions() throws Exception {
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company.nonExistentField from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because that field does not exist in a Company object");
        } catch (IllegalArgumentException e) {
            assertEquals("Field nonExistentField not found in interface org.intermine.model.testmodel.Company", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company.name.something from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression cannot extend beyond a field");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression Company.name.something extends beyond a field", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c from (select Company from Company) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression cannot end at a subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression c cannot end at a subquery", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c.Company from (select Company from Company) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because we cannot reference classes inside subqueries");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot reference classes inside subqueries - only QueryEvaluables, and fields inside classes inside subqueries, for path expression c.Company", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c.Company.nonExistentField from (select Company from Company) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because that field does not exist in a Company object");
        } catch (IllegalArgumentException e) {
            assertEquals("Field nonExistentField not found in interface org.intermine.model.testmodel.Company", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c.Company.name.something from (select Company from Company) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression cannot extend beyond a field");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression c.Company.name.something extends beyond a field", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c.name.something from (select Company.name as name from Company) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression cannot extend beyond a field");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression c.name.something extends beyond a field", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c.subquery from (select subquery.Company.name as name from (select Company from Company) as subquery) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression references a subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot reference subquery subquery inside subquery c", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c.something from (select Company from Company) as c", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because there is no object c.something");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object something found in subquery c", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select c from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because there is no object c");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object c", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select a.Company.name as a from (select Company.name as a from Company) as a", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because Company is not in the SELECT list of the subquery a");
        } catch (IllegalArgumentException e) {
            assertEquals("a.Company.name is not available, because Company is not in the SELECT list of subquery a", e.getMessage());
        }
    }

    public void testNormalExpressions() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select 1 + Company.vatNumber as a, 3 - 4 as b, 5 * 6 as c, 7 / 8 as d from Company", "org.intermine.model.testmodel"));
        assertEquals("SELECT 1 + Company.vatNumber AS a, 3 - 4 AS b, 5 * 6 AS c, 7 / 8 AS d FROM org.intermine.model.testmodel.Company AS Company", q.toString());
    }

    public void testInvalidNormalExpressions() throws Exception {
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company + 3 as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because a class cannot appear in an expression");
        } catch (IllegalArgumentException e) {
            assertEquals("Expressions cannot contain classes as arguments", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select 1 + 2 + 3 as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because an expression may only have two arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("expecting \"as\", found '+'", e.getCause().getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select '3flibble' + 3 as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because an expression must type-match");
        } catch (IllegalArgumentException e) {
            assertEquals("Incompatible expression with unknown type values", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company.name + 3 as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because an expression must type-match");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot parse value \"3\" into class java.lang.String", e.getMessage());
        }
    }

    public void testSafeFunctions() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select count(*) as a, sum(Company.vatNumber + 3) as b, avg(Company.vatNumber) as c, min(Company.vatNumber) as d, substr('flibble', 3, max(Company.vatNumber)) as e from Company", "org.intermine.model.testmodel"));
        assertEquals("SELECT COUNT(*) AS a, SUM(Company.vatNumber + 3) AS b, AVG(Company.vatNumber) AS c, MIN(Company.vatNumber) AS d, SUBSTR('flibble', 3, MAX(Company.vatNumber)) AS e FROM org.intermine.model.testmodel.Company AS Company", q.toString());
    }

    public void testInvalidSafeFunctions() throws Exception {
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select count(5) as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because count does not take an argument");
        } catch (IllegalArgumentException e) {
            assertEquals("expecting ASTERISK, found '5'", e.getCause().getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select sum(5, 3) as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because sum only takes one argument");
        } catch (IllegalArgumentException e) {
            assertEquals("expecting CLOSE_PAREN, found ','", e.getCause().getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select substr('fdsafds', 3, 4, 5) as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because substr only takes three arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("expecting CLOSE_PAREN, found ','", e.getCause().getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select max(Company) as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because functions cannot have classes as arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("Functions cannot contain classes as arguments", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select substr('fdsafds') as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because substr takes two or three arguments");
        } catch (IllegalArgumentException e) {
            assertEquals("expecting COMMA, found ')'", e.getCause().getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select min() as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because min takes an argument");
        } catch (IllegalArgumentException e) {
            assertEquals("unexpected token: min", e.getCause().getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select min(4) as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because min's argument must be a field or expression");
        } catch (IllegalArgumentException e) {
            assertEquals("Arguments to aggregate functions may be fields or expressions only", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select min(Company.name) as a from Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because min's argument must be numerical");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid argument type for specified operation", e.getMessage());
        }
    }

    public void testGroupOrder() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company group by 2 order by Company", "org.intermine.model.testmodel"));
        assertEquals("SELECT Company FROM org.intermine.model.testmodel.Company AS Company GROUP BY 2 ORDER BY Company", q.toString());
    }

    public void testValidConstraints() throws Exception {
        Query q = IqlQueryParser.parse(new IqlQuery("select c_, d_, e_ from Company as c_, Department as d_, CEO as e_ where c_.departments does not contain d_ and c_.CEO contains e_ and (c_.vatNumber < 5 or c_.name like 'fish%') and e_.salary is not null and c_.vatNumber > e_.age and c_.name in (select Company.name as name from Company)", "org.intermine.model.testmodel"));
        assertEquals("SELECT c_, d_, e_ FROM org.intermine.model.testmodel.Company AS c_, org.intermine.model.testmodel.Department AS d_, org.intermine.model.testmodel.CEO AS e_ WHERE (c_.departments DOES NOT CONTAIN d_ AND c_.CEO CONTAINS e_ AND (c_.vatNumber < 5 OR c_.name LIKE 'fish%') AND e_.salary IS NOT NULL AND c_.vatNumber > e_.age AND c_.name IN (SELECT Company.name AS name FROM org.intermine.model.testmodel.Company AS Company))", q.toString());
    }

    public void testInvalidConstraint() throws Exception {
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company in (select Company, Company.name as name from Company)", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because there are too many columns in the subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Subquery must have only one item in select list.", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from Company, Department where Company.departments.flibble contains Department", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression is too long");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression Company.departments.flibble extends beyond a collection or object reference", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from Company, Department where Company.name contains Department", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because Company.name is not a collection or object reference");
        } catch (IllegalArgumentException e) {
            assertEquals("Object Company.name does not exist, or is not a collection or object reference", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from Company, Department where Company.jhsfd contains Department", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because jhsfd does not exist");
        } catch (IllegalArgumentException e) {
            assertEquals("Object Company.jhsfd does not exist, or is not a collection or object reference", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from Company, Department where Company.departments contains Department.name", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because Department.name is not a class");
        } catch (IllegalArgumentException e) {
            assertEquals("Collection or object reference Company.departments cannot contain anything but a QueryClass or InterMineObject", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from Company, Department where Company contains Department", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the path expression is too short");
        } catch (IllegalArgumentException e) {
            assertEquals("Path expression for collection cannot end on a QueryClass", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from (select Company from Company) as Company, Department where Company.Company.departments contains Department", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because one cannot access a collection in a subquery");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot access a collection or object reference inside subquery Company", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company, Department from Company, Department where fkjsfd contains Department", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because there is no such object fkjsfd");
        } catch (IllegalArgumentException e) {
            assertEquals("No such object fkjsfd while looking for a collection or object reference", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company is null", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because one cannot compare a class to null");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a class to null", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company > Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because > is not a valid comparison for classes");
        } catch (IllegalArgumentException e) {
            assertEquals("Operation is not valid for comparing two classes", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company > Company.name", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because one cannot compare a class to a value");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a class to a value", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company.name > Company", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because one cannot compare a value to a class");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a value to a class", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company.name = Company.vatNumber", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because the two types do not match");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid constraint: QueryField(org.intermine.model.testmodel.Company, name) (a java.lang.String) = QueryField(org.intermine.model.testmodel.Company, vatNumber) (a java.lang.Integer)", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company.departments = Company.vatNumber", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because departments is a collection");
        } catch (IllegalArgumentException e) {
            assertEquals("Field departments is a collection type", e.getMessage());
        }
        try {
            Query q = IqlQueryParser.parse(new IqlQuery("select Company from Company where Company.CEO = Company.vatNumber", "org.intermine.model.testmodel"));
            fail("Expected: IllegalArgumentException, because CEO is an object reference");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot compare a QueryObjectReference using a SimpleConstraint - use CONTAINS or DOES NOT CONTAIN instead", e.getMessage());
        }
    }
}
