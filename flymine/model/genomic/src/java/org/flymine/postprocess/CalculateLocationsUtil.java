package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

import org.flymine.model.genomic.*;

import org.apache.log4j.Logger;

/**
 * Utility methods for CalculateLocations.
 *
 * @author Kim Rutherford
 */
public class CalculateLocationsUtil
{
    private static final Logger LOG = Logger.getLogger(CalculateLocationsUtil.class);

    /**
     * Query ObjectStore for all Location object between given object and
     * subject classes.  Return an iterator over the results ordered by subject.
     * @param os the ObjectStore to find the Locations in
     * @param objectCls object type of the Location
     * @param subjectCls subject type of the Location
     * @param orderBySubject if true order the results using the subjectCls, otherwise order by
     * objectCls
     * @return an iterator over the results: object.id, location, subject
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Iterator findLocations(ObjectStore os, Class objectCls, Class subjectCls,
                                         boolean orderBySubject)
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
        q.setConstraint(cs);
// temporarily disabled - causes query failures because of missing precomputed tables
//        ((ObjectStoreInterMineImpl) os).precompute(q);
        Results res = new Results(q, os, os.getSequence());

        res.setBatchSize(5000);

        return res.iterator();
    }
}
