package org.intermine.bio.like;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;
import org.intermine.objectstore.ObjectStore;
import org.junit.Test;

/**
 *
 * @author selma
 *
 */
public class LookupFindSimilarityCount
{

    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;
    // For rectangular matrices the gene ID is also in row zero
    private static final int MAX_RATING = 100;

    private LookupFindSimilarityCount() {
        // Don't.
    }

    public static Map<Coordinates, Integer> findSimilarityCount(Map<Coordinates, Integer> matrix) {
        Map<Coordinates, Integer> countedItems = new HashMap<Coordinates, Integer>();
        Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();
        int xCoordinate;
        int yCoordinate;
        int count;
        int rating;
        int xCoordinateInner;
        int yCoordinateInner;

        // Count the items (e.g. pathways) for each gene
        for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
            xCoordinate = entry.getKey().getKey();
            yCoordinate = entry.getKey().getValue();
            if (yCoordinate == SUBJECT_ID_COLUMN) {
                countedItems.put(entry.getKey(), entry.getValue());
                count = 0;
                for (Map.Entry<Coordinates, Integer> entry2 : matrix.entrySet()) {
                    if (entry.getKey().getKey() == entry2.getKey().getKey()) {
                        count += 1;
                    }
                }
                // "count - 1" because the gene ID is not part of the items (e.g. pathways)
                countedItems.put(new Coordinates(xCoordinate, 1), count - 1);
            }
        }

        // Build a rectangular matrix
        for (Map.Entry<Coordinates, Integer> outer : countedItems.entrySet()) {
            xCoordinate = outer.getKey().getKey();
            yCoordinate = outer.getKey().getValue();
            // Transfer the gene IDs
            if (yCoordinate == SUBJECT_ID_COLUMN) {
//                simMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate + 1),
//                        countedItems.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                simMat.put(new Coordinates(xCoordinate + 1, SUBJECT_ID_COLUMN),
                        countedItems.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
            }
            else { // If outer contains counted item
                for (Map.Entry<Coordinates, Integer> inner : countedItems.entrySet()) {
                    xCoordinateInner = inner.getKey().getKey();
                    yCoordinateInner = inner.getKey().getValue();
                    // Only transfer non-zero items -> makes the simMat more sparse
                    if (yCoordinateInner == 1 && outer.getValue() != 0 && inner.getValue() != 0) {
                        // Row-wise normalisation
                        rating = Math.abs(MAX_RATING * inner.getValue()) / outer.getValue();
                        if (rating > MAX_RATING) {
                            rating = MAX_RATING;
                        }
                        simMat.put(new Coordinates(xCoordinate + 1,
                                xCoordinateInner + 1), rating);

                    }
                }
//                if (countedItems.get(new Coordinates(xCoordinate, 0)) == 1112303) {
//                    System.out.print("\nnormMat:\n");
//                    for (int j = 0; j < 30; j++) {
//                        for (int k = 0; k < 30; k++) {
//                            System.out.print(simMat.get(new Coordinates(j, k)) + " ");
//                        }
//                        System.out.print("\n");
//                    }
//                }

                String geneId = Integer.toString(countedItems.get(new Coordinates(xCoordinate, 0)));
//                Storing.saveNormMatToDatabase(os, simMat, aspectNumber, geneId);
//                simMat = new HashMap<Coordinates, Integer>();
            }
        }
        return simMat;
    }
}
