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



import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.accesslayer.sql.SqlGeneratorDefaultImpl;
import org.apache.ojb.broker.platforms.Platform;

/**
 * SqlGeneratorFlymineImpl
 *
 * @author Richard Smith
 */

public class SqlGeneratorFlymineImpl extends SqlGeneratorDefaultImpl
{

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
     * @param cld ClassDescriptor
     *
     * @return sql statement as String
     */
    public String getPreparedSelectStatement(Query query, ClassDescriptor cld) {
        // throw away ClassDescriptor!

        // TODO: check for statement in cache...

        // get the flymine query.  Currently ignores StatementManager, should it be changed?

        QueryPackage qPackage = (QueryPackage) query;
        org.flymine.objectstore.query.Query flymineQuery = qPackage.getQuery();

        ClassDescriptor[] clds = qPackage.getDescriptors();

        FlymineSqlSelectStatement sql = new FlymineSqlSelectStatement(flymineQuery, clds);
        String result = sql.getStatement();

        return result;
    }




    // block execution of OJB queries (?)
    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param query an OJB query object
     * @param cld its associated ClassDescriptor
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    public String getSelectStatementDep(Query query, ClassDescriptor cld) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
    }

    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param table the indirection table to be used
     * @param selectColumns a list of columns in the select
     * @param columns a list of other columns
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    public String getSelectMNStatement(String table, String[] selectColumns, String[] columns) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
    }

    /**
     * Refuses to create a Select statement for the OJB-specific statement type.
     *
     * @param cld a ClassDescriptor
     * @return never
     * @throws UnsupportedOperationException all the time
     */
    public String getPreparedSelectByPkStatement(ClassDescriptor cld) {
        throw (new UnsupportedOperationException("Method not "
                    + "supported in SqlGeneratorFlymineImpl"));
    }

}
