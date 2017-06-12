package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreException;

/**
 * @author Alex
 */
public class BagNotFound extends ObjectStoreException
{
    private final String name;

    /**
     * @param name name of bag
     */
    public BagNotFound(String name) {
        super(String.format("A bag (%s) used by this query does not exist", name));
        this.name = name;
    }

    /**
     * @param name name of bag
     * @param t error
     */
    public BagNotFound(String name, Throwable t) {
        super(String.format("A bag (%s) used by this query does not exist", name), t);
        this.name = name;
    }

    /**
     * @return The name of the missing bag.
     */
    public String getName() {
        return name;
    }

}
