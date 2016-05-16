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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.services.TemplateService;

/**
 * The TemplateClient is an example of client fetching template results from InterMine web service.
 * It demonstrates using of InterMine template web service.This example returns first 100 predicted
 * orthologues between two organisms sorted by FlyBase gene identifier.
 *
 * NOTE: The template name or template parameters can change at the server in next versions of
 * FlyMine. In this case please download newer version of samples or modify sample properly.
 *
 * @author Jakub Kulaviak
 **/
public class TemplateClient
{
    private static final String serviceRootUrl = "http://www.flymine.org/query/service";
    private static final String templateName = "Pathway_Genes";
    private static final String pathway = "ABC transporters";;
    private static final ServiceFactory services = new ServiceFactory(serviceRootUrl);
    private static final Page page = new Page(0, 10);
    /**
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {

        TemplateService service = services.getTemplateService();

        TemplateQuery template = service.getTemplate(templateName);
        template.replaceConstraint(template.getConstraintForCode("A"),
                Constraints.eq("Pathway.name", pathway));

        FormatInfo f = new FormatInfo(template);

        List<List<String>> result = service.getResults(template, page);

        int total = service.getCount(template);
        int showing = Math.min(total, page.getSize());

        System.out.println(template.getDescription());
        System.out.println("Showing " + showing + " of " + total);
        System.out.println(f.top);
        System.out.printf(f.format, template.getView().toArray());

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
                int cellWidth = Math.max(query.getView().get(i).length(), 20);
                format += " %-" + cellWidth + "s";
                width += cellWidth + 3;
                divider += StringUtils.repeat("-", cellWidth) + "--+";
            }
            format += "\n";
            top = StringUtils.repeat("=", width);
        }
    }
}
