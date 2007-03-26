package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.model.InterMineObject;

/**
 * Interface providing method to look up equivalent objects by primary key in a production
 * objectstore.
 *
 * @author Matthew Wakeling
 */

public interface EquivalentObjectFetcher
{
    /**
     * Returns a Set of objects that are equivalent to the given object, according to the primary
     * keys defined by the given Source.
     *
     * @param obj the Object to look for
     * @param source the data Source
     * @return a Set of InterMineObjects
     * @throws ObjectStoreException if an error occurs
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
        throws ObjectStoreException;
}
