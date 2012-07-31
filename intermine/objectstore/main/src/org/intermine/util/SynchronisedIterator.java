package org.intermine.util;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * @param <E> The element type of the iterator
 */
public class SynchronisedIterator<E> implements Iterator<E>
{
    protected Iterator<E> iterator = null;

    /**
     * Constructs a SynchronisedIterator from an Iterator.
     *
     * @param iterator an Iterator
     */
    public SynchronisedIterator(Iterator<E> iterator) {
        this.iterator = iterator;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized E next() {
        return iterator.next();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void remove() {
        iterator.remove();
    }
}
