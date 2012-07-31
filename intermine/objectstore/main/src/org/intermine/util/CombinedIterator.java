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
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An Iterator that combines the contents of various other Iterators. The provided Iterators will
 * be followed one by one, until the last Iterator has no remaining elements.
 *
 * @author Matthew Wakeling
 * @param <E> the element type of the iterator
 */
public class CombinedIterator<E> implements Iterator<E>
{
    protected Iterator<Iterator<? extends E>> iteratorIterator = null;
    protected Iterator<? extends E> currentIterator = null;

    /**
     * Constructs a CombinedIterator from a List of Iterators. The provided Iterators should be in a
     * state where the desired elements are present. The act of iterating through this combined
     * iterator will result in the provided Iterators being iterated.
     *
     * @param iterators a List of Iterators to be followed in order
     */
    public CombinedIterator(List<Iterator<? extends E>> iterators) {
        iteratorIterator = iterators.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        while (((currentIterator == null) || (!currentIterator.hasNext()))
                && iteratorIterator.hasNext()) {
            currentIterator = iteratorIterator.next();
        }
        return currentIterator.hasNext() || iteratorIterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public E next() {
        if (hasNext()) {
            return currentIterator.next();
        } else {
            throw new NoSuchElementException("CombinedIterator is finished");
        }
    }

    /**
     * {@inheritDoc}
     * This operation is not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
