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

import java.util.Iterator;

/**
 * An Iterator that passes through to an underlying Iterator, synchronising all calls.
 *
 * @author Matthew Wakeling
 */
public class SynchronisedIterator implements Iterator
{
    protected Iterator iterator = null;

    /**
     * Constructs a SynchronisedIterator from an Iterator.
     *
     * @param iterator an Iterator
     */
    public SynchronisedIterator(Iterator iterator) {
        this.iterator = iterator;
    }

    /**
     * @see Iterator#hasNext
     */
    public synchronized boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * @see Iterator#next
     */
    public synchronized Object next() {
        return iterator.next();
    }

    /**
     * @see Iterator#remove
     */
    public synchronized void remove() {
        iterator.remove();
    }
}
