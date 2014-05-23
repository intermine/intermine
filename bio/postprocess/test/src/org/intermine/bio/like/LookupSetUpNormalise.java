package org.intermine.bio.like;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;
import org.junit.Test;

/**
 *
 * @author selma
 *
 */
public class LookupSetUpNormalise
{
    /**
    *
    * @throws IOException
    * @throws ClassNotFoundException
    */
    @Test
    public void test() throws IOException, ClassNotFoundException {

//        File file1 = new File("simMat" + 0);
//        FileInputStream f1 = new FileInputStream(file1);
//        ObjectInputStream s1 = new ObjectInputStream(f1);
//        Map<Coordinates, Integer> matrix = (HashMap<Coordinates, Integer>) s1.readObject();
//        s1.close();

        Map<Coordinates, Integer> smallMatrix = new HashMap<Coordinates, Integer>() { {
                put(new Coordinates(0, 1), 111);
                put(new Coordinates(0, 2), 222);
                put(new Coordinates(0, 3), 333);
                put(new Coordinates(0, 4), 444);
                put(new Coordinates(1, 0), 111);
                put(new Coordinates(2, 0), 222);
                put(new Coordinates(3, 0), 333);
                put(new Coordinates(4, 0), 444);
                put(new Coordinates(1, 1), 2);
                put(new Coordinates(1, 2), 1);
                put(new Coordinates(1, 4), 1);
                put(new Coordinates(2, 1), 1);
                put(new Coordinates(2, 2), 1);
                put(new Coordinates(3, 3), 1);
                put(new Coordinates(4, 1), 1);
                put(new Coordinates(4, 4), 3);
            }
        };

        System.out.print("\nsmallMatrix: \n");
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                Integer val = smallMatrix.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

    //    long t1 = System.currentTimeMillis();
        Map<Coordinates, Integer> res = LookupNormalise.normalise(smallMatrix);
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
                put(new Coordinates(1, 2), 50);
                put(new Coordinates(1, 4), 50);
                put(new Coordinates(2, 1), 100);
                put(new Coordinates(2, 2), 100);
                put(new Coordinates(3, 3), 100);
                put(new Coordinates(4, 1), 33);
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

//        File file = new File("SimilarityMatrix" + 0);
//        FileInputStream f = new FileInputStream(file);
//        ObjectInputStream s = new ObjectInputStream(f);
//        Map<Coordinates, Integer> result = (Map<Coordinates, Integer>) s.readObject();
//        s.close();

        assertEquals(res, smallResult);
    }
}
