package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

/**
 * Provides an interface between a DataTranslator and the source Item ObjectStore which it wishes to
 * read.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class ObjectStoreItemReader extends AbstractItemReader
{
    private ObjectStoreItemPathFollowingImpl os;
    private int defaultBatchSize = 1000;

    /**
     * Constructs a new ObjectStoreItemReader.
     *
     * @param os the ObjectStore
     */
    public ObjectStoreItemReader(ObjectStore os) {
        this.os = new ObjectStoreItemPathFollowingImpl(os);
    }

    /**
     * Constructs a new ObjectStoreItemReader with the given path info.
     *
     * @param os the ObjectStore
     * @param paths the paths
     */
    public ObjectStoreItemReader(ObjectStore os, Map paths) {
        this.os = new ObjectStoreItemPathFollowingImpl(os, paths);
    }

    /**
     * Returns the default itemIterator Query.
     *
     * @return a Query
     */
    public Query getDefaultQuery() {
        Query q = new Query();
        // database has a hard time selecting distinct on object xml
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        return q;
    }

    /**
     * @see ItemReader#itemIterator
     */
    public Iterator itemIterator() throws ObjectStoreException {
        SingletonResults sr = new SingletonResults(getDefaultQuery(), os, os.getSequence());
        sr.setBatchSize(1000);
        return sr.iterator();
    }

    /**
     * Returns an iterator through the set of Items present in the source Item ObjectStore for the
     * given Query.
     *
     * @param q the Query - must have one element in the SELECT list, which must be an Item Object
     * @return an Iterator
     * @throws ObjectStoreException if anything goes wrong
     */
    public Iterator itemIterator(Query q) throws ObjectStoreException {
        return itemIterator(q, defaultBatchSize);
    }

   /**
     * Returns an iterator through the set of Items present in the source Item ObjectStore for the
     * given Query and batch size.
     *
     * @param q the Query - must have one element in the SELECT list, which must be an Item Object
     * @param batchSize number of items to read in each batch
     * @return an Iterator
     * @throws ObjectStoreException if anything goes wrong
     */
    public Iterator itemIterator(Query q, int batchSize) throws ObjectStoreException {
        SingletonResults sr = new SingletonResults(q, os, os.getSequence());
        sr.setBatchSize(batchSize);
        return sr.iterator();
    }

    /**
     * @see ItemReader#getItemById
     */
    public Item getItemById(String objectId) throws ObjectStoreException {
        List results = getItemsByDescription(Collections.singleton(
                    new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, objectId,
                        false)));
        if (results.size() > 1) {
            throw new IllegalStateException("Multiple Items in the objectstore with identifier "
                    + objectId + ", size = " + results.size()
                    + (results instanceof SingletonResults ? "query = " + ((SingletonResults)
                            results).getQuery() : ""));
        } else if (results.size() == 1) {
            return (Item) results.get(0);
        }
        return null;
    }

    /**
     * @see ItemReader#getItemsByDescription
     */
    public List getItemsByDescription(Set constraints) throws ObjectStoreException {
        return os.getItemsByDescription(constraints);
    }
}
