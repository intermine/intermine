package org.intermine.bio.like;

import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;
import org.intermine.bio.like.LookupFindCommonItems;
import org.junit.Test;

/**
 *
 * @author selma
 *
 */
public class LookupSetUpFindCommonItems
{

    /**
    *
    * @throws IOException
    * @throws ClassNotFoundException
    */
    @Test
    public void test() throws IOException, ClassNotFoundException {

//        File file1 = new File("matrix" + 0);
//        FileInputStream f1 = new FileInputStream(file1);
//        ObjectInputStream s1 = new ObjectInputStream(f1);
//        Map<Coordinates, Integer> matrix = (Map<Coordinates, Integer>) s1.readObject();
//        s1.close();

        // do calculations on server for bigMatrix

        Map<Coordinates, Integer> smallMatrix = new HashMap<Coordinates, Integer>() { {
                put(new Coordinates(0, 0), 111);
                put(new Coordinates(0, 1), 100);
                put(new Coordinates(0, 2), 200);
                put(new Coordinates(1, 0), 222);
                put(new Coordinates(1, 1), 100);
                put(new Coordinates(2, 0), 333);
                put(new Coordinates(2, 3), 300);
                put(new Coordinates(3, 0), 444);
                put(new Coordinates(3, 2), 200);
            }
        };

        System.out.print("\nsmallMatrix: \n");
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 4; j++) {
                Integer val = smallMatrix.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

    //    long t1 = System.currentTimeMillis();
        Map<Coordinates, ArrayList<Integer>> res =
                LookupFindCommonItems.findCommonItems(smallMatrix);
    //    long t2 = System.currentTimeMillis();
    //    System.out.print((t2 - t1) + "ms for CommonItems1 calculations" + "\n");

        System.out.print("\nres: \n");
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                ArrayList<Integer> val = res.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

        Map<Coordinates, ArrayList<Integer>> smallResult =
                new HashMap<Coordinates, ArrayList<Integer>>() { {
                put(new Coordinates(0, 1), new ArrayList<Integer>() { { add(111); } });
                put(new Coordinates(0, 2), new ArrayList<Integer>() { { add(222); } });
                put(new Coordinates(0, 3), new ArrayList<Integer>() { { add(333); } });
                put(new Coordinates(0, 4), new ArrayList<Integer>() { { add(444); } });
                put(new Coordinates(1, 0), new ArrayList<Integer>() { { add(111); } });
                put(new Coordinates(2, 0), new ArrayList<Integer>() { { add(222); } });
                put(new Coordinates(3, 0), new ArrayList<Integer>() { { add(333); } });
                put(new Coordinates(4, 0), new ArrayList<Integer>() { { add(444); } });
                put(new Coordinates(1, 1), new ArrayList<Integer>() { { add(200);
                add(100); } });
                put(new Coordinates(1, 2), new ArrayList<Integer>() { { add(100); } });
                put(new Coordinates(1, 4), new ArrayList<Integer>() { { add(200); } });
                put(new Coordinates(2, 1), new ArrayList<Integer>() { { add(100); } });
                put(new Coordinates(2, 2), new ArrayList<Integer>() { { add(100); } });
                put(new Coordinates(3, 3), new ArrayList<Integer>() { { add(300); } });
                put(new Coordinates(4, 1), new ArrayList<Integer>() { { add(200); } });
                put(new Coordinates(4, 4), new ArrayList<Integer>() { { add(200); } });
            }
        };

        System.out.print("\nsmallResult: \n");
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                ArrayList<Integer> val = smallResult.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

//        File file = new File("CommonItems" + 0);
//        FileInputStream f = new FileInputStream(file);
//        ObjectInputStream s = new ObjectInputStream(f);
//        Map<Coordinates, ArrayList<Integer>> result = (Map<Coordinates, ArrayList<Integer>>)
//                s.readObject();
//        s.close();

        assertEquals(res, smallResult);


////////////////////////// Presence //////////////////////////////
//        File file11 = new File("matrix" + 3);
//        FileInputStream f11 = new FileInputStream(file11);
//        ObjectInputStream s11 = new ObjectInputStream(f11);
//        Map<Coordinates, Integer> matrix1 = (Map<Coordinates, Integer>) s11.readObject();
//        s11.close();
//
////        System.out.print("\nmatrix1: \n");
////        for (int k = 0; k < 48; k++) {
////            for (int j = 0; j < 48; j++) {
////                Integer val = matrix1.get(new Coordinates(k, j));
////                System.out.print(val + " ");
////            }
////            System.out.print("\n");
////        }
//
//    //    long t1 = System.currentTimeMillis();
//        Map<Coordinates, ArrayList<Integer>> res1 =
//        LookupFindCommonItems.findCommonItemsPresence(matrix1);
//    //    long t2 = System.currentTimeMillis();
//    //    System.out.print((t2 - t1) + "ms for CommonItems1 calculations" + "\n");
//
//        System.out.print("\nres1: \n");
//        for (int k = 0; k < 48; k++) {
//            for (int j = 0; j < 48; j++) {
//                ArrayList<Integer> val = res1.get(new Coordinates(k, j));
//                System.out.print(val + " ");
//            }
//            System.out.print("\n");
//        }
//
//        File file0 = new File("CommonItems" + 3);
//        FileInputStream f0 = new FileInputStream(file0);
//        ObjectInputStream s0 = new ObjectInputStream(f0);
//        Map<Coordinates, ArrayList<Integer>> result1 = (Map<Coordinates, ArrayList<Integer>>)
//        s0.readObject();
//        s0.close();
//
//        System.out.print("\nresult1: \n");
//        for (int k = 0; k < 48; k++) {
//            for (int j = 0; j < 48; j++) {
//                ArrayList<Integer> val = result1.get(new Coordinates(k, j));
//                System.out.print(val + " ");
//            }
//            System.out.print("\n");
//        }
//
//        assertEquals(res1, result1);
    }
}
