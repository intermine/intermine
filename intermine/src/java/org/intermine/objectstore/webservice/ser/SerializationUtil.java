package org.intermine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.namespace.QName;

import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.xml.lite.LiteParser;
import org.intermine.xml.lite.LiteRenderer;

import org.apache.axis.encoding.TypeMapping;

import org.apache.log4j.Logger;

/**
 * Utilities used by (de)serializers
 *
 * @author Mark Woodbridge
 */
public class SerializationUtil
{
    protected static final Logger LOG = Logger.getLogger(SerializationUtil.class);

    /**
     * Register type mappings for the 5 built-in FlyMine types that are sent over the wire
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
        registerMapping(tm, org.intermine.objectstore.webservice.ser.FlyMineBusinessString.class);
        registerMapping(tm, org.intermine.objectstore.query.fql.FqlQuery.class);
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

    /**
     * Use the LiteParser to produce a business object from its serialized string version
     * @param string the FlyMineBusinessString representation of the object
     * @param os the ObjectStore used by LiteParser to parse the string
     * @return the corresponding object
     */
    public static InterMineObject stringToObject(FlyMineBusinessString string,
                                                       ObjectStore os) {
        InterMineObject obj = null;
        try {
            obj = LiteParser.parseXml(new ByteArrayInputStream(string.getString().getBytes()), os);
            os.cacheObjectById(obj.getId(), obj);
        } catch (Exception e) {
            LOG.error("Error in parsing FlyMineBusinessString returned from ObjectStoreServer");
        }
        return obj;
    }

    /**
     * Recurse through a collection converting InterMineObjects
     * to FlyMineBusinessStrings suitable for sending over the wire
     * @param c the Collection
     * @param model the relevant model, used by LiteRenderer
     * @return the corresponding list
     */
    public static List collectionToStrings(Collection c, Model model) {
        List l = new ArrayList();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof Collection) {
                l.add(collectionToStrings((Collection) o, model));
            } else if (o instanceof InterMineObject) {
                l.add(objectToString((InterMineObject) o, model));
            } else {
                l.add(o);
            }
        }
        return l;
    }

    /**
     * Use the LiteRenderer to produce a string from business object for serialization
     * @param obj the object
     * @param model the model used by LiteRendered to render the object
     * @return the corresponding FlyMineBusinessString
     */
    public static FlyMineBusinessString objectToString(InterMineObject obj, Model model) {
        return new FlyMineBusinessString(LiteRenderer.renderXml(obj, model));
    }

    /**
     * Recurse through a collection converting FlyMineBusinessStrings
     * sent over the wire to InterMineObjects
     * @param c the Collection
     * @param os the relevant ObjectStore, used by LiteRenderer
     * @return the corresponding list
     */
    public static List collectionToObjects(Collection c, ObjectStore os) {
        List l = new ArrayList();
        for (Iterator i = c.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof Collection) {
                l.add(collectionToObjects((Collection) o, os));
            } else if (o instanceof FlyMineBusinessString) {
                l.add(stringToObject((FlyMineBusinessString) o, os));
            } else {
                l.add(o);
            }
        }
        return l;
    }
}
