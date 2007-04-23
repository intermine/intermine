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
import java.util.Collections;
import java.util.Properties;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.model.InterMineObject;


/**
 * An IntegrationWriter designed to speed up loading of a first data source.  Used
 * with DataTrackerFirstSource.  If there loading the first source there are no
 * objects in the target database so we don't need to run queries for equivalent
 * objects.  We write to the DataTracker as normal but do not perform reads.
 *
 * @author Richard Smith
 * @author Wenyan Ji
 */
public class IntegrationWriterFirstSourceImpl extends IntegrationWriterDataTrackingImpl
{

    /**
     * Creates a new instance of this class, given the properties defining it.
     *
     * @param osAlias the alias of this objectstore
     * @param props the Properties
     * @return an instance of this class
     * @throws ObjectStoreException sometimes
     */
    public static IntegrationWriterDataTrackingImpl getInstance(String osAlias, Properties props)
            throws ObjectStoreException {
        return (IntegrationWriterFirstSourceImpl) IntegrationWriterDataTrackingImpl.getInstance(
            osAlias, props, IntegrationWriterFirstSourceImpl.class, DataTrackerFirstSource.class);
    }

    /**
     * Constructs a new instance of IntegrationWriterFirstSourceImpl.
     *
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     * @param dataTracker an instance of DataTracker, which we can use to store data tracking
     * information
     */
    public IntegrationWriterFirstSourceImpl(ObjectStoreWriter osw, DataTracker dataTracker) {
        super(osw, dataTracker);
    }

    /**
     * {@inheritDoc}
     */
    protected InterMineObject store(InterMineObject o, Source source, Source skelSource,
            int type) throws ObjectStoreException {
        ((DataTrackerFirstSource) dataTracker).setSkelSource(getSkeletonSource(
                                                                  skelSource.getName()));
        return super.store(o, source, skelSource, type);
    }

    /**
     * Override IntegrationWriterAbstractImpl method to find equivalent objects, now
     * hardwired to return an empty set.  We are assuming that this is the first source
     * so no data exists in the target database, therefore can save running queries.
     *
     * @param obj the Object to look for
     * @param source the data Source
     * @return an empty set
     * @throws ObjectStoreException if an error occurs
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
        throws ObjectStoreException {
        return Collections.EMPTY_SET;
    }


}
