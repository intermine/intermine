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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Calendar;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.xml.lite.LiteParser;
import org.intermine.xml.lite.LiteRenderer;

import org.apache.log4j.Logger;

/**
 * Utilities used by (de)serializers
 *
 * @author Mark Woodbridge
 */
public class SerializationUtil
{
    private static final Logger LOG = Logger.getLogger(SerializationUtil.class);

    /**
     * Use the LiteParser to produce a business object from its serialized string version
     * @param string the InterMineString representation of the object
     * @param os the ObjectStore used by LiteParser to parse the string
     * @return the corresponding object
     */
    public static InterMineObject stringToObject(InterMineString string,
                                                       ObjectStore os) {
        InterMineObject obj = null;
        try {
            obj = LiteParser.parseXml(new ByteArrayInputStream(string.getString().getBytes()), os);
            os.cacheObjectById(obj.getId(), obj);
        } catch (Exception e) {
            LOG.error("Error in parsing InterMineString returned from ObjectStoreServer");
        }
        return obj;
    }

    /**
     * Recurse through a collection converting InterMineObjects
     * to InterMineStrings suitable for sending over the wire
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
     * @return the corresponding InterMineString
     */
    public static InterMineString objectToString(InterMineObject obj, Model model) {
        return new InterMineString(LiteRenderer.renderXml(obj, model));
    }

    /**
     * Recurse through a collection converting InterMineStrings
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
            } else if (o instanceof InterMineString) {
                l.add(stringToObject((InterMineString) o, os));
            } else if (o instanceof Calendar) {
                //axis sends dates as calendars to provide timezone context
                //because not all platforms represent dates as milliseconds since 1970 or whenever
                //we don't handle calendars as fields so it's safe to convert back indiscriminantly
                l.add(((Calendar) o).getTime());
            } else {
                l.add(o);
            }
        }
        return l;
    }
}
