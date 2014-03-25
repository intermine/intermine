package org.intermine.webservice.server.jbrowse;

import java.util.HashMap;

import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;

public class Queries {

    /**
     * For making queries that will not require LOOKUP constraints.
     * @param pq The PathQuery
     * @return A Query
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Query pathQueryToOSQ(PathQuery pq) {
        return pathQueryToOSQ(pq, null);
    }

    /**
     * For making queries that will or might require LOOKUP constraints.
     * @param pq The PathQuery
     * @param bqr The machinery for resolving LOOKUPs to real constraints.
     * @return A Query
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Query pathQueryToOSQ(PathQuery pq, BagQueryRunner bqr) {
        Query q;
        try {
            q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), bqr, new HashMap());
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error generating query.", e);
        }
        return q;
    }
}
