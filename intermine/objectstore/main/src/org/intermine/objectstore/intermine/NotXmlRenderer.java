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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringConstructor;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Render on object into a String suitable for storing in the OBJECT field of database tables.
 *
 * @author Matthew Wakeling
 */
public class NotXmlRenderer
{
    private static final Logger LOG = Logger.getLogger(NotXmlRenderer.class);
    protected static final String DELIM = "$_^";
    protected static final String ENCODED_DELIM = "d";

    /**
     * Render the given object as NotXml.
     *
     * @param obj the object to render
     * @return the NotXml String
     */
    public static StringConstructor render(InterMineObject obj) {
        try {
            StringConstructor sb = new StringConstructor();
            sb.append(DELIM);
            boolean needComma = false;
            Iterator classIter = DynamicUtil.decomposeClass(obj.getClass()).iterator();
            while (classIter.hasNext()) {
                if (needComma) {
                    sb.append(" ");
                }
                needComma = true;
                Class clazz = (Class) classIter.next();
                sb.append(clazz.getName());
            }

            Map infos = TypeUtil.getFieldInfos(obj.getClass());
            Iterator fieldIter = infos.keySet().iterator();
            while (fieldIter.hasNext()) {
                // If reference, value is id of referred-to object
                // If field, value is field value
                // If collection, no element output
                // Element is not output if the value is null
                String fieldName = (String) fieldIter.next();
                Object value = TypeUtil.getFieldProxy(obj, fieldName);

                if ((value != null) && (!Collection.class.isAssignableFrom(value.getClass()))) {
                    // It is not null or a collection.
                    if (value instanceof InterMineObject) {
                        // Dereference ID of reference, and use that instead.
                        Integer id = ((InterMineObject) value).getId();
                        sb.append(DELIM);
                        sb.append("r");
                        sb.append(fieldName);
                        sb.append(DELIM);
                        sb.append(id.toString());
                    } else {
                        sb.append(DELIM);
                        sb.append("a");
                        sb.append(fieldName);
                        sb.append(DELIM);
                        if (value instanceof Date) {
                            sb.append(Long.toString(((Date) value).getTime()));
                        } else if (value instanceof String) {
                            String string = (String) value;
                            //if (string.length() > 10000000) {
                            //    sb.ensureCapacity(sb.length() + string.length() + 1000000);
                            //}
                            while (string != null) {
                                int delimPosition = string.indexOf(DELIM);
                                if (delimPosition == -1) {
                                    sb.append(string);
                                    string = null;
                                } else {
                                    sb.append(string.substring(0, delimPosition + 3));
                                    sb.append(ENCODED_DELIM);
                                    string = string.substring(delimPosition + 3);
                                }
                            }
                        } else {
                            sb.append(value.toString());
                        }
                    }
                }
            }
            //if (((sb.length() > 1000000) && ((1.3 * sb.length()) < sb.capacity()))
            //        || ((2 * sb.length()) < sb.capacity())) {
            //    // Yes, this looks silly, but it prevents the String holding a reference to a char
            //    // array that is up to twice the size of the String. We throw away the larger char
            //    // array immediately anyway.
            //    LOG.info("Converting StringBuffer (size = " + (sb.length() / 512)
            //            + " kB, capacity = " + (sb.capacity() / 512) + " kb) to String");
            //    return new String(sb.toString());
            //}
            return sb;
        } catch (IllegalAccessException e) {
            IllegalArgumentException e2 = new IllegalArgumentException();
            e2.initCause(e);
            throw e2;
        }
    }
}
