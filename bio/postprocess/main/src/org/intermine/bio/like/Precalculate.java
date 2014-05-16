package org.intermine.bio.like;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.log4j.Logger;
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

//    private static final LOGger LOG = LOGger.getLOGger(Precalculate.class);

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
        Map<Coordinates, String> views = Precalculation.getProperties();
        long t2 = System.currentTimeMillis();
        System.out.print((t2 - t1) + "ms to read the property file" + "\n");

        long t24 = 0;
        for (int i = 0; i < views.size() / 4; i++) {
            Map<Coordinates, Integer> matrix = new HashMap<Coordinates, Integer>();
            Map<Coordinates, ArrayList<Integer>> commonMat =
                    new HashMap<Coordinates, ArrayList<Integer>>();
            Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();
            Map<Coordinates, Integer> normMat = new HashMap<Coordinates, Integer>();
            long t3 = System.currentTimeMillis();

            // Build query i
            List<Object> items = precalc.fetchDMelGenes(views, i);
            long t4 = System.currentTimeMillis();
            System.out.print((t4 - t3) + "ms to build query " + i + "\n");

            if ("category".equals(views.get(new Coordinates(i, 3)))) {
                t4 = System.currentTimeMillis();

                // Generate matrix i out of the query
                matrix = precalc.runQueryCategory(items);
                long t5 = System.currentTimeMillis();
                System.out.print((t5 - t4) + "ms to run query " + i + "\n");

//                System.out.print("\nmatrix:\n");
//                for (int j = 0; j < 40; j++) {
//                    for (int k = 0; k < 40; k++) {
//                        System.out.print(matrix.get(new Coordinates(j, k)) + " ");
//                    }
//                    System.out.print("\n");
//                }

//                File queryOut = new File("matrix" + i);
//                FileOutputStream f = new FileOutputStream(queryOut);
//                ObjectOutputStream s = new ObjectOutputStream(f);
//                s.writeObject(matrix);
//                s.close();

                // Calculate common items i
                commonMat = Matrices.findCommonItems(matrix);
                long t6 = System.currentTimeMillis();
                System.out.print((t6 - t5) + "ms to find common items " + i + "\n");
                matrix = new HashMap<Coordinates, Integer>();

                File commonItems = new File("build/CommonItems" + i);
                FileOutputStream f1 = new FileOutputStream(commonItems);
                ObjectOutputStream s1 = new ObjectOutputStream(f1);
                s1.writeObject(commonMat);
                s1.close();
                long t7 = System.currentTimeMillis();
                System.out.print((t7 - t6) + "ms to store common items " + i + "\n");

                // Calculate number of common items i
                simMat = Matrices.countCommonItemsCategory(commonMat);
                long t8 = System.currentTimeMillis();
                System.out.print((t8 - t7) + "ms to calculate matrix " + i + "\n");
                commonMat = new HashMap<Coordinates, ArrayList<Integer>>();

//                File simMats = new File("simMat" + i);
//                FileOutputStream f3 = new FileOutputStream(simMats);
//                ObjectOutputStream s3 = new ObjectOutputStream(f3);
//                s3.writeObject(simMat);
//                s3.close();

                // Normalise similarity matrix i
                normMat = Matrices.normalise(simMat);
                long t9 = System.currentTimeMillis();
                System.out.print((t9 - t8) + "ms to normalise matrix " + i + "\n");
                simMat = new HashMap<Coordinates, Integer>();

                System.out.print("\nnormMat:\n");
                for (int j = 0; j < 40; j++) {
                    for (int k = 0; k < 40; k++) {
                        System.out.print(normMat.get(new Coordinates(j, k)) + " ");
                    }
                    System.out.print("\n");
                }

                File similarityMatrix = new File("build/SimilarityMatrix" + i);
                FileOutputStream f2 = new FileOutputStream(similarityMatrix);
                ObjectOutputStream s2 = new ObjectOutputStream(f2);
                s2.writeObject(normMat);
                s2.close();
                long t10 = System.currentTimeMillis();
                System.out.print((t10 - t9) + "ms to store similarity matrix " + i + "\n");
                normMat = new HashMap<Coordinates, Integer>();
            }

            if ("count".equals(views.get(new Coordinates(i, 3)))) {
                long t11 = System.currentTimeMillis();

                // Generate matrix i out of the query
                matrix = precalc.runQueryCount(items);
                long t12 = System.currentTimeMillis();
                System.out.print((t12 - t11) + "ms to run query " + i + "\n");

//                File queryOut = new File("matrix" + i);
//                FileOutputStream f = new FileOutputStream(queryOut);
//                ObjectOutputStream s = new ObjectOutputStream(f);
//                s.writeObject(matrix);
//                s.close();

                // Calculate common items i
                commonMat = Matrices.findCommonItems(matrix);
                long t13 = System.currentTimeMillis();
                System.out.print((t13 - t12) + "ms to find common items " + i + "\n");

                File commonItems = new File("build/CommonItems" + i);
                FileOutputStream f1 = new FileOutputStream(commonItems);
                ObjectOutputStream s1 = new ObjectOutputStream(f1);
                s1.writeObject(commonMat);
                s1.close();
                long t14 = System.currentTimeMillis();
                System.out.print((t14 - t13) + "ms to store common items " + i + "\n");
                commonMat = new HashMap<Coordinates, ArrayList<Integer>>();

                // Calculate differences in number matrix i
                simMat = Matrices.findSimilarityCount(matrix);
                long t15 = System.currentTimeMillis();
                System.out.print((t15 - t14) + "ms to calculate matrix " + i + "\n");
                matrix = new HashMap<Coordinates, Integer>();

//                File simMats = new File("simMat" + i);
//                FileOutputStream f3 = new FileOutputStream(simMats);
//                ObjectOutputStream s3 = new ObjectOutputStream(f3);
//                s3.writeObject(simMat);
//                s3.close();

                System.out.print("\nsimMat:\n");
                for (int j = 0; j < 40; j++) {
                    for (int k = 0; k < 40; k++) {
                        System.out.print(simMat.get(new Coordinates(j, k)) + " ");
                    }
                    System.out.print("\n");
                }

                File similarityMatrix = new File("build/SimilarityMatrix" + i);
                FileOutputStream f2 = new FileOutputStream(similarityMatrix);
                ObjectOutputStream s2 = new ObjectOutputStream(f2);
                s2.writeObject(simMat);
                s2.close();
                long t17 = System.currentTimeMillis();
                System.out.print((t17 - t15) + "ms to store similarity matrix " + i + "\n");
                simMat = new HashMap<Coordinates, Integer>();
            }

            if ("presence".equals(views.get(new Coordinates(i, 3)))) {
                long t18 = System.currentTimeMillis();

                // Generate matrix i out of the query
                matrix = precalc.runQueryPresence(items);
                long t19 = System.currentTimeMillis();
                System.out.print((t19 - t18) + "ms to run query " + i + "\n");

//                File queryOut = new File("matrix" + i);
//                FileOutputStream f = new FileOutputStream(queryOut);
//                ObjectOutputStream s = new ObjectOutputStream(f);
//                s.writeObject(matrix);
//                s.close();

                // Calculate common items i
                commonMat = Matrices.findCommonItemsPresence(matrix);
                long t20 = System.currentTimeMillis();
                System.out.print((t20 - t19) + "ms to find common items " + i + "\n");

                File commonItems = new File("build/CommonItems" + views.get(new Coordinates(i, 0)));
                FileOutputStream f1 = new FileOutputStream(commonItems);
                ObjectOutputStream s1 = new ObjectOutputStream(f1);
                s1.writeObject(commonMat);
                s1.close();
                long t21 = System.currentTimeMillis();
                System.out.print((t21 - t20) + "ms to store common items " + i + "\n");
                commonMat = new HashMap<Coordinates, ArrayList<Integer>>();

                // Calculate common items i
                simMat = Matrices.findSimilarityPresence(matrix);
                long t22 = System.currentTimeMillis();
                System.out.print((t22 - t21) + "ms to calculate matrix " + i + "\n");
                matrix = new HashMap<Coordinates, Integer>();

//                File simMats = new File("simMat" + i);
//                FileOutputStream f3 = new FileOutputStream(simMats);
//                ObjectOutputStream s3 = new ObjectOutputStream(f3);
//                s3.writeObject(simMat);
//                s3.close();

                System.out.print("\nsimMat:\n");
                for (int j = 0; j < 40; j++) {
                    for (int k = 0; k < 40; k++) {
                        System.out.print(simMat.get(new Coordinates(j, k)) + " ");
                    }
                    System.out.print("\n");
                }

                File similarityMatrix =
                        new File("build/SimilarityMatrix" + views.get(new Coordinates(i, 0)));
                FileOutputStream f2 = new FileOutputStream(similarityMatrix);
                ObjectOutputStream s2 = new ObjectOutputStream(f2);
                s2.writeObject(simMat);
                s2.close();
                long t23 = System.currentTimeMillis();
                System.out.print((t23 - t22) + "ms to store similarity matrix " + i + "\n");
                simMat = new HashMap<Coordinates, Integer>();
            }
            t24 = System.currentTimeMillis();
        }
        System.out.print("-> " + (t24 - t1) + "ms to precalculate" + "\n");
    }
}
