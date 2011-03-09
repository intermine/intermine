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
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.TemplateService;
import org.intermine.webservice.client.template.TemplateParameter;

/**
 * An example web service query to be extended in the !InterMine workshop tutorial. 
 **/
public class WorkshopExample
{
    private static String serviceRootUrl = "http://preview.flymine.org/preview/service";

    private static final int MAX_ROWS = 10000;

    /**
     * 
     * @param args command line arguments
     * @throws IOException
     */
    public static void main(String[] args) {
        QueryService service =
            new ServiceFactory(serviceRootUrl, "QueryAPIClient").getQueryService();
        Model model = getModel();

        PathQuery query = new PathQuery(model);
        query.addViews("Protein.primaryIdentifier", "Protein.proteinDomains.name");
        query.addOrderBy("Protein.primaryIdentifier", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Protein.proteinDomains.name", "Homeobox"));
        query.addConstraint(Constraints.eq("Protein.organism.shortName", "D. melanogaster"));

        // Run the query
        List<List<String>> result = service.getResult(query, MAX_ROWS);

        // Output results
        printResults(result);
    }

    private static void printResults(List<List<String>> result) {
        int count = 0;
        for (List<String> row : result) {
            for (String cell : row) {
                System.out.print(cell + "\t");
            }
            System.out.println();
            count++;
        }
        System.out.println(System.getProperty("line.separator") + count + " results");
    }

    private static String makeCommaSeparatedString(Collection<String> elements) {
        StringBuilder sb = new StringBuilder();
        for (String element : elements) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(element);
        }
        return sb.toString();
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }
}
