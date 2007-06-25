package org.intermine.web.logic.results;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.query.PathQuery;

/**
 * A List that can understand ResultElement objects.
 * @author Kim Rutherford
 */
public interface WebTable extends List
{
    /**
     * Return a List containing a ResultElement object for each element given in the given row.
     * @param index the row of the results to fetch
     * @return the results row
     */
    public List<ResultElement> getResultElements(int index);
    
    /**
     * Returns the Column objects for this table.
     * @return the columns
     */
    public List<Column> getColumns();
    
    /**
     * Check whether the result of size() is an estimate
     * @return true if the size is an estimate
     */
    public boolean isSizeEstimate();

    /**
     * Get the exact number of rows of this table
     * @return the number of rows
     */
    public int getExactSize();
    
    /**
     * Return the maximum retrievable index for this PagedTable.  This will only ever return less
     * than getExactSize() if the underlying data source has a restriction on the maximum index
     * that can be retrieved.
     * @return the maximum retrieved index
     */
    public int getMaxRetrievableIndex();
    
    /**
     * Returns the pathToBagQueryResult Map.
     *
     * @return a Map
     */
    public Map<String, BagQueryResult> getPathToBagQueryResult();
    
    /**
     * Get the PathQuery associated with this WebTable
     * @return the PathQuery
     */
    public PathQuery getPathQuery();
    
}
