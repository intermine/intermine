package org.flymine.objectstore.proxy;

import org.flymine.objectstore.ObjectStore;

/**
 * LazyReference
 * This the interface a proxy implements so that its objectstore can be set by the
 * results object before being returned to the client.
 *
 * @author Mark Woodbridge
 */
public interface LazyReference
{
    /**
     * set the objectstore used to run the proxy's embedded query
     *
     * @param os the ObjectStore
     */
    public void setObjectStore(ObjectStore os);
}
