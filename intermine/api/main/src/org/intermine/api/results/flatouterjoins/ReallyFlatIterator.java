package org.intermine.api.results.flatouterjoins;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.NoSuchElementException;

import org.intermine.objectstore.query.ResultsRow;

/**
 * An Iterator that flattens the contents of another Iterator. It expects the kind of contents
 * produced by the ObjectStoreFlatOuterJoinsImpl.
 *
 * @author Matthew Wakeling
 */
public class ReallyFlatIterator implements Iterator
{
    protected Iterator iter = null;
    protected Iterator currentIterator = null;

    /**
     * Constructs a ReallyFlatIterator from another Iterator. The act of iterating through this
     * iterator will result in the provided Iterator being iterated.
     *
     * @param iter an Iterator to be followed in order
     */
    public ReallyFlatIterator(Iterator iter) {
        this.iter = iter;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        while (((currentIterator == null) || (!currentIterator.hasNext()))
                && iter.hasNext()) {
            Object nextCurrent = iter.next();
            if (nextCurrent instanceof MultiRow) {
                currentIterator = ((MultiRow) nextCurrent).iterator();
            } else {
                currentIterator = Collections.singletonList(nextCurrent).iterator();
            }
        }
        return (currentIterator != null) && currentIterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        if (hasNext()) {
            List origRow = (List) currentIterator.next();
            ResultsRow translatedRow = new ResultsRow();
            for (Object element : origRow) {
                if (element instanceof MultiRowValue) {
                    translatedRow.add(((MultiRowValue) element).getValue());
                } else {
                    translatedRow.add(element);
                }
            }
            return translatedRow;
        } else {
            throw new NoSuchElementException("ReallyFlatIterator is finished");
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
