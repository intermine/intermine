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

import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.accesslayer.sql.SqlGeneratorDefaultImpl;
import org.apache.ojb.broker.platforms.Platform;

import org.flymine.objectstore.query.Query;

import org.apache.log4j.Logger;

/**
 * SqlGeneratorFlyMineImpl
 *
 * @author Richard Smith
 */
public class SqlGeneratorFlyMineImpl extends SqlGeneratorDefaultImpl
{
    protected static final Logger LOG = Logger.getLogger(SqlGeneratorFlyMineImpl.class);

    /**
     * Constructor, chains to SqlGeneratorDefaultImpl constructor
     *
     * @param pf the database to be used
     */
    public SqlGeneratorFlyMineImpl(Platform pf) {
        super(pf);
    }

    /**
     * generate a select-Statement according to query
     * @param query the Query
     * @param dr DescriptorRepository for the database
     * @param start the number of the first row to return, numbered from zero
     * @param limit the maximum number of rows to return
     * @return sql statement as String
     */
    public String getPreparedSelectStatement(Query query, DescriptorRepository dr, int start,
            int limit) {

        // caching of generated sql statements requires that a proper equals() method
        // is implemented for org.flymine.objectstore.Query.  Most queries tested take
        // around 1ms to produce so a cache would be of limited value unless this changes.

        FlyMineSqlSelectStatement sql = new FlyMineSqlSelectStatement(query, dr);
        String result = sql.getStatement();

        if (result != null && (start > 0 || limit < Integer.MAX_VALUE)) {
            result += (" LIMIT " + limit + " OFFSET " + start);
        }
        return result;
    }

    /**
     * generate a select statement that will run COUNT(*) on the given query
     * @param query the Query
     * @param dr DescriptorRepository for the database
     * @return sql statement as String
     */
    public String getPreparedCountStatement(Query query, DescriptorRepository dr) {

        FlyMineSqlSelectStatement sql = new FlyMineSqlSelectStatement(query, dr, false, true);
        return sql.getStatement();
    }
}
