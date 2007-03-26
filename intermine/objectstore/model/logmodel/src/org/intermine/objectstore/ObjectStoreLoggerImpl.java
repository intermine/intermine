package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;
import org.intermine.log.InterMineLogger;
import org.intermine.model.logmodel.*;
import org.apache.log4j.Logger;

/**
 * @author Peter Mclaren
 * */
public class ObjectStoreLoggerImpl implements InterMineLogger
{

    private static final Logger LOG = Logger.getLogger(ObjectStoreLoggerImpl.class);

    private ObjectStoreWriter osw;


    /**
     * Constructor for this ObjectStoreWriter. This ObjectStoreWriter is bound to a single SQL
     * Connection, grabbed from the provided ObjectStore.
     *
     * @param osw A suitable writer that we can use to store our logging events with.
     */
    protected ObjectStoreLoggerImpl(ObjectStoreWriter osw) {

        this.osw = osw;
    }

    /**
     * Standard create instance style method.
     * @param alias The alias of the object store that this logger can use to record log statements.
     * @return ObjectStoreLoggerImpl an instance of the InterMineLogger interface
     * */
    public static ObjectStoreLoggerImpl getInstance(String alias) {

        ObjectStoreWriter oswInternal;
        try {
            oswInternal = ObjectStoreWriterFactory.getObjectStoreWriter(alias);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get ObjectStoreWriter for ObjectStoreLoggerImpl");
        }

        return new ObjectStoreLoggerImpl(oswInternal);
    }

    /** {@inheritDoc} */
    public void logMessage(String caller, String message) {

        LOG.debug("ObjectStoreLoggerImpl: caller:" + caller + " left a message:" + message);

        InterMineLoggable loggable = new InterMineLoggable();
        loggable.setCaller(caller);
        loggable.setMessage(message);
        loggable.setTimestamp(new Long(System.currentTimeMillis()));

        try {
            osw.beginTransaction();
            osw.store(loggable);
            osw.commitTransaction();
        } catch (ObjectStoreException ose) {
            LOG.error("Had a problem while trying to store a loggable message", ose);
        }
    }

    /** {@inheritDoc} */
    public void logQuery(
           String caller, String initiator, Query oql, String sql,
           Long optimise, Long estimated, Long execute, Long acceptable, Long conversion) {

        LoggableQuery query = new LoggableQuery();
        query.setCaller(caller);
        query.setInitiator(initiator);
        query.setQueryOQL(oql.toString());
        query.setQuerySQL(sql);
        query.setOptimise(optimise);
        query.setEstimated(estimated);
        query.setExecute(execute);
        query.setAcceptable(acceptable);
        query.setConversion(conversion);
        query.setTimestamp(new Long(System.currentTimeMillis()));
        query.setMessage("QUERY_LOG");

        try {
            osw.beginTransaction();
            osw.store(query);
            osw.commitTransaction();
        } catch (ObjectStoreException ose) {
            LOG.error("Had a problem while trying to store a loggable query", ose);
        }
    }

}
