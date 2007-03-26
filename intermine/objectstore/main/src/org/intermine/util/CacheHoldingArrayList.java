package org.intermine.util;

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
import java.util.Collection;

/**
 * This is an extension of the the ArrayList class, designed to be used by systems that return
 * Lists of data, with extra data that should not be flushed out of a cache until the List is
 * garbage collected.
 *
 * This is used for example in the ObjectStoreItemPathFollowingImpl, when it generates a batch.
 * The objectstore fetches additional useful objects, and stores them in the holder of a
 * CacheHoldingArrayList as well as a cache. The holder them prevents the extra useful objects from
 * being flushed out of the cache until the DataTranslator has finished with the batch.
 *
 * @author Matthew Wakeling
 */
public class CacheHoldingArrayList extends ArrayList
{
    private ArrayList holder = new ArrayList();

    /**
     * Empty constructor
     */
    public CacheHoldingArrayList() {
        super();
    }

    /**
     * Constructs a new instance from another Collection.
     *
     * @param col a Collection
     */
    public CacheHoldingArrayList(Collection col) {
        super(col);
    }
    
    /**
     * Constructs a new instance with the given initial capacity.
     *
     * @param capacity the initial capacity
     */
    public CacheHoldingArrayList(int capacity) {
        super(capacity);
    }

    /**
     * Adds an object to the holder. This prevents the given object from being garbage collected
     * until this List is garbage-collected.
     *
     * @param o any Object
     */
    public void addToHolder(Object o) {
        holder.add(o);
    }
}
