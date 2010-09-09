package org.modmine.web.logic;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.intermine.api.profile.InterMineBag;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Utility methods for the modMine package.
 * Refer to BioUtil.
 * @author Fengyuan Hu
 */
public final class ModMineUtil
{
    private ModMineUtil() {
    }

    /**
     * For a bag of Submission objects, returns a set of Submission objects.
     * @param os ObjectStore
     * @param bag InterMineBag
     * @return Set of Submissions
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Set<Submission> getSubmissions(ObjectStore os, InterMineBag bag) {

        Query q = new Query();

        QueryClass qcObject = new QueryClass(Submission.class);

        // InterMine id for any object
        QueryField qfObjectId = new QueryField(qcObject, "id");

        q.addFrom(qcObject);
        q.addToSelect(qcObject);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        BagConstraint bc = new BagConstraint(qfObjectId, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bc);

        q.setConstraint(cs);

        Results r = os.execute(q);
        @SuppressWarnings("unchecked") Iterator<ResultsRow> it = (Iterator) r.iterator();

        Set<Submission> subs = new LinkedHashSet<Submission>();

        while (it.hasNext()) {
            ResultsRow rr = it.next();
            Submission sub =  (Submission) rr.get(0);
            subs.add(sub);
        }
        return subs;
    }
}
