package org.flymine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import org.apache.axis.encoding.TypeMapping;

import org.flymine.util.StringUtil;
import org.flymine.util.TypeUtil;

/**
 * Utilities used by (de)serializers
 *
 * @author Mark Woodbridge
 */
public class SerializationUtil
{
    protected static final Logger LOG = Logger.getLogger(SerializationUtil.class);

    /**
     * Return the "gettable" fields (i.e. those with a getter) of a Class (not necessarily a bean)
     *
     * @param c the class
     * @return the fields
     */
    public static Collection gettableFields(Class c) {
        Collection gettableFields = new HashSet();
        do {
            Field[] fields = c.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                try {
                    c.getDeclaredMethod("get" + StringUtil.capitalise(fields[i].getName()),
                                        new Class[0]);
                    gettableFields.add(fields[i]);
                } catch (NoSuchMethodException e) {
                }
            }
        } while ((c = c.getSuperclass()) != null);
        return gettableFields;
    }

    /**
     * Return the "gettable" fields of an Object and their corresponding values
     *
     * @param o the object
     * @return a map from Fields to values
     */
    public static Map fieldValues(Object o) {
        Iterator i = gettableFields(o.getClass()).iterator();
        Map fieldValues = new HashMap();
        while (i.hasNext()) {
            Field field = (Field) i.next();
            try {
                fieldValues.put(field, TypeUtil.getFieldValue(o, field.getName()));
            } catch (IllegalAccessException e) {
                LOG.debug(e);
            }
        }
        return fieldValues;
    }

    /**
     * Register the default FlyMine type mappings
     * These should match those in the axis deployment descriptor
     * @param tm the type mapping to register to
     */
    public static void registerMappings(TypeMapping tm) {
        registerDefaultMapping(tm, org.flymine.objectstore.query.fql.FqlQuery.class);
        registerDefaultMapping(tm, org.flymine.objectstore.query.ResultsInfo.class);
        registerDefaultMapping(tm, ProxyBean.class);
        tm.register(org.flymine.metadata.Model.class,
                    new QName("", "model"),
                    new ModelSerializerFactory(),
                    new ModelDeserializerFactory());
        tm.register(java.util.ArrayList.class,
                    new QName("http://soapinterop.org/xsd", "list"),
                    new ListSerializerFactory(), new ListDeserializerFactory());
    }

    /**
     * Register type with our default (bean-like) serializer
     * @param tm the type mapping to register to
     * @param type the type to register
     */
    public static void registerDefaultMapping(TypeMapping tm, Class type) {
        QName qname = new QName("", TypeUtil.unqualifiedName(type.getName()));
        tm.register(type,
                    qname,
                    new DefaultSerializerFactory(),
                    new DefaultDeserializerFactory(type, qname));
    }
}
