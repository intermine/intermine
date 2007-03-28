package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;

/**
 * Helper methods for the PagedTable object.
 *
 * @author Kim Rutherford
 */
public abstract class TableHelper
{
    /**
     * Batch size for the underlying objectstore
     */
    public static final int BATCH_SIZE = 500;

    /**
     * Must be called after makeResults to check that the query is valid and
     * fetch the first row.
     * @param results the Results object returned by makeResults
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public static void initResults(Results results)
        throws ObjectStoreException {
        // call this so that if an exception occurs we notice now rather than in the JSP code
        try {
            results.get(0);
        } catch (IndexOutOfBoundsException err) {
            // no results - ignore
            // we don't call size() first to avoid this exception because that could be very slow
            // on a large results set
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ObjectStoreException) {
                throw (ObjectStoreException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    /**
     * Make a results object from an objectstore and a query
     * @param os the ObjectStore against which to run the Query
     * @param query the Query to create the results for
     * @return a results object for the argument Query
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     */
    public static Results makeResults(ObjectStore os, Query query)
        throws ObjectStoreException {
        Results r = os.execute(query);
        r.setBatchSize(BATCH_SIZE);
        return r;
    }
}
