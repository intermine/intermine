package org.intermine.webservice.client.live;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.TemplateService;
import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.TestUtil;
import org.junit.Test;

public class LiveCodeGenOutputTest {

    private String getExpectation(String resourceName) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resourceName));
    }

    @Test
    public void testOneView() throws IOException {

        /**
         * This is a Java program to run a query from TEST_PROJECT_TITLE.
         * It was automatically generated at __SOME-DATE__
         *
         */
        class QueryClient
        {
            
            final StringWriter sw = new StringWriter();
            final PrintWriter out = new PrintWriter(sw);

            /**
             * Perform the query and print the rows of results.
             * @param args command line arguments
             * @throws IOException
             */
            public void run() throws IOException {
                ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl());
                Model model = factory.getModel();
                PathQuery query = new PathQuery(model);

                // Select the output columns:
                query.addView("Manager.name");

                // Add orderby
                query.addOrderBy("Manager.name", OrderDirection.ASC);

                QueryService service = factory.getQueryService();
                Iterator<List<Object>> rows = service.getRowListIterator(query);
                out.println("Manager.name");
                while (rows.hasNext()) {
                    out.println(rows.next().get(0));
                }
                out.printf("%d rows\n", service.getCount(query));
            }

        }

        QueryClient qc = new QueryClient();
        qc.run();

        assertEquals("The correct stuff is printed", getExpectation("one-view.results.expectation"), qc.sw.toString());

    }

    @Test
    public void multiView() throws Exception {
        class QueryClient
        {
            final StringWriter sw = new StringWriter();

            /**
             * Perform the query and print the rows of results.
             * @param args command line arguments
             * @throws IOException
             */
            public void run() throws IOException {
                ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl());
                Model model = factory.getModel();
                PathQuery query = new PathQuery(model);

                // Select the output columns:
                query.addViews("Employee.name",
                        "Employee.end",
                        "Employee.department.name",
                        "Employee.address.address",
                        "Employee.department.company.name");

                // Add orderby
                query.addOrderBy("Employee.department.name", OrderDirection.ASC);

                QueryService service = factory.getQueryService();
                PrintWriter out = new PrintWriter(sw);
                String viewFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                String dataFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                out.printf(viewFormat, query.getView().toArray());
                Iterator<List<Object>> rows = service.getRowListIterator(query);
                while (rows.hasNext()) {
                    out.printf(dataFormat, rows.next().toArray());
                }
                out.printf("%d rows\n", service.getCount(query));
            }

        }
        QueryClient qc = new QueryClient();
        qc.run();

        assertEquals("The correct stuff is printed", getExpectation("multi-view.result.expectation"), qc.sw.toString());
    }

    @Test
    public void outerJoins() throws Exception {
        class QueryClient
        {
            final StringWriter sw = new StringWriter();
            /**
             * Perform the query and print the rows of results.
             * @param args command line arguments
             * @throws IOException
             */
            public void run() throws IOException {
                ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl());
                Model model = factory.getModel();
                PathQuery query = new PathQuery(model);

                // Select the output columns:
                query.addViews("Employee.name",
                        "Employee.age",
                        "Employee.fullTime",
                        "Employee.address.address",
                        "Employee.end");

                // Add orderby
                query.addOrderBy("Employee.age", OrderDirection.ASC);

                query.addConstraint(Constraints.lessThanEqualTo("Employee.name", "F"));

                // Outer Joins
                // Show all information about these relationships if they exist, but do not require that they exist.
                query.setOuterJoinStatus("Employee.address", OuterJoinStatus.OUTER);

                QueryService service = factory.getQueryService();

                PrintWriter out = new PrintWriter(sw);
                String viewFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                String dataFormat = "%-17.17s | %-17d | %-17b | %-17.17s | %-17.17s\n";
                out.printf(viewFormat, query.getView().toArray());
                Iterator<List<Object>> rows = service.getRowListIterator(query);
                while (rows.hasNext()) {
                    out.printf(dataFormat, rows.next().toArray());
                }
                out.printf("%d rows\n", service.getCount(query));
            }
        }

        QueryClient qc = new QueryClient();
        qc.run();

        assertEquals("The correct stuff is printed", getExpectation("outerjoins.result.expectation"), qc.sw.toString());
    }

    @Test
    public void eqAndNeq() throws Exception {
        class QueryClient
        {
            final StringWriter sw = new StringWriter();

            /**
             * Perform the query and print the rows of results.
             * @param args command line arguments
             * @throws IOException
             */
            public void run() throws IOException {
                ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl());
                Model model = factory.getModel();
                PathQuery query = new PathQuery(model);

                // Select the output columns:
                query.addViews("Employee.name",
                        "Employee.age",
                        "Employee.fullTime",
                        "Employee.end",
                        "Employee.address.address");

                // Add orderby
                query.addOrderBy("Employee.name", OrderDirection.ASC);

                // Filter the results with the following constraints:
                query.addConstraint(Constraints.eq("Employee.department.name", "Sales"));
                query.addConstraint(Constraints.neq("Employee.department.company.name", "W*"));

                QueryService service = factory.getQueryService();
                PrintWriter out = new PrintWriter(sw);
                String viewFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                String dataFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                out.printf(viewFormat, query.getView().toArray());
                Iterator<List<Object>> rows = service.getRowListIterator(query);
                while (rows.hasNext()) {
                    out.printf(dataFormat, rows.next().toArray());
                }
                out.printf("%d rows\n", service.getCount(query));
            }

        }
        QueryClient qc = new QueryClient();
        qc.run();

        assertEquals("The correct stuff is printed", getExpectation("eqneq.result.expectation"), qc.sw.toString());
    }

    @Test
    public void like() throws Exception {
        class QueryClient
        {
            final StringWriter sw = new StringWriter();

            /**
             * Perform the query and print the rows of results.
             * @param args command line arguments
             * @throws IOException
             */
            public void run() throws IOException {
                ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl());
                Model model = factory.getModel();
                PathQuery query = new PathQuery(model);

             // Select the output columns:
                query.addViews("Employee.name",
                        "Employee.age",
                        "Employee.fullTime",
                        "Employee.end",
                        "Employee.address.address");

                // Add orderby
                query.addOrderBy("Employee.name", OrderDirection.ASC);

                // Filter the results with the following constraints:
                query.addConstraint(Constraints.like("Employee.address.address", "Rue*"));

                QueryService service = factory.getQueryService();
                PrintWriter out = new PrintWriter(sw);
                String viewFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                String dataFormat = "%-17.17s | %-17.17s | %-17.17s | %-17.17s | %-17.17s\n";
                out.printf(viewFormat, query.getView().toArray());
                Iterator<List<Object>> rows = service.getRowListIterator(query);
                while (rows.hasNext()) {
                    out.printf(dataFormat, rows.next().toArray());
                }
                out.printf("%d rows\n", service.getCount(query));
            }

        }


        QueryClient qc = new QueryClient();
        qc.run();

        assertEquals("The correct stuff is printed", getExpectation("like.result.expectation"), qc.sw.toString());
    }

    @Test
    public void complicatedTemplate() throws Exception {
        class TemplateQueryFourConstraints
        {
            final StringWriter sw = new StringWriter();

            /**
            * Perform the query and print the rows of results.
            * @param args command line arguments
            * @throws IOException
            */
            public void run() throws IOException {
                ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl());
                // Edit the template parameter values to get different results
                List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
                parameters.add(new TemplateParameter("Employee.name", "CONTAINS", "Employee", null, "D"));
                parameters.add(new TemplateParameter("Employee.age", "<", "10", null, "B"));
                parameters.add(new TemplateParameter("Employee.age", ">", "30", null, "C"));
                parameters.add(new TemplateParameter("Employee.fullTime", "=", "true", null, "A"));

                // Name of template
                String name = "fourConstraints";
                // Template Service - use this object to fetch results.
                TemplateService service = factory.getTemplateService();
                PrintWriter out = new PrintWriter(sw);

                out.printf("%-22.22s | %-22.22s | %-22.22s | %-22.22s\n", "Employee.name", "Employee.age", "Employee.end", "Employee.fullTime");
                Iterator<List<Object>> rows = service.getRowListIterator(name, parameters);
                while (rows.hasNext()) {
                    out.printf("%-22.22s | %-22d | %-22.22s | %-22.22s\n", rows.next().toArray());
                }
                out.printf("%d rows\n", service.getCount(name, parameters));
            }

        }

        TemplateQueryFourConstraints tqfc = new TemplateQueryFourConstraints();
        tqfc.run();

        assertEquals("The correct stuff is printed", getExpectation("complex.template.expectation"), tqfc.sw.toString());

    }

}
