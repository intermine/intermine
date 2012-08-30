package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

/**
 * Bio utility methods for queries.
 * @author Kim Rutherford
 */
public abstract class BioQueries
{
    private BioQueries() {
        //disable external instantiation
    }

    /**
     * Query ObjectStore for all Location object between given object (eg. Chromosome) and
     * subject (eg. Gene) classes.  Return an iterator over the results ordered by subject if
     * orderBySubject is true, otherwise order by object.
     * @param os the ObjectStore to find the Locations in
     * @param objectCls object type of the Location
     * @param subjectCls subject type of the Location
     * @param orderBySubject if true order the results using the subjectCls, otherwise order by
     * objectCls
     * @param hasLength if true, only query locations where the objectCls object has a non-zero
     * length, e.g. a chromosome's length should be greater than zero
     * @param batchSize the batch size for the results object
     * @param hasChromosomeLocation if true, only query where the subject has a chromosome location
     * @return a Results object: object.id, location, subject
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Results findLocationAndObjects(ObjectStore os, Class<?> objectCls,
        Class<?> subjectCls, boolean orderBySubject, boolean hasLength,
        boolean hasChromosomeLocation, int batchSize)
        throws ObjectStoreException {
        // TODO check objectCls and subjectCls assignable to BioEntity

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(objectCls);
        QueryField qfObj = new QueryField(qcObj, "id");
        q.addFrom(qcObj);
        q.addToSelect(qfObj);
        if (!orderBySubject) {
            q.addToOrderBy(qfObj);
        }
        QueryClass qcSub = new QueryClass(subjectCls);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);
        if (orderBySubject) {
            q.addToOrderBy(qcSub);
        }
        Class<?> locationCls;
        try {
            locationCls = Class.forName("org.intermine.model.bio.Location");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        QueryClass qcLoc = new QueryClass(locationCls);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "locatedOn");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "feature");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        if (hasLength) {
            QueryField qfObjLength = new QueryField(qcObj, "length");
            SimpleConstraint lengthNotNull =
                new SimpleConstraint(qfObjLength, ConstraintOp.IS_NOT_NULL);
            cs.addConstraint(lengthNotNull);
        }

        if (hasChromosomeLocation) {
            QueryObjectReference chrLocationRef
                = new QueryObjectReference(qcSub, "chromosomeLocation");
            ContainsConstraint chrLocRefNotNull =
                new ContainsConstraint(chrLocationRef, ConstraintOp.IS_NOT_NULL);
            cs.addConstraint(chrLocRefNotNull);
        }
        q.setConstraint(cs);
        Set<QueryNode> indexesToCreate = new HashSet<QueryNode>();
        indexesToCreate.add(qfObj);
        indexesToCreate.add(qcLoc);
        indexesToCreate.add(qcSub);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
                                                   Constants.PRECOMPUTE_CATEGORY);

        /**
         * Query in a semi-SQL form:
         *
         * SELECT a1_.id AS a2_, a3_, a4_ FROM
         * org.intermine.model.bio.Chromosome AS a1_,
         * org.intermine.model.bio.SequenceFeature AS a3_,
         * org.intermine.model.bio.Location AS a4_ WHERE (a4_.locatedOn CONTAINS
         * a1_ AND a4_.feature CONTAINS a3_ AND a1_.length IS NOT NULL) ORDER BY
         * a1_.id with indexes [a2_, a3_id, a4_id, a2_, a3_id]
         */
        Results res = os.execute(q, batchSize, true, true, true);

        return res;
    }

}
