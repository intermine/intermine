package org.intermine.xml.lite;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.util.TypeUtil;
import org.intermine.xml.XmlHelper;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;

import org.apache.log4j.Logger;

/**
 * Render an object in InterMine Lite XML format
 *
 * @author Andrew Varley
 */
public class LiteRenderer
{
    private static final Logger LOG = Logger.getLogger(LiteRenderer.class);
    protected static final String DELIM = "$_^";

    /**
     * Don't allow construction
     */
    private LiteRenderer() {
        // empty
    }

    /**
     * Render the given object as XML in InterMine Lite format
     *
     * @param obj the object to render
     * @param model the parent model
     * @return the XML for that object
     */
    public static String renderXml(InterMineObject obj, Model model) {
        Item item = objectToItem(obj, model);
        return renderXml(item);
    }


    /**
     * Render the given item as XML in InterMine Lite format
     *
     * @param item the item to render
     * @return the XML for that object
     */
    protected static String renderXml(Item item) {
        StringBuffer sb = new StringBuffer();
        sb.append("<object class=\"")
            .append(item.getClassName())
            .append("\" implements=\"")
            .append(item.getImplementations())
            .append("\">");

        Iterator i = item.getFields().iterator();
        while (i.hasNext()) {
            Field f = (Field) i.next();
            sb.append("<field name=\"")
                .append(f.getName())
                .append("\" ")
                .append("value=\"")
                .append(f.getValue())
                .append("\"/>");
        }
        i = item.getReferences().iterator();
        while (i.hasNext()) {
            Field f = (Field) i.next();
            sb.append("<reference name=\"")
                .append(f.getName())
                .append("\" ")
                .append("value=\"")
                .append(f.getValue())
                .append("\"/>");
        }
        sb.append("</object>");
        if ((sb.length() > 1000000) && ((1.1 * sb.length()) < sb.capacity())) {
            // Yes, this looks silly, but it prevents the String holding a reference to a char
            // array that is up to twice the size of the String. We throw away the larger char
            // array immediately anyway.
            LOG.info("Converting StringBuffer (size = " + (sb.length() / 512) + " kB, capacity = "
                    + (sb.capacity() / 512) + " kb) to String");
            return new String(sb.toString());
        }
        return sb.toString();
    }


    /**
     * Render the given object as minimal string for starage in database
     *
     * @param obj the object to render
     * @param model the parent model
     * @return the XML for that object
     */
    public static String render(InterMineObject obj, Model model) {
        Item item = objectToItem(obj, model);
        int charLen = 300;
        Iterator i = item.getFields().iterator();
        while (i.hasNext()) {
            Field f = (Field) i.next();
            charLen += f.getValue().toString().length() + 200;
        }
        charLen += item.getReferences().size() * 200;
        
        StringBuffer sb = new StringBuffer(charLen);

        sb.append(item.getClassName())
            .append(DELIM)
            .append(item.getImplementations());

        i = item.getFields().iterator();
        while (i.hasNext()) {
            Field f = (Field) i.next();
            sb.append(DELIM)
                .append("a")
                .append(f.getName())
                .append(DELIM)
                .append(f.getValue());
        }

        i = item.getReferences().iterator();
        while (i.hasNext()) {
            Field f = (Field) i.next();
            sb.append(DELIM)
                .append("r")
                .append(f.getName())
                .append(DELIM)
                .append(f.getValue());
        }
        if ((sb.length() > 1000000) && ((1.1 * sb.length()) < sb.capacity())) {
            // Yes, this looks silly, but it prevents the String holding a reference to a char
            // array that is up to twice the size of the String. We throw away the larger char
            // array immediately anyway.
            LOG.info("Converting StringBuffer (size = " + (sb.length() / 512) + " kB, capacity = "
                    + (sb.capacity() / 512) + " kb) to String");
            return new String(sb.toString());
        }
        return sb.toString();
    }


    /**
     * Convert the InterMineObject to Lite XML Item.
     *
     * @param obj the object to convert
     * @param model the parent InterMine model
     * @return the generated item
     */
    protected static Item objectToItem(InterMineObject obj, Model model) {
        Item item = new Item();
        item.setClassName(XmlHelper.getClassName(obj, model));
        item.setImplementations(getImplements(obj));
        setFields(obj, item);
        return item;
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
     * Set fields and references in given item.
     *
     * @param obj the object
     * @param item the item to set fields/references in
     */
    protected static void setFields(Object obj, Item item) {

        Map infos = TypeUtil.getFieldInfos(obj.getClass());
        Iterator iter = infos.keySet().iterator();
        try {
            while (iter.hasNext()) {
                // If reference, value is id of referred-to object
                // If field, value is field value
                // If collection, no element output
                // Element is not output if the value is null

                String fieldname = (String) iter.next();
                Object value = TypeUtil.getFieldProxy(obj, fieldname);

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
                    id = m.invoke(value, (Object[]) null);
                } catch (InvocationTargetException e) {
                    // empty
                } catch (IllegalAccessException e) {
                    // empty
                } catch (NoSuchMethodException e) {
                    // empty
                }
                if (id != null) {
                    value = id;
                }

                Field f = new Field();
                f.setName(fieldname);

                if (value instanceof Date) {
                    f.setValue(new Long(((Date) value).getTime()).toString());
                } else {
                    f.setValue(value.toString());
                }

                if (id == null) {
                    item.addField(f);
                } else {
                    item.addReference(f);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
