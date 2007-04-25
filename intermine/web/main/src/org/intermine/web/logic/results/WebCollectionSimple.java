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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A wrapper for a collection that makes for easier rendering in the webapp.
 * @author kmr
 */
public class WebCollectionSimple extends AbstractList implements WebTable
{
    private List list = null;
    private List<Column> columns;
    private final Collection collection;
 
    /**
     * Create a new WebPathCollection object.
     * @param columnName the String to use when displaying this collection - used as the column name
     * for the single column of results
     * @param collection the Collection, which can be a List of objects or a List of List of
     * objects (like a Results object)
     * @param webConfig the WebConfig object the configures the columns in the view
     */
    public WebCollectionSimple(String columnName, Collection collection) {
        this.collection = collection;
        columns = new ArrayList<Column>();
        columns.add(new Column(columnName, 0, Object.class));
    }

    /**
     * Return a List containing a ResultElement object for each element in the given row.  The List
     * will be the same length as the view List.
     * @param index the row of the results to fetch
     * @return the results row as ResultElement objects
     */
    public List<ResultElement> getResultElements(int index) {
        return getElementsInternal(index, true);
    }

    /**
     * Return the given row as a List of primatives (rather than a List of ResultElement objects)
     * {@inheritDoc}
     */
    public Object get(int index) {
        return getElementsInternal(index, false);
    }

    private List getElementsInternal(int index, boolean makeResultElements) {
        Object object = getList().get(index);
        ArrayList<Object> rowCells = new ArrayList<Object>();
        if (makeResultElements) {
            rowCells.add(new ResultElement(object));
        } else {
            rowCells.add(object);
        }

        return rowCells;
    }

    private List getList() {
        if (list == null) {
            if (collection instanceof List) {
                list = (List) collection;
            } else {
                list = new ArrayList(collection);
            }
        }

        return list;
    }
    
    /**
     * {@inheritDoc}
     */
    public int size() {
        return getList().size();
    }

    /**
     * Return the List of Column objects for this WebPathCollection, configured by the WebConfig for
     * the class of the collection we're showing
     * @return the Column object List
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * {@inheritDoc}
     */
    public int getExactSize() {
        return size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSizeEstimate() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.intermine.web.logic.results.WebTable#getMaxRetrievableIndex()
     */
    public int getMaxRetrievableIndex() {
        return Integer.MAX_VALUE;
    }
}
