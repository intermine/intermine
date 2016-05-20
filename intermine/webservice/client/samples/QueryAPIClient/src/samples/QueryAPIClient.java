package samples;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintStream;

import java.util.Iterator;
import java.util.List;

import static org.apache.commons.lang.StringUtils.repeat;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;

/**
 * The QueryAPIClient is an example of query client fetching results of query from InterMine web
 * service.  It demonstrates using InterMine query API.
 **/
public class QueryAPIClient
{
    private static final String serviceRootUrl = "http://www.flymine.org/query/service";
    private static final String NL = System.getProperty("line.separator");
    private static final String FORMAT = "%-8s | %s" + NL;
    private static final ServiceFactory factory = new ServiceFactory(serviceRootUrl);
    private static final PrintStream o = System.out;

    /**
     * @param args command line arguments
     * @throws IOException
     */
    public static void main(String[] args) {

        QueryService service = factory.getQueryService();

        // Create a query
        PathQuery query = new PathQuery(factory.getModel());
        query.addViews("Organism.taxonId", "Organism.name");

        // Run the query
        Iterator<List<Object>> result = service.getRowListIterator(query);

        // Print a header
        o.printf(FORMAT, "Taxon ID", "Species");
        o.println(repeat("-", 9) + "+" + repeat("-", 30));

        // Print the results
        while (result.hasNext()) {
            o.printf(FORMAT, result.next().toArray());
        }

        // Print a summary
        o.println(NL + service.getCount(query) + " results");
    }
}
