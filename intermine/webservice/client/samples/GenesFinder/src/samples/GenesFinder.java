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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;

/**
 * The GenesFinder is an example of command line query client fetching Genes or
 * SequenceFeatures located at specific positions with some tolerance. It means that all
 * genes starting at position higher than start-tolerance and lower than start+tolerance and
 * ending at position higher then end-tolerance and lower than end+tolerance will be retrieved.
 *
 * NOTE: The model can change at the server in next versions of FlyMine and sample won't work. For
 * example primaryIdentifier gene attribute can be renamed. In this case please download newer
 * version of samples or modify sample properly.
 *
 * @author Jakub Kulaviak
 **/
public class GenesFinder
{
    private static String serviceRootUrl = "http://www.flymine.org/query/service";

    /**
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        try {

            String inputFileName = getParameter("--file", args);
            if (inputFileName == null) {
                throw new GenesFinderException("Missing parameter --file");
            }
            List<List<String>> locations = readLocations(inputFileName);

            String type = getParameter("--type", args);
            if (type == null) {
                throw new GenesFinderException("Missing parameter --type");
            }

            String tolerancePar = getParameter("--tolerance", args);
            int tolerance = 0;
            if (tolerancePar != null) {
                tolerance = Integer.parseInt(tolerancePar);
            }

            findFeatures(type, tolerance, locations);
        } catch (GenesFinderException e) {
            System.out.println(e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Required parameters: --file input_file"
                           + " --tolerance tolerance_value --type [Gene|SequenceFeature]");
        System.out.println("Format of a line in the input file"
                           + " (fields must be tab separated): chromozome_id start end");
    }

    private static String getParameter(String name, String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name) && (i + 1) <= (args.length - 1)) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static List<List<String>> readLocations(String fileName) {
        List<List<String>> ret = new ArrayList<List<String>>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                if ("".equals(line.trim())) {
                    continue;
                }
                String[] parts = line.split("\t");
                ret.add(arrayToList(parts));
            }
        } catch (FileNotFoundException e) {
            throw new GenesFinderException("File not found: " + fileName);
        } catch (IOException ex) {
            throw new GenesFinderException("Error during reading from file "
                    + fileName);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return ret;
    }

    private static List<String> arrayToList(String[] parts) {
        List<String> ret = new ArrayList<String>();
        for (String part : parts) {
            ret.add(part.trim());
        }
        return ret;
    }

    private static void findFeatures(String type, int tolerance,
            List<List<String>> locations) {
        List<String> allResults = new ArrayList<String>();
        for (int i = 0; i < locations.size(); i++) {
            List<String> line = locations.get(i);

            if (line.size() != 3) {
                throw new GenesFinderException("Invalid locations " + line
                        + " at line: " + i);
            }
            String chromozome = line.get(0);
            int start = getStart(line.get(1), tolerance);
            int end = Integer.parseInt(line.get(2)) + tolerance;

            List<String> results = findFeatures(type, chromozome, start, end);
            addToResults(results, allResults);
        }
        printResults(allResults);
    }

    private static int getStart(String s, int tolerance) {
        int ret = Integer.parseInt(s) - tolerance;
        ret = ret < 0 ? 0 : ret;
        return ret;
    }

    private static void addToResults(List<String> results,
            List<String> allResults) {
        for (String result : results) {
            allResults.add(result);
        }
    }

    private static void printResults(List<String> results) {
        for (String result : results) {
            System.out.println(result);
        }
    }

    private static List<String> findFeatures(String type, String chromozome,
            int start, int end) {
        String queryXml = "<query name=\"\" model=\"genomic\" "
                + "view=\"SequenceFeature.primaryIdentifier\" "
                + "sortOrder=\"SequenceFeature.primaryIdentifier asc\" "
                + "constraintLogic=\"A and B and C\">"
                + "<node path=\"SequenceFeature\" type=\"SequenceFeature\">"
                + "</node>"
                + "<node path=\"SequenceFeature.chromosome\" type=\"Chromosome\">"
                + "</node>"
                + "<node path=\"SequenceFeature.chromosome.primaryIdentifier\" "
                + "type=\"String\"> <constraint op=\"=\" value=\""
                + chromozome
                + "\" description=\"\" identifier=\"\" code=\"A\">"
                + "</constraint>"
                + "</node>"
                + "<node path=\"SequenceFeature.chromosomeLocation\" type=\"Location\">"
                + "</node>"
                + "<node path=\"SequenceFeature.chromosomeLocation.start\" type=\"Integer\">"
                + "<constraint op=\"&gt;\" value=\""
                + start
                + "\" description=\"\" identifier=\"\" code=\"B\">"
                + "</constraint>"
                + "</node>"
                + "<node path=\"SequenceFeature.chromosomeLocation.end\" type=\"Integer\">"
                + "<constraint op=\"&lt;\" value=\"" + end
                + "\" description=\"\" identifier=\"\" code=\"C\">"
                + "</constraint>" + "</node>" + "</query>";
        queryXml = queryXml.replaceAll("SequenceFeature", type);
        QueryService service = new ServiceFactory(serviceRootUrl, "GenesFinder")
                .getQueryService();
        int maxCount = 10000;
        List<List<String>> results = service.getResult(queryXml, maxCount);
        if (results.size() == maxCount) {
            throw new GenesFinderException("Too many genes for this location: "
                    + chromozome + " " + start + " " + end);
        }
        List<String> ret = new ArrayList<String>();
        for (List<String> item : results) {
            if (item.size() == 0) {
                throw new GenesFinderException(
                        "One of the returned results is empty.");
            }
            ret.add(item.get(0));
        }
        return ret;
    }

    /**
     * Example general exception.
     *
     */
    static class GenesFinderException extends RuntimeException
    {

        private static final long serialVersionUID = 1L;

        /**
         * Constructor.
         * @param msg error message
         */
        public GenesFinderException(String msg) {
            super(msg);
        }

    }
}
