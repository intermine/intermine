package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.Iterator;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SingletonResults;

import org.apache.log4j.Logger;

/**
 * Loads information from an ObjectStore into the Flymine database.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreDataLoader extends DataLoader
{
    protected static final Logger LOG = Logger.getLogger(ObjectStoreDataLoader.class);

    /**
     * Construct an ObjectStoreDataLoader
     * 
     * @param iw an IntegrationWriter to which to write
     */
    public ObjectStoreDataLoader(IntegrationWriter iw) {
        this.iw = iw;
    }

    /**
     * Performs the loading operation, reading data from the given ObjectStore, which must use the
     * same model as the destination IntegrationWriter.
     *
     * @param os the ObjectStore from which to read data
     * @param source the main Source
     * @param skelSource the skeleton Source
     * @throws ObjectStoreException if an error occurs on either the source or the destination
     */
    public void process(ObjectStore os, Source source, Source skelSource)
        throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        int opCount = 0;
        long time = (new Date()).getTime();
        iw.beginTransaction();
        Iterator iter = new SingletonResults(q, os, os.getSequence()).iterator();
        while (iter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) iter.next();
            iw.store(obj, source, skelSource);
            opCount++;
            if (opCount % 1000 == 0) {
                long now = (new Date()).getTime();
                LOG.error("Dataloaded " + opCount + " objects - running at "
                        + (60000000 / (now - time)) + " objects per minute");
                LOG.error("Now on " + obj.getClass().getName());
                time = now;
                iw.commitTransaction();
                iw.beginTransaction();
            }
        }
        iw.commitTransaction();
    }
}
