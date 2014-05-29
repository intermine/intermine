package org.intermine.bio.like;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.intermine.Coordinates;
import org.intermine.bio.like.Matrices.MatrixOperation;

/**
 *
 * @author selma
 *
 */
public final class LookupFindCommonItems
{

    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;
    // For rectangular matrices the gene ID is also in row zero
    private static final int SUBJECT_ID_ROW = 0;
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 100;

    private LookupFindCommonItems() {
        // Don't.
    }

    /**
     *
     * @param matrix
     * @return
     */
    public static Map<Coordinates, ArrayList<Integer>> findCommonItems(
            final Map<Coordinates, Integer> matrix) {
        return commonMatrixLoop(matrix, new MatrixOperation() {

            @Override
            public void loopAction(Map<Coordinates, ArrayList<Integer>> newMatrix,
                    Map<Coordinates, Integer> matrix, Coordinates coordinatesOuterGeneID) {
                final Map<Integer, ArrayList<Integer>> commonToOuter =
                        new HashMap<Integer, ArrayList<Integer>>();

                int xCoordinateOuter = coordinatesOuterGeneID.getKey();

                for (Map.Entry<Coordinates, Integer> inner : matrix.entrySet()) {
                    int xCoordinate = inner.getKey().getKey();
                    // if inner is in the same row than the current outer gene ID
                    if (xCoordinate == xCoordinateOuter) {
                        for (Map.Entry<Coordinates, Integer> inner2 : matrix.entrySet()) {
                            // if outer has not the same coordinates than inner2
                            // and if the items (e.g. pathways) have the same ID
                            // -> save the items, they are common
                            if (coordinatesOuterGeneID != inner2.getKey()
                                    && inner.getValue().equals(inner2.getValue())) {
                                ArrayList<Integer> commonItems;
                                int xCoordinate2 = inner2.getKey().getKey();
                                // check, if the corresponding gene ID is already saved
                                if (!commonToOuter.containsKey(xCoordinate2)) {
                                    // if "no": create new list
                                    commonItems = new ArrayList<Integer>();
                                    commonToOuter.put(inner2.getKey().getKey(), commonItems);
                                    commonItems.add(inner2.getValue());
                                } else {
                                    // if "yes": add the common item to the list
                                    commonItems = commonToOuter.get(inner2.getKey().getKey());
                                    commonItems.add(inner2.getValue());
                                }
                            }
                        }
                    }
                }

                // Transfer the information to the commonMat in the outer loop
                for (Map.Entry<Integer, ArrayList<Integer>> entry : commonToOuter.entrySet()) {
                    if (entry.getKey() > xCoordinateOuter) {
                        newMatrix.put(new Coordinates(entry.getKey() + 1, xCoordinateOuter + 1),
                                entry.getValue());
                    }
                    else {
                        newMatrix.put(new Coordinates(xCoordinateOuter + 1, entry.getKey() + 1),
                                entry.getValue());
                    }
                }

            }
        });
    }

    /**
     * Calculates the result for findCommonItems and findCommonItemsPresence.
     * Performs the outer loop and saves the gene IDs in the first column and row.
     *
     * @param matrix containing all genes and their related items.
     * Format: Its first column contains
     * the gene IDs, the other columns contain the related items (1 column for each unique item).
     * @param operation containing the overridden loopAction code
     * @return a rectangular matrix (HashMap with x- and y-coordinates as keys) containing all
     * gene IDs and the ArrayLists of related items, that genes have in common.
     * Format: The first row and the first column contain the gene IDs, whereas coordinates (0,1)
     * and (1,0) are the same ID (also (0,2) and (2,0), and so on). The other rows and columns
     * contain the ArrayLists of the common related items. E.g. ArrayList of (3,5) contains common
     * related items of the genes (3,0) and (0,5).
     */
    private static Map<Coordinates, ArrayList<Integer>> commonMatrixLoop(
            Map<Coordinates, Integer> matrix, MatrixOperation operation) {
        // The rectangular matrix to return
        Map<Coordinates, ArrayList<Integer>> commonMat =
                new HashMap<Coordinates, ArrayList<Integer>>();
        for (final Map.Entry<Coordinates, Integer> outer : matrix.entrySet()) {
            int xCoordinate = outer.getKey().getKey();
            int yCoordinate = outer.getKey().getValue();
            if (yCoordinate == SUBJECT_ID_COLUMN) {
                // Transfer the gene IDs and save in ArrayLists
//                ArrayList<Integer> geneInColumn = new ArrayList<Integer>();
                ArrayList<Integer> geneInRow = new ArrayList<Integer>();
//                commonMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate + 1), geneInColumn);
                commonMat.put(new Coordinates(xCoordinate + 1, SUBJECT_ID_COLUMN), geneInRow);
//                geneInColumn.add(matrix.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                geneInRow.add(matrix.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));

                // Perform the loopAction to find common items (e.g. pathways) for each subject
                // (gene) of the outer loop
                operation.loopAction(commonMat, matrix, outer.getKey());
            }
        }
        return commonMat;
    }

    /**
     * Used in commonMatrixLoop.
     *
     * @author selma
     *
     */
    interface MatrixOperation
    {
        /**
        * Which parameter are needed in the inner loop.
        *
        * @param newMatrix a rectangular matrix (HashMap with x- and y-coordinates as keys)
        * containing gene IDs and the ArrayLists of related items, that genes have in common.
        * @param matrix containing all genes and their related items.
        * @param relationShip coordinates of a gene ID
        */
        void loopAction(Map<Coordinates, ArrayList<Integer>> newMatrix,
                Map<Coordinates, Integer> matrix, Coordinates relationShip);

    }
}