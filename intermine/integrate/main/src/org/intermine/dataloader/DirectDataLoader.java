package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

/**
 * A DataLoader with helper methods for creating and storing objects using an IntegrationWriter.
 *
 * @author Kim Rutherford
 */

public class DirectDataLoader extends DataLoader
{
    private static final Logger LOG = Logger.getLogger(DirectDataLoader.class);
    private int idCounter = 0;
    private int storeCount = 0;
    private long startTime;
    private long stepTime;
    private String sourceName;
    private String sourceType;

    private static final int LOG_FREQUENCY = 100000;
    private static final int COMMIT_FREQUENCY = 500000;

    /**
     * Create a new DirectDataLoader using the given IntegrationWriter and source name.
     * @param iw an IntegrationWriter
     * @param sourceName the source name
     * @param sourceType the source type
     */
    public DirectDataLoader (IntegrationWriter iw, String sourceName, String sourceType) {
        super(iw);
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.startTime = System.currentTimeMillis();
        this.stepTime = startTime;
    }


    /**
     * Store an object using the IntegrationWriter.
     * @param o the InterMineObject
     * @throws ObjectStoreException if there is a problem in the IntegrationWriter
     */
    public void store(FastPathObject o) throws ObjectStoreException {
        Source source = getIntegrationWriter().getMainSource(sourceName, sourceType);
        Source skelSource = getIntegrationWriter().getSkeletonSource(sourceName, sourceType);

        getIntegrationWriter().store(o, source, skelSource);
        storeCount++;
        if (storeCount % LOG_FREQUENCY == 0) {
            long now = System.currentTimeMillis();
            LOG.info("Dataloaded " + storeCount + " objects - running at "
                    + ((60000L * LOG_FREQUENCY) / (now - stepTime)) + " (avg "
                    + ((60000L * storeCount) / (now - startTime))
                    + ") objects per minute -- now on "
                    + Util.getFriendlyName(o.getClass()));
            stepTime = now;
        }
        if (storeCount % COMMIT_FREQUENCY == 0) {
            LOG.info("Committing transaction after storing " + storeCount + " objects.");
            getIntegrationWriter().batchCommitTransaction();
        }
    }

    /**
     * Close the DirectDataLoader, this just prints a final log message with loading stats.
     */
    public void close() {
        long now = System.currentTimeMillis();
        LOG.info("Finished dataloading " + storeCount + " objects at " + ((60000L * storeCount)
                / (now - startTime)) + " objects per minute (" + (now - startTime)
            + " ms total) for source " + sourceName);
    }
    /**
     * Create a new object of the given class name and give it a unique ID.
     * @param className the class name
     * @return the new InterMineObject
     * @throws C o.setId(new Integer(idCounter));lassNotFoundException if the given class doesn't exist
     */
    @SuppressWarnings("unchecked")
    public InterMineObject createObject(String className) throws ClassNotFoundException {
        return createObject((Class<? extends InterMineObject>) Class.forName(className));
    }

    /**
     * Create a new object of the given class and give it a unique ID.
     * @param c the class
     * @param <C> the type of the class
     * @return the new InterMineObject
     */
    public <C extends InterMineObject> C createObject(Class<C> c) {
        C o = DynamicUtil.simpleCreateObject(c);
        o.setId(new Integer(idCounter));
        idCounter++;
        return o;
    }

    public <C extends FastPathObject> C createSimpleObject(Class<C> c) {
        C o = DynamicUtil.simpleCreateObject(c);
        return o;
    }

}
