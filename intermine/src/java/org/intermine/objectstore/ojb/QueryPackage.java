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

import org.flymine.objectstore.query.QueryClass;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;

import java.util.HashSet;
import java.util.Iterator;


/**
 * A holder for a flymine Query object and associated array of OJB ClassDescriptors.
 * This objects extends OJB's abstract query so that it can be passed between classes
 * as if it was a Query class.  Necessary because factories used in the PersistenceBroker
 * are based on interfaces such as JdbcAccess and SqlGenerator that specify methods with
 * an OJB Query as an argument.  <BR> All methods of origianl Query are overidden with null
 * implementations, getQuery and getDescriptors are added.  Constructor checks that
 * ClassDescriptor[] corresponds to Query.
 *
 * @author Richard Smith
 */

public class QueryPackage extends org.apache.ojb.broker.query.AbstractQueryImpl implements Query
{

    private org.flymine.objectstore.query.Query passengerQuery;
    private ClassDescriptor[] clds;


    /**
     * Constructor checks that ClassDEscriptor array corresponds to the flymine Query.
     * There should be a ClassDescriptor for each QueryClass in the FROM clause
     *
     * @param query The flymine query to be packaged and transported.
     * @param clds ojb metadata to describe all classes in from clause of Query.
     *
     */
    public QueryPackage(org.flymine.objectstore.query.Query query, ClassDescriptor[] clds) {
        if (query == null) {
            throw (new NullPointerException("query cannont be null"));
        }
        if (clds == null) {
            throw (new NullPointerException("clds array cannot be null"));
        }

        // check that there is a ClassDescriptor for each class type in FROM clause
        HashSet from = new HashSet(query.getFrom());
        Iterator fromIter = from.iterator();
        HashSet classes = new HashSet();

        for (int i = 0; i < clds.length; i++) {
            Class cls;
            if (clds[i] != null) {
                cls = clds[i].getClassOfObject();
                if (!classes.contains(cls.getName())) {
                    classes.add(cls.getName());
                }
            }
        }

        int j = 0;
        while (fromIter.hasNext()) {
            QueryClass fromElement = (QueryClass) fromIter.next();
            Class fromClass = fromElement.getType();
            if (!classes.contains(fromClass.getName())) {
                throw (new IllegalArgumentException("No ClassDescriptor found for class: "
                                                    + fromClass.getName() + "j = " + j));
            }
            j++;
        }

        // Does it matter if we have extra ClassDescriptors?

        this.passengerQuery = query;
        this.clds = clds;
    }


    /**
     * Return an array of ClassDescriptors for this query.
     *
     * @return array of ClassDescriptors for query
     */
    public ClassDescriptor[] getDescriptors() {
        return this.clds;
    }


    /**
     * Return the packaged flymine Query
     *
     * @return a flymine Query object
     */
    public org.flymine.objectstore.query.Query getQuery() {
        return this.passengerQuery;
    }


    /**** empty implementations for abstract methods ****/

    /**
     * Method declaration
     *
     * @return nothing
     *
     */
    public Criteria getCriteria() {
        throw (new UnsupportedOperationException("This is a dummy OJB Query "
                                                 + "used to pass a FlyMine query between classes"));
    }

    /**
     * Method declaration
     *
     * @return an object
     */
    public java.lang.Object getExampleObject() {
        throw (new UnsupportedOperationException("This is a dummy OJB Query  "
                                                 + "used to pass a FlyMine query between classes"));
    }

    /**
     * Method declaration
     *
     * @return a class
     */
    public Class getSearchClass() {
        throw (new UnsupportedOperationException("This is a dummy OJB Query "
                                                 + "used to pass a FlyMine query between classes"));
    }

    /**
     * Method declaration
     *
     * @return a boolean
     */
    public boolean isDistinct() {
        throw (new UnsupportedOperationException("This is a dummy OJB Query "
                                                 + "used to pass a FlyMine query between classes"));
    }
}


