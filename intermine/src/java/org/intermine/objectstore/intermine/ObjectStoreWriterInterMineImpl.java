package org.flymine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreWriter;

import org.apache.log4j.Logger;

/**
 * An SQL-backed implementation of the ObjectStoreWriter interface, backed by
 * ObjectStoreFlyMineImpl.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreWriterFlyMineImpl extends ObjectStoreFlyMineImpl
    implements ObjectStoreWriter
{
    protected static final Logger LOG = Logger.getLogger(ObjectStoreWriterFlyMineImpl.class);
    protected Connection conn = null;
    protected boolean connInUse = false;
    protected ObjectStoreFlyMineImpl os;

    /**
     * Constructor for this ObjectStoreWriter. This ObjectStoreWriter is bound to a single SQL
     * Connection, grabbed from the provided ObjectStore.
     *
     * @param os an ObjectStoreFlyMineImpl
     */
    public ObjectStoreWriterFlyMineImpl(ObjectStore os) throws ObjectStoreException {
        super(null, os.getModel());
        this.os = (ObjectStoreFlyMineImpl) os;
        everOptimise = false;
        try {
            conn = this.os.getConnection();
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not obtain connection to database", e);
        }
    }

    /**
     * @see ObjectStoreFlyMineImpl#getConnection
     */
    protected Connection getConnection() throws SQLException {
        synchronized(conn) {
            while (connInUse) {
                LOG.error("Connection in use - entering wait");
                try {
                    conn.wait(100L);
                } catch (InterruptedException e) {
                }
                LOG.error("Notified - leaving wait");
            }
            connInUse = true;
            LOG.error("getConnection returning connection");
            return conn;
        }
    }

    /**
     * @see ObjectStoreFlyMineImpl#releaseConnection
     */
    protected void releaseConnection(Connection c) {
        if (c == conn) {
            synchronized(conn) {
                connInUse = false;
                LOG.error("Released connection - notifying");
                conn.notify();
            }
        } else {
            LOG.error("Attempt made to release the wrong connection");
        }
    }

    /**
     * Overrides Object.finalize - release the connection back to the objectstore.
     */
    public void finalize() {
        close();
    }

    /**
     * @see ObjectStoreWriter#close
     */
    public void close() {
        try {
           if (isInTransaction()) {
               abortTransaction();
               LOG.error("ObjectStoreWriterFlyMineImpl closed in unfinished transaction"
                       + " - transaction aborted");
           }
           os.releaseConnection(conn);
           conn = null;
           connInUse = true;
        } catch (Exception e) {
        }
    }

    /**
     * @see ObjectStoreWriter#getObjectStore
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * @see ObjectStoreWriter#store
     */
    public void store(FlyMineBusinessObject o) throws ObjectStoreException {
        // TODO:
    }

    /**
     * @see ObjectStoreWriter#delete
     */
    public void delete(FlyMineBusinessObject o) throws ObjectStoreException {
        // TODO:
    }

    /**
     * @see ObjectStoreWriter#isInTransaction
     */
    public boolean isInTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return ! c.getAutoCommit();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error finding transaction status", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStoreWriter#beginTransaction
     */
    public void beginTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            if (! c.getAutoCommit()) {
                throw new ObjectStoreException("beginTransaction called, but already in"
                        + " transaction");
            }
            c.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error beginning transaction", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStoreWriter#commitTransaction
     */
    public void commitTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            if (c.getAutoCommit()) {
                throw new ObjectStoreException("commitTransaction called, but not in transaction");
            }
            c.commit();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error committing transaction", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStoreWriter#abortTransaction
     */
    public void abortTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            if (c.getAutoCommit()) {
                throw new ObjectStoreException("abortTransaction called, but not in transaction");
            }
            c.rollback();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error aborting transaction", e);
        } finally {
            releaseConnection(c);
        }
    }
}
