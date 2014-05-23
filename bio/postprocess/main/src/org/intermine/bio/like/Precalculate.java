package org.intermine.bio.like;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.Coordinates;
import org.intermine.objectstore.ObjectStore;
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

//    public static void main(String[] args) throws Exception {
//        precalculate();
//    }

    /**
     * Create a new CalculateLocations object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public Precalculate(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

    /**
     * @throws Exception
     */
    public static void precalculate() throws Exception {
        long t1 = System.currentTimeMillis();

        Precalculation precalc = new Precalculation(os);

        // read properties
        LOG.info("Loading the properties.\n");
        Map<Coordinates, String> views = Precalculation.getProperties();
        long t2 = System.currentTimeMillis();
        LOG.debug((t2 - t1) + "ms to read the property file" + "\n");

        long t24 = 0;
        for (int i = 0; i < views.size() / 4; i++) {
            LOG.info("Run the pre-calculation for aspect " + i + "\n");
            Map<Coordinates, Integer> matrix = new HashMap<Coordinates, Integer>();
            Map<Coordinates, ArrayList<Integer>> commonMat =
                    new HashMap<Coordinates, ArrayList<Integer>>();
            Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();
            Map<Coordinates, Integer> normMat = new HashMap<Coordinates, Integer>();
            long t3 = System.currentTimeMillis();

            LOG.info("Build the query " + i + "\n");
            List<Object> items = precalc.fetchDMelGenes(views, i);
            long t4 = System.currentTimeMillis();
            LOG.debug((t4 - t3) + "ms to build query " + i + "\n");

            if ("category".equals(views.get(new Coordinates(i, 3)))) {
                LOG.info("Its type is `category`.\n");
                t4 = System.currentTimeMillis();

                LOG.info("Run the query and generate a well-ordered matrix.\n");
                matrix = precalc.runQueryCategory(items);
                long t5 = System.currentTimeMillis();
                LOG.debug((t5 - t4) + "ms to run query " + i + "\n");

                LOG.info("Find the common items.\n");
                commonMat = Matrices.findCommonItems(matrix);
                long t6 = System.currentTimeMillis();
                LOG.debug((t6 - t5) + "ms to find common items " + i + "\n");
                matrix = new HashMap<Coordinates, Integer>();

                LOG.info("Storing of common items matrix " + i + " to the db.\n");
                Storing.saveCommonMatToDatabase(os, commonMat, views.get(new Coordinates(i, 0)));

                long t7 = System.currentTimeMillis();
                LOG.debug((t7 - t6) + "ms to store common items " + i + "\n");

                LOG.info("Calculate the number of common items.\n");
                simMat = Matrices.countCommonItemsCategory(commonMat);
                long t8 = System.currentTimeMillis();
                LOG.debug((t8 - t7) + "ms to calculate matrix " + i + "\n");
                commonMat = new HashMap<Coordinates, ArrayList<Integer>>();

                LOG.info("Normalise the number of common items.\n");
                normMat = Matrices.normalise(simMat);
                long t9 = System.currentTimeMillis();
                LOG.debug((t9 - t8) + "ms to normalise matrix " + i + "\n");
                simMat = new HashMap<Coordinates, Integer>();

//                System.out.print("\nnormMat:\n");
//                for (int j = 0; j < 30; j++) {
//                    for (int k = 0; k < 30; k++) {
//                        System.out.print(normMat.get(new Coordinates(j, k)) + " ");
//                    }
//                    System.out.print("\n");
//                }

                LOG.info("Storing of the normalised similarity matrix " + i + " to the db.\n");
                Storing.saveNormMatToDatabase(os, normMat, views.get(new Coordinates(i, 0)));

                long t10 = System.currentTimeMillis();
                LOG.debug((t10 - t9) + "ms to store similarity matrix " + i + "\n");
                normMat = new HashMap<Coordinates, Integer>();
            }

            if ("count".equals(views.get(new Coordinates(i, 3)))) {
                LOG.info("Its type is `count`.\n");
                long t11 = System.currentTimeMillis();

                LOG.info("Run the query and generate a well-ordered matrix.\n");
                matrix = precalc.runQueryCount(items);
                long t12 = System.currentTimeMillis();
                LOG.debug((t12 - t11) + "ms to run query " + i + "\n");

                LOG.info("Find the common items.\n");
                commonMat = Matrices.findCommonItems(matrix);
                long t13 = System.currentTimeMillis();
                LOG.debug((t13 - t12) + "ms to find common items " + i + "\n");

                LOG.info("Storing of common items matrix " + i + " to the db.\n");
                Storing.saveCommonMatToDatabase(os, commonMat, views.get(new Coordinates(i, 0)));

                long t14 = System.currentTimeMillis();
                LOG.debug((t14 - t13) + "ms to store common items " + i + "\n");
                commonMat = new HashMap<Coordinates, ArrayList<Integer>>();

                LOG.info("Find the similarity and normalise.\n");
                simMat = Matrices.findSimilarityCount(matrix);
                long t15 = System.currentTimeMillis();
                LOG.debug((t15 - t14) + "ms to calculate matrix " + i + "\n");
                matrix = new HashMap<Coordinates, Integer>();

//                System.out.print("\nsimMat:\n");
//                for (int j = 0; j < 30; j++) {
//                    for (int k = 0; k < 30; k++) {
//                        System.out.print(simMat.get(new Coordinates(j, k)) + " ");
//                    }
//                    System.out.print("\n");
//                }

                LOG.info("Storing of the normalised similarity matrix " + i + " to the db.\n");
                Storing.saveNormMatToDatabase(os, normMat, views.get(new Coordinates(i, 0)));

                long t17 = System.currentTimeMillis();
                LOG.debug((t17 - t15) + "ms to store similarity matrix " + i + "\n");
                simMat = new HashMap<Coordinates, Integer>();
            }

            if ("presence".equals(views.get(new Coordinates(i, 3)))) {

                LOG.info("Its type is `presence`.\n");
                long t18 = System.currentTimeMillis();

                LOG.info("Run the query and generate a well-ordered matrix.\n");
                matrix = precalc.runQueryPresence(items);
                long t19 = System.currentTimeMillis();
                LOG.debug((t19 - t18) + "ms to run query " + i + "\n");

                LOG.info("Find the common items.\n");
                commonMat = Matrices.findCommonItemsPresence(matrix);
                long t20 = System.currentTimeMillis();
                LOG.debug((t20 - t19) + "ms to find common items " + i + "\n");

                LOG.info("Storing of common items matrix " + i + " to the db.\n");
                Storing.saveCommonMatToDatabase(os, commonMat, views.get(new Coordinates(i, 0)));

                long t21 = System.currentTimeMillis();
                LOG.debug((t21 - t20) + "ms to store common items " + i + "\n");
                commonMat = new HashMap<Coordinates, ArrayList<Integer>>();

                LOG.info("Find the similarity and normalise.\n");
                simMat = Matrices.findSimilarityPresence(matrix);
                long t22 = System.currentTimeMillis();
                LOG.debug((t22 - t21) + "ms to calculate matrix " + i + "\n");
                matrix = new HashMap<Coordinates, Integer>();

//                System.out.print("\nsimMat:\n");
//                for (int j = 0; j < 30; j++) {
//                    for (int k = 0; k < 30; k++) {
//                        System.out.print(simMat.get(new Coordinates(j, k)) + " ");
//                    }
//                    System.out.print("\n");
//                }

                LOG.info("Storing of the normalised similarity matrix " + i + " to the db.\n");
                Storing.saveNormMatToDatabase(os, normMat, views.get(new Coordinates(i, 0)));

                long t23 = System.currentTimeMillis();
                LOG.debug((t23 - t22) + "ms to store similarity matrix " + i + "\n");
                simMat = new HashMap<Coordinates, Integer>();
            }
            t24 = System.currentTimeMillis();
        }
        LOG.info("-> " + (t24 - t1) + "ms to precalculate." + "\n");
    }
}
