package org.intermine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.xml.namespace.QName;

import org.intermine.util.TypeUtil;

import org.apache.axis.encoding.TypeMapping;

/**
 * Utilities used by (de)serializers
 *
 * @author Mark Woodbridge
 */
public class MappingUtil
{
    /**
     * Register type mappings for the 5 built-in InterMine types that are sent over the wire
     * Note that Axis only allows mappings for concrete classes (not superclasses or interfaces)
     * In particular this means that we only map ArrayLists (everything else is converted) and
     * we map InterMineObjects at the client/server level rather than at serialization time
     * @param tm the type mapping to register to
     */
    public static void registerDefaultMappings(TypeMapping tm) {
        tm.register(org.intermine.metadata.Model.class,
                    getQName(org.intermine.metadata.Model.class),
                    new ModelSerializerFactory(),
                    new ModelDeserializerFactory());
        tm.register(java.util.ArrayList.class,
                    getQName(java.util.ArrayList.class),
                    new ListSerializerFactory(),
                    new ListDeserializerFactory());
        registerMapping(tm, org.intermine.objectstore.webservice.ser.InterMineString.class);
        registerMapping(tm, org.intermine.objectstore.query.iql.IqlQuery.class);
        registerMapping(tm, org.intermine.objectstore.query.ResultsInfo.class);
    }

    /**
     * Register type with our default (bean-like) serializer
     * @param tm the type mapping to register to
     * @param type the type to register
     */
    protected static void registerMapping(TypeMapping tm, Class type) {
        tm.register(type,
                    getQName(type),
                    new DefaultSerializerFactory(),
                    new DefaultDeserializerFactory(type, getQName(type)));
    }

    /**
     * Convert a Java type to a QName
     * @param type the Java type
     * @return the QName
     */
    protected static QName getQName(Class type) {
        if (java.util.ArrayList.class.equals(type)) {
            return new QName("http://soapinterop.org/xsd", "list");
        } else {
            return new QName("", TypeUtil.unqualifiedName(type.getName()));
        }
    }
}
