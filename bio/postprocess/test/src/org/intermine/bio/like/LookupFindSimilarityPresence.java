package org.intermine.bio.like;

import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;
import org.intermine.objectstore.ObjectStore;

/**
 *
 * @author selma
 *
 */
public final class LookupFindSimilarityPresence
{

    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;
    // For rectangular matrices the gene ID is also in row zero
    private static final int SUBJECT_ID_ROW = 0;
    private static final int MAX_RATING = 100;

    private LookupFindSimilarityPresence() {
        //Don't.
    }

    public static Map<Coordinates, Integer> findSimilarityPresence(Map<Coordinates, Integer> matrix) {
        Map<Coordinates, Integer> hasMat = new HashMap<Coordinates, Integer>();
        Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();

        for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (yCoordinate == SUBJECT_ID_COLUMN) {
                hasMat.put(entry.getKey(), entry.getValue());
                if (matrix.get(new Coordinates(xCoordinate, 1)) == null) {
                    hasMat.put(new Coordinates(xCoordinate, 1), 0);
                }
                else {
                    hasMat.put(new Coordinates(xCoordinate, 1), 1);
                }
            }
        }

        for (Map.Entry<Coordinates, Integer> entry : hasMat.entrySet()) {
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (yCoordinate == 0) {
//                simMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate + 1),
//                        hasMat.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                simMat.put(new Coordinates(xCoordinate + 1, SUBJECT_ID_COLUMN),
                        hasMat.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
            }
            else {
                for (Map.Entry<Coordinates, Integer> inner : hasMat.entrySet()) {
                    int xCoordinateInner = inner.getKey().getKey();
                    int yCoordinateInner = inner.getKey().getValue();
//                    if (inner.getKey().getValue() == 1) {
                        if (inner.getValue().equals(entry.getValue())) {
                            simMat.put(new Coordinates(xCoordinate + 1,
                                    inner.getKey().getKey() + 1), MAX_RATING);
                        }
//                    }
                }

//                if (hasMat.get(new Coordinates(xCoordinate, 0)) == 1112303) {
//                    System.out.print("\nnormMat:\n");
//                    for (int j = 0; j < 30; j++) {
//                        for (int k = 0; k < 30; k++) {
//                            System.out.print(simMat.get(new Coordinates(j, k)) + " ");
//                        }
//                        System.out.print("\n");
//                    }
//                }

                String geneId = Integer.toString(hasMat.get(new Coordinates(xCoordinate, 0)));
//                Storing.saveNormMatToDatabase(os, simMat, aspectNumber, geneId);
//                simMat = new HashMap<Coordinates, Integer>();
            }
        }
        return simMat;
    }
}
