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

import java.util.ArrayList;
import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.TemplateService;
import org.intermine.webservice.client.template.TemplateParameter;

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
    private static String serviceRootUrl = "http://localhost:8080/query/service";

    /**
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {

        TemplateService service = new ServiceFactory(serviceRootUrl, "TemplateClient")
            .getTemplateService();
        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        // setting first template parameter
        // first organism should be equal to Drosophila melanogaster
        parameters.add(new TemplateParameter("Homologue.gene.organism.name", "eq", "Drosophila melanogaster"));
        // setting second template parameter
        // second organism should be equal to Caenorhabditis elegans
        parameters.add(new TemplateParameter("Homologue.homologue.organism.name", "eq", "Caenorhabditis elegans"));
        // first 100 results are fetched
        List<List<String>> result = service.getResults("GeneOrganism1_OrthologueOrganism2",
                                                      parameters, 0, 100);
        System.out.println("First 100 predicted orthologues between two organisms"
                           + " sorted by FlyBase gene identifier:");
        for (List<String> row : result) {
            for (String cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

}
