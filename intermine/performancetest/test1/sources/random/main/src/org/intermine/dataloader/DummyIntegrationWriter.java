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

import java.util.Properties;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;

/**
 * A dummy IntegrationWriter - just throws away the data. Used for performance tests.
 *
 * @author Matthew Wakeling
 */
public class DummyIntegrationWriter extends IntegrationWriterDataTrackingImpl
{
    public static IntegrationWriterDataTrackingImpl getInstance(String osAlias, Properties props)
            throws ObjectStoreException {
        return (DummyIntegrationWriter) IntegrationWriterDataTrackingImpl.getInstance(
            osAlias, props, DummyIntegrationWriter.class, DataTrackerFirstSource.class);
    }

    public DummyIntegrationWriter(ObjectStoreWriter osw, DataTracker dataTracker) {
        super(osw, dataTracker);
    }

    protected InterMineObject store(InterMineObject o, Source source, Source skelSource,
            int type) throws ObjectStoreException {
        return null;
    }
}
