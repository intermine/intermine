package org.flymine.objectstore.proxy;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Set;
import java.util.AbstractList;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;

/**
 * LazyCollection is used in paging of business object collections in materialization.
 *
 * @author Richard Smith
 */

public class LazyCollection extends AbstractList implements Set
{
    private Query query;

    /**
     * Construct with a flymine Query object.  NullPointerException if this
     * is null.  Checks that query returns one and only one object type.
     *
     * @param query a flymine query to get contents of the collection
     */
    public LazyCollection(Query query) {
        if (query == null) {
            throw (new NullPointerException("Query cannot be null"));
        }

        List select = query.getSelect();
        if (select.size() < 1) {
            throw (new IllegalArgumentException("Query has no items in select list."));
        }
        if (select.size() > 1) {
            throw (new IllegalArgumentException("Query has more than item in select list."));
        }
        if (!(select.get(0) instanceof QueryClass)) {
            throw (new IllegalArgumentException("Query can only select a QueryClass"));
        }

        this.query = query;
    }

    /**
     * Get the query that populates collection
     *
     * @return the query
     */
    public Query getQuery() {
        return query;
    }

    /**
     * get method not valid for a LazyCollection, throws an UnsupportedOperationException
     *
     * @param i index of object to get
     * @return nothing
     */
    public Object get(int i) {
        throw new UnsupportedOperationException("get() method not supported by LazyCollection");
    }

    /**
     * size method not valid for a LazyCollection, throws an UnsupportedOperationException
     *
     * @return nothing
     */
    public int size() {
        throw new UnsupportedOperationException("size() method not supported by LazyCollection");
    }
}
