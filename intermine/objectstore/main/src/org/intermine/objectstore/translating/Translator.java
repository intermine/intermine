package org.intermine.objectstore.translating;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.MetaDataException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;

/**
 * Interface specifying operations required for inline translation of queries and data objects
 * by a translating ObjectStore
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public abstract class Translator
{
    protected ObjectStore os;

    /**
     * Set this Translator's ObjectStore
     * @param os the ObjectStore
     */
     public void setObjectStore(ObjectStore os) {
         this.os = os;
     }

    /**
     * Translate a query
     * @param query the Query to translate
     * @return the translated query
     * @throws ObjectStoreException if the query cannot be translated
     */
    public abstract Query translateQuery(Query query) throws ObjectStoreException;

    /**
    * Translate an object entering the ObjectStore
    * @param o the Object to translate
    * @return the translated object
    */
    public abstract Object translateToDbObject(Object o);

    /**
     * Translate an object exiting the ObjectStore
     * @param o the object to translate
     * @return the translated object
     * @throws MetaDataException if item has a field that isn't in InterMine model
     */
    public abstract Object translateFromDbObject(Object o)
        throws MetaDataException;
}
