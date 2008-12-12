package samples;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;

/**
 * The QueryAPIClient is an example of query client fetching results of query from InterMine web
 * service.  It demonstrates using InterMine query API. It retrieves the same data as QueryClient
 * but demonstrates how to create path query from start and add constraints.
 * This example displays first 100 genes shorter than 1kB and sorted by the identifier.
 *
 * NOTE: The model can change at the server in next versions of FlyMine and sample won't work. For
 * example primaryIdentifier gene attribute can be renamed. In this case please download newer
 * version of samples or modify sample properly.
 *
 * @author Jakub Kulaviak
 **/
public class QueryAPIClient
{
    private static String serviceRootUrl = "http://localhost:8080/query/service";

    public static void main(String[] args) throws IOException {
        QueryService service =
            new ServiceFactory(serviceRootUrl, "QueryAPIClient").getQueryService();
        Model model = getModel();
        PathQuery query = new PathQuery(model);
        query.setView("Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol"
                      + " Gene.name Gene.organism.shortName");
        query.setOrderBy("Gene.primaryIdentifier");
        query.addConstraint("Gene.length", Constraints.lessThan(1000));
        // first 100 results are fetched
        List<List<String>> result = service.getResult(query, 0, 100);
        System.out.println("First 100 genes shorter than 1kB sorted according to the identifier: ");
        for (List<String> row : result) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }
}
