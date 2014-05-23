package org.intermine.bio.like;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.intermine.Coordinates;
import org.intermine.model.InterMineObject;

/**
 *
 * @author selma
 *
 */
public class LookupBuildMatrixColumns
{
    private static int buildMatrixColumns(Map<Coordinates, Integer> matrix, int currentRow,
            int latestColumn, Collection<?> relatedItems) {
        int rightMostColumn = latestColumn;

        // for this gene, relatedItems is all pathways (or protein domains, etc)
        for (Object rawRow : relatedItems) {

            List<InterMineObject> subRow = (List<InterMineObject>) rawRow;

            for (InterMineObject related: subRow) {

                if (related != null) {

                    boolean saved = false;
                    int currentColumn = latestColumn + 1;
                    for (Map.Entry<Coordinates, Integer> entry : matrix.entrySet()) {
                        if (!saved && related.getId() == entry.getValue()) {
                            Coordinates coordinate = entry.getKey();
                            currentColumn = coordinate.getValue();
                            saved = true;
                        }
                    }
                    if (!saved) {
                        // new column so put at the end
                        rightMostColumn += 1;
                    }
                    matrix.put(new Coordinates(currentRow, currentColumn), related.getId());
                }
            }
        }
        return rightMostColumn;
    }
}
