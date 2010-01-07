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
 * Calculate the distribution of BindingSite sizes.
 *
 * @author Matthew Wakeling
 **/
public class BindingSiteSize
{
    private static String serviceRootUrl = "http://intermine.modencode.org/release-14/service";

    /**
     * Executes a query and prints out the results.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        calculate("7227", "BindingSite", null);
    }

    private static void calculate(String taxonId, String featureType, String chromosomeId) {
        QueryService service =
            new ServiceFactory(serviceRootUrl, "BindingSiteSize").getQueryService();
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
        query.setView(featureType + ".chromosome.primaryIdentifier " + featureType + ".length");
        query.setOrderBy(featureType + ".chromosome.primaryIdentifier");
        query.addConstraint(featureType + ".chromosome.organism.taxonId", Constraints.eq(taxonId));
        if (chromosomeId != null) {
            query.addConstraint(featureType + ".chromosome.primaryIdentifier",
                    Constraints.eq(chromosomeId));
        }
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many rows for the web service");
        }
        String currentChromosome = null;
        int featureCount = 0;
        long totalSize = 0;
        long totalSquareSize = 0;
        System.out.print("Chromosome\tSize\tCount\tAverage\tStandardDeviation");
        for (int i = 1; i < 20000000; i += i) {
            String key = i + " - " + (i * 2 - 1);
            System.out.print("\t" + i + " - " + (i * 2 - 1));
        }
        System.out.println("");
        Map<String, Integer> bins = new HashMap<String, Integer>();
        for (List<String> row : result) {
            String chromosome = row.get(0);
            int length = Integer.parseInt(row.get(1));
            if ((currentChromosome != null) && !currentChromosome.equals(chromosome)) {
                printStatus(currentChromosome, chromosomeSizes, featureCount, totalSize,
                        totalSquareSize, bins);
                featureCount = 0;
                totalSize = 0;
                totalSquareSize = 0;
                bins.clear();
            }
            currentChromosome = chromosome;
            featureCount++;
            totalSize += length;
            totalSquareSize += ((long) length) * ((long) length);
            String binId = getBinId(length);
            Integer currentBinCount = bins.get(binId);
            if (currentBinCount == null) {
                bins.put(binId, 1);
            } else {
                bins.put(binId, 1 + currentBinCount);
            }
        }
        printStatus(currentChromosome, chromosomeSizes, featureCount, totalSize, totalSquareSize,
                bins);
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }

    private static void printStatus(String chromosome, Map<String, Integer> chromosomeSizes,
            int featureCount, long totalSize, long totalSquareSize, Map<String, Integer> bins) {
        if (chromosomeSizes.containsKey(chromosome)) {
            double average = ((double) totalSize) / featureCount;
            double std = Math.sqrt((((double) totalSquareSize) / featureCount)
                    - (average * average));
            //System.out.println("Chromosome " + chromosome + " of size "
            //        + chromosomeSizes.get(chromosome) + " has " + featureCount
            //        + " features with average size " + average + " and standard deviation " + std
            //        + " - bins: " + bins);
            System.out.print(chromosome + "\t" + chromosomeSizes.get(chromosome) + "\t"
                    + featureCount + "\t" + average + "\t" + std);
            for (int i = 1; i < 20000000; i += i) {
                String key = i + " - " + (i * 2 - 1);
                Integer binCount = bins.get(key);
                if (binCount == null) {
                    System.out.print("\t0");
                } else {
                    System.out.print("\t" + binCount);
                }
            }
            System.out.println("");
        }
    }

    private static String getBinId(int length) {
        int i = 1;
        while (length >= i * 2) {
            i += i;
        }
        return i + " - " + (i * 2 - 1);
    }
}
