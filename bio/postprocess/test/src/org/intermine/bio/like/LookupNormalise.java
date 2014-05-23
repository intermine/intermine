package org.intermine.bio.like;

import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;

/**
 *
 * @author selma
 *
 */
public class LookupNormalise {

    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;
    // For rectangular matrices the gene ID is also in row zero
    private static final int SUBJECT_ID_ROW = 0;
    private static final int MAX_RATING = 100;

    public static Map<Coordinates, Integer> normalise(Map<Coordinates, Integer> matrix) {
        Map<Coordinates, Integer> normMat = new HashMap<Coordinates, Integer>();
        for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
            // Transfer the gene IDs
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (yCoordinate == SUBJECT_ID_COLUMN) {
                normMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate),
                        matrix.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                normMat.put(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN),
                        matrix.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
            }
            // Calculations for normalisation
            if (xCoordinate != SUBJECT_ID_ROW && yCoordinate != SUBJECT_ID_COLUMN) {
                normMat.put(entry.getKey(), entry.getValue() * MAX_RATING / matrix.get(
                                new Coordinates(xCoordinate, xCoordinate)));
            }
        }
        return normMat;
    }
}
