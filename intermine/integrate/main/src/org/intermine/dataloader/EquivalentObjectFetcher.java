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

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;

/**
 * Interface providing methods to look up equivalent objects by primary key in a production
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

    /**
     * Generates a query that searches for all objects in the database equivalent to a given
     * example object according to the primary keys defined for the given source.
     *
     * @param obj the Object to take as an example
     * @param source the Source database
     * @param queryNulls if true allow primary keys to contain null values if the template obj has
     * nulls.  If false the Query will constrain only those keys that have a value in the template
     * obj
     * @return a new Query (or null if all the primary keys from obj contain a null)
     * @throws MetaDataException if anything goes wrong
     */
    public Query createPKQuery(InterMineObject obj, Source source, boolean queryNulls)
    throws MetaDataException;

    /**
     * Generates a query that searches for all objects in the database equivalent to a given
     * example object, considering only one of it's classes.
     *
     * @param obj the Object to take as an example
     * @param source the Source database
     * @param queryNulls if true allow primary keys to contain null values if the template obj has
     * nulls.  If false the Query will constrain only those keys that have a value in the template
     * obj
     * @param cld one of the classes that obj is.  Only primary keys for this classes will be
     * considered
     * @return a new Query (or null if all the primary keys from obj contain a null)
     * @throws MetaDataException if anything goes wrong
     */
    public Set createPKQueriesForClass(InterMineObject obj, Source source, boolean queryNulls,
            ClassDescriptor cld) throws MetaDataException;
}
