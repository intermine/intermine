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
import java.util.SortedSet;
import java.util.TreeSet;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.util.IteratorIterable;
import org.intermine.util.TextTable;

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
        if (args.length == 0) {
            System.err.println("Using default parameters of 7227 TranscriptRegion Gene");
            calculate("7227", "TranscriptRegion", "Gene", null);
        } else {
            String taxonId = args[0];
            if ("fly".equals(taxonId)) {
                taxonId = "7227";
            } else if ("worm".equals(taxonId)) {
                taxonId = "6239";
            }
            if (args.length == 3) {
                calculate(taxonId, args[1], args[2], null);
            } else if (args.length == 4) {
                calculate(taxonId, args[1], args[2], args[3]);
            } else {
                System.err.println("Usage: program <taxonId> <coverage feature> <query feature> [<chromosome>]");
                System.err.println("Instead of numeric taxonIds, you can specify \"fly\" or \"worm\"");
            }
        }
    }

    private static void calculate(String taxonId, String featureType1, String featureType2, String chromosomeId) {
        QueryService service =
            new ServiceFactory(serviceRootUrl, "ChromosomeCoverage").getQueryService();
        System.err.println("Getting " + featureType1 + "s");
        PathQuery query = makeQuery(taxonId, featureType1, chromosomeId);
        Map<String, TreeSet<IntRange>> coverage = new HashMap<String, TreeSet<IntRange>>();
        IntRange lastRange = null;
        String currentChromosome = null;
        TreeSet<IntRange> coverageForChromosome = null;
        int entryCount = 0;
        for (List<String> row : new IteratorIterable<List<String>>(service.getResultIterator(query, 10000000))) {
            entryCount++;
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
        if (entryCount >= 10000000) {
            throw new IllegalArgumentException("There are too many " + featureType1 + "s for the web service");
        }
        //System.err.println("Coverage: " + coverage.keySet());
        int coverageCount = 0;
        for (Map.Entry<String, TreeSet<IntRange>> coverageEntry : coverage.entrySet()) {
            coverageCount += coverageEntry.getValue().size();
        }
        System.err.println("Reduced " + entryCount + " overlapping " + featureType1 + "s to "
                + coverageCount + " coverage ranges");
        System.err.println("Getting " + featureType2 + "s");
        Output results = new Output(featureType1, featureType2);
        query = makeQuery(taxonId, featureType2, chromosomeId);
        currentChromosome = null;
        int overlapping = 0;
        int geneCount = 0;
        entryCount = 0;
        for (List<String> row : new IteratorIterable<List<String>>(service.getResultIterator(query, 10000000))) {
            String chromosome = row.get(0);
            if (!(chromosome.startsWith("U") || chromosome.equals("dmel_mitochondrion_genome"))) {
                int start = Integer.parseInt(row.get(1));
                int end = Integer.parseInt(row.get(2));
                if (!chromosome.equals(currentChromosome)) {
                    if (currentChromosome != null) {
                        results.add(currentChromosome, geneCount, overlapping);
                    }
                    coverageForChromosome = coverage.get(chromosome);
                    if (coverageForChromosome == null) {
                        System.err.println("No " + featureType1 + "s for Chromosome " + chromosome);
                        coverageForChromosome = new TreeSet<IntRange>();
                    }
                    geneCount = 0;
                    overlapping = 0;
                }
                currentChromosome = chromosome;
                geneCount++;
                IntRange range = new IntRange(start, end);
                IntRange floor = coverageForChromosome.floor(range);
                SortedSet<IntRange> coverageRanges;
                if (floor == null) {
                    coverageRanges = coverageForChromosome;
                } else {
                    coverageRanges = coverageForChromosome.tailSet(floor);
                }
                boolean thisOneOverlaps = false;
                for (IntRange covered : coverageRanges) {
                    if (covered.getStart() >= end) {
                        break;
                    }
                    if (covered.getEnd() > start) {
                        overlapping++;
                        thisOneOverlaps = true;
                        break;
                    }
                }
                if ((!thisOneOverlaps) && "Gene".equals(featureType2)) {
                    System.out.println(row.get(3));
                }
            }
        }
        if (entryCount >= 10000000) {
            throw new IllegalArgumentException("There are too many " + featureType2 + "s for the web service");
        }
        results.add(currentChromosome, geneCount, overlapping);
        results.printResults();
    }

    private static Model getModel() {
        ModelService service = new ServiceFactory(serviceRootUrl, "ClientAPI").getModelService();
        return service.getModel();
    }

    private static PathQuery makeQuery(String taxonId, String featureType, String chromosomeId) {
        PathQuery query = new PathQuery(getModel());
        query.setView(featureType + ".chromosome.primaryIdentifier " + featureType
                + ".chromosomeLocation.start " + featureType + ".chromosomeLocation.end "
                + featureType + ".primaryIdentifier");
        query.setOrderBy(featureType + ".chromosome.primaryIdentifier " + featureType
                + ".chromosomeLocation.start");
        query.addConstraint(featureType + ".chromosome.organism.taxonId", Constraints.eq(taxonId));
        if ("Gene".equals(featureType) || "Exon".equals(featureType)) {
            if ("7227".equals(taxonId)) {
                query.addConstraint(featureType + ".dataSets.title",
                        Constraints.eq("FlyBase Drosophila melanogaster data set"));
            } else if ("6239".equals(taxonId)) {
                //query.addConstraint("Gene.dataSets.title", Constraints.eq("We don't know this"));
                query.addConstraint(featureType + ".symbol", Constraints.isNotNull());
            }
        }
        if (chromosomeId != null) {
            query.addConstraint(featureType + ".chromosome.primaryIdentifier",
                    Constraints.eq(chromosomeId));
        }
        return query;
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

    private static class Output
    {
        String featureType1, featureType2;
        TextTable table;
        int totalCount = 0;
        int totalOverlaps = 0;

        public Output(String featureType1, String featureType2) {
            this.featureType1 = featureType1;
            this.featureType2 = featureType2;
            table = new TextTable(false, false, false);
            table.addRow("Chromosome", featureType2 + "s", "Overlapping", "Percent", "Non-overlapping", "Percent");
            table.addRow(TextTable.ROW_SEPARATOR);
        }

        public void add(String chromosome, int count, int overlaps) {
            double percent = Math.round(((10000.0 * overlaps) / count)) / 100.0;
            int nonOverlap = count - overlaps;
            double nonPercent = Math.round(((10000.0 * nonOverlap) / count)) / 100.0;
            table.addRow(chromosome, "" + count, "" + overlaps, "" + percent, "" + nonOverlap, "" + nonPercent);
            totalCount += count;
            totalOverlaps += overlaps;
        }

        public void printResults() {
            System.out.println("Overlaps report for " + featureType2 + "s which overlap " + featureType1 + "s");
            table.addRow(TextTable.ROW_SEPARATOR);
            add("Total", totalCount, totalOverlaps);
            System.out.print(table.toString());
        }
    }
}
