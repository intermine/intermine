package org.intermine.bio.like;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 * @author selma
 *
 */
public class LookupSetUpFindSimilarityCount
{
    /**
    *
    * @throws IOException
    * @throws ClassNotFoundException
    */
    @Test
    public void test() throws IOException, ClassNotFoundException {

//        File file1 = new File("matrix" + 2);
//        FileInputStream f1 = new FileInputStream(file1);
//        ObjectInputStream s1 = new ObjectInputStream(f1);
//        Map<Pair,Integer> matrix = (Map<Pair,Integer>)s1.readObject();
//        s1.close();

        // do calculations on server for matrix and bigMatrix

        Map<Coordinates, Integer> smallMatrix = new HashMap<Coordinates, Integer>() { {
                put(new Coordinates(0, 0), 111); put(new Coordinates(0, 1), 200);
                put(new Coordinates(1, 0), 222); put(new Coordinates(1, 1), 100);
                put(new Coordinates(2, 0), 333);
                put(new Coordinates(3, 0), 444); put(new Coordinates(3, 1), 200);
                put(new Coordinates(3, 2), 300);
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
        Map<Coordinates, Integer> res = LookupFindSimilarityCount.findSimilarityCount(smallMatrix);
    //    long t2 = System.currentTimeMillis();
    //    System.out.print((t2 - t1) + "ms for CommonItems1 calculations" + "\n");

        System.out.print("\nres: \n");
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                Integer val = res.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

        Map<Coordinates, Integer> smallResult = new HashMap<Coordinates, Integer>() { {
                put(new Coordinates(0, 1), 111);
                put(new Coordinates(0, 2), 222);
                put(new Coordinates(0, 3), 333);
                put(new Coordinates(0, 4), 444);
                put(new Coordinates(1, 0), 111);
                put(new Coordinates(2, 0), 222);
                put(new Coordinates(3, 0), 333);
                put(new Coordinates(4, 0), 444);
                put(new Coordinates(1, 1), 100);
                put(new Coordinates(1, 2), 100);
                put(new Coordinates(1, 4), 100);
                put(new Coordinates(2, 1), 100);
                put(new Coordinates(2, 2), 100);
                put(new Coordinates(2, 4), 100);
                put(new Coordinates(4, 1), 50);
                put(new Coordinates(4, 2), 50);
                put(new Coordinates(4, 4), 100);
            }
        };

        System.out.print("\nsmallResult: \n");
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                Integer val = smallResult.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

//        File file = new File("simMat" + 2);
//        FileInputStream f = new FileInputStream(file);
//        ObjectInputStream s = new ObjectInputStream(f);
//        Map<Pair,Integer> result = (Map<Pair,Integer>)s.readObject();
//        s.close();

        assertEquals(res, smallResult);
    }
}
