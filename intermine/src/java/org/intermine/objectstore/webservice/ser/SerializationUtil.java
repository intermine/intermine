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

import org.flymine.objectstore.query.fql.FqlQuery;
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
     * Bean that represents a proxy (reference or collection), used in serialization
     */
    public static class ProxyBean
    {
        String type;
        FqlQuery fqlQuery;
        Integer id;

        /**
         * Constructor
          * @param type the type of the underlying object
          * @param fqlQuery the query to retrieve the object
          * @param id the internal id of the underlying object
          */
        public ProxyBean(String type, FqlQuery fqlQuery, Integer id) {
            this.type = type;
            this.fqlQuery = fqlQuery;
            this.id = id;
        }

        /**
         * Returns the type of the underlying object
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the query to retrieve the object
         * @return the query
         */
        public FqlQuery getFqlQuery() {
            return fqlQuery;
        }

        /**
         * Returns the internal id of the underlying object
         * @return the id
         */
        public Integer getId() {
            return id;
        }
    }
}
