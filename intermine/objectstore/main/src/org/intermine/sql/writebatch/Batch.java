package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A class representing a collection of writes to an SQL database. This class is intended for the
 * purpose of improving the performance of systems that write to an SQL database, by bunching all
 * the writes into a large batch, and then using tricks to speed up the batch commit.
 *
 * One should create one of these objects with a BatchWriter, which will perform the writes.
 * BatchWriters are database-specific, in that they use different tricks to speed up the write,
 * some of which depend on a certain database product.
 *
 * @author Matthew Wakeling
 */
public class Batch
{
    private static final Logger LOG = Logger.getLogger(Batch.class);
    private static final int MAX_BATCH_SIZE = 20000000;

    private Map tables = new HashMap();
    private BatchWriter batchWriter;
    private int batchSize = 0;

    private List flushJobs = Collections.EMPTY_LIST;
    private SQLException problem = null;
    
    private volatile int lastDutyCycle = 100;
    private boolean closed = false;
    private static final List CLOSE_DOWN_COMMAND = new ArrayList();

    /**
     * Constructs an empty Batch, with no tables.
     *
     * @param batchWriter the BatchWriter to use
     */
    public Batch(BatchWriter batchWriter) {
        this.batchWriter = batchWriter;
        BatchFlusher flusher = new BatchFlusher();
        Thread thread = new Thread(flusher);
        thread.setDaemon(true);
        thread.setName("WriteBatch Flusher");
        thread.start();
    }

    /**
     * Adds a row to the batch for a given table. This action will override any previously deleted
     * rows. Multiple rows with the same id can be added. If the table is not set up, then it will
     * be set up using the provided array of field names, but without an idField (which will be set
     * up the first time deleteRow is called).
     * <br>
     * Note that the batch may hang on to (and use) the Connection that you provide. It is your
     * responsibility to call flush(Connection) before using that same Connection with anything
     * other than this batch.
     *
     * @param con a Connection for writing to the database
     * @param name the name of the table
     * @param idValue the value of the id field for this row
     * @param colNames an array of names of fields that are in the table
     * @param values an array of Objects to be put in the row, in the same order as colNames
     * @throws SQLException if a flush occurs, and an error occurs while flushing
     */
    public void addRow(Connection con, String name, Object idValue, String colNames[],
            Object values[]) throws SQLException {
        if (closed) {
            throw new SQLException("Batch is closed");
        }
        TableBatch table = (TableBatch) tables.get(name);
        if (table == null) {
            table = new TableBatch();
            tables.put(name, table);
        }
        batchSize += table.addRow(idValue, colNames, values);
        if (batchSize > MAX_BATCH_SIZE) {
            backgroundFlush(con, null);
        }
    }

    /**
     * Adds a row to the batch for a given indirection table.
     *
     * @param con a Connection for writing to the database
     * @param name the name of the indirection table
     * @param leftColName the name of the left-hand field
     * @param rightColName the name of the right-hand field
     * @param left the int value of the left field
     * @param right the int value of the right field
     * @throws SQLException if a flush occurs, and an error occurs while flushing
     */
    public void addRow(Connection con, String name, String leftColName, String rightColName,
            int left, int right) throws SQLException {
        if (closed) {
            throw new SQLException("Batch is closed");
        }
        IndirectionTableBatch table = (IndirectionTableBatch) tables.get(name);
        if (table == null) {
            table = new IndirectionTableBatch(leftColName, rightColName);
            tables.put(name, table);
        }
        batchSize += table.addRow(left, right);
        if (batchSize > MAX_BATCH_SIZE) {
            backgroundFlush(con, null);
        }
    }

    /**
     * Deletes a row from the batch for a given table. This action will override any previously
     * added rows. If the batch already shows that row to be in a deleted state (by idField), then
     * no further action is taken. If the table if not set up, then it will be set up with an
     * idField, but without an array of field names (which will be set up the first time addRow is
     * called.
     * <br>
     * Note that the batch may hang on to (and use) the Connection that you provide. It is your
     * responsibility to call flush(Connection) before using that same Connection with anything
     * other than this batch.
     *
     * @param con a Connection for writing to the database
     * @param name the name of the table
     * @param idField the name of the field that you wish to use as a unique primary key
     * @param idValue the value of the id field for the row to be deleted
     * @throws SQLException if a flush occurs, and an error occurs while flushing
     */
    public void deleteRow(Connection con, String name, String idField,
            Object idValue) throws SQLException {
        if (closed) {
            throw new SQLException("Batch is closed");
        }
        TableBatch table = (TableBatch) tables.get(name);
        if (table == null) {
            table = new TableBatch();
            tables.put(name, table);
        }
        batchSize += table.deleteRow(idField, idValue);
        if (batchSize > MAX_BATCH_SIZE) {
            backgroundFlush(con, null);
        }
    }

    /**
     * Deletes a row from the batch for a given indirection table.
     *
     * @param con a Connection for writing to the database
     * @param name the name of the indirection table
     * @param leftColName the name of the left-hand field
     * @param rightColName the name of the right-hand field
     * @param left the int value of the left field
     * @param right the int value of the right field
     * @throws SQLException if a flush occurs, and an error occurs while flushing
     */
    public void deleteRow(Connection con, String name, String leftColName, String rightColName,
            int left, int right) throws SQLException {
        if (closed) {
            throw new SQLException("Batch is closed");
        }
        IndirectionTableBatch table = (IndirectionTableBatch) tables.get(name);
        if (table == null) {
            table = new IndirectionTableBatch(leftColName, rightColName);
            tables.put(name, table);
        }
        batchSize += table.deleteRow(left, right);
        if (batchSize > MAX_BATCH_SIZE) {
            backgroundFlush(con, null);
        }
    }

    /**
     * Flushes the batch out to the database server. This method guarantees that the Connection
     * is no longer in use by the batch even if it does not return normally.
     *
     * @param con a Connection for writing to the database
     * @throws SQLException if an error occurs while flushing
     */
    public void flush(Connection con) throws SQLException {
        flush(con, null);
    }

    /**
     * Flushes the batch out to the database server. This method guarantees that the Connection
     * is no longer in use by the batch even if it does not return normally.
     *
     * @param con a Connection for writing to the database
     * @param filter a Set of table names to write, or null to write all of them
     * @throws SQLException if an error occurs while flushing
     */
    public void flush(Connection con, Set filter) throws SQLException {
        backgroundFlush(con, filter);
        putFlushJobs(Collections.EMPTY_LIST);
    }

    /**
     * Flushes the batch out to the database server, but does not guarantee that the operation
     * is finished when this method returns. Do not use the Connection until you have called flush()
     * or clear(). This method also guarantees that any exception thrown by any operations being
     * completed by this flush will be thrown out of this method.
     *
     * @param con a Connection for writing to the database
     * @param filter a Set of the table names to write, or null to write all of them
     * @throws SQLException if an error occurs while flushing
     */
    public void backgroundFlush(Connection con, Set filter) throws SQLException {
        if (closed) {
            throw new SQLException("Batch is closed");
        }
        long start = System.currentTimeMillis();
        List jobs = batchWriter.write(con, tables, filter);
        int oldBatchSize = batchSize;
        batchSize = 0;
        Iterator tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            Table table = (Table) tableEntry.getValue();
            batchSize += table.getSize();
        }
        long middle = System.currentTimeMillis();
        putFlushJobs(jobs);
        long end = System.currentTimeMillis();
        if ((end > middle + 10) && (lastDutyCycle < 75)) {
            LOG.info("Enqueued " + (oldBatchSize - batchSize) + " of " + oldBatchSize
                    + " byte batch - took " + (middle - start) + " + " + (end - middle) + " ms");
        } else {
            LOG.debug("Enqueued " + (oldBatchSize - batchSize) + " of " + oldBatchSize
                    + " byte batch - took " + (middle - start) + " + " + (end - middle) + " ms");
        }
    }

    /**
     * Closes this BatchWriter. This method guarantees that the Connection is no longer in use by
     * the batch, and the background writer Thread will die soon, even if it does not return
     * normally.
     *
     * @param con a Connection for writing to the database
     * @throws SQLException if an error occurs while flushing.
     */
    public void close(Connection con) throws SQLException {
        if (closed) {
            throw new SQLException("Batch is already closed");
        }
        try {
            backgroundFlush(con, null);
        } catch (SQLException e) {
        }
        closed = true;
        putFlushJobs(CLOSE_DOWN_COMMAND);
    }

    /**
     * Clears the batch without writing it to the database. NOTE that some data may have already
     * made it to the database. It is expected that this will be called just before a transaction
     * is aborted, removing the data anyway. This method guarantees that the Connection is no longer
     * in use by the batch once it returns. This method also discards any deferred exceptions
     * waiting to be thrown
     */
    public void clear() {
        if (closed) {
            throw new IllegalStateException("Batch is closed");
        }
        Iterator iter = tables.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Table table = (Table) entry.getValue();
            table.clear();
        }
        batchSize = 0;
        waitForFreeConnection();
        clearProblem();
    }

    /**
     * Changes the BatchWriter for a new one.
     *
     * @param batchWriter the new BatchWriter
     */
    public void setBatchWriter(BatchWriter batchWriter) {
        if (closed) {
            throw new IllegalStateException("Batch is closed");
        }
        waitForFreeConnection();
        this.batchWriter = batchWriter;
    }

    /**
     * Returns a List of flush jobs (each as fully-processed as possible) when one becomes
     * available.
     *
     * @return a List
     */
    private synchronized List getFlushJobs() {
        flushJobs = null;
        notifyAll();
        while (flushJobs == null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        // By this point, any problem must necessarily have been picked up, so we can reset it.
        problem = null;
        return flushJobs;
    }

    /**
     * Waits for the flushJobs variable to be empty, which guarantees that the connection is
     * currently unused.
     */
    private synchronized void waitForFreeConnection() {
        while (flushJobs != null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Puts a List of flush jobs into the flushJobs variable, and tells the writer thread to write
     * it.
     *
     * @param jobs a List of jobs
     * @throws SQLException if the last background flush resulted in an error - note that the
     * operation will go ahead anyway (although it is likely to throw another exception of its own,
     * because the transaction will be invalid).
     */
    private synchronized void putFlushJobs(List jobs) throws SQLException {
        while (flushJobs != null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        if ((!jobs.isEmpty()) || (jobs == CLOSE_DOWN_COMMAND)) {
            flushJobs = jobs;
            notifyAll();
        }
        if (problem != null) {
            throw problem;
        }
    }

    /**
     * Reports a problem to the Batch - it will be thrown on the next background flush, or discarded
     * by the clear method.
     *
     * @param problem the SQLException
     */
    private synchronized void reportProblem(SQLException problem) {
        this.problem = problem;
    }

    /**
     * Clears the problem with the Batch.
     */
    public synchronized void clearProblem() {
        problem = null;
    }
    
    private class BatchFlusher implements Runnable
    {
        public BatchFlusher() {
        }

        public void run() {
            long flusherStart = System.currentTimeMillis();
            long totalSpent = 0;
            long timeAtLastMessage = flusherStart;
            long spentAtLastMessage = totalSpent;
            List jobs = null;
            while (jobs != CLOSE_DOWN_COMMAND) {
                try {
                    jobs = getFlushJobs();
                    long start = System.currentTimeMillis();
                    Iterator jobIter = jobs.iterator();
                    while (jobIter.hasNext()) {
                        FlushJob job = (FlushJob) jobIter.next();
                        job.flush();
                    }
                    long end = System.currentTimeMillis();
                    totalSpent += end - start;
                    if (end / 100000 > (timeAtLastMessage) / 100000) {
                        int totalDutyCycle = (int) (((100 * totalSpent + ((end - flusherStart) / 2))
                                    / (end - flusherStart)));
                        lastDutyCycle = (int) (((100 * (totalSpent - spentAtLastMessage)
                                        + ((end - timeAtLastMessage) / 2))
                                    / (end - timeAtLastMessage)));
                        LOG.info("Batch flusher has spent " + totalSpent + " ms waiting for the"
                                + " database (duty cycle " + totalDutyCycle
                                + "%) (current duty cycle " + lastDutyCycle + "%)");
                        timeAtLastMessage = end;
                        spentAtLastMessage = totalSpent;
                    }
                    //LOG.error("Flushed batch at " + end + " - took " + (end - start)
                    //        + " ms, total " + totalSpent + " of " + (end - flusherStart)
                    //        + " (duty cycle " + ((100 * totalSpent + ((end - flusherStart) / 2))
                    //                / (end - flusherStart)) + "%)");
                } catch (SQLException e) {
                    reportProblem(e);
                } catch (Throwable t) {
                    SQLException e = new SQLException("Caught a Throwable in the Batch Flusher");
                    e.initCause(t);
                    reportProblem(e);
                }
                if (jobs != CLOSE_DOWN_COMMAND) {
                    jobs = null;
                }
            }
        }
    }
}
