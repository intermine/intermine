package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.objectstore.intermine.NotXmlParser.DELIM;
import static org.intermine.objectstore.intermine.NotXmlParser.ENCODED_DELIM;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.TypeUtil.FieldInfo;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.model.StringConstructor;
import org.intermine.objectstore.query.ClobAccess;

/**
 * Render on object into a String suitable for storing in the OBJECT field of database tables.
 *
 * @author Matthew Wakeling
 */
public final class NotXmlRenderer
{
    private NotXmlRenderer() {
    }

    /**
     * Render the given object as NotXml.
     *
     * @param obj the object to render
     * @return the NotXml String
     */
    public static StringConstructor render(Object obj) {
        try {
            StringConstructor sb = new StringConstructor();
            sb.append(DELIM);
            boolean needComma = false;
            for (Class<?> clazz : Util.decomposeClass(obj.getClass())) {
                if (needComma) {
                    sb.append(" ");
                }
                needComma = true;
                sb.append(clazz.getName());
            }

            Map<String, FieldInfo> infos = TypeUtil.getFieldInfos(obj.getClass());
            for (String fieldName : infos.keySet()) {
                // If reference, value is id of referred-to object
                // If field, value is field value
                // If collection, no element output
                // Element is not output if the value is null
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
                        } else if (value instanceof ClobAccess) {
                            sb.append(((ClobAccess) value).getDbDescription());
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
