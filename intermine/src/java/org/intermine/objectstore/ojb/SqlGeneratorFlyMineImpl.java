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

/**
 * SqlGeneratorFlyMineImpl
 *
 * @author Richard Smith
 */

public class SqlGeneratorFlyMineImpl extends SqlGeneratorDefaultImpl
{
    protected static final org.apache.log4j.Logger LOG
        = org.apache.log4j.Logger.getLogger(SqlGeneratorFlyMineImpl.class);

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
