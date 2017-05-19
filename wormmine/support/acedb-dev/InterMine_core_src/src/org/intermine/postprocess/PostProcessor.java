package org.intermine.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Generic defn of a post process step...
 * @author Peter Mclaren
 * @author Richard Smith
 * */

public abstract class PostProcessor
{
    protected ObjectStoreWriter osw;

    /**
     * Typical constructor with a handle to the object store we are working on.
     *
     * @param osw The object store that the postprocessor should operate on.
     * */
    public PostProcessor(ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Return the ObjectStoreWriter that was passed to the constructor.
     * @return the ObjectStoreWriter
     */
    public ObjectStoreWriter getObjectStoreWriter () {
        return osw;
    }

    /**
     * All subclasses should override this method so they can be called in a generic fashion.
     *
     * @throws  ObjectStoreException if there is a problem with the object store.
     * */
    public abstract void postProcess() throws ObjectStoreException;

}
