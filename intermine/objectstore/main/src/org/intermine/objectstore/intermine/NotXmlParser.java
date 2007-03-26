package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
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
    protected static final String DELIM = "$_^";
    protected static final String ENCODED_DELIM = "d";

    /**
     * Parse the given NotXml String into an Object.
     *
     * @param xml the NotXml String
     * @param os the ObjectStore from which to create lazy objects
     * @return an InterMineObject
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static InterMineObject parse(String xml, ObjectStore os) throws ClassNotFoundException {
        String a[] = StringUtil.split(xml, DELIM);
        
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
                Class fieldClass = TypeUtil.getFieldInfo(retval.getClass(), fieldName).getType();
                StringBuffer string = new StringBuffer(i + 1 == a.length ? "" : a[i + 1]);
                while ((i + 2 < a.length) && (a[i + 2].startsWith(ENCODED_DELIM))) {
                    i++;
                    string.append(DELIM).append(a[i + 1].substring(1));
                }
                TypeUtil.setFieldValue(retval, fieldName,
                        TypeUtil.stringToObject(fieldClass, string.toString()));
            } else if (a[i].startsWith("r")) {
                String fieldName = a[i].substring(1);
                Integer id = Integer.valueOf(a[i + 1]);
                ReferenceDescriptor ref = (ReferenceDescriptor) fields.get(fieldName);
                if (ref == null) {
                    throw new RuntimeException("failed to get field " + fieldName
                            + " for object from XML: " + xml);
                }
                TypeUtil.setFieldValue(retval, fieldName,
                        new ProxyReference(os, id, ref.getReferencedClassDescriptor().getType()));
            }
        }

        Iterator collIter = fields.entrySet().iterator();
        while (collIter.hasNext()) {
            Map.Entry collEntry = (Map.Entry) collIter.next();
            Object maybeColl = collEntry.getValue();
            if (maybeColl instanceof CollectionDescriptor) {
                CollectionDescriptor coll = (CollectionDescriptor) maybeColl;
                Collection lazyColl = new ProxyCollection(os, retval, coll.getName(),
                        coll.getReferencedClassDescriptor().getType());
                TypeUtil.setFieldValue(retval, coll.getName(), lazyColl);
            }
        }
        return retval;
    }
}

