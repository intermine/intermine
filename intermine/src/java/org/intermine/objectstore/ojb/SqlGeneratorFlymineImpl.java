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


import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.accesslayer.sql.SqlGeneratorDefaultImpl;
import org.apache.ojb.broker.platforms.Platform;
import org.flymine.objectstore.query.Query;
import org.apache.log4j.Logger;
/**
 * SqlGeneratorFlymineImpl
 *
 * @author Richard Smith
 */

public class SqlGeneratorFlymineImpl extends SqlGeneratorDefaultImpl
{

    protected static final Logger LOG = Logger.getLogger(SqlGeneratorFlymineImpl.class);

    /**
     * Constructor, chains to SqlGeneratorDefaultImpl constructor
     *
     * @param pf the database to be used
     */
    public SqlGeneratorFlymineImpl(Platform pf) {
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

        // TODO - if SQL statements are to be cached this is where to do it
        // implemetation is awaiting a proper equals() method for org.flymine.objectstore.ojb.Query

        FlymineSqlSelectStatement sql = new FlymineSqlSelectStatement(query, dr);
        String result = sql.getStatement();

        if (result != null && (start > 0 && limit > 0)) {
            result += (" LIMIT " + limit + " OFFSET " + start);
        }
        return result;
    }

    // block execution of OJB queries (?) - currently need OJB queries to be used
    // e.g. when doing a store or delete
    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param query an OJB query object
     * @param cld its associated ClassDescriptor
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    /*    public String getPreparedSelectStatement(org.apache.ojb.broker.query.Query query,
            ClassDescriptor cld) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
                    }
    */

    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param query an OJB query object
     * @param cld its associated ClassDescriptor
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    /*
    public String getSelectStatementDep(org.apache.ojb.broker.query.Query query,
            ClassDescriptor cld) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
                    }
    */

    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param table the indirection table to be used
     * @param selectColumns a list of columns in the select
     * @param columns a list of other columns
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    /*
    public String getSelectMNStatement(String table, String[] selectColumns, String[] columns) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
    }
    */

    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param cld a ClassDescriptor
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    /*
    public String getPreparedSelectByPkStatement(ClassDescriptor cld) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
    }
    */
}
