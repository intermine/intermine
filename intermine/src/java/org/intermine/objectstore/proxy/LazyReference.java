package org.flymine.objectstore.proxy;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.fql.FqlQuery;

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
     * Return the query that will be used to retrieve the real object
     * @return the query
     */
    public FqlQuery getFqlQuery();

    /**
     * Get the type of the real object
     * @return the type
     */
    public Class getType();

    /**
     * Get the internal id of the real object
     * @return the id
     */
    public Integer getId();

    /**
     * Set the objectstore used to run the proxy's embedded query
     *
     * @param os the ObjectStore
     */
    public void setObjectStore(ObjectStore os);

    /**
     * Check whether the proxy has been materialised
     *
     * @return true if the proxy has been materialised
     */
    public boolean isMaterialised();
}
