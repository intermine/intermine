/**
 *
 */
package org.intermine.webservice.server.query.result;

import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * @author Alex Kalderimis
 *
 * Note, as this test requires access to the query.xsd resource, it must be run from ant, and
 * will fail to run in Eclipse.
 *
 */
public class PathQueryBuilderTest extends TestCase {

    /**
     * @param name
     */
    public PathQueryBuilderTest(String name) {
        super(name);
    }

    private final Model model = Model.getInstanceByName("testmodel");
    private final String schemaUrl = this.getClass().getClassLoader().getResource("webservice/query.xsd").toString();
    private final String goodXML = "<query model=\"testmodel\" view=\"Employee.age Employee.name\">" +
        "<constraint path=\"Employee.name\" op=\"=\" value=\"Tim Canterbury\" />" +
        "</query>";
    private final String invalidXML = "<query model=\"testmodel\" >" +
        "<constraint path=\"Employee.name\" value=\"Tim Canterbury\" />" +
        "</query>";
    private final String badQuery = "<query model=\"testmodel\" view=\"Employee.age Employee.name\">" +
    "<constraint path=\"Department.name\" op=\"=\" value=\"Sales\" />" +
    "</query>";
    private final String bagXML = "<query model=\"testmodel\" view=\"Employee.age Employee.name\">" +
    "<constraint path=\"Employee\" op=\"IN\" value=\"Decent Human Beings\" />" +
    "</query>";


    private final Map<String, InterMineBag> bags = new HashMap<String, InterMineBag>();
    private PathQuery expectedGoodQuery;
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        expectedGoodQuery = new PathQuery(model);
        expectedGoodQuery.addViews("Employee.age", "Employee.name");
        expectedGoodQuery.addConstraint(Constraints.eq("Employee.name", "Tim Canterbury"));
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBuildGoodQuery() {
        PathQueryBuilder pqb = new PathQueryBuilder();

        pqb.buildQuery(goodXML, schemaUrl, bags);
        assertEquals(expectedGoodQuery.toString(), pqb.getQuery().toString());

    }

    public void testBuildBadQuery() {
        PathQueryBuilder pqb = new PathQueryBuilder();

        try {
            pqb.buildQuery(invalidXML, schemaUrl, bags);
            fail("Build query did not throw an exception - despite being given bad input - got this:" + pqb.getQuery());
        } catch (AssertionFailedError e) {
            throw e;
        } catch (BadRequestException e) {
            assertEquals(
                    "cvc-complex-type.4: Attribute 'view' must appear on element 'query'.",
                    e.getMessage()
            );
        } catch (Throwable t) {
            fail("Unexpected error when building a query from bad xml" + t.getMessage());
        }

        try {
            pqb.buildQuery(badQuery, schemaUrl, bags);
            fail("Build query did not throw an exception - despite being given bad input - got this:" + pqb.getQuery());
        } catch (AssertionFailedError e) {
            throw e;
        } catch (BadRequestException e) {
            assertEquals(
                    "XML is well formatted but query contains errors: Multiple root classes in query: Employee and Department.",
                    e.getMessage()
            );
        } catch (Throwable t) {
            fail("Unexpected error when building a query from bad xml" + t.getMessage());
        }

        try {
            pqb.buildQuery(bagXML, schemaUrl, bags);
            fail("Build query did not throw an exception - despite being given bad input - got this:" + pqb.getQuery());
        } catch (AssertionFailedError e) {
            throw e;
        } catch (BadRequestException e) {
            assertEquals(
                    "The query XML is well formatted but you do not have access to the following " +
                    "mentioned lists: [Decent Human Beings] query: <query model=\"testmodel\" " +
                    "view=\"Employee.age Employee.name\"><constraint path=\"Employee\" " +
                    "op=\"IN\" value=\"Decent Human Beings\" /></query>",
                    e.getMessage()
            );
        } catch (Throwable t) {
            fail("Unexpected error when building a query from bad xml" + t.getMessage());
        }

    }

}
