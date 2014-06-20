package org.intermine.bio.like;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;

/**
 *
 * @author selma
 *
 */
public class LookupCountCommonItemsCategory {

    // For rectangular matrices the gene ID is also in row zero
    private static final int SUBJECT_ID_ROW = 0;

    public static Map<Coordinates, Integer> countCommonItemsCategory(
            Map<Coordinates, ArrayList<Integer>> commonMat) {
        Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();

        for (Map.Entry<Coordinates, ArrayList<Integer>> entry : commonMat.entrySet()) {
            // Transfer the gene IDs
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (yCoordinate == SUBJECT_ID_ROW) {
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
