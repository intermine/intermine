/**
 * Tests for the PathQuery builder for JSON object formatting.
 * This format requires certain things from the view (select statements)
 * of the pathquery).
 */
package org.intermine.webservice.server.query.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * @author Alexis Kalderimis
 *
 */
public class PathQueryBuilderForJSONTest extends TestCase {

    private final Model model = Model.getInstanceByName("testmodel");

    PathQueryBuilderForJSONObj builder;
    protected void setUp() {
        builder = new PathQueryBuilderForJSONObj();
    }

    public void testPublicConstructor() {
        System.out.println("classpath=" + System.getProperty("java.class.path"));
        String xml = "<query model=\"testmodel\" name=\"test-query\" view=\"Manager.department.name\"></query>";
        String schemaUrl = this.getClass().getClassLoader().getResource("webservice/query.xsd").toString();
        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();

        PathQueryBuilderForJSONObj publicBuilder = new PathQueryBuilderForJSONObj(
                xml, schemaUrl, savedBags);
        assertTrue(publicBuilder != null);

    }

    public void testPublicGetQuery() {
        String xml = "<query model=\"testmodel\" name=\"test-query\" view=\"Manager.department.name\"></query>";
        String schemaUrl = this.getClass().getClassLoader().getResource("webservice/query.xsd").toString();
        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();

        PathQueryBuilderForJSONObj publicBuilder = new PathQueryBuilderForJSONObj(
                xml, schemaUrl, savedBags);
        PathQuery pq = publicBuilder.getQuery();
        List<String> expectedViews = Arrays.asList("Manager.id", "Manager.department.name");
        assertEquals(expectedViews, pq.getView());
    }

    public void testDoesntChangeAcceptableViews() {

        List<PathQuery> pathQueries  = new ArrayList<PathQuery>();
        PathQuery pq1a = new PathQuery(model);
        pq1a.addViews("Manager.id", "Manager.name");
        pathQueries.add(pq1a);

        PathQuery pq1b = new PathQuery(model);
        pq1b.addViews("Manager.id", "Manager.age", "Manager.name");
        pathQueries.add(pq1b);

        PathQuery pq2a = new PathQuery(model);
        pq2a.addViews("Manager.id", "Manager.name", "Manager.department.name");
        pathQueries.add(pq2a);

        PathQuery pq2b = new PathQuery(model);
        pq2b.addViews("Manager.id", "Manager.name", "Manager.address.address", "Manager.department.name");
        pathQueries.add(pq2b);

        PathQuery pq3a = new PathQuery(model);
        pq3a.addViews("Manager.id", "Manager.name", "Manager.department.name", "Manager.department.employees.name");
        pathQueries.add(pq3a);

        PathQuery pq3b = new PathQuery(model);
        pq3b.addViews("Company.id", "Company.name", "Company.contractors.seniority", "Company.departments.name");
        pathQueries.add(pq3b);

        PathQuery pq4a = new PathQuery(model);
        pq4a.addViews("Company.id", "Company.name", "Company.departments.name", "Company.departments.employees.name", "Company.departments.employees.address.address");
        pathQueries.add(pq4a);

        PathQuery pq4b = new PathQuery(model);
        pq4b.addViews(
                "Company.id",
                "Company.name",
                "Company.contractors.name",
                "Company.departments.name",
                "Company.contractors.oldComs.name",
                "Company.departments.employees.name",
                "Company.contractors.oldComs.departments.name",
                "Company.departments.employees.address.address");
        pathQueries.add(pq4b);

        for (PathQuery pq : pathQueries) {
            List<String> oldViews = pq.getView();
            List<String> newViews = builder.getAlteredViews(pq);
            assertEquals(oldViews, newViews);
        }
    }

    public void testRearrangesBadlyOrderedViews() {
        List<PathQuery> pathQueries  = new ArrayList<PathQuery>();
        List<List<String>> expectedViews = new ArrayList<List<String>>();

        PathQuery pq2a = new PathQuery(model);
        pq2a.addViews( "Manager.department.name", "Manager.name");
        pathQueries.add(pq2a);
        expectedViews.add(Arrays.asList("Manager.id", "Manager.name", "Manager.department.name"));

        PathQuery pq2b = new PathQuery(model);
        pq2b.addViews("Manager.address.address", "Manager.name", "Manager.department.name");
        pathQueries.add(pq2b);
        expectedViews.add(Arrays.asList("Manager.id", "Manager.name", "Manager.address.address", "Manager.department.name"));

        PathQuery pq3a = new PathQuery(model);
        pq3a.addViews("Manager.name", "Manager.department.employees.name", "Manager.department.name");
        pathQueries.add(pq3a);
        expectedViews.add(Arrays.asList("Manager.id", "Manager.name", "Manager.department.name", "Manager.department.employees.name"));

        PathQuery pq3b = new PathQuery(model);
        pq3b.addViews("Company.contractors.seniority", "Company.departments.name", "Company.name");
        pathQueries.add(pq3b);
        expectedViews.add(Arrays.asList("Company.id", "Company.name", "Company.contractors.seniority", "Company.departments.name"));

        PathQuery pq4a = new PathQuery(model);
        pq4a.addViews("Company.departments.employees.name", "Company.departments.name",
                "Company.departments.employees.address.address",
                "Company.name");
        pathQueries.add(pq4a);
        expectedViews.add(Arrays.asList("Company.id", "Company.name", "Company.departments.name",
                "Company.departments.employees.name", "Company.departments.employees.address.address"));

        PathQuery pq4b = new PathQuery(model);
        pq4b.addViews(
                "Company.departments.employees.address.address",
                "Company.departments.name",
                "Company.departments.employees.name",
                "Company.contractors.oldComs.departments.name",
                "Company.contractors.name",
                "Company.contractors.oldComs.name",
                "Company.name");
        pathQueries.add(pq4b);
        expectedViews.add(Arrays.asList(
                "Company.id",
                "Company.name",
                "Company.contractors.name",
                "Company.departments.name",
                "Company.contractors.oldComs.name",
                "Company.departments.employees.name",
                "Company.contractors.oldComs.departments.name",
                "Company.departments.employees.address.address"));

        for (int index = 0; index < expectedViews.size(); index++) {
            List<String> expected = expectedViews.get(index);
            List<String> newViews = builder.getAlteredViews(pathQueries.get(index));
            assertEquals(expected, newViews);
        }
    }

    public void testAddsDefaultAttributeToAttributelessNodes() {
        List<PathQuery> pathQueries  = new ArrayList<PathQuery>();
        List<List<String>> expectedViews = new ArrayList<List<String>>();

        PathQuery pq2a = new PathQuery(model);
        pq2a.addViews( "Manager.department.name");
        pathQueries.add(pq2a);
        expectedViews.add(Arrays.asList("Manager.id", "Manager.department.name"));

        PathQuery pq2b = new PathQuery(model);
        pq2b.addViews("Manager.address.address", "Manager.department.name");
        pathQueries.add(pq2b);
        expectedViews.add(Arrays.asList("Manager.id", "Manager.address.address", "Manager.department.name"));

        PathQuery pq3a = new PathQuery(model);
        pq3a.addViews("Manager.department.employees.name");
        pathQueries.add(pq3a);
        expectedViews.add(Arrays.asList("Manager.id", "Manager.department.id", "Manager.department.employees.name"));

        PathQuery pq3b = new PathQuery(model);
        pq3b.addViews("Company.contractors.seniority", "Company.departments.name");
        pathQueries.add(pq3b);
        expectedViews.add(Arrays.asList("Company.id", "Company.contractors.seniority", "Company.departments.name"));

        PathQuery pq4a = new PathQuery(model);
        pq4a.addViews("Company.departments.employees.address.address");
        pathQueries.add(pq4a);
        expectedViews.add(Arrays.asList("Company.id", "Company.departments.id",
                "Company.departments.employees.id", "Company.departments.employees.address.address"));

        PathQuery pq4b = new PathQuery(model);
        pq4b.addViews(
                "Company.departments.employees.address.address",
                "Company.contractors.oldComs.departments.name");
        pathQueries.add(pq4b);
        expectedViews.add(Arrays.asList(
                "Company.id",
                "Company.contractors.id",
                "Company.contractors.oldComs.id",
                "Company.contractors.oldComs.departments.name",
                "Company.departments.id",
                "Company.departments.employees.id",
                "Company.departments.employees.address.address"));

        for (int index = 0; index < expectedViews.size(); index++) {
            List<String> expected = expectedViews.get(index);
            List<String> newViews = builder.getAlteredViews(pathQueries.get(index));
            assertEquals(expected, newViews);
        }
    }

    public void testPathProblems() throws Exception {
        List<PathQuery> pathQueries  = new ArrayList<PathQuery>();
        List<String> expectedErrors = new ArrayList<String>();

        PathQuery pq1 = new PathQuery(model);
        pq1.addViews("Foo.bar");
        pathQueries.add(pq1);
        expectedErrors.add("Problem making path Foo.bar");

        PathQuery pq2 = new PathQuery(model);
        pq2.addViews("Manager");
        pathQueries.add(pq2);
        expectedErrors.add("The view can only contain attribute paths - Got: 'Manager'");

        for (int index = 0; index < expectedErrors.size(); index++) {
            String expected = expectedErrors.get(index);
            try {
                List<String> newViews = builder.getAlteredViews(pathQueries.get(index));
                fail("No exception was thrown when processing the bad view list " +
                        pathQueries.get(index).getView() + " - got: " + newViews);
            } catch (AssertionFailedError e) {
                    // rethrow the fail from within the try
                    throw e;
            } catch (RuntimeException e) {
                assertEquals(expected, e.getMessage());
            } catch (Throwable t) {
                fail("Encountered unexpected exception processing the bad view list " +
                        pathQueries.get(index).getView());
            }
        }
    }

    public void testCantCreateAttributeNode() {
        try {
            Path cantAddToThis = new Path(model, "Manager.name");
            String newNode = builder.getNewAttributeNode(new HashSet(), cantAddToThis);
            fail("No exception thrown when trying to handle bad path Manager.name - got: " + newNode);
        } catch (AssertionFailedError e) {
            // rethrow the fail from within the try
            throw e;
        } catch (RuntimeException e) {
            assertEquals("Couldn't extend Manager.name with 'id'", e.getMessage());
        } catch (Throwable t) {
            fail("Encountered unexpected exception processing the bad path Manager.name");
        }
    }

}
