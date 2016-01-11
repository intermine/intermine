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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import static org.intermine.webservice.client.core.ContentType.TEXT_PLAIN;
import org.intermine.webservice.client.core.Request;
import static org.intermine.webservice.client.core.Request.RequestType.GET;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONObject;

/**
 * The GenesFinder is an example of using a specific web-service, in this case the Genomic
 * Region search service (See <a href="http://www.intermine.org/wiki/WebService">The Web-Service Listing</a>).
 *
 *
 * This service finds features located in specific genomic regions and provides results
 * in standard biological formats. This program allows you to specify the format of the results,
 * as well as the regions and types of features you are hoping to find.
 *
 * @author Alex Kalderimis
 * @author Jakub Kulaviak
 **/
public class GenesFinder
{
    private static final String PROG_NAME = "GenesFinder-v2.0";
    private static final String ROOT_URL = "http://www.flymine.org/query/service";
    private static final String NL = System.getProperty("line.separator");
    private static final String USAGE =
            "GenesFinder: Use the InterMine Region Search to find Features" + NL
            + "-----------------------------------" + NL
            + "Required parameters: --file input_file" + NL
            + "Optional parameters:" + NL
            + "   --tolerance  number, eg: 1000 (base-pairs)" + NL
            + "   --type       class, eg: Gene (repeatable)" + NL
            + "   --organism   name, eg: D. melanogaster" + NL
            + "   --format     [gff3, fasta, bed] (default = gff3)" + NL
            + NL
            + "To see an example, run with the flag: --sample-data";

    /**
     * @param args command line arguments
     */
    public static void main(String[] args)  {
        try {
            Input input = new Input(args);
            printResult(input);
        } catch (GenesFinderException e) {
            bail(e);
        }
    }

    private static void printResult(Input input) {
        String resource = "/regions/" + input.getFormat();
        Service service = new ServiceFactory(ROOT_URL).getService(resource, PROG_NAME);
        Request request = new RequestImpl(GET, service.getUrl(), TEXT_PLAIN);

        Map<String, Object> query = new HashMap<String, Object>();

        query.put("organism",     input.getOrganism());
        query.put("regions",      input.getRegions());
        query.put("featureTypes", input.getTypes());
        query.put("extension",    input.getTolerance());

        request.setParameter("query", new JSONObject(query).toString());
        HttpConnection con = service.executeRequest(request);
        try {
            IOUtils.copy(con.getResponseBodyAsStream(), System.out);
        } catch (IOException e) {
            throw new GenesFinderException("Error reading from connection");
        } finally {
            con.close();
        }
    }

    private static void bail(Exception e) {
        System.out.println("[ERROR] " + e.getMessage());
        System.out.println();
        System.out.println(USAGE);
        System.exit(1);
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

    /**
     * Example Input class
     *
     * The purpose of this class is to encapsulate the arguments in a tidy
     * object with a meaningful interface.
     * @author Alex Kalderimis
     *
     */
    private static class Input {

        private enum Format { GFF3, FASTA, BED };

        private static final String DEFAULT_ORGANISM = "D. melanogaster";
        private static final Collection<? extends String> SAMPLE_REGIONS
            = new HashSet<String>(
                    Arrays.asList(
                            "2L:14615455..14619002",
                            "2R:5866646..5868384",
                            "3R:2578486..2580016"));

        // Fields
        private final String organism;
        private final List<String> regions = new ArrayList<String>();
        private final Set<String> types = new HashSet<String>();
        private final int tolerance;
        private final Format format;

        /**
         * Parse the input from the command line arguments into
         * a typed and named bundle of configuration options.
         *
         * @param args The command-line arguments.
         */
        public Input(String[] args) {

            // ORGANISM
            String org = getParameter("--organism", args);
            if (org != null) {
                organism = org;
            } else {
                organism = DEFAULT_ORGANISM;
            }

            // REGIONS
            if (getFlag("--sample-data", args)) {
                regions.addAll(SAMPLE_REGIONS);
            } else {
                String fileName = getParameter("--file", args);
                if (fileName == null) {
                    throw new GenesFinderException("no identifier file specified");
                }
                regions.addAll(readLocations(fileName));
            }

            // TYPES
            types.addAll(getParameters("--type", args));
            if (types.isEmpty())
                types.add("SequenceFeature");

            // TOLERANCE
            String toleranceVal = getParameter("--tolerance", args);
            if (toleranceVal != null) {
                tolerance = Integer.parseInt(toleranceVal);
            } else {
                tolerance = 0;
            }

            // FORMAT
            String formatVal = getParameter("--format", args);
            if (formatVal == null) {
                format = Format.GFF3;
            } else {
                try {
                    format = Enum.valueOf(Format.class, formatVal.toUpperCase());
                } catch (Exception e) {
                    throw new GenesFinderException("Unknown format: " + formatVal);
                }
            }
        }


        public String getOrganism() {
            return organism;
        }

        public List<String> getRegions() {
            return regions;
        }

        public Set<String> getTypes() {
            return types;
        }

        public int getTolerance() {
            return tolerance;
        }

        public String getFormat() {
            return format.toString().toLowerCase();
        }

        private static boolean getFlag(String name, String[] args) {
            return new HashSet<String>(Arrays.asList(args)).contains(name);
        }

        private static String getParameter(String name, String[] args) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals(name) && (i + 1) <= (args.length - 1)) {
                    return args[i + 1];
                }
            }
            return null;
        }

        private static List<String> getParameters(String param, String[] args) {
            List<String> ret = new ArrayList<String>();
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals(param) && (i + 1) <= (args.length - 1)) {
                    ret.add(args[i + 1]);
                    i++; // We can skip two here.
                }
            }
            return ret;
        }

        private static Set<String> readLocations(String fileName) {
            Set<String> ret = new HashSet<String>();
            Reader r = null;
            try {
                r = new FileReader(fileName);
                ret.addAll(IOUtils.readLines(r));
            } catch (FileNotFoundException e) {
                throw new GenesFinderException("File not found: " + fileName);
            } catch (IOException ex) {
                throw new GenesFinderException("Error reading from file " + fileName);
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            return ret;
        }
    }
}
