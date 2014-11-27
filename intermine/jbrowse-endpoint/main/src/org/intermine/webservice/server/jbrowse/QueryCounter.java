package org.intermine.webservice.server.jbrowse;

import java.util.concurrent.Callable;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;

/**
 * Simple class that encapsulates calling count on a query.
 * @author Alex Kalderimis
 *
 */
final public class QueryCounter implements Callable<Integer> {
    final Query q;
    final ObjectStore os;

    /**
     * Construct a new counter.
     * @param pq The query to count.
     * @param os The object-store to count in.
     */
    public QueryCounter(Query q, ObjectStore os) {
        this.q = q;
        this.os = os;
    }

    @Override
    public Integer call() throws Exception {
        return os.count(q, ObjectStore.SEQUENCE_IGNORE);
    }
}