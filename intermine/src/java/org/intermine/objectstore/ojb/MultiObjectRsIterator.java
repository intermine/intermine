package org.flymine.objectstore.ojb;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.ojb.broker.accesslayer.*;

import org.apache.ojb.broker.PBLifeCycleEvent;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;
import org.apache.ojb.broker.util.SqlHelper;

import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryNode;

import org.apache.log4j.Logger;

/**
 * An extension to RsIterator that can be used to retrieve multiple
 * objects from a single row of a JDBC ResultSet.
 *
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class MultiObjectRsIterator extends RsIterator
{
    protected static final Logger LOG = Logger.getLogger(MultiObjectRsIterator.class);

    private Query query;

    /**
     * No-argument MultiObjectRsIterator constructor
     */
    protected MultiObjectRsIterator() {
    }

    /**
     * MultiObjectRsIterator constructor
     * @param query the ObjectStore query we should use
     * @param broker the broker we should use.
     * @param start the number of the first row to return, numbered from zero
     * @param limit the maximum number of rows to return
     */
    public MultiObjectRsIterator(Query query, PersistenceBrokerImpl broker, int start, int limit) {
        //logger = LoggerFactory.getLogger(this.getClass());
        cache = broker.serviceObjectCache();
        m_row = new HashMap();
        m_broker = broker;
        afterLookupEvent = new PBLifeCycleEvent(m_broker, PBLifeCycleEvent.Type.AFTER_LOOKUP);
        this.query = query;
        m_rsAndStmt = ((JdbcAccessFlyMineImpl) broker.serviceJdbcAccess())
            .executeQuery(query, start, limit);
        //prefetchRelationships(query);
    }

    /**
     * moves to the next row of the underlying ResultSet and
     * returns the corresponding Objects materialized from this row.
     * @return the Object[] from the next ResultSet Row
     * @throws NoSuchElementException if there are no more rows
     */
    public synchronized Object next() throws NoSuchElementException {
        try {
            if (!hasCalledCheck) {
                hasNext();
            }
            hasCalledCheck = false;
            if (hasNext) {
                // for each cld: set m_rsAndStmt, call getObjectFromResultSet, put in results array
                DescriptorRepository dr = m_broker.getDescriptorRepository();
                ResultSet rsTemp = m_rsAndStmt.m_rs;
                Object[] results = new Object[query.getSelect().size()];
                for (int i = 0; i < results.length; i++) {
                    Object obj = null;
                    QueryNode node = (QueryNode) query.getSelect().get(i);
                    String alias = (String) query.getAliases().get(node);
                    if (node instanceof QueryClass) {
                        m_rsAndStmt.m_rs = getUnaliasedColumns(rsTemp, alias);
                        String rowClass = null;
                        try {
                            rowClass = m_rsAndStmt.m_rs.getString("CLASS");
                        } catch (SQLException e) {
                            rowClass = null;
                        }
                        if (rowClass == null) {
                            m_cld = dr.getDescriptorFor(node.getType());
                        } else {
                            m_cld = dr.getDescriptorFor(Class.forName(rowClass));
                        }
                        itemExtentClass = null;
                        obj = getObjectFromResultSet();
                        afterLookupEvent.setTarget(obj);
                        m_broker.fireBrokerEvent(afterLookupEvent);
                    } else {
                        int columnIndex = m_rsAndStmt.m_rs.findColumn(alias);
                        int jdbcType = m_rsAndStmt.m_rs.getMetaData().getColumnType(columnIndex);
                        obj = SqlHelper.getObjectFromColumn(m_rsAndStmt.m_rs, jdbcType, alias);
                    }
                    results[i] = obj;
                    m_rsAndStmt.m_rs = rsTemp;
                }
                m_current_row++;
                return results;
            } else {
                throw new NoSuchElementException();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex);
            throw new NoSuchElementException();
        }
    }

    /**
     * Searches the specified ResultSet for column names starting with alias.
     * Matching columns have their values placed in a new result set.
     * Columns in the new set have the same names, minus the alias prefix.
     * The resulting ResultSet will therefore be the same size or smaller than the input.
     * @param rs the input ResultSet
     * @param alias the column prefix to search for
     * @return the ResultSet containing the matching columns
     * @throws SQLException if there was a problem accessing the ResultSet
     */
    protected ResultSet getUnaliasedColumns(ResultSet rs, String alias) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map map = new HashMap();

        for (int i = 1; i <= md.getColumnCount(); i++) {
            String colName = md.getColumnName(i);
            if (colName.startsWith(alias)) {
                int jdbcType = md.getColumnType(i);
                Object obj = SqlHelper.getObjectFromColumn(rs, jdbcType, i);
                String s = colName.substring(alias.length());
                map.put(s, obj);
            }
        }
       return new MapResultSet(map);
    }
}
