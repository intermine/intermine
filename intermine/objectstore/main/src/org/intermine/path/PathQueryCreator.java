package org.intermine.path;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;

/**
 * Generate an objectstore query from a collection of paths.
 *
 * @author Richard Smith
 */

public class PathQueryCreator
{
    private boolean allowCollections = false;

    /**
     * Generate an objectstore query from a collection of paths.
     * @param paths the Path objects
     * @param attributesOnly
     * @return the new Query
     */
    public Query generate(Set paths, boolean attributesOnly) {
        Map fldToQueryClass = new HashMap();
        ClassDescriptor cld = null;

        // do some error checking first
        Iterator pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            Path path = (Path) pathIter.next();

            if (cld == null) {
                cld = path.getStartClassDescriptor();
            } else if (!cld.equals(path.getStartClassDescriptor())) {
                throw new IllegalArgumentException("Not all paths start from same class: " + paths);
            }

            // collections in path could create large results sets
            // TODO make this an option when path query in ObjectStore implemented
            if (!allowCollections && path.containsCollections()) {
                throw new IllegalArgumentException("Was given a path with a collection: " + path);
            }

            if (attributesOnly && !path.endIsAttribute()) {
                throw new IllegalArgumentException("Constructing a query for attributes only"
                                                   + " but was given a path without an attribute"
                                                   + " terminator: " + path);
            }
        }

        // generate query
        Query q = new Query();
        QueryClass qcCurrent = new QueryClass(cld.getType());
        q.addFrom(qcCurrent);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            Path path = (Path) pathIter.next();

            Iterator iter = path.getElements().iterator();
            while (iter.hasNext()) {
                FieldDescriptor fld = (FieldDescriptor) iter.next();
                if (fldToQueryClass.containsKey(fld)) {
                    // already got to this point, move on to next QueryClass
                    // if fld is an attribute will return current class
                    qcCurrent = (QueryClass) fldToQueryClass.get(fld);
                } else {
                    // new part of a path
                    if (fld.isReference()) {
                        ReferenceDescriptor rfd = (ReferenceDescriptor) fld;
                        QueryClass qcNew = new QueryClass(rfd.getReferencedClassDescriptor()
                                                           .getType());
                        QueryObjectReference ref = new QueryObjectReference(qcCurrent,
                                                                            rfd.getName());
                        ContainsConstraint cc =
                            new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcNew);
                        cs.addConstraint(cc);
                        q.addFrom(qcNew);
                        qcCurrent = qcNew;
                        fldToQueryClass.put(fld, qcNew);
                    }
                    // currently not allowing collections

                    if (fld.equals(path.getEndFieldDescriptor())) {
                        if (fld.isAttribute()) {
                            QueryField qf = new QueryField(qcCurrent, fld.getName());
                            q.addToSelect(qf);
                        } else {
                            q.addToSelect(qcCurrent);
                        }
                    }
                }
            }
        }
        if (cs.getConstraints().size() > 0) {
            q.setConstraint(cs);
        }
        return q;
    }
}
