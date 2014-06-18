package org.intermine.bio.like;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.Coordinates;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;

/**
 * This class contains the main method to pre-calculate the similarity matrices.
 * It calls all methods of the classes in org.intermine.like.prcalcultion and in
 * org.intermine.like.precalculation.utils.
 *
 * result:
 * for each aspect: 1 HashMap containing the similarity rating between genes
 *                & 1 HashMap containing the common items between genes
 *
 * @author selma
 *
 */
public final class Precalculate
{

    private static final Logger LOG = Logger.getLogger(Precalculate.class);

    protected ObjectStoreWriter osw;
    protected static ObjectStore os;

    /**
     * Create a new CalculateLocations object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public Precalculate(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

    /**
     *
     * @param os InterMine object store
     */
    public Precalculate(ObjectStore os) {
        this.os = os;
    }

    /**
     *
     * @throws IOException if the properties can't be read
     * @throws ObjectStoreException if the query build fails
     */
    public static void precalculate() throws IOException, ObjectStoreException {
        long t1 = System.currentTimeMillis();

        Precalculation precalc = new Precalculation(os);

        // read properties
        LOG.info("Loading the properties.\n");
        Map<Coordinates, String> views = Precalculation.getProperties();
        long t2 = System.currentTimeMillis();
        LOG.debug((t2 - t1) + "ms to read the property file" + "\n");

        long t24 = 0;
        Map<Coordinates, Integer> matrix;
        for (int i = 2; i < Integer.parseInt(views.get(new Coordinates(0, 0))) + 2; i++) {
            LOG.info("Run the pre-calculation for aspect " + i + "\n");
            matrix = new HashMap<Coordinates, Integer>();
            long t3 = System.currentTimeMillis();

            LOG.info("Build the query " + i + "\n");
            List<Object> items = precalc.fetchDMelGenes(views, i);
            long t4 = System.currentTimeMillis();
            LOG.debug((t4 - t3) + "ms to build query " + i + "\n");

            if ("category".equals(views.get(new Coordinates(i, 2)))) {
                LOG.info("Its type is `category`.\n");
                t4 = System.currentTimeMillis();

                LOG.info("Run the query and generate a well-ordered matrix.\n");
                matrix = precalc.runQueryCategory(items);
                long t5 = System.currentTimeMillis();
                LOG.debug((t5 - t4) + "ms to run query " + i + "\n");

                LOG.info("Find the common items.\n");
                Matrices.findCommonItems(os, matrix, views.get(new Coordinates(i, 0)));
                long t6 = System.currentTimeMillis();
                LOG.debug((t6 - t5) + "ms to find common items " + i + "\n");
            }

            if ("count".equals(views.get(new Coordinates(i, 2)))) {
                LOG.info("Its type is `count`.\n");
                long t11 = System.currentTimeMillis();

                LOG.info("Run the query and generate a well-ordered matrix.\n");
                matrix = precalc.runQueryCount(items);
                long t12 = System.currentTimeMillis();
                LOG.debug((t12 - t11) + "ms to run query " + i + "\n");

                LOG.info("Find the similarity and normalise.\n");
                Matrices.findSimilarityCount(os, matrix, views.get(new Coordinates(i, 0)));
                long t15 = System.currentTimeMillis();
                LOG.debug((t15 - t12) + "ms to calculate matrix " + i + "\n");
            }

            if ("presence".equals(views.get(new Coordinates(i, 2)))) {

                LOG.info("Its type is `presence`.\n");
                long t18 = System.currentTimeMillis();

                LOG.info("Run the query and generate a well-ordered matrix.\n");
                matrix = precalc.runQueryPresence(items);
                long t19 = System.currentTimeMillis();
                LOG.debug((t19 - t18) + "ms to run query " + i + "\n");

                LOG.info("Find the similarity and normalise.\n");
                Matrices.findSimilarityPresence(os, matrix, views.get(new Coordinates(i, 0)));
                long t22 = System.currentTimeMillis();
                LOG.debug((t22 - t18) + "ms to calculate matrix " + i + "\n");
            }
            t24 = System.currentTimeMillis();
        }
        LOG.info("-> " + (t24 - t1) + "ms to precalculate." + "\n");
    }
}
