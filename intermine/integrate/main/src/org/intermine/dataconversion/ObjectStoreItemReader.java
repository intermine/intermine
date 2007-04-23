package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.QueryValue;



import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private int batchSize = 1000;

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
     * Returns the underlying ObjectStoreItemPathFollowingImpl
     *
     * @return an ObjectStore
     */
    public ObjectStoreItemPathFollowingImpl getObjectStore() {
        return os;
    }


    /**
     * {@inheritDoc}
     *
     */
    public Iterator itemIterator() {
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(batchSize);
        res.setNoExplain();
        res.setNoOptimise();
        return res.iterator();
    }

    /**
     * Read items of one particular class only, or exclude one particular class.
     * @param clsName fully qualified name of class to include/exclude
     * @param notEquals if true then exclude the given classname
     * @return and iterator over the selected items
     * @throws ObjectStoreException if a problem running the query
     */
    public Iterator itemIterator(String clsName, boolean notEquals) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "className");
        SimpleConstraint sc = new SimpleConstraint(qf,
            notEquals ? ConstraintOp.NOT_EQUALS : ConstraintOp.EQUALS, new QueryValue(clsName));
        q.setConstraint(sc);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(batchSize);
        res.setNoExplain();
        res.setNoOptimise();
        return res.iterator();
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
        return itemIterator(q, batchSize);
    }

   /**
     * Returns an iterator through the set of Items present in the source Item ObjectStore for the
     * given Query and batch size.
     *
     * @param q the Query - must have one element in the SELECT list, which must be an Item Object
     * @param batchSize number of items to read in each batch
     * @return an Iterator
     */
    public Iterator itemIterator(Query q, int batchSize) {
        SingletonResults sr = new SingletonResults(q, os, os.getSequence());
        sr.setBatchSize(batchSize);
        sr.setNoExplain();
        sr.setNoOptimise();
        return sr.iterator();
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public List getItemsByDescription(Set constraints) throws ObjectStoreException {
        return os.getItemsByDescription(constraints);
    }


    /**
     * Set the size of batches retrieved from the source ObjectStore.  Large numbers
     * should generally give better performance but require memory.  Default value
     * is 1000.
     * @param batchSize size of batches to read
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
