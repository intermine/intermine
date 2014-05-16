package org.intermine.bio.like;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Matrices() is used for the pre-calculation of a matrix, that includes all items, that every
 * gene has in common with every other gene in the dataset (based on one single aspect).
 * Out of this another matrix is calculated, that contains the similarity between every gene.
 * The similarity is a rating from 0 to 100, where 0 (null) means "nothing in common"
 * and 100 means "these are the most similar genes in the dataset".
 *
 * The matrices are rectangular, where both the first row and the first column contains
 * all gene IDs. That is to simplify the run time calculations: If you want to get the
 * similar Genes for one specific gene, you just have to read out one row; the one where
 * the gene ID is in the first column.
 *
 * All calculations are based on one single aspect!
 *
 * @author selma
 */
public final class Matrices
{
    // The gene ID is always in column zero
    private static final int SUBJECT_ID_COLUMN = 0;
    // For rectangular matrices the gene ID is also in row zero
    private static final int SUBJECT_ID_ROW = 0;
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 100;

    private Matrices() {
        // Don't.
    }

    /**
     * Overrides interface MatrixOperation.
     * Finds common related items between all genes.
     *
     * @param matrix containing all genes and their related items.
     * @return a rectangular matrix (HashMap with x- and y-coordinates as keys) containing all
     * gene IDs and the ArrayLists of related items, that genes have in common.
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
                            // if inner2 has not the same coordinates than inner2
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
                    newMatrix.put(new Coordinates(xCoordinateOuter + 1, entry.getKey() + 1),
                            entry.getValue());
                }

            }
        });
    }

    /**
     * Overrides interface MatrixOperation.
     * Does the same like method findCommonItems but for the type "presence".
     *
     * @param matrix containing all genes and their related items.
     * @return a rectangular matrix (HashMap with x- and y-coordinates as keys) containing all
     * gene IDs and the ArrayLists of related items, that genes have in common.
     */
    public static Map<Coordinates, ArrayList<Integer>> findCommonItemsPresence(
            final Map<Coordinates, Integer> matrix) {
        return commonMatrixLoop(matrix, new MatrixOperation() {

            @Override
            public void loopAction(Map<Coordinates, ArrayList<Integer>> newMatrix,
                    Map<Coordinates, Integer> matrix, Coordinates coordinatesOuterGeneID) {
                int xCoordinateOuter = coordinatesOuterGeneID.getKey();

                for (final Map.Entry<Coordinates, Integer> inner : matrix.entrySet()) {
                    int xCoordinate = inner.getKey().getKey();
                    // if inner is not a gene ID and
                    // and if inner is in the same row than the current outer gene ID
                    // -> save the items, they are common
                    if (xCoordinate != SUBJECT_ID_COLUMN && xCoordinate == xCoordinateOuter) {
                        ArrayList<Integer> commonItems;
                        // check, if the corresponding gene ID is already saved
                        if (!newMatrix.containsKey(new Coordinates(xCoordinateOuter + 1,
                                xCoordinateOuter + 1))) {
                            // if "no": create new list
                            commonItems = new ArrayList<Integer>();
                            newMatrix.put(new Coordinates(xCoordinateOuter + 1,
                                    xCoordinateOuter + 1), commonItems);
                            commonItems.add(inner.getValue());
                        }
                        else {
                            // if "yes": add the common item to the list
                            commonItems = newMatrix.get(new Coordinates(xCoordinateOuter + 1,
                                    xCoordinateOuter + 1));
                            commonItems.add(inner.getValue());
                        }
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
                ArrayList<Integer> geneInColumn = new ArrayList<Integer>();
                ArrayList<Integer> geneInRow = new ArrayList<Integer>();
                commonMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate + 1), geneInColumn);
                commonMat.put(new Coordinates(xCoordinate + 1, SUBJECT_ID_COLUMN), geneInRow);
                geneInColumn.add(matrix.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                geneInRow.add(matrix.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));

                // Perform the loopAction to find common items (e.g. pathways) for each subject
                // (gene) of the outer loop
                operation.loopAction(commonMat, matrix, outer.getKey());
            }
        }
        return commonMat;
    }

    /**
     * Calculates the similarity ratings pairwise and for one aspect for the type "category".
     *
     * @param commonMat a rectangular matrix (HashMap with x- and y-coordinates as keys) containing
     * all gene IDs and the ArrayLists of related items, that genes have in common.
     * @return a rectangular matrix (HashMap with x- and y-coordinates as keys) containing all
     * gene IDs and pairwise similarity ratings between the genes.
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

    /**
     * Calculates the similarity ratings pairwise and for one aspect for the type "count".
     *
     * @param matrix containing all genes and their related items.
     * @return a rectangular matrix (HashMap with x- and y-coordinates as keys) containing all
     * gene IDs and pairwise similarity ratings between the genes.
     */
    public static Map<Coordinates, Integer> findSimilarityCount(Map<Coordinates, Integer> matrix) {
        Map<Coordinates, Integer> countedItems = new HashMap<Coordinates, Integer>();
        Map<Coordinates, Integer> simMat = new HashMap<Coordinates, Integer>();

        // Count the items (e.g. pathways) for each gene
        for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (yCoordinate == SUBJECT_ID_COLUMN) {
                countedItems.put(entry.getKey(), entry.getValue());
                int count = 0;
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
            int xCoordinate = outer.getKey().getKey();
            int yCoordinate = outer.getKey().getValue();
            // Transfer the gene IDs
            if (yCoordinate == SUBJECT_ID_COLUMN) {
                simMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate + 1),
                        countedItems.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                simMat.put(new Coordinates(xCoordinate + 1, SUBJECT_ID_COLUMN),
                        countedItems.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
            }
            else { // If outer contains counted item
                int rating;
                for (Map.Entry<Coordinates, Integer> inner : countedItems.entrySet()) {
                    int xCoordinateInner = inner.getKey().getKey();
                    int yCoordinateInner = inner.getKey().getValue();
                    // Only transfer non-zero items -> makes the simMat more sparse
                    if (yCoordinateInner == 1 && outer.getValue() != SUBJECT_ID_COLUMN
                            && inner.getValue() != SUBJECT_ID_COLUMN) {
                        // Row-wise normalisation
                        rating = Math.abs(MAX_RATING * inner.getValue()) / outer.getValue();
                        if (rating > MAX_RATING) {
                            rating = MAX_RATING;
                        }
                        simMat.put(new Coordinates(xCoordinate + 1,
                                xCoordinateInner + 1), rating);
                    }
                }
            }
        }
        return simMat;
    }

    /**
     * Calculates the similarity ratings pairwise and for one aspect for the type "presence".
     *
     * @param matrix containing all genes and their related items.
     * @return a rectangular matrix (HashMap with x- and y-coordinates as keys) containing all
     * gene IDs and pairwise similarity ratings between the genes.
     */
    public static Map<Coordinates, Integer> findSimilarityPresence(
            Map<Coordinates, Integer> matrix) {
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

        System.out.print("\nsmallResult (correct result): \n");
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                Integer val = hasMat.get(new Coordinates(k, j));
                System.out.print(val + " ");
            }
            System.out.print("\n");
        }

        for (Map.Entry<Coordinates, Integer> entry : hasMat.entrySet()) {
            int xCoordinate = entry.getKey().getKey();
            int yCoordinate = entry.getKey().getValue();
            if (yCoordinate == 0) {
                simMat.put(new Coordinates(SUBJECT_ID_ROW, xCoordinate + 1),
                        hasMat.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
                simMat.put(new Coordinates(xCoordinate + 1, SUBJECT_ID_COLUMN),
                        hasMat.get(new Coordinates(xCoordinate, SUBJECT_ID_COLUMN)));
            }
            else {
                for (Map.Entry<Coordinates, Integer> inner : hasMat.entrySet()) {
                    int xCoordinateInner = inner.getKey().getKey();
                    int yCoordinateInner = inner.getKey().getValue();
                    if (inner.getKey().getValue() == 1) {
                        if (inner.getValue().equals(entry.getValue())) {
                            simMat.put(new Coordinates(xCoordinate + 1,
                                    inner.getKey().getKey() + 1), MAX_RATING);
                        }
                        else {
                            simMat.put(new Coordinates(xCoordinate + 1,
                                    inner.getKey().getKey() + 1), MIN_RATING);
                        }
                    }
                }
            }
        }
        return simMat;
    }

    /**
     * Normalises the input matrix row-wise.
     * E.g. given matrix:        ->   normalised matrix:
     *   -   gene1 gene2 gene3          -   gene1 gene2 gene3
     * gene1   5     2   null         gene1  5/5   2/5  null
     * gene2   2     2    1           gene2  2/2   2/2  1/2
     * gene3  null   1    3           gene3  null  1/3  3/3
     *
     * @param matrix containing all gene IDs and pairwise similarity ratings between the genes.
     * @return the input matrix normalised (with values between 0 and 100)
     */
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
