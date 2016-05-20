package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ListIterator;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * container to hold results for a reference query and associated iterator
 * @author nils
 */
class InterMineResultsContainer
{
    final Results results;
    final ListIterator<ResultsRow<InterMineObject>> iterator;

    /**
     * create container and set iterator
     * @param results
     *            result object from os.execute(query)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public InterMineResultsContainer(Results results) {
        this.results = results;
        this.iterator = (ListIterator) results.listIterator();
    }

    /**
     * the results
     * @return the results
     */
    public Results getResults() {
        return results;
    }

    /**
     * the iterator on the results
     * @return iterator
     */
    public ListIterator<ResultsRow<InterMineObject>> getIterator() {
        return iterator;
    }
}
