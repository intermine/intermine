package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * An implementation of the DBReader interface that attempts to speed up the execute() method
 * by performing batch fetches.
 *
 * @author Matthew Wakeling
 */
public class BatchingDBReader extends DirectDBReader
{
    private static final Logger LOG = Logger.getLogger(BatchingDBReader.class);

    protected Model model;
    private int executeCount = 0;

    /**
     * Constructs a new BatchingDBReader.
     *
     * @param db the Database to access
     * @param model a Model from which to get collection names to batch
     */
    public BatchingDBReader(Database db, Model model) {
        super(db);
        this.model = model;
    }

    /**
     * @see DBReader#execute
     */
    public List execute(String sql) throws SQLException {
        if (batch != null) {
            Map cache = batch.getCache();
            if (cache != null) {
                List retval = (List) cache.get(sql);
                if (retval != null) {
                    executeCount++;
                    if (executeCount % 12347 == 0) {
                        List compare = super.execute(sql);
                        if (!(new HashSet(compare)).equals(new HashSet(retval))) {
                            LOG.error("Incorrect results for sql \"" + sql + "\"");
                        }
                    }
                    return retval;
                }
            }
        }
        LOG.warn("SQL not cached: " + sql);
        return super.execute(sql);
    }

    /**
     * Creates a new DBBatch for a given offset, including batched out-of-band queries in the cache.
     *
     * @param previous the previous batch, or null if this is the first
     * @return a DBBatch
     * @throws SQLException if the database has a problem
     */
    protected DBBatch getBatch(DBBatch previous) throws SQLException {
        DBBatch retval = super.getBatch(previous);
        if (retval.getRows().isEmpty()) {
            return retval;
        }
        ClassDescriptor cld = model.getClassDescriptorByName(model.getPackageName() + "."
                + tableName);
        if (cld == null) {
            LOG.error("No such class descriptor " + model.getPackageName() + "." + tableName
                    + " in model");
        }
        Set colls = cld.getAllCollectionDescriptors();
        Iterator collIter = colls.iterator();
        while (collIter.hasNext()) {
            CollectionDescriptor coll = (CollectionDescriptor) collIter.next();
            if (coll.relationType() == FieldDescriptor.M_N_RELATION) {
                String refClsName = TypeUtil.unqualifiedName(coll.getReferencedClassDescriptor()
                        .getName());
                String indirTableName = tableName + "_" + refClsName;
                Connection c = null;
                try {
                    c = db.getConnection();
                    if (!DatabaseUtil.tableExists(c, indirTableName.toLowerCase())) {
                        indirTableName = refClsName + "_" + tableName;
                    }
                    String collSql = "SELECT " + tableName + "_id, " + refClsName + "_id FROM "
                        + indirTableName + " WHERE " + tableName + "_id >= " + retval.getFirstId()
                        + " AND " + tableName + "_id <= " + retval.getLastId();
                    String refClsNameId = refClsName + "_id";
                    Map thisIdToThatIdList = new HashMap();
                    Iterator rowIter = retval.getRows().iterator();
                    while (rowIter.hasNext()) {
                        Map row = (Map) rowIter.next();
                        Object idValue = row.get(idField);
                        thisIdToThatIdList.put(idValue, new ArrayList());
                    }
                    long start = System.currentTimeMillis();
                    Statement s = c.createStatement();
                    ResultSet r = s.executeQuery(collSql);
                    long afterExecute = System.currentTimeMillis();
                    while (r.next()) {
                        Object thisIdValue = r.getObject(1);
                        Object thatIdValue = r.getObject(2);
                        List rowList = (List) thisIdToThatIdList.get(thisIdValue);
                        if (rowList != null) {
                            rowList.add(Collections.singletonMap(refClsNameId.toLowerCase(),
                                        thatIdValue));
                        } else {
                            LOG.warn("Missing value for this: " + thisIdValue + ", that: "
                                    + thatIdValue + " for " + tableName + "." + refClsName);
                        }
                    }
                    Iterator thisThatIter = thisIdToThatIdList.entrySet().iterator();
                    while (thisThatIter.hasNext()) {
                        Map.Entry thisThatEntry = (Map.Entry) thisThatIter.next();
                        Object thisIdValue = thisThatEntry.getKey();
                        List rowList = (List) thisThatEntry.getValue();
                        retval.getCache().put("SELECT " + refClsName + "_id FROM " + indirTableName
                                + " WHERE " + tableName + "_id = "
                                + DatabaseUtil.objectToString(thisIdValue), rowList);
                    }
                    long end = System.currentTimeMillis();
                    oobTime += end - start;
                    if (oobTime / 100000 > (oobTime - end + start) / 100000) {
                        LOG.info("Spent " + oobTime + " ms on out-of-band queries like ("
                                + (afterExecute - start) + " + " + (end - afterExecute) + " ms) "
                                + collSql);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
        return retval;
    }
}
