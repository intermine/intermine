package org.intermine.objectstore.proxy;

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

/**
 * Class which uses an ObjectStore to perform lazy fetching of data
 *
 * @author Matthew Wakeling
 */
public interface Lazy
{
    /**
     * Returns the ObjectStore that this lazy object will use
     *
     * @return an ObjectStore
     */
    public ObjectStore getObjectStore();
}
