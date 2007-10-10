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

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Define methods needed to deal with integration when writing to an ObjectStore.  To retain
 * O/R mapping independence implementations of this interface should delegate writing to
 * a mapping tool specific implementation of ObjectStoreWriter.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */

public interface IntegrationWriter extends ObjectStoreWriter
{
    /**
     * Stores the given object in the objectstore. To update an objectstore with data in the form
     * of an interconnected graph of objects, call this method on each of the objects in that
     * structure. This method will take care of merging objects, and resolving field priority
     * issues.
     *
     * @param o the object to store
     * @param source the data Source to which to attribute the data
     * @param skelSource the data Source to which to attribute skeleton data
     * @throws ObjectStoreException if an error occurs in the underlying objectstore
     */
    public void store(Object o, Source source, Source skelSource)
        throws ObjectStoreException;

    /**
     * Converts a string describing the data source into a Source object suitable for passing to the
     * store method as the main source.
     *
     * @param name the name of the data source
     * @return a Source
     * @throws ObjectStoreException if something goes wrong
     */
    public Source getMainSource(String name) throws ObjectStoreException;

    /**
     * Converts a string describing the data source into a Source object suitable for passing to the
     * store method as the skeleton source.
     *
     * @param name the name of the data source
     * @return a skeleton Source
     * @throws ObjectStoreException if something goes wrong
     */
    public Source getSkeletonSource(String name) throws ObjectStoreException;


    /**
     * Tell this IntegrationWriter whether to ignore duplicate objects from the same source.
     * ALL DUPLICATES OF THE OBJECT MUST HAVE THE SAME FIELDS FILLED IN WITH THE SAME DATA.
     * Data that differs between copies will result in undefined behaviour as so what data will
     * appear in the destination database. Data that differs in primary keys may result in
     * an exception being thrown during data loading. Note that setting a field to null differs
     * from another copy with the field set to a value.
     * 
     * @param ignoreDuplicates the value of ignoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates);
}
