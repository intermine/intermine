package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Parses a String suitable for storing in the OBJECT field of database tables into an Object.
 *
 * @author Matthew Wakeling
 */
public class NotXmlParser
{
    private static final Logger LOG = Logger.getLogger(NotXmlParser.class);
    protected static final String DELIM = "\\$_\\^";

    /**
     * Parse the given NotXml String into an Object.
     *
     * @param xml the NotXml String
     * @param os the ObjectStore from which to create lazy objects
     * @return an InterMineObject
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static InterMineObject parse(String xml, ObjectStore os) throws ClassNotFoundException {
        try {
            String a[] = xml.split(DELIM);

            Set classes = new HashSet();
            if (!"".equals(a[0])) {
                classes.add(Class.forName(a[0]));
            }
            if (!"".equals(a[1])) {
                String b[] = a[1].split(" ");
                for (int i = 0; i < b.length; i++) {
                    classes.add(Class.forName(b[i]));
                }
            }
            InterMineObject retval = (InterMineObject) DynamicUtil.createObject(classes);

            Map fields = os.getModel().getFieldDescriptorsForClass(retval.getClass());
            for (int i = 2; i < a.length; i += 2) {
                if (a[i].startsWith("a")) {
                    String fieldName = a[i].substring(1);
                    Class fieldClass = TypeUtil.getFieldInfo(retval.getClass(), fieldName)
                        .getType();
                    TypeUtil.setFieldValue(retval, fieldName, TypeUtil.stringToObject(fieldClass,
                                a[i + 1]));
                } else if (a[i].startsWith("r")) {
                    String fieldName = a[i].substring(1);
                    Integer id = Integer.valueOf(a[i + 1]);
                    ReferenceDescriptor ref = (ReferenceDescriptor) fields.get(fieldName);
                    TypeUtil.setFieldValue(retval, fieldName, new ProxyReference(os, id,
                                ref.getReferencedClassDescriptor().getType()));
                }
            }

            Iterator collIter = fields.entrySet().iterator();
            while (collIter.hasNext()) {
                Map.Entry collEntry = (Map.Entry) collIter.next();
                Object maybeColl = collEntry.getValue();
                if (maybeColl instanceof CollectionDescriptor) {
                    CollectionDescriptor coll = (CollectionDescriptor) maybeColl;
                    // Now build a query - SELECT that FROM this, that WHERE this.coll CONTAINS that
                    //                         AND this = <this>
                    // Or if we have a one-to-many collection, then:
                    //    SELECT that FROM that WHERE that.reverseColl CONTAINS <this>
                    Query q = new Query();
                    if (coll.relationType() == CollectionDescriptor.ONE_N_RELATION) {
                        QueryClass qc1 = new QueryClass(coll.getReferencedClassDescriptor()
                                .getType());
                        q.addFrom(qc1);
                        q.addToSelect(qc1);
                        QueryObjectReference qor = new QueryObjectReference(qc1,
                                coll.getReverseReferenceDescriptor().getName());
                        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS,
                                retval);
                        q.setConstraint(cc);
                        q.setDistinct(false);
                    } else {
                        QueryClass qc1 = new QueryClass(coll.getClassDescriptor().getType());
                        QueryClass qc2 = new QueryClass(coll.getReferencedClassDescriptor()
                            .getType());
                        q.addFrom(qc1);
                        q.addFrom(qc2);
                        q.addToSelect(qc2);
                        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                        cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1,
                                        coll.getName()), ConstraintOp.CONTAINS, qc2));
                        cs.addConstraint(new SimpleConstraint(new QueryField(qc1, "id"),
                                    ConstraintOp.EQUALS, new QueryValue(retval.getId())));
                        q.setConstraint(cs);
                        q.setDistinct(false);
                    }
                    Collection lazyColl = new SingletonResults(q, os, os.getSequence());
                    TypeUtil.setFieldValue(retval, coll.getName(), lazyColl);
                }
            }
            return retval;
        } catch (IllegalAccessException e) {
            IllegalArgumentException e2 = new IllegalArgumentException();
            e2.initCause(e);
            throw e2;
        }
    }
}

