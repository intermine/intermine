package org.intermine.xml.lite;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.*;

import org.xml.sax.SAXException;

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
import org.intermine.util.DynamicBean;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * Read XML Lite format into an Object
 *
 * @author Andrew Varley
 */
public class LiteParser
{
    protected static final String DELIM = "\\$_\\^";

    /**
     * Parse a InterMine Lite XML file
     *
     * @param is the InputStream to parse
     * @param os the ObjectStore with which to associate any new lazy objects
     * @return an object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static InterMineObject parseXml(InputStream is, ObjectStore os)
        throws IOException, SAXException, ClassNotFoundException {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("object", Item.class);
        digester.addSetProperties("object", "id", "id");
        digester.addSetProperties("object", "class", "className");
        digester.addSetProperties("object", "implements", "implementations");

        digester.addObjectCreate("object/field", Field.class);
        digester.addSetProperties("object/field", "name", "name");
        digester.addSetProperties("object/field", "value", "value");

        digester.addObjectCreate("object/reference", Field.class);
        digester.addSetProperties("object/reference", "name", "name");
        digester.addSetProperties("object/reference", "value", "value");

        digester.addSetNext("object/field", "addField");
        digester.addSetNext("object/reference", "addReference");

        InterMineObject retval = convertToObject(((Item) digester.parse(is)), os);
        return retval;
    }

    /**
     * Parse string representation of a InterMineObject as used in databases.
     *
     * @param objStr the string to parse
     * @param os the ObjectStore with which to associate any new lazy objects
     * @return an object
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static InterMineObject parse(String objStr, ObjectStore os)
        throws ClassNotFoundException {
        String a[] = objStr.split(DELIM);

        Item item = new Item();
        item.setClassName(a[0]);
        item.setImplementations(a[1]);
        for (int i = 2; i < a.length; i += 2) {
            Field f = new Field();
            f.setName(a[i].substring(1));
            if (i + 1 == a.length) {
                f.setValue("");
            } else {
                f.setValue(a[i + 1]);
            }

            if (a[i].startsWith("a")) {
                item.addField(f);
            } else if (a[i].startsWith("r")) {
                item.addReference(f);
            }
        }
        return convertToObject(item, os);
    }


    /**
     * Convert Item to object
     *
     * @param item the Item to convert
     * @param os the ObjectStore with which to associate any new lazy objects
     * @return the converted object
     * @throws ClassNotFoundException if a class cannot be found
     */
    protected static InterMineObject convertToObject(Item item, ObjectStore os)
            throws ClassNotFoundException {
        Class clazz = null;
        if ((item.getClassName() != null) && (!"".equals(item.getClassName()))) {
            clazz = Class.forName(item.getClassName());
        }
        List interfaces = StringUtil.tokenize(item.getImplementations());
        Iterator intIter = interfaces.iterator();
        List intClasses = new ArrayList();
        while (intIter.hasNext()) {
            String className = (String) intIter.next();
            if (!"net.sf.cglib.Factory".equals(className)) {
                Class intClass = Class.forName(className);
                if ((clazz == null) || (!intClass.isAssignableFrom(clazz))) {
                    intClasses.add(Class.forName(className));
                }
            }
        }

        InterMineObject obj = null;
        if (intClasses.isEmpty()) {
            try {
                obj = (InterMineObject) clazz.newInstance();
            } catch (Exception e) {
                throw new ClassNotFoundException(e.getMessage());
            }
        } else {
            obj = (InterMineObject) DynamicBean.create(clazz,
                    (Class []) intClasses.toArray(new Class [] {}));
        }

        // Set the data for every given Field
        Iterator fieldIter = item.getFields().iterator();
        while (fieldIter.hasNext()) {
            Field field = (Field) fieldIter.next();
            Class fieldClass = TypeUtil.getFieldInfo(obj.getClass(), field.getName()).getType();
            TypeUtil.setFieldValue(obj, field.getName(),
                                   TypeUtil.stringToObject(fieldClass, field.getValue()));
        }

        // Set the data for every given reference
        Map fields = os.getModel().getFieldDescriptorsForClass(obj.getClass());
        Iterator refIter = item.getReferences().iterator();
        while (refIter.hasNext()) {
            Field field = (Field) refIter.next();
            Integer id = new Integer(Integer.parseInt(field.getValue()));
            ReferenceDescriptor ref = (ReferenceDescriptor) fields.get(field.getName());
            ProxyReference proxyReference =
                new ProxyReference(os, id, ref.getReferencedClassDescriptor().getType());
            TypeUtil.setFieldValue(obj, field.getName(), proxyReference);
        }

        // Set the data for every given Collection
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
                    String revRefDescriptor = coll.getReverseReferenceDescriptor().getName();
                    QueryObjectReference qor = new QueryObjectReference(qc1, revRefDescriptor);
                    ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, obj);
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
                    
                    QueryCollectionReference queryCollectionRef =
                        new QueryCollectionReference(qc1, coll.getName());
                    cs.addConstraint(new ContainsConstraint(queryCollectionRef, 
                                     ConstraintOp.CONTAINS, qc2));
                    cs.addConstraint(new SimpleConstraint(new QueryField(qc1, "id"),
                                                          ConstraintOp.EQUALS,
                                                          new QueryValue(obj.getId())));
                    q.setConstraint(cs);
                    q.setDistinct(false);
                }
                Collection lazyColl = new SingletonResults(q, os, os.getSequence());
                TypeUtil.setFieldValue(obj, coll.getName(), lazyColl);
            }
        }
        return obj;
    }
}
