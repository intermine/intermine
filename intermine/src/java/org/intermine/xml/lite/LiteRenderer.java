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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Render an object in FlyMine Lite XML format
 *
 * @author Andrew Varley
 */
public class LiteRenderer
{
    protected static final Logger LOG = Logger.getLogger(LiteRenderer.class);

    /**
     * Don't allow construction
     */
    private LiteRenderer() {
    }

    /**
     * Render the given object as XML in FlyMine Lite format
     *
     * @param obj the object to render
     * @return the XML for that object
     */
    public static String render(Object obj) {
        StringBuffer sb = new StringBuffer();

        sb.append("<object class=\"")
            .append(getClassName(obj))
            .append("\" implements=\"")
            .append(getImplements(obj))
            .append("\">")
            .append(getFields(obj))
            .append("</object>");
        return sb.toString();
    }

    /**
     * Get all interfaces that an object implements.
     *
     * @param obj the object
     * @return space separated list of extended/implemented classes/interfaces
     */
    protected static String getImplements(Object obj) {
        StringBuffer sb = new StringBuffer();

        Class [] interfaces = obj.getClass().getInterfaces();

        for (int i = 0; i < interfaces.length; i++) {
            sb.append(interfaces[i].getName())
                .append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Get all interfaces that an object implements.
     *
     * @param obj the object
     * @return space separated list of extended/implemented classes/interfaces
     */
    protected static String getClassName(Object obj) {
        StringBuffer sb = new StringBuffer();

        // This class - will need to be cleverer when dynamic classes introduced
        sb.append(obj.getClass().getName())
            .append(" ");
        return sb.toString().trim();
    }

    /**
     * Get all classes and interfaces that an object extends/implements.
     *
     * @param obj the object
     * @return string separated list of extended/implemented classes/interfaces
     */
    protected static String getFields(Object obj) {
        StringBuffer sb = new StringBuffer();

        Map infos = TypeUtil.getFieldInfos(obj.getClass());
        Iterator iter = infos.keySet().iterator();
        try {
            while (iter.hasNext()) {
                // If reference, value is id of referred-to object
                // If field, value is field value
                // If collection, no element output
                // Element is not output if the value is null

                String fieldname = (String) iter.next();
                Object value = TypeUtil.getFieldValue(obj, fieldname);

                if (value == null) {
                    continue;
                }
                // Collection
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    continue;
                }

                Object id = null;

                // Reference
                try {
                    Method m = value.getClass().getMethod("getId", new Class[] {});
                    id = m.invoke(value, null);
                } catch (InvocationTargetException e) {
                } catch (IllegalAccessException e) {
                } catch (NoSuchMethodException e) {
                }
                if (id != null) {
                    value = id;
                }
                sb.append((id == null ? "<field" : "<reference") + " name=\"")
                    .append(fieldname)
                    .append("\" value=\"");
                    if (value instanceof Date) {
                        sb.append(((Date) value).getTime());
                    } else {
                        sb.append(value);
                    }
                    sb.append("\"/>");
            }
        } catch (IllegalAccessException e) {
        }
        return sb.toString().trim();
    }
}
