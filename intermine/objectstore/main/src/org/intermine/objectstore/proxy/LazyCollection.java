package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Set;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsInfo;

/**
 * Class which uses an ObjectStore to perform lazy fetching of data
 *
 * @author Matthew Wakeling
 * @param <E> The element type
 */
public interface LazyCollection<E> extends Lazy, Set<E>
{
    /**
     * Sets this LazyCollection to bypass the optimiser
     */
    void setNoOptimise();

    /**
     * Sets this LazyCollection to bypass the explain check in ObjectStore.execute().
     */
    void setNoExplain();

    /**
     * Returns the Query used by this LazyCollection
     *
     * @return a Query
     */
    Query getQuery();

    /**
     * Return this Collection as a List.  This may create a new ArrayList if necessary so the
     * returned List is not guaranteed to be consistent if the LazyCollection changes.
     *
     * @return a List
     */
    List<E> asList();

    /**
     * Returns Returns the current best estimate of the characteristics of the LazyCollection
     *
     * @return a ResultsInfo object
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    ResultsInfo getInfo() throws ObjectStoreException;

    /**
     * Sets the number of rows requested from the ObjectStore whenever an execute call is made
     *
     * @param size the number of rows
     */
    void setBatchSize(int size);
}
