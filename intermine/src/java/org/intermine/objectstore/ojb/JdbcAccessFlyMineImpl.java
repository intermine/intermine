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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.accesslayer.JdbcAccessImpl;
import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.apache.ojb.broker.metadata.DescriptorRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flymine.objectstore.query.Query;
import org.flymine.sql.query.ExplainResult;
import org.flymine.sql.precompute.QueryOptimiser;

import org.apache.log4j.Logger;

/**
 * This Implementation of JdbcAccess overrides executeQuery to
 * take a flymine query and call a custom flymine SqlGenerator.
 *
 * @author Richard Smith
 */
public class JdbcAccessFlyMineImpl extends JdbcAccessImpl
{
    protected static final Logger LOG = Logger.getLogger(JdbcAccessFlyMineImpl.class);

    /**
     * Constructor, calls JdbcAccessImpl constructor with broker
     *
     * @param broker the PersistenceBroker in which to execute JDBC calls
     */
    public JdbcAccessFlyMineImpl (PersistenceBroker broker) {
        super(broker);
    }

    /**
     * Performs a select statement on database, returns the jdbc Statement
     * and ResultSet
     *
     * @param query should be a FlyMine Query
     * @param start the number of the first row to return, starting from zero
     * @param limit the maximum number of rows to return
     * @return the JDBC ResultSet and Statement
     * @throws PersistenceBrokerException if anything goes wrong
     */
    public ResultSetAndStatement executeQuery(Query query, int start, int limit)
        throws PersistenceBrokerException {
        if (logger.isDebugEnabled()) {
            logger.safeDebug("executeQuery", query);
        }

        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());

        try {
            SqlGeneratorFlyMineImpl gen = (SqlGeneratorFlyMineImpl)
                this.broker.serviceSqlGenerator();
            DescriptorRepository dr = this.broker.getDescriptorRepository();
            String sql = gen.getPreparedSelectStatement(query, dr, start, limit);

            // StatementManager is used to serve statements and cache statements related to a
            // partcular class (wraps a ConnectionManager).  We only want something to get a
            // connection so deal directly with ConnectionManeger (?)
            // ...
            //PreparedStatement stmt = broker.serviceStatementManager()
            // .getPreparedStatement(cld, sql, scrollable);
            //broker.serviceStatementManager().bindStatement(stmt, query.getCriteria(), cld, 1);

            // should probably put jdbc stuff somewhere else...?
            ConnectionManagerIF conMan = broker.serviceConnectionManager();
            Connection conn = conMan.getConnection();
            sql = QueryOptimiser.optimise(sql,
                    ((PersistenceBrokerFlyMineImpl) broker).getDatabase());
            PreparedStatement stmt = conn.prepareStatement(sql);

            ResultSet rs = stmt.executeQuery();

            retval.m_rs = rs;
            retval.m_stmt = stmt;

        } catch (PersistenceBrokerException e) {
            logger.error("PersistenceBrokerException during the execution of the query: "
                         + e.getMessage(), e);
            // ResultSetAndStatement opened before try, must be released if a problem
            if (retval != null) {
                retval.close();
            }
            throw e;
        } catch (SQLException e) {
            logger.error("SQLException during the execution of the query: " + e.getMessage(), e);
            // ResultSetAndStatement opened before try, must be released if a problem
            if (retval != null) {
                retval.close();
            }
            throw new PersistenceBrokerSQLException(e);
        } catch (LookupException e) {
            throw new PersistenceBrokerException(
                    "ConnectionManager instance could not obtain a connection", e);
        }
        return retval;
    }

    /**
     * Runs an EXPLAIN for given query with LIMIT and OFFSET values
     *
     * @param query should be a FlyMine Query
     * @param start the number of the first row to return, starting from zero
     * @param limit the maximum number of rows to return
     * @return parsed results of EXPLAIN
     * @throws PersistenceBrokerException if anyhting goes wrong
     */
    public ExplainResult explainQuery(Query query, int start, int limit)
        throws PersistenceBrokerException {

        PreparedStatement stmt = null;
        ExplainResult explain = null;

        try {
            SqlGeneratorFlyMineImpl gen = (SqlGeneratorFlyMineImpl)
                this.broker.serviceSqlGenerator();
            DescriptorRepository dr = this.broker.getDescriptorRepository();

            String sql = "EXPLAIN " + gen.getPreparedSelectStatement(query, dr, start, limit);

            sql = QueryOptimiser.optimise(sql,
                    ((PersistenceBrokerFlyMineImpl) broker).getDatabase());

            ConnectionManagerIF conMan = broker.serviceConnectionManager();
            Connection conn = conMan.getConnection();
            stmt = conn.prepareStatement(sql);

            explain = ExplainResult.getInstance(stmt);
        } catch (PersistenceBrokerException e) {
            logger.error("PersistenceBrokerException during the explanantion of the query: "
                         + e.getMessage(), e);
            throw e;
        } catch (SQLException e) {
            logger.error("SQLException exlaining the query: " + e.getMessage(), e);
            // PreparedStatement opened before try, must be released if a problem
            throw new PersistenceBrokerSQLException(e);
        } catch (LookupException e) {
            throw new PersistenceBrokerException(
                    "ConnectionManager instance could not obtain a connection", e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
            // pb.servicePlatform().afterStatementClose();
        }

        return explain;
    }

    /**
     * Runs a COUNT(*) on the given query.
     *
     * @param query a FlyMine Query object to COUNT
     * @return number of rows to be returned from the query
     * @throws PersistenceBrokerException if anyhting goes wrong
     */
    public int countQuery(Query query)
        throws PersistenceBrokerException {

        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());

        PreparedStatement stmt = null;
        int count = -1;

        try {
            SqlGeneratorFlyMineImpl gen = (SqlGeneratorFlyMineImpl)
                this.broker.serviceSqlGenerator();
            DescriptorRepository dr = this.broker.getDescriptorRepository();

            String sql = gen.getPreparedCountStatement(query, dr);

            sql = QueryOptimiser.optimise(sql,
                    ((PersistenceBrokerFlyMineImpl) broker).getDatabase());

            ConnectionManagerIF conMan = broker.serviceConnectionManager();
            Connection conn = conMan.getConnection();
            stmt = conn.prepareStatement(sql);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new PersistenceBrokerException("ResultSet was empty doing count on query");
            }

            count = rs.getInt(1);

            if (count == -1) {
                throw new PersistenceBrokerException("Error retieving count on query");
            }

        } catch (PersistenceBrokerException e) {
            logger.error("PersistenceBrokerException during the explanantion of the query: "
                         + e.getMessage(), e);
            throw e;
        } catch (SQLException e) {
            logger.error("SQLException exlaining the query: " + e.getMessage(), e);
            // PreparedStatement opened before try, must be released if a problem
            throw new PersistenceBrokerSQLException(e);
        } catch (LookupException e) {
            throw new PersistenceBrokerException(
                    "ConnectionManager instance could not obtain a connection", e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                }
            }
            // pb.servicePlatform().afterStatementClose();
        }

        return count;
    }
}
