package org.flymine.xml.full;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import java.util.Arrays;
import java.util.Comparator;

import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.xml.XmlHelper;

import org.apache.log4j.Logger;

/**
 * Render an object in FlyMine Full XML format
 *
 * @author Andrew Varley
 */
public class FullRenderer
{
    protected static final Logger LOG = Logger.getLogger(FullRenderer.class);
    protected static final String ENDL = System.getProperty("line.separator");

    /**
     * Don't allow construction
     */
    private FullRenderer() {
    }

    /**
     * Render a collection of objects as XML in FlyMine Full format.
     *
     * @param objects a collection of objects to render
     * @param model the parent model
     * @return the XML for the list of objects
     */
    public static String render(Collection objects, Model model) {
        StringBuffer sb = new StringBuffer();

        sb.append("<items>" + ENDL);
        Iterator iter = objects.iterator();
        while (iter.hasNext()) {
            sb.append(renderObject((FlyMineBusinessObject) iter.next(), model));
        }
        sb.append("</items>" + ENDL);

        return sb.toString();
    }

    /**
     * Render a collection of items in FlyMine Full XML format.
     *
     * @param items a collection of items to render
     * @return the XML for the list of items
     */
    public static String render(Collection items) {
        StringBuffer sb = new StringBuffer();
        sb.append("<items>" + ENDL);
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next().toString());
        }
        sb.append("</items>" + ENDL);

        return sb.toString();
    }

    /**
     * Render the given object as XML.
     * @param obj the object to render
     * @param model the parent model
     * @return an XML representation of the object
     */
    protected static String renderObject(FlyMineBusinessObject obj, Model model) {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("Id of object was null (" + obj.toString() + ")");
        }

        String className = XmlHelper.getClassName(obj, model);

        StringBuffer sb = new StringBuffer();
        sb.append("<object xml_id=\"" + obj.getId() + "\" class=\"")
            .append(className == "" ? "" : model.getNameSpace()
                     + TypeUtil.unqualifiedName(XmlHelper.getClassName(obj, model)))
            .append("\" implements=\"")
            .append(getImplements(obj, model))
            .append("\">" + ENDL)
            .append(getFields(obj))
            .append("</object>" + ENDL);
        return sb.toString();
    }


    /**
     * Get all interfaces that an object implements.
     *
     * @param obj the object
     * @param model the parent model
     * @return space separated list of extended/implemented classes/interfaces
     */
    protected static String getImplements(FlyMineBusinessObject obj, Model model) {
        StringBuffer sb = new StringBuffer();

        Class [] interfaces = obj.getClass().getInterfaces();
        Arrays.sort(interfaces, new SimpleComparator());

        for (int i = 0; i < interfaces.length; i++) {
            ClassDescriptor cld = model.getClassDescriptorByName(interfaces[i].getName());
            if (cld != null && cld.isInterface()) {
                sb.append(model.getNameSpace().toString()
                          + TypeUtil.unqualifiedName(interfaces[i].getName()))
                    .append(" ");
            }
        }
        return sb.toString().trim();
    }


    /**
     * Get all fields of an object.
     *
     * @param obj the object
     * @return string containing XML representation of all fields
     */
    protected static String getFields(FlyMineBusinessObject obj) {
        StringBuffer sb = new StringBuffer();

        try {
            Map infos = TypeUtil.getFieldInfos(obj.getClass());
            Iterator iter = infos.keySet().iterator();
            while (iter.hasNext()) {
                // If reference, value is id of referred-to object
                // If field, value is field value
                // If collection, ...............
                // Element is not output if the value is null

                String fieldname = (String) iter.next();
                Object value = TypeUtil.getFieldValue(obj, fieldname);

                if (value == null) {
                    continue;
                }
                // Collection
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    Collection col = (Collection) value;
                    if (col.size() > 0) {
                        sb.append("<collection name=\"")
                            .append(fieldname)
                            .append("\">" + ENDL);
                        Iterator i = col.iterator();
                        while (i.hasNext()) {
                            sb.append("<reference ref_id=\"")
                                .append(((FlyMineBusinessObject) i.next()).getId())
                            .append("\"/>" + ENDL);
                        }
                        sb.append("</collection>" + ENDL);
                    }
                } else if (value instanceof FlyMineBusinessObject) {
                    sb.append("<reference name=\"")
                        .append(fieldname)
                        .append("\" ref_id=\"")
                        .append(((FlyMineBusinessObject) value).getId())
                        .append("\"/>" + ENDL);
                } else {
                    if (!fieldname.equalsIgnoreCase("id")) {
                        sb.append("<field name=\"")
                            .append(fieldname)
                            .append("\" value=\"");
                        if (value instanceof Date) {
                            sb.append(((Date) value).getTime());
                        } else {
                            sb.append(value);
                        }
                        sb.append("\"/>" + ENDL);
                    }
                }
            }

        } catch (IllegalAccessException e) {
        }
        return sb.toString();
    }

    /**
     * A simple implementation of Comparator to order pairs of Class objects.
     */
    static class SimpleComparator implements Comparator
    {
        /**
         * Compare two Class objects by name.
         * @param a an object to compare
         * @param b an object to compare
         * @return integer result of comparason
         */
        public int compare(Object a, Object b) {
            return ((Class) a).getName().compareTo(((Class) b).getName());
        }
    }
}
