package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    /**
     * Constructs an empty Batch, with no tables.
     *
     * @param batchWriter the BatchWriter to use
     */
    public Batch(BatchWriter batchWriter) {
        this.batchWriter = batchWriter;
    }

    /**
     * Adds a row to the batch for a given table. This action will override any previously deleted
     * rows. If the batch already shows that row to exist (by idField), then an exception will be
     * thrown. If the table is not set up, then it will be set up using the provided array of field
     * names, but without an idField (which will be set up the first time deleteRow is called).
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
        TableBatch table = (TableBatch) tables.get(name);
        if (table == null) {
            table = new TableBatch();
            tables.put(name, table);
        }
        batchSize += table.addRow(idValue, colNames, values);
        if (batchSize > MAX_BATCH_SIZE) {
            flush(con);
        }
    }

    /**
     * Deletes a row from the batch for a given table. This action will override any previously
     * added rows. If the batch already shows that row to be in a deleted state (by idField), then
     * no further action is taken. If the table if not set up, then it will be set up with an
     * idField, but without an array of field names (which will be set up the first time addRow is
     * called.
     *
     * @param con a Connection for writing to the database
     * @param name the name of the table
     * @param idField the name of the field that you wish to use as a unique primary key
     * @param idValue the value of the id field for the row to be deleted
     * @throws SQLException if a flush occurs, and an error occurs while flushing
     */
    public void deleteRow(Connection con, String name, String idField,
            Object idValue) throws SQLException {
        TableBatch table = (TableBatch) tables.get(name);
        if (table == null) {
            table = new TableBatch();
            tables.put(name, table);
        }
        batchSize += table.deleteRow(idField, idValue);
        if (batchSize > MAX_BATCH_SIZE) {
            flush(con);
        }
    }

    /**
     * Flushes the batch out to the database server.
     *
     * @param con a Connection for writing to the database
     * @throws SQLException if a flush occurs, and an error occurs while flushing
     */
    public void flush(Connection con) throws SQLException {
        //long start = System.currentTimeMillis();
        batchWriter.write(con, tables);
        batchSize = 0;
        //long end = System.currentTimeMillis();
        //LOG.error("Flushed batch - took " + (end - start) + " ms");
    }

    /**
     * Clears the batch without writing it to the database.
     */
    public void clear() {
        Iterator iter = tables.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            TableBatch table = (TableBatch) entry.getValue();
            table.getIdsToInsert().clear();
            table.getIdsToDelete().clear();
        }
        batchSize = 0;
    }

    /**
     * Changes the BatchWriter for a new one.
     *
     * @param batchWriter the new BatchWriter
     */
    public void setBatchWriter(BatchWriter batchWriter) {
        this.batchWriter = batchWriter;
    }
}
