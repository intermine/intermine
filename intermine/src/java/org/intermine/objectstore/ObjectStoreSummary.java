package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashMap;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryClass;

/**
 * A summary of an ObjectStore.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreSummary
{
    private final ObjectStore os;
    private final Map counts = new HashMap();

    /**
     * Create a new ObjectStoreSummary object.
     * @param os the ObjectStore to summarise.
     */
    public ObjectStoreSummary (ObjectStore os) {
        this.os = os;
    }

    /**
     * Get the number of instances of a particular class in the ObjectStore.
     * @param className the class name to look up
     * @throws ObjectStoreException if an error occurs during the database query
     * @throws ClassNotFoundException if the className doesn't refer to a known class
     * @return the count of the instances of the class
     */
    public int getClassCount(String className)
        throws ObjectStoreException, ClassNotFoundException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Class.forName(className));
        q.addToSelect(new QueryField(qc, "id"));
        q.addFrom(qc);

        if (counts.get(className) != null) {
            return ((Integer) counts.get(className)).intValue();
        } else {
            int count = os.count(q, os.getSequence());
            counts.put(className, new Integer(count));
            return count;
        }
    }
}
