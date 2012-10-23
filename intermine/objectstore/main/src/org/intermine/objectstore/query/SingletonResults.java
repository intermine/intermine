package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * This class is equivalent to a Result object with ResultRows consisting only of single items
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class SingletonResults extends Results implements Set<Object>
{
    /**
     * Constructor for a SingletonResults object
     *
     * @param q the Query that produces this Results
     * @param os the ObjectStore that can be used to get results rows from
     * @param sequence an object representing the state of the ObjectStore, which should be quoted
     * back to the ObjectStore when requests are made
     * @throws IllegalArgumentException if q does not return a single column
     */
    public SingletonResults(Query q, ObjectStore os, Map<Object, Integer> sequence) {
        super(q, os, sequence);

        // Test that this Query returns a single column of type QueryClass
        if (q.getSelect().size() != 1) {
            throw new IllegalArgumentException("Query must return a single column");
        }
    }

    /**
     * Constructor for a SingletonResults object, given a ResultsBatches object.
     *
     * @param batches a ResultsBatches object that will back this new object
     * @param optimise true if queries should be optimised
     * @param explain true if queries should be explained
     * @param prefetch true to switch on the PrefetchManager
     */
    public SingletonResults(ResultsBatches batches, boolean optimise, boolean explain,
            boolean prefetch) {
        super(batches, optimise, explain, prefetch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Object> range(int start, int end) throws ObjectStoreException {
        List<Object> rows = super.range(start, end);
        for (int i = 0; i < rows.size(); i++) {
            rows.set(i, ((List<?>) rows.get(i)).get(0));
        }
        return rows;
    }
}
