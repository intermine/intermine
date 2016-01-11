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
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
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
    private static final String ROOT_URL = "http://risu.flymine.org/flymine/service";
    private static final ServiceFactory FACTORY = new ServiceFactory(ROOT_URL);
    private static final String FORMAT = "%-13s | %8s | %-11s | %s\n";
    private static final String DIVIDER = StringUtils.repeat("-", 70);

    /**
     *
     * @param args command line arguments
     * @throws IOException
     */
    public static void main(String[] args) {
        QueryService service = FACTORY.getQueryService();

        PathQuery query = new PathQuery(FACTORY.getModel());
        query.addViews("Protein.primaryIdentifier", "Protein.molecularWeight",
                "Protein.genes.symbol", "Protein.proteinDomains.name");
        query.addOrderBy("Protein.primaryIdentifier", OrderDirection.ASC);
        query.addConstraint(Constraints.eq("Protein.proteinDomains.name", "*Homeobox*"));
        query.addConstraint(Constraints.eq("Protein.organism.shortName", "D. melanogaster"));

        // Run the query
        Iterator<List<Object>> result = service.getRowListIterator(query);

        // Print out a header
        System.out.printf(FORMAT, "PROTEIN", "WEIGHT", "GENE SYMBOL", "PROTEIN DOMAIN");
        System.out.println(DIVIDER);

        // Print out the results
        Object last = null;
        int count = 1;
        while (result.hasNext()) {
            List<Object> row = new ArrayList<Object>(result.next());
            if (last != null && !last.equals(row.get(0))) {
                System.out.println(DIVIDER);
                count++;
            } else  if (last != null) {
                row.set(0, "");
                row.set(1, "");
                row.set(2, "");
            }
            last = row.get(0);
            System.out.printf(FORMAT, row.toArray());
        }

        System.out.println(((service.getCount(query) == 0) ? 0 : count) + " results");
    }
}
