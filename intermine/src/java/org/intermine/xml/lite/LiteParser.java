package org.flymine.xml.lite;

/*
 * Copyright (C) 2002-2003 FlyMine
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

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.metadata.CollectionDescriptor;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.util.DynamicBean;
import org.flymine.util.StringUtil;
import org.flymine.util.TypeUtil;
import org.apache.log4j.Logger;
/**
 * Read XML Lite format into an Object
 *
 * @author Andrew Varley
 */
public class LiteParser
{
    protected static final Logger LOG = Logger.getLogger(LiteParser.class);
    protected static final String DELIM = "\\$_\\^";

    /**
     * Parse a FlyMine Lite XML file
     *
     * @param is the InputStream to parse
     * @param os the ObjectStore with which to associate any new lazy objects
     * @return an object
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static FlyMineBusinessObject parseXml(InputStream is, ObjectStore os)
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

        FlyMineBusinessObject retval = convertToObject(((Item) digester.parse(is)), os);
        return retval;
    }

    /**
     * Parse string representation of a FlyMineBusinessObject as used in databases.
     *
     * @param objStr the string to parse
     * @param os the ObjectStore with which to associate any new lazy objects
     * @return an object
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static FlyMineBusinessObject parse(String objStr, ObjectStore os)
        throws IOException, ClassNotFoundException {
        String a[] = objStr.split(DELIM);

        Item item = new Item();
        item.setClassName(a[0]);
        item.setImplementations(a[1]);
        for (int i = 2; i < a.length; i += 2) {
            Field f = new Field();
            f.setName(a[i].substring(1));
            f.setValue(a[i + 1]);

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
    protected static FlyMineBusinessObject convertToObject(Item item, ObjectStore os)
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

        FlyMineBusinessObject obj = null;
        if (intClasses.isEmpty()) {
            try {
                obj = (FlyMineBusinessObject) clazz.newInstance();
            } catch (Exception e) {
                throw new ClassNotFoundException(e.getMessage());
            }
        } else {
            obj = (FlyMineBusinessObject) DynamicBean.create(clazz,
                    (Class []) intClasses.toArray(new Class [] {}));
        }

        try {
            // Set the data for every given Field
            Iterator fieldIter = item.getFields().iterator();
            while (fieldIter.hasNext()) {
                Field field = (Field) fieldIter.next();
                Class fieldClass = TypeUtil.getFieldInfo(obj.getClass(), field.getName()).getType();
                TypeUtil.setFieldValue(obj, field.getName(),
                                       TypeUtil.stringToObject(fieldClass, field.getValue()));
            }

            // Set the data for every given reference
            Iterator refIter = item.getReferences().iterator();
            while (refIter.hasNext()) {
                Field field = (Field) refIter.next();
                Integer id = new Integer(Integer.parseInt(field.getValue()));
                TypeUtil.setFieldValue(obj, field.getName(),
                                       new ProxyReference(os, id));
            }

            // Set the data for every given Collection
            Map fields = os.getModel().getFieldDescriptorsForClass(obj.getClass());
            Iterator collIter = fields.entrySet().iterator();
            while (collIter.hasNext()) {
                Map.Entry collEntry = (Map.Entry) collIter.next();
                Object maybeColl = collEntry.getValue();
                if (maybeColl instanceof CollectionDescriptor) {
                    CollectionDescriptor coll = (CollectionDescriptor) maybeColl;
                    // Now build a query - SELECT that FROM this, that WHERE this.coll CONTAINS that
                    //                         AND this = <this>
                    Query q = new Query();
                    QueryClass qc1 = new QueryClass(coll.getClassDescriptor().getType());
                    QueryClass qc2 = new QueryClass(coll.getReferencedClassDescriptor().getType());
                    q.addFrom(qc1);
                    q.addFrom(qc2);
                    q.addToSelect(qc2);
                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                    cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qc1,
                                    coll.getName()), ConstraintOp.CONTAINS, qc2));
                    cs.addConstraint(new SimpleConstraint(new QueryField(qc1, "id"),
                                ConstraintOp.EQUALS, new QueryValue(obj.getId())));
                    q.setConstraint(cs);
                    Collection lazyColl = new SingletonResults(q, os, os.getSequence());
                    TypeUtil.setFieldValue(obj, coll.getName(), lazyColl);
                }
            }
        } catch (IllegalAccessException e) {
            IllegalArgumentException e2 = new IllegalArgumentException();
            e2.initCause(e);
            throw e2;
        }

        return obj;
    }
}
