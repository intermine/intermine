package org.intermine.bio.util;

/* 
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

import org.flymine.model.genomic.Location;

import java.lang.String;

/**
 *
 * @author Kim Rutherford
 */
public abstract class BioQueries
{

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
     * length
     * @return a Results object: object.id, location, subject
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Results findLocationAndObjects(ObjectStore os, Class objectCls, Class subjectCls,
                                                 boolean orderBySubject, boolean hasLength)
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
        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);
    
        if (hasLength) {
            QueryField qfObjLength = new QueryField(qcObj, "length");
            SimpleConstraint lengthNotNull =
                new SimpleConstraint(qfObjLength, ConstraintOp.IS_NOT_NULL);
            cs.addConstraint(lengthNotNull);
        }
        q.setConstraint(cs);
        Set indexesToCreate = new HashSet();
        indexesToCreate.add(qfObj);
        indexesToCreate.add(qcLoc);
        indexesToCreate.add(qcSub);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
                                                   Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q);
    
        return res;
    }

}
