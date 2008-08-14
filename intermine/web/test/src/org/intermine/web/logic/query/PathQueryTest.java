package org.intermine.web.logic.query;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.path.Path;
import org.intermine.path.PathError;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;


public class PathQueryTest extends TestCase
{
    Map expected, classKeys;
    PathQuery e, q;
    Model model;

    public void setUp() throws Exception {
        super.setUp();
        model = TestUtil.getModel();
        classKeys = TestUtil.getClassKeys(model);
        InputStream is = getClass().getClassLoader().getResourceAsStream("PathQueryTest.xml");
        expected = PathQueryBinding.unmarshal(new InputStreamReader(is), classKeys);
    }

    public PathQueryTest(String arg) {
        super(arg);
    }

    public void testPathQueryModel() {
        new PathQuery(model);
    }

    public void testPathQueryPathQuery() {
        e = (PathQuery) expected.get("companyName");
        PathQuery actual = new PathQuery(e);
        assertEquals(e, actual);
    }

    public void testSetViewString() {

        // simple
        e = (PathQuery) expected.get("employeeName");
        q = new PathQuery(model);
        q.setView("Employee.name");
        assertEquals(e.getViewStrings(), q.getViewStrings());

        // multiple, long paths, multiple delims
        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        q.setView("Employee.name ,Employee.department.name, Employee.department.company.name");
        assertEquals(e.getViewStrings(), q.getViewStrings());

        // bad path
        q = new PathQuery(model);
        q.setView("monkey");
        assertTrue(q.getViewStrings().isEmpty());
        assertFalse(q.isValid());

        // empty path
        q.setView("");
        assertEquals("setView() was passed null or empty string", q.getProblems()[1].getMessage().toString());
        assertFalse(q.isValid());

        // null path
        q.setView(new String());
        assertEquals("setView() was passed null or empty string", q.getProblems()[2].getMessage().toString());
        assertFalse(q.isValid());
    }

    public void testSetViewListOfString() {
        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        List<String> view = new ArrayList<String>() {{
            add("Employee.name");
            add("Employee.department.name");
            add("Employee.department.company.name");
        }};
        q.setView(view);
        assertEquals(e.getViewStrings(), q.getViewStrings());

        // bad path
        q = new PathQuery(model);
        view = new ArrayList<String>() {{
            add("Monkey.paths");
        }};
        q.setView(view);
        assertTrue(q.getViewStrings().isEmpty());
        assertFalse(q.isValid());

        // bad path with good paths
        q = new PathQuery(model);
        view = new ArrayList<String>() {{
            add("Employee.name");
            add("Employee.department.name");
            add("Monkey.paths");
            add("Employee.department.company.name");
        }};
        q.setView(view);
        assertEquals(e.getViewStrings(), q.getViewStrings());
        assertFalse(q.isValid());

        // bad path with empty path
        q = new PathQuery(model);
        view = new ArrayList<String>() {{
            add("Employee.name");
            add("Employee.department.name");
            add("");
            add("Employee.department.company.name");
        }};
        q.setView(view);
        assertEquals(e.getViewStrings(), q.getViewStrings());
        assertFalse(q.isValid());

        // null path
        q = new PathQuery(model);
        q.setView(new ArrayList<String>());
        assertFalse(q.isValid());

        // bad paths with empty path
        q = new PathQuery(model);
        view = new ArrayList<String>() {{
            add("Employee.name");
            add("Employee.department.name");
            String isnull = null;
            add(isnull);
            add("Employee.department.company.name");
        }};
        q.setView(view);
        assertEquals(e.getViewStrings(), q.getViewStrings());
        assertFalse(q.isValid());
    }

    public void testSetViewPaths() {
        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        List<Path> view = new ArrayList<Path>() {{
            add(new Path(model, "Employee.name"));
            add(new Path(model, "Employee.department.name"));
            add(new Path(model, "Employee.department.company.name"));
        }};
        q.setViewPaths(view);
        assertEquals(e.getView(), q.getView());

        // TODO need to test a bad/empty path
        q = new PathQuery(model);
        view = new ArrayList<Path>() {{
            add(new Path(model, "Employee.name"));
            try {
                add(new Path(model, "monkey"));
            } catch (PathError pathError) {
                // caught!
            }
            add(new Path(model, "Employee.department.name"));
            add(new Path(model, "Employee.department.company.name"));
        }};
        q.setViewPaths(view);
        assertEquals(e.getView(), q.getView());
        assertTrue(q.isValid());

        // null paths
        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        view = new ArrayList<Path>() {{
            add(new Path(model, "Employee.name"));
            Path nullpath = null;
            add(nullpath);
            add(new Path(model, "Employee.department.name"));
            add(new Path(model, "Employee.department.company.name"));
        }};
        q.setViewPaths(view);
        assertEquals(e.getView(), q.getView());
        assertTrue(q.isValid());
    }

    public void testAddViewString() {

        // simple
        e = (PathQuery) expected.get("companyName");
        q = new PathQuery(model);
        q.addView("Company.name");
        assertEquals(e.getViewStrings(), q.getViewStrings());

        // add one then another
        e = (PathQuery) expected.get("employeeDepartmentName");
        q = new PathQuery(model);
        q.addView("Employee.name");
        q.addView("Employee.department.name");
        assertEquals(e.getViewStrings(), q.getViewStrings());

        // add three at once
        e = (PathQuery) expected.get("employeeDepartmentCompany");
        q = new PathQuery(model);
        q.addView("Employee.name, Employee.department.name,Employee.department.company.name");
        assertEquals(e.getViewStrings(), q.getViewStrings());

        // bad path
        q = new PathQuery(model);
        q.addView("monkey");
        assertTrue(q.getViewStrings().isEmpty());
        assertFalse(q.isValid());

        // empty path
        q.addView("");
        assertEquals("addView() was passed null or empty string", q.getProblems()[1].getMessage().toString());
        assertFalse(q.isValid());

        // null path
        q.addView(new String());
        assertEquals("addView() was passed null or empty string", q.getProblems()[2].getMessage().toString());
        assertFalse(q.isValid());

    }

    public void testAddViewListOfString() {

        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        List<String> view = new ArrayList<String>() {{
            add("Employee.name");
            add("Employee.department.name");
            add("Employee.department.company.name");
        }};
        q.addView(view);
        assertEquals(e.getViewStrings(), q.getViewStrings());


        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        q.setView("Employee.name");
        view = new ArrayList<String>() {{
            add("Employee.department.name");
            add("Employee.department.company.name");
        }};
        q.addView(view);
        assertEquals(e.getViewStrings(), q.getViewStrings());

    }

    public void testAddPathToView() {

        e = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        q = new PathQuery(model);
        List<Path> view = new ArrayList<Path>() {{
            add(new Path(model, "Employee.name"));
            add(new Path(model, "Employee.department.name"));
            add(new Path(model, "Employee.department.company.name"));
        }};
        q.addViewPaths(view);

        assertEquals(e.getView(), q.getView());
    }

//    public void testAddPathStringToView() {
//        //deprecated
//    }

    // -- old methods -- //

    public void testRemoveFromView() {
        e = (PathQuery) expected.get("employeeName");
        q = (PathQuery) expected.get("employeeDepartmentName");
        q.removeFromView("Employee.department.name");
        assertEquals(e.getView(), q.getView());
    }

    public void testViewContains() {
        q = (PathQuery) expected.get("employeeDepartmentCompanyWildcard");
        assertTrue(q.viewContains("Employee.name"));
        assertFalse(q.viewContains("Department.name"));
    }


    /***********************************************************************************/

//    public void testAddConstraintStringConstraint() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddConstraintStringConstraintString() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetAllConstraints() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetConstraintLogic() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetLogic() {
//        fail("Not yet implemented");
//    }
//
//    public void testSyncLogicExpression() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetUnusedConstraintCode() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetConstraintByCode() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddCodesToLogic() {
//        fail("Not yet implemented");
//    }


    /***********************************************************************************/


    public void testSetOrderByString() {

        // no order by clauses
        e = (PathQuery) expected.get("employeeName");
        q = new PathQuery(model);
        q.setView("Employee.name");
        assertEquals(e.getSortOrderStrings(), q.getSortOrderStrings());

        e = (PathQuery) expected.get("noOrderBy");
        q = new PathQuery(model);
        q.setView("Employee.name,Employee.department.name");
        q.setOrderBy("Employee.name");
        assertEquals(e.getSortOrder(), q.getSortOrder());

        // ASC
        e = (PathQuery) expected.get("orderByAsc");
        q = new PathQuery(model);
        q.setView("Employee.name,Employee.department.name");
        q.setOrderBy("Employee.name");
        assertEquals(e.getSortOrder(), q.getSortOrder());

        // long path
        e = (PathQuery) expected.get("longPath");
        q = new PathQuery(model);
        q.setView("Employee.name,Employee.department.name");
        q.setOrderBy("Employee.department.name");
        assertEquals(e.getSortOrder(), q.getSortOrder());

        // multiple paths
        e = (PathQuery) expected.get("orderByVat");
        q = new PathQuery(model);
        q.setView("Company.vatNumber,Company.name,Company.address.address");
        q.setOrderBy("Company.vatNumber,Company.name,Company.departments.name");
        assertEquals(e.getSortOrderStrings(), q.getSortOrderStrings());

        // overwrite current orderby
        e = (PathQuery) expected.get("orderByVat");
        q = new PathQuery(model);
        q.setView("Company.vatNumber,Company.name,Company.address.address");
        q.setOrderBy("Company.address.address");
        q.setOrderBy("Company.vatNumber,Company.name,Company.departments.name");
        assertEquals(e.getSortOrderStrings(), q.getSortOrderStrings());

        q = (PathQuery) expected.get("orderByCompany");
        q.setOrderBy("Company.departments.name");
        assertEquals("Company.departments.name asc", q.getSortOrderStrings().get(0));

        // bad path
        q = (PathQuery) expected.get("companyName");
        q.setOrderBy("monkey.pants");
        assertTrue(q.getSortOrder().isEmpty());

        // empty value
        q = (PathQuery) expected.get("companyName");
        q.setOrderBy("");
        assertTrue(q.getSortOrder().isEmpty());

    }

//    public void testSetOrderByStringBoolean() {
//        fail("Not yet implemented");
//    }
//
//    public void testSetOrderByListOfString() {
//        fail("Not yet implemented");
//    }
//
//    public void testSetOrderByListOfStringBoolean() {
//        fail("Not yet implemented");
//    }
//
//    public void testSetOrderByList() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddOrderByString() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddOrderByStringBoolean() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddOrderByListOfString() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddOrderByListOfStringBoolean() {
//        fail("Not yet implemented");
//    }

    // --- old methods -- //

//    public void testChangeDirection() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddPathStringToSortOrder() {
//        fail("Not yet implemented");
//    }
//
//    public void testResetSortOrder() {
//        // this is for the querybuilder only
//        fail("Not yet implemented");
//    }
//
//    public void testRemoveOrderBy() {
//        // this is for the querybuilder only
//        fail("Not yet implemented");
//    }


    /***********************************************************************************/


    public void testGetModel() {
        q = new PathQuery(model);
        q.getModel().equals(model);
    }

//    public void testGetNodes() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetNode() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddNode() {
//        fail("Not yet implemented");
//    }
//
//    public void testClone() {
//        fail("Not yet implemented");
//    }
//
//    public void testCloneNode() {
//        fail("Not yet implemented");
//    }

    public void testInfo() {
        int start = 123;
        int complete = 456;
        int rows = 789;
        int min = 1;
        int max = 123456789;

        ResultsInfo r = new ResultsInfo(start, complete, rows, min, max);
        q = new PathQuery(model);
        q.setInfo(r);
        assertEquals(123, q.getInfo().getStart());
        assertEquals(456, q.getInfo().getComplete());
        assertEquals(789, q.getInfo().getRows());
        assertEquals(1, q.getInfo().getMin());
        assertEquals(123456789, q.getInfo().getMax());
    }

    public void testGetBagNames() {
        e = (PathQuery) expected.get("departmentBagConstraint");
        List<Object> bags = e.getBagNames();
        assertEquals(1, bags.size());
        assertEquals("departmentBag", bags.get(0).toString());
    }

    public void testProblems() {
        q = new PathQuery(model);

        List<Throwable> errors = new ArrayList<Throwable>() {{
            add(new Throwable("boo"));
            add(new PathError("monkeypants is not a valid path", "monkey.pants"));
        }};

        q.setProblems(null);
        assertTrue(q.getProblems().length == 0);

        q.setProblems(errors);
        assertEquals(2, q.getProblems().length);

        q.addProblem(new ClassNotFoundException("monkeypants is not a valid class"));
        assertEquals(3, q.getProblems().length);

    }

    public void testIsValid() {
        q = new PathQuery(model);
        q.setProblems(null);
        assertTrue(q.isValid());
        q.setProblems(Arrays.asList(new Throwable("problem!")));
        assertFalse(q.isValid());
    }

//    public void testEqualsObject() {
//        fail("Not yet implemented");
//    }

//    public void testGetPathDescriptions() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetPathDescription() {
//        fail("Not yet implemented");
//    }
//
//    public void testGetPathStringDescriptions() {
//        fail("Not yet implemented");
//    }
//
//    public void testAddPathStringDescription() {
//        fail("Not yet implemented");
//    }
//
//    public void testMakePath() {
//        fail("Not yet implemented");
//    }
}
