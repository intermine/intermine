
package samples;

/*
 * Copyright (C) 2002-2010 FlyMine
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
        calculate("7227", "TFBindingSite", null);
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
        int genomeSize = 0;
        for (List<String> row : result) {
            if (row.size() == 2) {
                String chrName = row.get(0);
                int size = Integer.parseInt(row.get(1));
                if (chrName.startsWith("U")) {
                    continue;
                }
                genomeSize += size;
                chromosomeSizes.put(chrName, size);
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

        System.out.println("\nCoverage report for " + featureType + "s in organism " + taxonId + ":\n");
        String currentChromosome = null;
        int coverage = 0;
        int totalCoverage = 0;
        int lastEnd = Integer.MIN_VALUE;
        int lastStart = Integer.MIN_VALUE;
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many rows for the web service");
        }
        for (List<String> row : result) {
            String chromosome = row.get(0);

            if (!chromosomeSizes.containsKey(chromosome)) {
                continue;
            }
            int start = Integer.parseInt(row.get(1));
            int end = Integer.parseInt(row.get(2));
            if ((currentChromosome != null) && !currentChromosome.equals(chromosome)) {
                printStatus("Chromosome " + currentChromosome, coverage, chromosomeSizes.get(currentChromosome));
                totalCoverage += coverage;
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
        totalCoverage += coverage;
        printStatus("Chromosome " + currentChromosome, coverage, chromosomeSizes.get(currentChromosome));
        printStatus("\nWhole geneome", totalCoverage, genomeSize);
        System.out.println("\n");
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }

    private static void printStatus(String startMessage, int coverage, Integer size) {
        if (size != null) {
            System.out.println(startMessage + " has coverage " + coverage
                    + " with size " + size
                    + " equals coverage of " + (Math.round(((10000.0 * coverage)
                                / size)) / 100.0) + "%");
        }
    }
}
