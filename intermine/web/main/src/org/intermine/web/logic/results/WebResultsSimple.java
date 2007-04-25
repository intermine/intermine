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

import java.util.AbstractList;
import java.util.List;

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.objectstore.ObjectStoreException;

/**
 * @author Xavier Watkins
 *
 */
public class WebResultsSimple extends AbstractList implements WebTable
{
    private Results results;
    private List columnNames;
    
    /**
     * 
     */
    public WebResultsSimple(Results results, List columnNames) {
        this.results = results;
        this.columnNames = columnNames;
    }

    /** 
     * 
     */
    public Object get(int index) {
        return results.get(index);
    }

    /**
     *
     */
    public int size() {
        try {
            return results.getInfo().getRows();
        } catch (ObjectStoreException e) {
            throw new RuntimeException("failed to get a ResultsInfo object", e);
        }
    }

    /**
     *
     */
    public List getColumns() {
        return columnNames;
    }

    public List getResultElements(int index) {
        return (ResultsRow) results.get(index);
//        for (Iterator iter = resRow.iterator(); iter.hasNext();) {
//            Object element = (Object) iter.next();
//            
//        }
//        resRow.size()
//        return results.get(index);
    }

    /**
     * Returns the ObjectStore's maximum allowable offset.
     *
     * @return an int
     */
    public int getMaxRetrievableIndex() {
        return results.getObjectStore().getMaxOffset();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isSizeEstimate() {
        try {
            return results.getInfo().getStatus() != ResultsInfo.SIZE;
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getExactSize() {
        return results.size();
    }

}
