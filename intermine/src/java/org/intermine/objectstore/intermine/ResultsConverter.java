package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DatabaseUtil;

import org.apache.log4j.Logger;

/**
 * Provides a method to convert from SQL ResultSet data to InterMine object-based data.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ResultsConverter
{
    private static final Logger LOG = Logger.getLogger(ResultsConverter.class);

    /**
     * Method to convert from SQL results to InterMine object-based results.
     * This method accepts an SQL ResultSet and a Query as an input. The ResultSet must contain a
     * column named the same as the aliases of the elements in the SELECT list of the Query,
     * each containing either the OBJECT column in the case of a business object, or the value of
     * any other value.
     * <br>
     * This method will return a List of ResultsRow objects.
     *
     * @param sqlResults the ResultSet
     * @param q the Query
     * @param os the ObjectStore with which to associate any new lazy objects
     * @return a List of ResultsRow objects
     * @throws ObjectStoreException if the ResultSet does not match the Query in any way, or if a
     * SQL exception occurs
     */
    public static List convert(ResultSet sqlResults, Query q, ObjectStore os)
            throws ObjectStoreException {
        Object currentColumn = null;
        try {
            ArrayList retval = new ArrayList();
            while (sqlResults.next()) {
                ResultsRow row = new ResultsRow();
                Iterator selectIter = q.getSelect().iterator();
                while (selectIter.hasNext()) {
                    QueryNode node = (QueryNode) selectIter.next();
                    String alias = DatabaseUtil.generateSqlCompatibleName((String) q.getAliases()
                            .get(node));
                    if (node instanceof QueryClass) {
                        Integer idField = new Integer(sqlResults.getInt(alias + "id"));
                        InterMineObject obj = os.pilferObjectById(idField);
                        if (obj == null) {
                            String objectField = sqlResults.getString(alias);
                            currentColumn = objectField;
                            obj = NotXmlParser.parse(objectField, os);
                            //if (objectField.length() < ObjectStoreInterMineImpl
                            //        .CACHE_LARGEST_OBJECT) {
                                os.cacheObjectById(obj.getId(), obj);
                            //} else {
                            //    LOG.debug("Not cacheing large object " + obj.getId()
                            //            + " on read"
                            //            + " (size = " + (objectField.length() / 512) + " kB)");
                            //}
                        }
                        row.add(obj);
                    } else {
                        currentColumn = sqlResults.getObject(alias);
                        if (Date.class.equals(node.getType())) {
                            currentColumn = new Date(((Long) currentColumn).longValue());
                        }
                        row.add(currentColumn);
                    }
                }
                retval.add(row);
            }
            return retval;
        } catch (SQLException e) {
            throw new ObjectStoreException("Error converting results: " + currentColumn, e);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Unknown class mentioned in database OBJECT field"
                    + " while converting results: " + currentColumn, e);
        }
    }
}
