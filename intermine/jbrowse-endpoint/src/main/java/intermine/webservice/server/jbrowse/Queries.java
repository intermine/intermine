package org.intermine.webservice.server.jbrowse;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;

import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.query.MainHelper;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.ServiceException;

/**
 *
 * @author Alex
 *
 */
public final class Queries
{
    private Queries() {
        // don't
    }

    /**
     * For making queries that will not require LOOKUP constraints.
     * @param pq The PathQuery
     * @return A Query
     */
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

    /**
     *
     * @param fpo fastpath object
     * @param path path
     * @return object
     */
    public static Object resolveValue(FastPathObject fpo, String path) {
        FastPathObject o = fpo;
        String[] parts = path.split("\\.");
        Object res = null;
        for (int i = 0; i < parts.length; i++) {
            if (o == null) {
                return res;
            }
            try {
                res = o.getFieldValue(parts[i]);
            } catch (IllegalAccessException e) {
                throw new ServiceException("Could not read object value.", e);
            }
            if (i + 1 < parts.length && res instanceof FastPathObject) {
                o = (FastPathObject) res;
            }
        }
        return res;
    }
}
