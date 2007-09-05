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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.intermine.sql.Database;

import org.apache.log4j.Logger;

/**
 * Interface providing access to data tracking.
 *
 * This class is almost a generic map. However, it particularly maps from an Integer and a String
 * to a String and a boolean, where entries are grouped by the Integer.
 *
 * @author Matthew Wakeling
 */
public class DataTracker
{
    private static final Logger LOG = Logger.getLogger(DataTracker.class);

    /* We need a Map or two to store the entries. Each entry can be in several states:
     * 1. Recently-used and new - must be written to the database eventually.
     * 2. Recently-used and dirty - must be written back to the database eventually.
     * 3. Recently-used and clean - need not be written, but should be kept in memory.
     * 4. Not recently-used, and clean. These can be thrown away whenever the GC fancies it.
     *
     * This class will store types 1, 2 and 3 in a LinkedHashMap, ordered by access, and type 4 in a
     * WeakHashMap or CacheMap, or may not even store them at all.
     *
     * The LinkedHashMap has a threshold size. When it grows bigger than maxSize, a database write
     * occurs, which stores commitSize least-recently-used entries in the database, which then
     * become type 4.
     */
    private int maxSize;
    private int commitSize;
    private LinkedHashMap cache;
    private HashMap writeBack = new HashMap();
    private HashMap nameToSource = new HashMap();
    private HashMap sourceToName = new HashMap();
    private Connection conn;
    private Connection storeConn;
    private Connection prefetchConn;
    protected Exception broken = null;
    private CacheStorer cacheStorer;
    private int version = 0;
    // This reference is here so that the Database doesn't get garbage collected.
    @SuppressWarnings("unused") private Database db;

    private int ops = 0;
    private int misses = 0;
    private int batched = 0;
    private long timeSpentReading = 0;
    private long timeSpentPrefetching = 0;

    /**
     * Constructor for DataTracker.
     *
     * @param db a Database to back the tracker
     * @param maxSize maximum number of cache entries
     * @param commitSize number of entries to write to the database at a time
     */
    public DataTracker(Database db, int maxSize, int commitSize) {
        this.maxSize = maxSize;
        this.commitSize = commitSize;
        this.db = db;
        cache = new LinkedHashMap(maxSize * 14 / 10, 0.75F, true);
        try {
            conn = db.getConnection();
            conn.setAutoCommit(true);
            storeConn = db.getConnection();
            storeConn.setAutoCommit(false);
            prefetchConn = db.getConnection();
            prefetchConn.setAutoCommit(true);
            Statement s = conn.createStatement();
            try {
                s.executeQuery("SELECT * FROM tracker LIMIT 1");
            } catch (SQLException e2) {
                clear();
            }
            prefetchConn.createStatement().execute("SET enable_seqscan = off;");
        } catch (SQLException e) {
            IllegalArgumentException e2 = new IllegalArgumentException(
                    "Could not access SQL database");
            e2.initCause(e);
            throw e2;
        }
        cacheStorer = new CacheStorer();
        Thread cacheStorerThread = new Thread(cacheStorer, "DataTracker CacheStorer");
        cacheStorerThread.setDaemon(true);
        cacheStorerThread.start();
    }

    /**
     * Clears the data tracker of all entries. This method may only be called immediately after
     * construction.
     *
     * @throws SQLException sometimes
     */
    public void clear() throws SQLException {
        Statement s = conn.createStatement();
        try {
            s.executeQuery("drop table tracker");
        } catch (SQLException e) {
        }
        s = conn.createStatement();
        s.execute("create table tracker (objectid int, fieldname text, sourcename text,"
                + " version int)");
        s.execute("create index tracker_objectid on tracker (objectid);");
        conn.commit();
    }

    /**
     * Prefetches data for a specified set of object ids.
     *
     * @param ids a Set of Integers
     */
    public void prefetchIds(Set<Integer> ids) {
        long startTime = System.currentTimeMillis();
        Set<Integer> toFetch = new HashSet();
        synchronized (this) {
            if (broken != null) {
                IllegalArgumentException e = new IllegalArgumentException();
                e.initCause(broken);
                throw e;
            }
            for (Integer id : ids) {
                ObjectDescription desc = (ObjectDescription) cache.get(id);
                if (desc == null) {
                    desc = (ObjectDescription) writeBack.get(id);
                    cache.put(id, desc);
                }
                if (desc == null) {
                    toFetch.add(id);
                }
            }
        }
        Map<Integer, ObjectDescription> idsFetched = new HashMap();
        if (!toFetch.isEmpty()) {
            StringBuffer sql = new StringBuffer("SELECT objectid, fieldname, sourcename, version"
                    + " FROM tracker WHERE objectid IN (");
            boolean needComma = false;
            for (Integer id : toFetch) {
                if (needComma) {
                    sql.append(", ");
                }
                needComma = true;
                sql.append("" + id);
                idsFetched.put(id, new ObjectDescription());
            }
            sql.append(") ORDER BY version");
            try {
                Statement s = prefetchConn.createStatement();
                ResultSet r = s.executeQuery(sql.toString());
                while (r.next()) {
                    idsFetched.get(new Integer(r.getInt(1))).putClean(r.getString(2).intern(),
                            stringToSource(r.getString(3)));
                }
            } catch (SQLException e) {
                broken = e;
                IllegalArgumentException e2 = new IllegalArgumentException();
                e2.initCause(broken);
                throw e2;
            }
        }
        synchronized (this) {
            cache.putAll(idsFetched);
            maybePoke();
            batched += idsFetched.size();
        }
        timeSpentPrefetching += System.currentTimeMillis() - startTime;
    }

    /**
     * Retrieve the Source for a specified field of an Object stored in the database.
     *
     * @param id the ID of the object
     * @param field the name of the field
     * @return the Source
     */
    public synchronized Source getSource(Integer id, String field) {
        if (id == null) {
            throw new NullPointerException("id cannot be null");
        }
        if (broken != null) {
            IllegalArgumentException e = new IllegalArgumentException();
            e.initCause(broken);
            throw e;
        }
        ObjectDescription desc = getDesc(id, false);
        return desc.getSource(field);
    }

    /**
     * Gets the object descriptor for a given object id
     *
     * @param id the ID
     * @param forWrite true if the returned value is going to be modified
     * @return an ObjectDescriptor
     */
    private ObjectDescription getDesc(Integer id, boolean forWrite) {
        long startTime = System.currentTimeMillis();
        ObjectDescription desc = (ObjectDescription) cache.get(id);
        if (desc == null) {
            desc = (ObjectDescription) writeBack.get(id);
            if (forWrite && (desc != null)) {
                desc = new ObjectDescription(desc);
            }
            cache.put(id, desc);
        }
        if (desc == null) {
            desc = new ObjectDescription();
            try {
                long start = System.currentTimeMillis();
                Statement s = conn.createStatement();
                ResultSet r = s.executeQuery("select fieldname, sourcename, version from tracker"
                        + " where objectid = " + id + " ORDER BY version");
                while (r.next()) {
                    desc.putClean(r.getString(1).intern(), stringToSource(r.getString(2)));
                }
                long now = System.currentTimeMillis();
                //LOG.debug("Fetched entry from DB (time = " + (now - start) + " ms)");
                if (now - start > 2000) {
                    LOG.error("Query on tracker table took too long (" + (now - start) + " ms) "
                            + "- switching off sequential scans. You should analyse the database");
                    conn.createStatement().execute("SET enable_seqscan = off;");
                }
            } catch (SQLException e) {
                broken = e;
                IllegalArgumentException e2 = new IllegalArgumentException();
                e2.initCause(broken);
                throw e2;
            }
            cache.put(id, desc);
            maybePoke();
            misses++;
        }
        timeSpentReading += System.currentTimeMillis() - startTime;
        ops++;
        if (ops % 1000000 == 0) {
            LOG.info("Operations: " + ops + ", cache misses: " + misses + ", time spent reading: "
                    + timeSpentReading);
        }
        return desc;
    }

    /**
     * Set the Source for a field of an object in the database
     *
     * @param id the ID of the object
     * @param field the name of the field
     * @param source the Source of the field
     */
    public synchronized void setSource(Integer id, String field, Source source) {
        if (id == null) {
            throw new NullPointerException("id cannot be null");
        }
        if (!sourceToName.containsKey(source)) {
            throw new NullPointerException("Could not find given source (" + source
                    + ") in tracker. sourceToName = " + sourceToName);
        }
        if (broken != null) {
            IllegalArgumentException e = new IllegalArgumentException();
            e.initCause(broken);
            throw e;
        }
        ObjectDescription desc = getDesc(id, true);
        desc.put(field.intern(), source);
        // Lastly, we put the description into the cache, just in case we got it out of the
        // write-back cache. This guarantees that we won't lose data by forgetting to write it to
        // the database.
        cache.put(id, desc);
        maybePoke();
    }

    /**
     * Clears the cache for a particular object, in preparation for writing all the data for that
     * object. This allows the data tracker to cache the writes that are about to happen. This
     * method should only be called with Ids that this DataTracker has never seen before.
     *
     * @param id the ID of the object
     */
    public synchronized void clearObj(Integer id) {
        if (broken != null) {
            IllegalArgumentException e = new IllegalArgumentException();
            e.initCause(broken);
            throw e;
        }
        ObjectDescription desc = new ObjectDescription();
        cache.put(id, desc);
        maybePoke();
    }

    /**
     * Performs maintenance of the cache, writing stuff to the backing database.
     *
     * @return true if some action was performed
     */
    public boolean doWrite() {
        if (broken != null) {
            IllegalArgumentException e = new IllegalArgumentException();
            e.initCause(broken);
            throw e;
        }
        synchronized (writeBack) {
            int cacheSize = cache.size();
            Map writeBatch = getWriteBatch();
            if (writeBatch != null) {
                LOG.info("Writing cache batch - batch size: " + writeBatch.size()
                        + ", cache size: " + cacheSize + "->" + cache.size());
                try {
                    writeMap(writeBatch, false);
                } catch (SQLException e) {
                    broken = e;
                    IllegalArgumentException e2 = new IllegalArgumentException();
                    e2.initCause(broken);
                    throw e2;
                }
                clearWriteBack();
                return true;
            } else {
                LOG.debug("Not writing cache batch - no dirty entries");
                return false;
            }
        }
    }

    /**
     * Flushes everything to the backing database.
     */
    public void flush() {
        if (broken != null) {
            IllegalArgumentException e = new IllegalArgumentException();
            e.initCause(broken);
            throw e;
        }
        LOG.info("Flushing cache - size: " + cache.size());
        // Synchronise in this order to prevent deadlocks.
        synchronized (writeBack) {
            synchronized (this) {
                try {
                    writeMap(cache, true);
                } catch (SQLException e) {
                    broken = e;
                    IllegalArgumentException e2 = new IllegalArgumentException();
                    e2.initCause(broken);
                    throw e2;
                }
            }
        }
    }

    /**
     * Closes this DataTracker, releasing both connections to the database. No further operations
     * can be performed on the tracker.
     */
    public void close() {
        LOG.info("Closing DataTracker. Operations: " + ops + ", cache misses: " + misses
                + ", time spent reading: " + timeSpentReading + ", prefetched: " + batched
                + ", time spent prefetching: " + timeSpentPrefetching);
        cacheStorer.die();
        flush();
        synchronized (this) {
            try {
                conn.close();
                storeConn.close();
                conn = null;
                storeConn = null;
            } catch (SQLException e) {
                IllegalArgumentException e2 = new IllegalArgumentException();
                e2.initCause(e);
                throw e2;
            }
        }
    }

    /**
     * Returns a Map created from cache, containing the entries that should be flushed to the
     * backing database. The entries are removed from the cache and put in a special write-back
     * cache before this method terminates. Once you have finished storing the entries, you should
     * call clearWriteBack() to clear this write-back cache.
     * This method will return null if it does not recommend flushing any entries to the backing
     * database. The Map will probably only contain those entries that are dirty, however the method
     * that uses this method should not rely on this fact, because such a method may be passed the
     * cache instead in the instance of a flush().
     *
     * @return a Map from Integer to ObjectDescription
     */
    private synchronized Map getWriteBatch() {
        if (cache.size() > maxSize) {
            Map retval = new HashMap();
            int count = 0;
            Iterator iter = cache.entrySet().iterator();
            while ((count < commitSize) && iter.hasNext()) {
                Map.Entry iterEntry = (Map.Entry) iter.next();
                Integer id = (Integer) iterEntry.getKey();
                ObjectDescription desc = (ObjectDescription) iterEntry.getValue();
                if (desc.isDirty()) {
                    retval.put(id, desc);
                    writeBack.put(id, desc);
                }
                iter.remove();
                count++;
            }
            return retval;
        } else {
            return null;
        }
    }

    /**
     * Clears the write-back cache. This should be called after the data has been committed to the
     * database. Note that some of the entries may have been altered since they were put in the
     * write-back cache. Data loss is avoided by the fact that such entries are placed back in the
     * main cache.
     */
    private synchronized void clearWriteBack() {
        writeBack.clear();
    }

    /**
     * Writes the contents of the given Map to the backing database. Attempts to make use of all the
     * SQL tricks to speed this operation up.
     *
     * @param map a Map from Integer to ObjectDesciption
     * @param clean true if this method should call clean() on all the entries in the given Map, or
     * false if the given Map is going to be thrown away.
     * @throws SQLException on any error with the backing database
     */
    private void writeMap(Map map, boolean clean) throws SQLException {
        long start = System.currentTimeMillis();
        try {
            org.postgresql.copy.CopyManager copyManager = null;
            ByteArrayOutputStream baos = null;
            DataOutputStream dos = null;
            Statement s = null;
            if (storeConn instanceof org.postgresql.PGConnection) {
                copyManager = ((org.postgresql.PGConnection) storeConn).getCopyAPI();
                baos = new ByteArrayOutputStream();
                dos = new DataOutputStream(baos);
                dos.writeBytes("PGCOPY\n");
                dos.writeByte(255);
                dos.writeBytes("\r\n");
                dos.writeByte(0); // Signature done
                dos.writeInt(0); // Flags - we aren't supplying OIDS
                dos.writeInt(0); // Length of header extension
            }
            if (copyManager == null) {
                s = storeConn.createStatement();
                LOG.warn("Using slow portable writing method");
            }
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Integer id = (Integer) entry.getKey();
                ObjectDescription desc = (ObjectDescription) entry.getValue();
                if (desc.isDirty()) {
                    Map orig = desc.getOrig();
                    Map newData = desc.getNewData();
                    Iterator fieldIter = newData.entrySet().iterator();
                    while (fieldIter.hasNext()) {
                        Map.Entry fieldEntry = (Map.Entry) fieldIter.next();
                        String field = (String) fieldEntry.getKey();
                        Source source = (Source) fieldEntry.getValue();
                        if (!orig.containsKey(field) || (!orig.get(field).equals(source))) {
                            // Insert required
                            if (s == null) {
                                dos.writeShort(4); // Number of fields
                                dos.writeInt(4); // Length of an integer
                                dos.writeInt(id.intValue()); // objectid
                                dos.writeInt(field.length()); // Length of fieldname
                                dos.writeBytes(field); // Field name
                                String sourceName = sourceToString(source);
                                dos.writeInt(sourceName.length()); // Length of source name
                                dos.writeBytes(sourceName); // Source name
                                dos.writeInt(4); // Length of an integer
                                dos.writeInt(version); // version
                            } else {
                                s.addBatch("INSERT INTO tracker (objectid, fieldname, sourcename,"
                                        + " version) VALUES (" + id + ", '" + field + "', '"
                                        + sourceToString(source) + "', " + version + ")");
                            }
                        }
                    }
                    if (clean) {
                        desc.clean();
                    }
                }
            }
            if (s == null) {
                dos.writeShort(-1); // No more tuples
                dos.flush();
                copyManager.copyInQuery("COPY tracker FROM STDIN BINARY",
                        new ByteArrayInputStream(baos.toByteArray()));
            } else {
                s.executeBatch();
            }
            version++;
            storeConn.commit();
        } catch (IOException e) {
            throw new SQLException(e.toString());
        }
        long now = System.currentTimeMillis();
        LOG.debug("Finished storing batch (time = " + (now - start) + " ms)");
    }

    /**
     * Pokes the CacheStorer thread if there are too many entries in the cache.
     */
    private void maybePoke() {
        if (cache.size() > maxSize) {
            cacheStorer.poke();
        }
    }

    /**
     * Converts a string sourcename to a Source.
     *
     * @param name a string source name
     * @return a Source
     */
    public synchronized Source stringToSource(String name) {
        Source retval = (Source) nameToSource.get(name);
        if (retval == null) {
            retval = new Source();
            if (name.startsWith("skel_")) {
                retval.setName(name.substring(5));
                retval.setSkeleton(true);
            } else {
                retval.setName(name);
                retval.setSkeleton(false);
            }
            nameToSource.put(name, retval);
            sourceToName.put(retval, name);
        }
        return retval;
    }

    /**
     * Converts a Source to a source name.
     *
     * @param source a Source
     * @return the name
     */
    public synchronized String sourceToString(Source source) {
        String retval = (String) sourceToName.get(source);
        if (retval == null) {
            throw new NullPointerException("Could not find given source in tracker");
        }
        return retval;
    }

    private class CacheStorer implements Runnable
    {
        private boolean needAction = false;
        private boolean dontQuit = true;
        private boolean notDead = true;

        public CacheStorer() {
        }

        public synchronized void poke() {
            needAction = true;
            notify();
        }

        public synchronized void die() {
            needAction = true;
            dontQuit = false;
            notify();
            while (notDead) {
                try {
                    wait(100000L);
                } catch (InterruptedException e) {
                }
            }
        }

        private synchronized boolean dontQuitNow() {
            return needAction || dontQuit;
        }

        public void run() {
            while (dontQuitNow()) {
                synchronized (this) {
                    while (!needAction) {
                        try {
                            wait(100000L);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                while (needAction) {
                    try {
                        synchronized (this) {
                            needAction = false;
                        }
                        boolean tempNeedAction = doWrite();
                        synchronized (this) {
                            needAction = needAction || tempNeedAction;
                        }
                    } catch (Exception e) {
                        LOG.error("CacheStorer received exception: " + e);
                    }
                }
            }
            synchronized (this) {
                notDead = false;
                notifyAll();
            }
        }
    }
}
