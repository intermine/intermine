package org.intermine.bio.like;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.intermine.bio.like.Coordinates;

/**
 *
 * @author selma
 *
 */
public class LookupCountCommonItemsCategory {

    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;
    // For rectangular matrices the gene ID is also in row zero
    private static final int SUBJECT_ID_ROW = 0;
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 100;

    /**
     *
     * @param commonMat
     * @return
     */
    public static Map<Coordinates, Integer> countCommonItemsCategory(
            Map<Coordinates, ArrayList<Integer>> commonMat) {
        Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();

        for (Map.Entry<Coordinates, ArrayList<Integer>> entry : commonMat.entrySet()) {
            // Transfer the gene IDs
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (xCoordinate == SUBJECT_ID_ROW || yCoordinate == SUBJECT_ID_COLUMN) {
                simMat.put(entry.getKey(), entry.getValue().get(0));
            }
            else {
                // Save the number of common items
                simMat.put(entry.getKey(), entry.getValue().size());
            }
        }
        return simMat;
    }
}
