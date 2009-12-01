package samples;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;

/**
 * Calculate the coverage of BindingSites over Chromosomes.
 *
 * @author Matthew Wakeling
 **/
public class ChromosomeCoverage
{
    private static String serviceRootUrl = "http://intermine.modencode.org/release-14/service";

    /**
     * Executes a query and prints out the coverage of the given type of object over the given
     * chromosomes.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        calculate("7227", "BindingSite", null);
    }

    private static void calculate(String taxonId, String featureType, String chromosomeId) {
        QueryService service =
            new ServiceFactory(serviceRootUrl, "ChromosomeCoverage").getQueryService();
        Model model = getModel();
        PathQuery query = new PathQuery(model);
        query.setView("Chromosome.primaryIdentifier Chromosome.length");
        query.addConstraint("Chromosome.organism.taxonId", Constraints.eq(taxonId));
        if (chromosomeId != null) {
            query.addConstraint("Chromosome.primaryIdentifier", Constraints.eq(chromosomeId));
        }
        List<List<String>> result = service.getResult(query, 100000);
        Map<String, Integer> chromosomeSizes = new HashMap<String, Integer>();
        for (List<String> row : result) {
            if (row.size() == 2) {
                chromosomeSizes.put(row.get(0), Integer.parseInt(row.get(1)));
            }
        }
        query = new PathQuery(model);
        query.setView(featureType + ".chromosome.primaryIdentifier " + featureType
                + ".chromosomeLocation.start " + featureType + ".chromosomeLocation.end");
        query.setOrderBy(featureType + ".chromosome.primaryIdentifier " + featureType
                + ".chromosomeLocation.start");
        query.addConstraint(featureType + ".chromosome.organism.taxonId", Constraints.eq(taxonId));
        if (chromosomeId != null) {
            query.addConstraint(featureType + ".chromosome.primaryIdentifier",
                    Constraints.eq(chromosomeId));
        }
        String currentChromosome = null;
        int coverage = 0;
        int lastEnd = Integer.MIN_VALUE;
        int lastStart = Integer.MIN_VALUE;
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many rows for the web service");
        }
        for (List<String> row : result) {
            String chromosome = row.get(0);
            int start = Integer.parseInt(row.get(1));
            int end = Integer.parseInt(row.get(2));
            if ((currentChromosome != null) && !currentChromosome.equals(chromosome)) {
                printStatus(currentChromosome, coverage, chromosomeSizes);
                coverage = 0;
                lastEnd = Integer.MIN_VALUE;
                lastStart = Integer.MIN_VALUE;
            }
            currentChromosome = chromosome;
            if (start < lastStart) {
                throw new IllegalArgumentException("Features are not sorted by start position");
            }
            lastStart = start;
            if (end > lastEnd) {
                start = Math.max(start, lastEnd);
                coverage += end - start;
                lastEnd = end;
            }
        }
        printStatus(currentChromosome, coverage, chromosomeSizes);
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }

    private static void printStatus(String chromosome, int coverage,
            Map<String, Integer> chromosomeSizes) {
        if (chromosomeSizes.containsKey(chromosome)) {
            System.out.println("Chromosome " + chromosome + " has coverage " + coverage
                    + " with size " + chromosomeSizes.get(chromosome)
                    + " equals coverage of " + (Math.round(((10000.0 * coverage)
                                / chromosomeSizes.get(chromosome))) / 100.0) + "%");
        }
    }
}
