/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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

    /**
     * check whether the proxy has been materialised
     *
     * @return true if the proxy has been materialised
     */
    public boolean isMaterialised();
}
