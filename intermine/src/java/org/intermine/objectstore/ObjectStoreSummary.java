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
import java.util.Properties;
import java.util.Iterator;

/**
 * A summary of an ObjectStore.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreSummary
{
    private final Map classCounts = new HashMap();

    static final String CLASS_COUNTS_SUFFIX = ".classCount";

    /**
     * Create a new ObjectStoreSummary object.
     * @param properties the Properties object to retrieve the summary from
     */
    public ObjectStoreSummary(Properties properties) {
        Iterator keyIterator = properties.keySet().iterator();
        while (keyIterator.hasNext()) {
            String key = (String) keyIterator.next();

            if (key.endsWith(CLASS_COUNTS_SUFFIX)) {
                String className = key.substring(0, key.length() - CLASS_COUNTS_SUFFIX.length());
                Integer count = Integer.valueOf((String) properties.get(key));
                classCounts.put(className, count);
            }
        }
    }

    /**
     * Get the number of instances of a particular class in the ObjectStore.
     * @param className the class name to look up
     * @return the count of the instances of the class
     * @throws ObjectStoreException if there is a problem with the ObjectStore
     * @throws ClassNotFoundException if className is unknown
     */
    public int getClassCount(String className)
        throws ObjectStoreException, ClassNotFoundException {

        return ((Integer) classCounts.get(className)).intValue();
    }
}
