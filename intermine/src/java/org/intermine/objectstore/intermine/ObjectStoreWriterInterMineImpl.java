package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriter;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.CacheMap;
import org.intermine.util.DatabaseUtil;
import org.intermine.util.TypeUtil;
import org.intermine.xml.lite.LiteParser;
import org.intermine.xml.lite.LiteRenderer;

import org.apache.log4j.Logger;

/**
 * An SQL-backed implementation of the ObjectStoreWriter interface, backed by
 * ObjectStoreInterMineImpl.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreWriterInterMineImpl extends ObjectStoreInterMineImpl
    implements ObjectStoreWriter
{
    protected static final Logger LOG = Logger.getLogger(ObjectStoreWriterInterMineImpl.class);
    protected Connection conn = null;
    protected boolean connInUse = false;
    protected ObjectStoreInterMineImpl os;
    protected int sequenceBase = 0;
    protected int sequenceOffset = SEQUENCE_MULTIPLE;
    protected Batch batch;
    protected String createSituation;
    protected Map recentSequences;

    protected static final int SEQUENCE_MULTIPLE = 100;
    protected static final int MAX_BATCH_CHARS = 10000000;
    /**
     * Constructor for this ObjectStoreWriter. This ObjectStoreWriter is bound to a single SQL
     * Connection, grabbed from the provided ObjectStore.
     *
     * @param os an ObjectStoreInterMineImpl
     * @throws ObjectStoreException if a problem occurs
     */
    public ObjectStoreWriterInterMineImpl(ObjectStore os) throws ObjectStoreException {
        super(null, os.getModel());
        this.os = (ObjectStoreInterMineImpl) os;
        db = this.os.db;
        everOptimise = false;
        try {
            conn = this.os.getConnection();
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not obtain connection to database", e);
        }
        this.os.writers.add(this);
        Runtime.getRuntime().addShutdownHook(new StatsShutdownHook(this));
        Exception e = new Exception();
        e.fillInStackTrace();
        StringWriter message = new StringWriter();
        PrintWriter pw = new PrintWriter(message);
        e.printStackTrace(pw);
        pw.close();
        createSituation = message.toString();
        int index = createSituation.indexOf("at junit.framework.TestCase.runBare");
        createSituation = (index < 0 ? createSituation : createSituation.substring(0, index));
        recentSequences = Collections.synchronizedMap(new CacheMap(getClass().getName()
                    + " with sequence = " + sequence + ", model = \"" + model.getName()
                    + "\" recentSequences cache"));
        batch = new Batch(new BatchWriterPostgresCopyImpl());
    }
    
    /**
     * Allows the changing of the BatchWriter that this ObjectStoreWriter uses.
     *
     * @param batchWriter the new BatchWriter - use BatchWriterSimpleImpl for writers likely to see
     * small batches, and optimised (eg BatchWriterPostgresCopyImpl) implementations for bulk-write
     * writers.
     * @throws ObjectStoreException if something goes wrong
     */
    public void setBatchWriter(BatchWriter batchWriter) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection(); // Must get connection - it is our concurrency control.
            batch.setBatchWriter(batchWriter);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStoreInterMineImpl#getConnection
     */
    public Connection getConnection() throws SQLException {
        synchronized (conn) {
            if (conn == null) {
                throw new SQLException("This ObjectStoreWriter is closed");
            }
            int loops = 0;
            while (connInUse) {
                /*Exception trace = new Exception();
                trace.fillInStackTrace();
                StringWriter message = new StringWriter();
                PrintWriter pw = new PrintWriter(message);
                trace.printStackTrace(pw);
                pw.flush();
                LOG.error("Connection in use - entering wait - " + message.toString());*/
                if (loops > 1000) {
                    LOG.error("Waited for connection for 100 seconds - probably a deadlock"
                            + " - throwing exception");
                    throw new SQLException("This ObjectStoreWriter appears to be dead due to"
                            + " deadlock");
                } else if (loops > 1) {
                    LOG.error("Waited for connection for " + 2 + " seconds - perhaps there's"
                            + " a deadlock");
                } else {
                    LOG.info("Connection in use - entering wait");
                }
                try {
                    conn.wait(1000L);
                } catch (InterruptedException e) {
                }
                LOG.info("Notified or timed out");
                loops++;
            }
            connInUse = true;
            /*
            Exception trace = new Exception();
            trace.fillInStackTrace();
            StringWriter message = new StringWriter();
            PrintWriter pw = new PrintWriter(message);
            trace.printStackTrace(pw);
            pw.flush();
            LOG.error("getConnection returning connection - " + message.toString());*/
            LOG.info("getConnection returning connection");
            return conn;
        }
    }

    /**
     * @see ObjectStoreInterMineImpl#releaseConnection
     */
    public void releaseConnection(Connection c) {
        if (c == conn) {
            synchronized (conn) {
                connInUse = false;
                LOG.info("Released connection - notifying");
                conn.notify();
            }
        } else if (c != null) {
            Exception trace = new Exception();
            trace.fillInStackTrace();
            StringWriter message = new StringWriter();
            PrintWriter pw = new PrintWriter(message);
            trace.printStackTrace(pw);
            pw.flush();
            LOG.error("Attempt made to release the wrong connection - " + message.toString());
        }
    }

    /**
     * Overrides Object.finalize - release the connection back to the objectstore.
     */
    public void finalize() {
        if (conn != null) {
            LOG.error("Garbage collecting open ObjectStoreWriterInterMineImpl with sequence = "
                    + sequence + " createSituation: " + createSituation);
            close();
        }
    }

    /**
     * @see ObjectStoreWriter#close
     */
    public void close() {
        LOG.error("Close called on ObjectStoreWriterInterMineImpl with sequence = " + sequence);
        try {
           if (isInTransaction()) {
               abortTransaction();
               LOG.error("ObjectStoreWriterInterMineImpl closed in unfinished transaction"
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
    public void store(InterMineObject o) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            storeWithConnection(c, o);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Performs store with a given Connection.
     *
     * @param c the Connection
     * @param o the object to store
     * @throws ObjectStoreException sometimes
     */
    protected void storeWithConnection(Connection c,
            InterMineObject o) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        boolean doDeletes = true;
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            // Make sure this object has an ID
            if (o.getId() == null) {
                o.setId(getSerialWithConnection(c));
                doDeletes = false;
            } else {
                doDeletes = !recentSequences.containsKey(o.getId());
            }
            recentSequences.remove(o.getId());

            // Make sure all objects pointed to have IDs
            Map fieldInfos = TypeUtil.getFieldInfos(o.getClass());
            Iterator fieldIter = fieldInfos.entrySet().iterator();
            while (fieldIter.hasNext()) {
                Map.Entry fieldEntry = (Map.Entry) fieldIter.next();
                TypeUtil.FieldInfo fieldInfo = (TypeUtil.FieldInfo) fieldEntry.getValue();
                if (InterMineObject.class.isAssignableFrom(fieldInfo.getType())) {
                    InterMineObject obj = (InterMineObject) TypeUtil.getFieldProxy(o,
                            fieldInfo.getName());
                    if ((obj != null) && (obj.getId() == null)) {
                        obj.setId(getSerialWithConnection(c));
                    }
                } else if (Collection.class.isAssignableFrom(fieldInfo.getType())) {
                    Collection coll = (Collection) TypeUtil.getFieldValue(o, fieldInfo.getName());
                    if (!(coll instanceof Results)) {
                        Iterator collIter = coll.iterator();
                        while (collIter.hasNext()) {
                            InterMineObject obj = (InterMineObject) collIter.next();
                            if (obj.getId() == null) {
                                obj.setId(getSerialWithConnection(c));
                            }
                        }
                    }
                }
            }
            String xml = LiteRenderer.render(o, model);
            Set classDescriptors = model.getClassDescriptorsForClass(o.getClass());

            Iterator cldIter = classDescriptors.iterator();
            while (cldIter.hasNext()) {
                ClassDescriptor cld = (ClassDescriptor) cldIter.next();
                String tableName = DatabaseUtil.getTableName(cld);
                if (doDeletes) {
                    batch.deleteRow(c, tableName, "id", o.getId());
                }
                int colCount = 1;
                fieldIter = cld.getAllFieldDescriptors().iterator();
                while (fieldIter.hasNext()) {
                    FieldDescriptor field = (FieldDescriptor) fieldIter.next();
                    if (!(field instanceof CollectionDescriptor)) {
                        colCount++;
                    }
                }
                String colNames[] = new String[colCount];
                Object values[] = new Object[colCount];
                colNames[0] = "OBJECT";
                values[0] = xml;
                int colNo = 1;
                fieldIter = cld.getAllFieldDescriptors().iterator();
                while (fieldIter.hasNext()) {
                    FieldDescriptor field = (FieldDescriptor) fieldIter.next();
                    if (field instanceof CollectionDescriptor) {
                        Collection coll = (Collection) TypeUtil.getFieldValue(o, field.getName());
                        if (!((coll instanceof Results)
                                    && (((Results) coll).getObjectStore().equals(this)))) {
                            CollectionDescriptor collection = (CollectionDescriptor) field;
                            // Collection - if it's many to many, then write indirection table.
                            if (field.relationType() == FieldDescriptor.M_N_RELATION) {
                                String indirectTableName =
                                    DatabaseUtil.getIndirectionTableName(collection);
                                String inwardColumnName =
                                    DatabaseUtil.getInwardIndirectionColumnName(collection);
                                String outwardColumnName =
                                    DatabaseUtil.getOutwardIndirectionColumnName(collection);
                                boolean swap = (inwardColumnName.compareTo(outwardColumnName) > 0);
                                String indirColNames[] = new String[2];
                                indirColNames[0] = (swap ? inwardColumnName : outwardColumnName);
                                indirColNames[1] = (swap ? outwardColumnName : inwardColumnName);
                                Iterator collIter = coll.iterator();
                                while (collIter.hasNext()) {
                                    InterMineObject inCollection = (InterMineObject)
                                        collIter.next();
                                    Object indirValues[] = new Object[2];
                                    indirValues[0] = (swap ? o.getId() : inCollection.getId());
                                    indirValues[1] = (swap ? inCollection.getId() : o.getId());
                                    batch.addRow(c, indirectTableName, null, indirColNames,
                                            indirValues);
                                }
                            }
                        }
                    } else {
                        colNames[colNo] = DatabaseUtil.getColumnName(field);
                        Object value = TypeUtil.getFieldProxy(o, field.getName());
                        if (value instanceof Date) {
                            value = new Long(((Date) value).getTime());
                        } else if (value instanceof InterMineObject) {
                            value = ((InterMineObject) value).getId();
                        }
                        values[colNo] = value;
                        colNo++;
                    }
                }
                batch.addRow(c, tableName, o.getId(), colNames, values);
            }

            try {
                InterMineObject toCache = LiteParser.parse(xml, this);
                cacheObjectById(toCache.getId(), toCache);
            } catch (Exception e) {
            }
        } catch (SQLException e) {
            throw new ObjectStoreException("Error while storing", e);
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException("Illegal access to value while storing", e);
        }

        if (!wasInTransaction) {
            commitTransactionWithConnection(c);
        }
    }

    /**
     * Gets an ID number which is unique in the database.
     *
     * @return an Integer
     * @throws SQLException if a problem occurs
     */
    public Integer getSerial() throws SQLException {
        Connection c = null;
        try {
            c = getConnection();
            return getSerialWithConnection(c);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Gets an ID number which is unique in the database, given a Connection.
     *
     * @param c the Connection
     * @return an Integer
     * @throws SQLException if a problem occurs
     */
    protected Integer getSerialWithConnection(Connection c) throws SQLException {
        if (sequenceOffset >= SEQUENCE_MULTIPLE) {
            sequenceOffset = 0;
            Statement s = c.createStatement();
            ResultSet r = s.executeQuery("SELECT nextval('serial');");
            //System//.out.println(getModel().getName()
            //        + ": Executed SQL: SELECT nextval('serial');");
            if (!r.next()) {
                throw new SQLException("No result while attempting to get a unique id");
            }
            long nextSequence = r.getLong(1);
            sequenceBase = (int) (nextSequence * SEQUENCE_MULTIPLE);
        }
        Integer retval = new Integer(sequenceBase + (sequenceOffset++));
        recentSequences.put(retval, retval);
        return retval;
    }

    /**
     * @see ObjectStoreWriter#delete
     */
    public void delete(InterMineObject o) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            deleteWithConnection(c, o);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Performs a delete, with a connection.
     *
     * @param c the Connection
     * @param o the object to delete
     * @throws ObjectStoreException sometimes
     */
    protected void deleteWithConnection(Connection c,
            InterMineObject o) throws ObjectStoreException {
        boolean wasInTransaction = isInTransactionWithConnection(c);
        if (!wasInTransaction) {
            beginTransactionWithConnection(c);
        }

        try {
            // Make sure this object has an ID
            if (o.getId() == null) {
                throw new IllegalArgumentException("Attempt to delete an object without an ID: "
                        + o.toString());
            }

            Set classDescriptors = model.getClassDescriptorsForClass(o.getClass());

            Iterator cldIter = classDescriptors.iterator();
            while (cldIter.hasNext()) {
                ClassDescriptor cld = (ClassDescriptor) cldIter.next();
                String tableName = DatabaseUtil.getTableName(cld);
                batch.deleteRow(c, tableName, "id", o.getId());
            }
            invalidateObjectById(o.getId());
        } catch (SQLException e) {
            throw new ObjectStoreException("Error while deleting", e);
        }

        if (!wasInTransaction) {
            commitTransactionWithConnection(c);
        }
    }

    /**
     * @see ObjectStoreWriter#isInTransaction
     */
    public boolean isInTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            return isInTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Finds if we are in a transaction.
     *
     * @param c the Connection
     * @return true or false
     * @throws ObjectStoreException sometimes
     */
    protected boolean isInTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            return !c.getAutoCommit();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error finding transaction status", e);
        }
    }

    /**
     * @see ObjectStoreWriter#beginTransaction
     */
    public void beginTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            beginTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Begins a transaction.
     *
     * @param c the Connection
     * @throws ObjectStoreException if we are already in a transaction
     */
    protected void beginTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            if (!c.getAutoCommit()) {
                throw new ObjectStoreException("beginTransaction called, but already in"
                        + " transaction");
            }
            c.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ObjectStoreException("Error beginning transaction", e);
        }
    }

    /**
     * @see ObjectStoreWriter#commitTransaction
     */
    public void commitTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            commitTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Commits a transaction.
     *
     * @param c the Connection
     * @throws ObjectStoreException if we are not in a transaction
     */
    protected void commitTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            batch.flush(c);
            if (c.getAutoCommit()) {
                throw new ObjectStoreException("commitTransaction called, but not in transaction");
            }
            c.commit();
            c.setAutoCommit(true);
            os.flushObjectById();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error committing transaction", e);
        }
    }

    /**
     * @see ObjectStoreWriter#abortTransaction
     */
    public void abortTransaction() throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            abortTransactionWithConnection(c);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Aborts a transaction.
     *
     * @param c the Connection
     * @throws ObjectStoreException if we are not in a transaction
     */
    public void abortTransactionWithConnection(Connection c) throws ObjectStoreException {
        try {
            batch.flush(c);
            if (c.getAutoCommit()) {
                throw new ObjectStoreException("abortTransaction called, but not in transaction");
            }
            c.rollback();
            c.setAutoCommit(true);
            os.flushObjectById();
        } catch (SQLException e) {
            throw new ObjectStoreException("Error aborting transaction", e);
        }
    }

    /**
     * @see ObjectStoreInterMineImpl#execute(Query, int, int, boolean, boolean, int)
     *
     * This method is overridden in order to flush batches properly before the read.
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            batch.flush(c);
            return executeWithConnection(c, q, start, limit, optimise, explain, sequence);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }
    
    /**
     * @see ObjectStoreInterMineImpl#count
     * 
     * This method is overridden in order to flush batches properly before the read.
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            batch.flush(c);
            return countWithConnection(c, q, sequence);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * @see ObjectStoreInterMineImpl#internalGetObjectById
     *
     * This method is overridden in order to flush matches properly before the read.
     */
    protected InterMineObject internalGetObjectById(Integer id) throws ObjectStoreException {
        Connection c = null;
        try {
            c = getConnection();
            batch.flush(c);
            return internalGetObjectByIdWithConnection(c, id);
        } catch (SQLException e) {
            throw new ObjectStoreException("Could not get connection to database", e);
        } finally {
            releaseConnection(c);
        }
    }

    /**
     * Called by the StatsShutdownHook on shutdown
     */
    public void shutdown() {
        if (conn != null) {
            LOG.error("Shutting down open ObjectStoreWriterInterMineImpl with sequence = "
                    + sequence + ", createSituation = " + createSituation);
            close();
        }
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return false;
    }
}
