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

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.accesslayer.JdbcAccessImpl;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;
import org.apache.ojb.broker.accesslayer.ConnectionManagerIF;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * This Implementation of JdbcAccess overrides executeQuery to
 * take a flymine query and call a custom flymine SqlGenerator.
 *
 * @author Richard Smith
 */


public class JdbcAccessFlymineImpl extends JdbcAccessImpl
{

    /**
     * Constructor, calls JdbcAccessImpl constructor with broker
     *
     * @param broker the PersistenceBroker in which to execute JDBC calls
     */
    public JdbcAccessFlymineImpl (PersistenceBroker broker) {
        super(broker);
    }

    /**
     * Performs a select statement on database, returns the jdbc Statement
     * and ResultSet
     *
     * @param query should be a QueryPackage which implements OJB Query interface but
     * actually contains a FlyMine query and a ClassDescriptor array
     *
     * @return the JDBC ResultSet and Statement
     * @throws PersistenceBrokerException if anything goes worong
     */
    public ResultSetAndStatement executeQuery(QueryPackage query)
        throws PersistenceBrokerException {
        if (logger.isDebugEnabled()) {
            logger.safeDebug("executeQuery", query);
        }

        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());

        ClassDescriptor dummy = null;

        try {
            String sql = this.broker.serviceSqlGenerator().getPreparedSelectStatement(query, dummy);

            // statementManager is used to serve statements and cache statements related to a
            // partcular class (wraps a ConnectionManager).  We only want something to get a
            // connection so deal directly with ConnectionManeger (?)
            // ...
            //PreparedStatement stmt = broker.serviceStatementManager()
            // .getPreparedStatement(cld, sql, scrollable);
            //broker.serviceStatementManager().bindStatement(stmt, query.getCriteria(), cld, 1);

            // should probably put jdbc stuff somewhere else...?
            ConnectionManagerIF conMan = broker.serviceConnectionManager();
            Connection conn = conMan.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);

            ResultSet rs = stmt.executeQuery();

            retval.m_rs = rs;
            retval.m_stmt = stmt;

        } catch (Exception e) {
            // what exceptions?  Do something.
        }

        return retval;
    }
}
