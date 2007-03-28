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

import java.util.ArrayList;
import java.util.List;

import org.intermine.web.struts.WebCollection;

/**
 * A pageable and configurable table created from a Collection.
 * @author Mark Woodbridge
 */
public class PagedCollection extends PagedTable
{
    private final WebCollection webCollection;

    /**
     * Create a new PagedCollection object from the given Collection.
     * @param webCollection the collection
     */
    public PagedCollection(WebCollection webCollection) {
        super(webCollection.getColumns());
        this.webCollection = webCollection;
        updateRows();
    }
    
    /**
     * @see PagedTable#getAllRows
     */
    public WebColumnTable getAllRows() {
        return webCollection;
    }

    /**
     * @see PagedTable#getSize
     */
    public int getSize() {
        return getExactSize();
    }

    /**
     * @see PagedTable#isSizeEstimate
     */
    public boolean isSizeEstimate() {
        return false;
    }

    /**
     * @see PagedTable#getExactSize
     */
    public int getExactSize() {
        return webCollection.size();
    }

    /**
     * @see PagedTable#updateRows
     */
    protected void updateRows() {
        List newRows = new ArrayList();
        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {
            try {
                List resultsRow = webCollection.getResultElements(i);
                newRows.add(resultsRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
        setRows(newRows);
    }

    /**
     * @see PagedTable#getMaxRetrievableIndex
     */
    public int getMaxRetrievableIndex() {
        return Integer.MAX_VALUE;
    }

}
