package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.datatracking.Source;
import org.flymine.model.datatracking.Field;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.QueryObjectReference;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ConstraintOp;

/**
 * Class providing API for datatracking
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class DataTracking
{
    /**
     * Retrieve the Source for a specified field of an object stored in the database
     *
     * @param obj the object
     * @param field the name of the field
     * @param os the ObjectStore used for datatracking
     * @return the Source
     * @throws ObjectStoreException if an error occurs
     */
    public static Source getSource(FlyMineBusinessObject obj, String field, ObjectStore os)
        throws ObjectStoreException {
        // select source from source, field where field.source contains source
        // and field.name = name and field.objectId = objectId
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Source.class);
        QueryClass qc2 = new QueryClass(Field.class);
        QueryReference qr = new QueryObjectReference(qc2, "source");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc2);
        QueryField qf1 = new QueryField(qc2, "name");
        SimpleConstraint sc1 = new SimpleConstraint(qf1,
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue(field));
        QueryField qf2 = new QueryField(qc2, "objectId");
        SimpleConstraint sc2 = new SimpleConstraint(qf2,
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue(obj.getId()));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc);
        cs.addConstraint(sc1);
        cs.addConstraint(sc2);
        q.setConstraint(cs);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc1);

        Results res = os.execute(q);
        if (res.size() == 0) {
            throw new ObjectStoreException("No source for field '" + field
                                           + "' in object with id '" + obj.getId() + "'");
        }
        if (res.size() > 1) {
            throw new ObjectStoreException("Found more than one source for field '" + field
                                           + "' in object with id '" + obj.getId() + "'");
        }
        return (Source) ((ResultsRow) res.get(0)).get(0);
    }

    /**
     * Set the Source for a field of an object stored in the database
     * 
     * @param obj the object
     * @param field the name of the field
     * @param source the Source of the field
     * @param osw the ObjectStoreWriter used for data tracking
     * @throws ObjectStoreException if an error occurs
     */
    public static void setSource(FlyMineBusinessObject obj, String field, Source source,
                                 ObjectStoreWriter osw) throws ObjectStoreException {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("obj id is null");
        }
        if (source.getId() == null) {
            throw new IllegalArgumentException("source id is null");
        }
        Field f = new Field();
        f.setObjectId(obj.getId());
        f.setName(field);
        Set s = new HashSet();
        s.add("objectId");
        s.add("name");
        Field existingField = (Field) osw.getObjectByExample(f, s);
        if (existingField == null) {
            f.setSource(source);
            osw.store(f);
        } else {
            existingField.setSource(source);
            osw.store(existingField);
        }
    }
}
