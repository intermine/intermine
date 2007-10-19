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

import java.util.Iterator;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.fastcollections.ObjectStoreFastCollectionsForTranslatorImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

import org.apache.log4j.Logger;

/**
 * Loads information from an ObjectStore into the InterMine database.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreDataLoader extends DataLoader
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreDataLoader.class);

    /**
     * Construct an ObjectStoreDataLoader
     *
     * @param iw an IntegrationWriter to which to write
     */
    public ObjectStoreDataLoader(IntegrationWriter iw) {
        super(iw);
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
        process(os, source, skelSource, InterMineObject.class);
    }


    /**
     * Loads only a specified class reading data from the given ObjectStore, which must use the
     * same model as the destination IntegrationWriter.  If the class is InterMineObject then
     * all data will be loaded.
     *
     * @param os the ObjectStore from which to read data
     * @param source the main Source
     * @param skelSource the skeleton Source
     * @param queryClass the class to load data for
     * @throws ObjectStoreException if an error occurs on either the source or the destination
     */
    public void process(ObjectStore os, Source source, Source skelSource, Class queryClass)
        throws ObjectStoreException {
        ObjectStore origOs = os;
        try {
            if (os instanceof ObjectStoreFastCollectionsForTranslatorImpl) {
                ((ObjectStoreFastCollectionsForTranslatorImpl) os).setSource(source);
            }
            if (getIntegrationWriter() instanceof IntegrationWriterDataTrackingImpl) {
                BatchingFetcher eof = new BatchingFetcher(((IntegrationWriterAbstractImpl)
                            getIntegrationWriter()).getBaseEof(),
                        ((IntegrationWriterDataTrackingImpl) getIntegrationWriter())
                        .getDataTracker(), source);
                ((IntegrationWriterAbstractImpl) getIntegrationWriter()).setEof(eof);
                os = eof.getNoseyObjectStore(os);
            }
            long times[] = new long[20];
            for (int i = 0; i < 20; i++) {
                times[i] = -1;
            }
            Query q = new Query();
            QueryClass qc = new QueryClass(queryClass);
            q.addFrom(qc);
            q.addToSelect(qc);
            q.setDistinct(false);
            long opCount = 0;
            long time = System.currentTimeMillis();
            long startTime = time;
            long timeSpentRead = 0;
            long timeSpentWrite = 0;
            long timeSpentCommit = 0;
            long timeSpentLoop = 0;
            getIntegrationWriter().beginTransaction();
            SingletonResults res = os.executeSingleton(q);
            res.setNoOptimise();
            res.setNoExplain();
            res.setBatchSize(1000);
            Iterator iter = res.iterator();
            long time4 = System.currentTimeMillis();
            long time1, time2, time3;
            while (iter.hasNext()) {
                time1 = System.currentTimeMillis();
                timeSpentLoop += time1 - time4;
                Object obj = iter.next();
                time2 = System.currentTimeMillis();
                timeSpentRead += time2 - time1;
                //if (obj.getClass().getName().equals("org.intermine.model.chado.feature")) {
                //    String objText = obj.toString();
                //    int objTextLen = objText.length();
                //    System//.out.println("Storing " + objText.substring(0, (objTextLen > 60 ? 60
                //                    : objTextLen)));
                //}
                getIntegrationWriter().store(obj, source, skelSource);
                time3 = System.currentTimeMillis();
                timeSpentWrite += time3 - time2;
                opCount++;
                if (opCount % 1000 == 0) {
                    long now = System.currentTimeMillis();
                    if (times[(int) ((opCount / 1000) % 20)] == -1) {
                        LOG.info("Dataloaded " + opCount + " objects - running at "
                                + (60000000 / (now - time)) + " (avg "
                                + ((60000L * opCount) / (now - startTime))
                                + ") objects per minute -- now on "
                                + DynamicUtil.getFriendlyName(obj.getClass()));
                    } else {
                        LOG.info("Dataloaded " + opCount + " objects - running at "
                                + (60000000 / (now - time)) + " (20000 avg "
                                + (1200000000 / (now - times[(int) ((opCount / 1000) % 20)]))
                                + ") (avg = " + ((60000L * opCount) / (now - startTime))
                                + ") objects per minute -- now on "
                                + DynamicUtil.getFriendlyName(obj.getClass()));
                    }
                    time = now;
                    times[(int) ((opCount / 1000) % 20)] = now;
                    if (opCount % 500000 == 0) {
                        getIntegrationWriter().batchCommitTransaction();
                    }
                }
                time4 = System.currentTimeMillis();
                timeSpentCommit += time4 - time3;
            }
            time3 = System.currentTimeMillis();
            getIntegrationWriter().commitTransaction();
            getIntegrationWriter().close();
            long now = System.currentTimeMillis();
            timeSpentCommit += now - time3;
            LOG.info("Finished dataloading " + opCount + " objects at " + ((60000L * opCount)
                        / (now - startTime)) + " objects per minute (" + (now - startTime)
                    + " ms total) for source " + source.getName());
            LOG.info("Time spent: Reading: " + (timeSpentRead + timeSpentLoop) + ", Writing: "
                    + timeSpentWrite + ", Committing: " + timeSpentCommit);
        } catch (RuntimeException e) {
            if (origOs instanceof ObjectStoreFastCollectionsForTranslatorImpl) {
                LOG.error("Exception while dataloading - doneAlreadyMap = "
                        + ((ObjectStoreFastCollectionsForTranslatorImpl) origOs).getDoneAlready(),
                        e);
            }
            throw e;
        }
    }
}
