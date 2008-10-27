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

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;


/**
 * The QueryClient is an example of query client fetching query results from InterMine web service. 
 * It demonstrates using of InterMine query web service.
 * This example displays first 100 Drosophila  melanogaster genes sorted by the FlyBase identifier. 
 *
 * NOTE: The model can change at the server in next versions of FlyMine and sample won't work. For example
 * primaryIdentifier gene attribute can be renamed. In this case please download newer version of samples 
 * or modify sample properly.
 * 
 * @author Jakub Kulaviak
 **/
public class QueryClient
{
    
    private static String serviceRootUrl = "http://www.flymine.org/query/service";
    
    public static void main(String[] args) throws IOException {
        
        QueryService service = new ServiceFactory(serviceRootUrl, "QueryClient").getQueryService();
        // XML representation of PathQuery, XML representation can be downloaded from InterMine website for your query or template
        // following query fetches Drosophila melanogaster genes 
        String queryXml = 
          "<query model=\"genomic\" view=\"Gene.primaryIdentifier Gene.secondaryIdentifier " 
          + "Gene.symbol Gene.name Gene.organism.shortName\" sortOrder=\"Gene.primaryIdentifier\">"
          + "<node path=\"Gene\" type=\"Gene\">"
          + "</node>"
          + "<node path=\"Gene.organism\" type=\"Organism\">"
          + "</node>"
          + "<node path=\"Gene.organism.name\" type=\"String\">"
          +  "<constraint op=\"=\" value=\"Drosophila melanogaster\" description=\"\" identifier=\"\" code=\"A\">"
          +  "</constraint>"
          + "</node>"
          + "</query>";
        // first 100 results are fetched
        List<List<String>> result = service.getResult(queryXml, 0, 100);
        System.out.println("First 100 Drosophila  melanogaster genes sorted according to the FlyBase identifier: ");
        for (List<String> row : result) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }
}
