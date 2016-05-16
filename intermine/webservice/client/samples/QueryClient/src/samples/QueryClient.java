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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.results.Page;


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

    private static String serviceRootUrl = "http://www.flymine.org/query/service";
    private static final Page page = new Page(0, 10);

    /**
     *
     * @param args command line arguments
     * @throws IOException
     */
    public static void main(String[] args) {

        ServiceFactory services = new ServiceFactory(serviceRootUrl);
        PathQuery query = new PathQuery(services.getModel());
        query.addViews("Gene.primaryIdentifier", "Gene.symbol", "Gene.length", "Gene.organism.species");
        query.addConstraint(Constraints.lessThan("Gene.length", "1000"));
        query.addConstraint(Constraints.eq("Gene.organism.genus", "Drosophila"));

        FormatInfo f = new FormatInfo(query);

        int total = services.getQueryService().getCount(query);
        int showing = Math.min(total, page.getSize());

        List<List<String>> result = services.getQueryService().getResults(query, page);
        System.out.println("Genes shorter than 1kB sorted according to identifier");
        System.out.println("Showing " + showing + " of " + total);
        System.out.println(f.top);
        System.out.printf(f.format, query.getView().toArray());
        System.out.println(f.divider);
        for (List<String> row : result) {
            System.out.printf(f.format, row.toArray());
        }
        System.out.println(f.divider);
    }

    private static class FormatInfo
    {
        String format;
        String divider;
        String top;

        FormatInfo(PathQuery query) {
            format = "|";
            divider = "+";
            int width = 1;
            for (int i = 0; i < query.getView().size(); format += " |", i++) {
                int cellWidth = Math.max(query.getView().get(i).length(), 14);
                format += " %-" + cellWidth + "s";
                width += cellWidth + 3;
                divider += StringUtils.repeat("-", cellWidth) + "--+";
            }
            format += "\n";
            top = StringUtils.repeat("=", width);
        }
    }
}
