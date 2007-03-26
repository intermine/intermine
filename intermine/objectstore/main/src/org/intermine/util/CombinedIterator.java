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
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An Iterator that combines the contents of various other Iterators. The provided Iterators will
 * be followed one by one, until the last Iterator has no remaining elements.
 *
 * @author Matthew Wakeling
 */
public class CombinedIterator implements Iterator
{
    protected Iterator iteratorIterator = null;
    protected Iterator currentIterator = null;

    /**
     * Constructs a CombinedIterator from a List of Iterators. The provided Iterators should be in a
     * state where the desired elements are present. The act of iterating through this combined
     * iterator will result in the provided Iterators being iterated.
     *
     * @param iterators a List of Iterators to be followed in order
     */
    public CombinedIterator(List iterators) {
        iteratorIterator = iterators.iterator();
    }

    /**
     * @see Iterator#hasNext
     */
    public boolean hasNext() {
        while (((currentIterator == null) || (!currentIterator.hasNext()))
                && iteratorIterator.hasNext()) {
            currentIterator = (Iterator) iteratorIterator.next();
        }
        return currentIterator.hasNext() || iteratorIterator.hasNext();
    }

    /**
     * @see Iterator#next
     */
    public Object next() {
        if (hasNext()) {
            return currentIterator.next();
        } else {
            throw new NoSuchElementException("CombinedIterator is finished");
        }
    }

    /**
     * @see Iterator#remove
     * This operation is not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
