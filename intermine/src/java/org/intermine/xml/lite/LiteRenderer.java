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
import org.flymine.xml.XmlHelper;
import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;

import org.apache.log4j.Logger;

/**
 * Render an object in FlyMine Lite XML format
 *
 * @author Andrew Varley
 */
public class LiteRenderer
{
    protected static final Logger LOG = Logger.getLogger(LiteRenderer.class);
    protected static String DELIM = "$_^";
    /**
     * Don't allow construction
     */
    private LiteRenderer() {
    }

    /**
     * Render the given object as XML in FlyMine Lite format
     *
     * @param obj the object to render
     * @param model the parent model
     * @return the XML for that object
     */
    public static String renderXml(FlyMineBusinessObject obj, Model model) {
        Item item = objectToItem(obj, model);
        return renderXml(item);
    }


    /**
     * Render the given item as XML in FlyMine Lite format
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
        return sb.toString();
    }


    /**
     * Render the given object as minimal string for starage in database
     *
     * @param obj the object to render
     * @param model the parent model
     * @return the XML for that object
     */
    public static String render(FlyMineBusinessObject obj, Model model) {
        StringBuffer sb = new StringBuffer();
        Item item = objectToItem(obj, model);

        sb.append(item.getClassName())
            .append(DELIM)
            .append(item.getImplementations());

        Iterator i = item.getFields().iterator();
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
        return sb.toString();
    }


    /**
     * Convert the FlyMineBusinessObject to Lite XML Item.
     *
     * @param obj the object to convert
     * @param model the parent FlyMine model
     * @return the generated item
     */
    protected static Item objectToItem(FlyMineBusinessObject obj, Model model) {
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
        }
    }
}
