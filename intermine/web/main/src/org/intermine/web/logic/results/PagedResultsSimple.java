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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Xavier Watkins
 *
 */
public class PagedResultsSimple extends PagedTable implements Serializable
{
    private WebResultsSimple results;

    public PagedResultsSimple(List columns, WebResultsSimple results) {
        super(columns);
        this.results = results;
        setColumnNames(columns);
        updateRows();
    }

    public PagedResultsSimple(List columns, boolean summary) {
        super(columns, summary);
    }

    public WebColumnTable getAllRows() {
        return results;
    }

    public int getExactSize() {
        return results.size();
    }

    public int getMaxRetrievableIndex() {
        return Integer.MAX_VALUE;
    }

    public int getSize() {
        return getExactSize();
    }

    public boolean isSizeEstimate() {
        return false;
    }

    public void updateRows() {
        List newRows = new ArrayList();
        for (int i = getStartRow(); i < getStartRow() + getPageSize(); i++) {

            try {
                List resultsRow = results.getResultElements(i);
                newRows.add(resultsRow);
            } catch (IndexOutOfBoundsException e) {
                // we're probably at the end of the results object, so stop looping
                break;
            }
        }
        setRows(newRows);
    }

}
