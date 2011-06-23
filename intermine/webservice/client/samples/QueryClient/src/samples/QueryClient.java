package samples;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;


/**
 * The QueryClient is an example of query client fetching query results from InterMine web service.
 * It demonstrates using of InterMine query web service.
 * This example displays first 100 genes shorter than 1kB and sorted by the identifier.
 *
 * NOTE: The model can change at the server in next versions of FlyMine and sample won't work. For
 * example primaryIdentifier gene attribute can be renamed. In this case please download newer
 * version of samples or modify sample properly.
 *
 * @author Jakub Kulaviak
 **/
public class QueryClient
{

    private static String serviceRootUrl = "http://localhost:8080/query/service";

    /**
     *
     * @param args command line arguments
     * @throws IOException
     */
    public static void main(String[] args) {

        QueryService service = new ServiceFactory(serviceRootUrl, "QueryClient").getQueryService();
        // XML representation of PathQuery, XML representation can be downloaded from InterMine
        // website for your query or template following query fetches genes shorter than 1000 bases
        String queryXml = "<query name=\"\" model=\"genomic\""
            + " view=\"Gene.primaryIdentifier Gene.symbol Gene.length\""
            + " sortOrder=\"Gene.primaryIdentifier asc\">"
            + " <constraint path=\"Gene.length\" op=\"&lt;\" value=\"1000\"/>"
            + "</query>";

        // first 100 results are fetched
        List<List<String>> result = service.getResults(queryXml, 0, 100);
        System.out.println("First 100 genes shorter than 1kB sorted according to the identifier: ");
        for (List<String> row : result) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }
}
