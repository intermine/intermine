package org.flymine.objectstore.ojb;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache ObjectRelationalBridge" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache ObjectRelationalBridge", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import org.apache.ojb.broker.accesslayer.*;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;
import org.apache.ojb.broker.util.SqlHelper;

import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.flymine.objectstore.query.Query;

/**
 * An extension to RsIterator that can be used to retrieve multiple
 * objects from a single row of a JDBC ResultSet.
 *
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class MultiObjectRsIterator extends RsIterator
{
    private Query query = null;
    private ClassDescriptor[] clds;

    /**
     * No-argument MultiObjectRsIterator constructor
     */
    protected MultiObjectRsIterator() {
    }

    /**
     * MultiObjectRsIterator constructor
     * @param queryPackage the QueryPackage (objectstore query + class descriptors) we should use
     * @param broker the broker we should use.
     */
    public MultiObjectRsIterator(QueryPackage queryPackage, PersistenceBrokerImpl broker) {
        //m_rsAndStmt = broker.serviceJdbcAccess().executeQuery(queryPackage);
        m_row = new HashMap();
        query = queryPackage.getQuery();
        m_broker = broker;
        clds = queryPackage.getDescriptors();
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
                    ResultSet rsTemp = m_rsAndStmt.m_rs;
                    Object[] results = new Object[clds.length];
                    for (int i = 0; i < clds.length; i++) {
                        Object obj = null;
                        m_rsAndStmt.m_rs = getUnaliasedColumns(rsTemp, (String) query.getAliases()
                                                               .get(query.getSelect().get(i)));

                        if (clds[i] != null) {
                            m_cld = clds[i];
                            itemProxyClass = m_cld.getProxyClass();
                            itemExtentClass = null;

                            obj = getObjectFromResultSet();
                            // Invoke events on PersistenceBrokerAware instances and listeners
                            m_broker.fireBrokerEvent(obj, PersistenceBrokerImpl.EVENT_AFTER_LOOKUP);
                        } else {
                            int jdbcType = m_rsAndStmt.m_rs.getMetaData().getColumnType(1);
                            obj = SqlHelper.getObjectFromColumn(m_rsAndStmt.m_rs, jdbcType, 1);
                        }
                        results[i] = obj;
                    }
                    m_rsAndStmt.m_rs = rsTemp;
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

        for (int i = 0; i < md.getColumnCount(); i++) {
            String colName = md.getColumnName(i);
            if (colName.startsWith(alias)) {
                int jdbcType = md.getColumnType(i + 1);
                Object obj = SqlHelper.getObjectFromColumn(rs, jdbcType, i + 1);
                String s = colName.substring(alias.length());
                map.put(s, obj);
            }
        }
       return new MapResultSet(map);
    }
}
