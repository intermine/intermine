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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.xml.lite.LiteParser;

import org.xml.sax.SAXException;

/**
 * Provides a method to convert from SQL ResultSet data to FlyMine object-based data.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ResultsConverter
{
    /**
     * Method to convert from SQL results to FlyMine object-based results.
     * This method accepts an SQL ResultSet and a Query as an input. The ResultSet must contain a
     * column named the same as the aliases of the elements in the SELECT list of the Query,
     * each containing either the OBJECT column in the case of a business object, or the value of
     * any other value.
     * <br>
     * This method will return a List of ResultsRow objects.
     *
     * @param sqlResults the ResultSet
     * @param q the Query
     * @return a List of ResultsRow objects
     * @throws ObjectStoreException if the ResultSet does not match the Query in any way, or if a
     * SQL exception occurs
     */
    public static List convert(ResultSet sqlResults, Query q) throws ObjectStoreException {
        try {
            ArrayList retval = new ArrayList();
            while (sqlResults.next()) {
                ResultsRow row = new ResultsRow();
                Iterator selectIter = q.getSelect().iterator();
                while (selectIter.hasNext()) {
                    QueryNode node = (QueryNode) selectIter.next();
                    String alias = (String) q.getAliases().get(node);
                    if (node instanceof QueryClass) {
                        String objectField = sqlResults.getString(alias);
                        row.add(LiteParser.parse(new ByteArrayInputStream(objectField.getBytes())));
                    } else {
                        row.add(sqlResults.getObject(alias));
                    }
                }
                retval.add(row);
            }
            return retval;
        } catch (SQLException e) {
            throw new ObjectStoreException("Error converting results", e);
        } catch (IOException e) {
            throw new ObjectStoreException("Impossible IO error reading from ByteArrayInputStream"
                    + " while converting results", e);
        } catch (SAXException e) {
            throw new ObjectStoreException("Illegal data in OBJECT field in database while"
                    + " converting results", e);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Unknown class mentioned in database OBJECT field"
                    + " while converting results", e);
        }
    }
}
