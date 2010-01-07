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
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;

/**
 * Calculate the proportion of Genes that overlap TranscriptRegions by using bioseg.
 *
 * @author Matthew Wakeling
 **/
public class OverlapsWithBioseg
{
    private static String serviceRootUrl = "http://intermine.modencode.org/release-14/service";

    /**
     * Executes a query and prints out the results.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        calculate("7227", "TranscriptRegion", null);
    }

    private static void calculate(String taxonId, String featureType, String chromosomeId) {
        QueryService service =
            new ServiceFactory(serviceRootUrl, "OverlapsWithBioseg").getQueryService();
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
        query = makeQuery(taxonId, featureType, chromosomeId, true);
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many rows for the web service");
        }
        String currentChromosome = null;
        int featureCount = 0;
        Map<String, Integer> overlaps = new HashMap<String, Integer>();
        for (List<String> row : result) {
            String chromosome = row.get(0);
            String gene = row.get(1);
            if ((currentChromosome != null) && !currentChromosome.equals(chromosome)) {
                overlaps.put(currentChromosome, featureCount);
                featureCount = 0;
            }
            currentChromosome = chromosome;
            featureCount++;
        }
        overlaps.put(currentChromosome, featureCount);
        query = makeQuery(taxonId, featureType, chromosomeId, false);
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many rows for the web service");
        }
        currentChromosome = null;
        featureCount = 0;
        for (List<String> row : result) {
            String chromosome = row.get(0);
            String gene = row.get(1);
            if ((currentChromosome != null) && !currentChromosome.equals(chromosome)) {
                printStatus(currentChromosome, chromosomeSizes, featureCount, overlaps.get(currentChromosome));
                featureCount = 0;
            }
            currentChromosome = chromosome;
            featureCount++;
        }
        printStatus(currentChromosome, chromosomeSizes, featureCount, overlaps.get(currentChromosome));
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }

    private static void printStatus(String chromosome, Map<String, Integer> chromosomeSizes,
            int featureCount, Integer overlaps) {
        if (chromosomeSizes.containsKey(chromosome)) {
            int overlapping = (overlaps == null ? 0 : overlaps);
            System.out.println("Chromosome " + chromosome + " of size "
                    + chromosomeSizes.get(chromosome) + " has " + featureCount + " genes, of which "
                    + overlapping + " overlap, which is " + (Math.round(((10000.0 * overlapping)
                                / featureCount)) / 100.0) + "%");
        }
    }

    public static PathQuery makeQuery(String taxonId, String featureType, String chromosomeId, boolean onlyOverlaps) {
        PathQuery query = new PathQuery(getModel());
        query.setView("Gene.chromosome.primaryIdentifier Gene.primaryIdentifier");
        query.setOrderBy("Gene.chromosome.primaryIdentifier Gene.primaryIdentifier");
        query.addConstraint("Gene.chromosome.organism.taxonId", Constraints.eq(taxonId));
        if ("7227".equals(taxonId)) {
            query.addConstraint("Gene.dataSets.title", Constraints.eq("FlyBase Drosophila melanogaster data set"));
        } else if ("6239".equals(taxonId)) {
            //query.addConstraint("Gene.dataSets.title", Constraints.eq("We don't know this"));
            query.addConstraint("Gene.symbol", Constraints.isNotNull());
        }
        if (chromosomeId != null) {
            query.addConstraint(featureType + ".chromosome.primaryIdentifier",
                    Constraints.eq(chromosomeId));
        }
        if (onlyOverlaps) {
            query.addConstraint("Gene.overlappingFeatures.primaryIdentifier", Constraints.isNotNull());
            PathNode node = query.addNode("Gene.overlappingFeatures");
            node.setType(featureType);
        }
        return query;
    }
}
