package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

/**
 * This is an Iterable that returns a given Iterator. This is useful for using the new for loop
 * syntax when you only have an Iterator.
 *
 * @author Matthew Wakeling
 * @param <T> The element type
 */
public class IteratorIterable<T> implements Iterable<T>
{
    Iterator<T> iterator;

    /**
     * Constructs an Iterable from an Iterator.
     *
     * @param iterator an Iterator
     */
    public IteratorIterable(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    /**
     * Returns the iterator.
     *
     * @return an iterator
     */
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }
}
