package org.intermine.webservice.client.live;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;
import org.junit.Test;

/**
 * This is a Java program to run a query from TestMine-Alex.
 * It was automatically generated at Fri Dec 23 14:25:00 GMT 2011
 *
 * @author TestMine-Alex
 *
 */
public class LiveGeneratedQueryTest
{
    private static final String ROOT = "http://squirrel.flymine.org/intermine-test/service";

    /**
     * Perform the query and print the rows of results.
     * @param args command line arguments
     * @throws IOException
     */
    @Test
    public void run() throws IOException {
        ServiceFactory factory = new ServiceFactory(ROOT);
        Model model = factory.getModel();
        PathQuery query = new PathQuery(model);

        // Select the output columns:
        query.addViews("Bank.name",
                "Bank.debtors.owedBy.name",
                "Bank.debtors.owedBy.salary",
                "Bank.debtors.debt");

        // Add orderby
        query.addOrderBy("Bank.name", OrderDirection.ASC);

        // Filter the results with the following constraints:
        query.addConstraint(Constraints.greaterThan("Bank.debtors.debt", "10000000"), "A");
        query.addConstraint(Constraints.type("Bank.debtors.owedBy", "CEO"));

        // Outer Joins
        // Show all information about these relationships if they exist, but do not require that they exist.
        query.setOuterJoinStatus("Bank.debtors", OuterJoinStatus.OUTER);

        QueryService service = factory.getQueryService();
        PrintStream out = System.out;
        String viewFormat = "%-22.22s | %-22.22s | %-22.22s | %-22.22s\n";
        String dataFormat = "%-22.22s | %-22.22s | %-22.22s | %-22.22s\n";
        out.printf(viewFormat, query.getView().toArray());
        Iterator<List<Object>> rows = service.getRowListIterator(query);
        while (rows.hasNext()) {
            out.printf(dataFormat, rows.next().toArray());
        }
        out.printf("%d rows\n", service.getCount(query));
    }

}
