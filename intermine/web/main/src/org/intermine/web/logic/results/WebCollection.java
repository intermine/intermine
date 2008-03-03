package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.ResultsInfo;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.query.PathQuery;

import org.apache.log4j.Logger;

/**
 * A wrapper for a collection that makes for easier rendering in the webapp.
 * @author kmr
 */
public class WebCollection extends AbstractList<List<Object>> implements WebTable
{
    private static final Logger LOG = Logger.getLogger(WebCollection.class);

    private List list = null;
    private List<Column> columns;
    private final Collection collection;

    /**
     * Create a new WebPathCollection object.
     * @param columnName the String to use when displaying this collection - used as the column name
     * for the single column of results
     * @param collection the Collection, which can be a List of objects or a List of List of
     * objects (like a Results object)
     */
    public WebCollection(String columnName, Collection collection) {
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
     * {@inheritDoc}
     */
    public Map<String, BagQueryResult> getPathToBagQueryResult() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the given row as a List of primatives (rather than a List of ResultElement objects)
     * {@inheritDoc}
     */
    public List<Object> get(int index) {
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
    public int getEstimatedSize() {
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
    public int size() {
        if (list != null) {
            return list.size();
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSizeEstimate() {
        if (collection instanceof LazyCollection) {
            LazyCollection lazy = (LazyCollection) collection;
            try {
                return lazy.getInfo().getStatus() != ResultsInfo.SIZE;
            } catch (ObjectStoreException e) {
                LOG.error("unexpected exception while getting ResultsInfo for collection", e);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxRetrievableIndex() {
        return Integer.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    public PathQuery getPathQuery() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ResultsInfo getInfo() throws ObjectStoreException {
        return null;
    }
}
