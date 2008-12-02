package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;

/**
 * An Iterator that produces data in a format suitable for exporting. The data is flattened, so if
 * there are outer joined collections, there may be more rows than in the original results.
 *
 * @author Matthew Wakeling
 */
public class ExportResultsIterator implements Iterator<ResultsRow>
{

    public ExportResultsIterator(ObjectStore os, PathQuery pq) {
    }

    public boolean hasNext() {
        return false;
    }

    public ResultsRow next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
