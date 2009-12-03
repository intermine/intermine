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
import java.util.SortedSet;
import java.util.TreeSet;

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
public class GenesOverlappingTranscriptRegions
{
    private static String serviceRootUrl = "http://intermine.modencode.org/release-14/service";

    /**
     * Executes a query and prints out results. Finds the number of primary genes that overlap
     * any TranscriptRegion, per Chromosome.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        calculate("7227", "TranscriptRegion", null);
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
        System.err.println("Getting " + featureType + "s");
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
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many rows for the web service");
        }
        System.err.println("Flattening " + featureType + " coverage");
        Map<String, TreeSet<IntRange>> coverage = new HashMap<String, TreeSet<IntRange>>();
        IntRange lastRange = null;
        String currentChromosome = null;
        TreeSet<IntRange> coverageForChromosome = null;
        for (List<String> row : result) {
            String chromosome = row.get(0);
            int start = Integer.parseInt(row.get(1));
            int end = Integer.parseInt(row.get(2));
            if (!chromosome.equals(currentChromosome)) {
                if (lastRange != null) {
                    coverageForChromosome.add(lastRange);
                }
                coverageForChromosome = coverage.get(chromosome);
                if (coverageForChromosome == null) {
                    coverageForChromosome = new TreeSet<IntRange>();
                    coverage.put(chromosome, coverageForChromosome);
                }
                lastRange = null;
            }
            if (lastRange != null) {
                if (lastRange.end < end) {
                    if (lastRange.end < start) {
                        coverageForChromosome.add(lastRange);
                        lastRange = new IntRange(start, end);
                    } else {
                        lastRange = new IntRange(lastRange.start, end);
                    }
                }
            } else {
                lastRange = new IntRange(start, end);
            }
            currentChromosome = chromosome;
        }
        if (lastRange != null) {
            coverageForChromosome.add(lastRange);
        }
        System.err.println("Coverage: " + coverage.keySet());
        int coverageCount = 0;
        for (Map.Entry<String, TreeSet<IntRange>> coverageEntry : coverage.entrySet()) {
            coverageCount += coverageEntry.getValue().size();
        }
        System.err.println("Reduced " + result.size() + " overlapping " + featureType + "s to "
                + coverageCount + " coverage ranges");
        result = null;
        System.gc();
        System.err.println("Getting Genes");
        query = new PathQuery(model);
        query.setView("Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end");
        query.setOrderBy("Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start");
        query.addConstraint("Gene.chromosome.organism.taxonId", Constraints.eq(taxonId));
        if ("7227".equals(taxonId)) {
            query.addConstraint("Gene.dataSets.title", Constraints.eq("FlyBase Drosophila melanogaster data set"));
        } else if ("6239".equals(taxonId)) {
            //query.addConstraint("Gene.dataSets.title", Constraints.eq("We don't know this"));
            query.addConstraint("Gene.symbol", Constraints.isNotNull());
        }
        if (chromosomeId != null) {
            query.addConstraint("Gene.chromosome.primaryIdentifier", Constraints.eq(chromosomeId));
        }
        result = service.getResult(query, 10000000);
        if (result.size() >= 10000000) {
            throw new IllegalArgumentException("There are too many Genes for the web service");
        }
        currentChromosome = null;
        int overlapping = 0;
        int geneCount = 0;
        for (List<String> row : result) {
            String chromosome = row.get(0);
            int start = Integer.parseInt(row.get(1));
            int end = Integer.parseInt(row.get(2));
            if (!chromosome.equals(currentChromosome)) {
                printStatus(currentChromosome, chromosomeSizes, geneCount, overlapping);
                coverageForChromosome = coverage.get(chromosome);
                if (coverageForChromosome == null) {
                    System.err.println("No " + featureType + "s for Chromosome " + chromosome);
                    coverageForChromosome = new TreeSet<IntRange>();
                }
                geneCount = 0;
                overlapping = 0;
            }
            currentChromosome = chromosome;
            geneCount++;
            IntRange range = new IntRange(start, end);
            //System.err.println("Inspecting Gene with range " + range);
            IntRange floor = coverageForChromosome.floor(range);
            SortedSet<IntRange> coverageRanges;
            if (floor == null) {
                coverageRanges = coverageForChromosome;
            } else {
                coverageRanges = coverageForChromosome.tailSet(floor);
            }
            for (IntRange covered : coverageRanges) {
                if (covered.getStart() >= end) {
                    break;
                }
                if (covered.getEnd() > start) {
                    overlapping++;
                    break;
                }
            }
        }
        printStatus(currentChromosome, chromosomeSizes, geneCount, overlapping);
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }

    private static void printStatus(String chromosome, Map<String, Integer> chromosomeSizes,
            int geneCount, int overlapping) {
        if (chromosomeSizes.containsKey(chromosome)) {
            System.out.println("Chromosome " + chromosome + " of size "
                    + chromosomeSizes.get(chromosome) + " has " + geneCount + " genes, of which "
                    + overlapping + " overlap, which is " + (Math.round(((10000.0 * overlapping)
                                / geneCount)) / 100.0) + "%");
        }
    }

    private static class IntRange implements Comparable<IntRange>
    {
        int start, end;

        public IntRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int compareTo(IntRange o) {
            if (o.start > start) {
                return -1;
            } else if (o.start < start) {
                return 1;
            }
            return 0;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String toString() {
            return start + " - " + end;
        }
    }
}
