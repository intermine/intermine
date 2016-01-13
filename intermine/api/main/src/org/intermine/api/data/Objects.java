package org.intermine.api.data;

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
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Helper class to encapsulate some common logic regarding the use of data objects.
 * @author Alex Kalderimis
 *
 */
public final class Objects
{

    private Objects() {
        // Hidden.
    }

    private static final Logger LOG = Logger.getLogger(Objects.class);

    /**
     * Resolve a set of ids to mapping from ID to object.
     * @param im The InterMine state object.
     * @param objectIds The ids we want to resolve.
     * @return The mapping.
     * @throws ObjectStoreException If we can't run the required query.
     */
    public static Map<Integer, InterMineObject> getObjects(InterMineAPI im, Set<Integer> objectIds)
        throws ObjectStoreException {
        long time = System.currentTimeMillis();
        Map<Integer, InterMineObject> objMap = new HashMap<Integer, InterMineObject>();
        for (InterMineObject obj : im.getObjectStore().getObjectsByIds(objectIds)) {
            objMap.put(obj.getId(), obj);
        }
        LOG.debug("Getting objects took " + (System.currentTimeMillis() - time) + " ms");
        return objMap;
    }
}
