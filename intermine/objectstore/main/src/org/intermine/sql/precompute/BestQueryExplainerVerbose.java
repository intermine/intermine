package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.sql.query.Query;
import org.intermine.sql.query.ExplainResult;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gets the database to explain each Query added and keeps hold of the best one, logging all
 * actions for the benefit of the IqlShell user.
 *
 * @author Matthew Wakeling
 */
public class BestQueryExplainerVerbose extends BestQueryExplainer
{
    private int unexplained = 0;
    
    /**
     * Constructs an empty BestQueryExplainerVerbose.
     *
     * @param con the Connection to use
     * @param timeLimit the time limit
     */
    public BestQueryExplainerVerbose(Connection con, long timeLimit) {
        super(con, timeLimit);
    }

    /**
     * {@inheritDoc}
     */
    protected ExplainResult getExplainResult(Query q) throws SQLException {
        if (unexplained != 0) {
            System.out .println("Did not explain " + unexplained + " queries");
            unexplained = 0;
        }
        long startTime = System.currentTimeMillis();
        ExplainResult retval = ExplainResult.getInstance(q, con);
        System.out .println("Optimiser: Explained query with " + q.getFrom().size()
                + " FROM entries took " + (System.currentTimeMillis() - startTime) + " ms, "
                + retval.toString());
        //System.out .println("Optimiser: Explained query " + q + ", took "
        //        + (System.currentTimeMillis() - startTime) + " ms, " + retval.toString());
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    protected ExplainResult getExplainResult(String q) throws SQLException {
        if (unexplained != 0) {
            System.out .println("Did not explain " + unexplained + " queries");
            unexplained = 0;
        }
        long startTime = System.currentTimeMillis();
        ExplainResult retval = ExplainResult.getInstance(q, con);
        System.out .println("Optimiser: Explained query " + q + ", took "
                + (System.currentTimeMillis() - startTime) + " ms, " + retval.toString());
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    protected void didNotExplain(Candidate c) {
        //if (c.getQuery() == null) {
        //    System.out .println("Optimiser: Not explaining query " + c.getQueryString());
        //} else {
        //    System.out .println("Optimiser: Not explaining query with " + c.getQuery().getFrom()
        //            .size() + " FROM entries");
        //}
        unexplained++;
    }
}
