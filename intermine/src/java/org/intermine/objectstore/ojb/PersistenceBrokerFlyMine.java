/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.ojb;

import java.util.List;

import org.apache.ojb.broker.PersistenceBroker;

import org.flymine.objectstore.query.Query;
import org.flymine.sql.Database;
import org.flymine.sql.query.ExplainResult;

/**
 * Extension of PersistenceBroker to allow execution of ObjectStore queries
 *
 * @author Mark Woodbridge
 */
public interface PersistenceBrokerFlyMine extends PersistenceBroker
{
    /**
     * Executes a query with start and limit result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param limit maximum number of rows to return
     * @return a list of ResultsRows
     */
    public List execute(Query query, int start, int limit);

    /**
     * Runs EXPLAIN on the given query with start and limit result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param limit maximum number of rows to return
     * @return parsed results of the EXPLAIN
     */
    public ExplainResult explain(Query query, int start, int limit);

    /**
     * Runs a COUNT(*) on the given query
     *
     * @param query the query Object
     * @return count of rows produced by the query
     */
    public int count(Query query);

    /**
     * Sets the database object that this PersistenceBroker object carries around.
     *
     * @param db the Database object
     */
    public void setDatabase(Database db);

    /**
     * Gets the database object from this PersistenceBroker object.
     *
     * @return the Database object
     */
    public Database getDatabase();
}
