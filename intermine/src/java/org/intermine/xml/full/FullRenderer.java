package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.xml.XmlHelper;

/**
 * Render an object in InterMine Full XML format
 *
 * @author Andrew Varley
 */
public class FullRenderer
{
    protected static final String ENDL = System.getProperty("line.separator");

    /**
     * Don't allow construction
     */
    private FullRenderer() {
    }

    /**
     * Render a collection of objects as XML in InterMine Full format.
     *
     * @param objects a collection of objects to render
     * @param model the parent model
     * @return the XML for the list of objects
     */
    public static String render(Collection objects, Model model) {
        return render(toItems(objects, model));
    }

    /**
     * Render a InterMineObject as Xml in Full Data format.
     * @param obj an object to render
     * @param model the parent model
     * @return the XML for object
     */
    public static String render(InterMineObject obj, Model model) {
        return render(toItem(obj, model));
    }

    /**
     * Convert a collection of InterMineObjects to Item format.
     * @param objects objects to convert
     * @param model the parent model
     * @return a list of Full Data Items
     */
    public static List toItems(Collection objects, Model model) {
        List items = new ArrayList();

        Iterator objIter = objects.iterator();
        while (objIter.hasNext()) {
            InterMineObject obj = (InterMineObject) objIter.next();
            items.add(toItem(obj, model));
        }
        return items;
    }

    /**
     * Render a collection of items in InterMine Full XML format.
     *
     * @param items a collection of items to render
     * @return the XML for the list of items
     */
    public static String render(Collection items) {
        StringBuffer sb = new StringBuffer();
        sb.append(getHeader() + ENDL);
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            sb.append(render((Item) iter.next()));
        }
        sb.append(getFooter() + ENDL);

        return sb.toString();
    }

    /**
     * Convert a InterMineObject to Item format.
     * @param obj object to convert
     * @param model the parent model
     * @return a new Full Data Item
     */
    public static Item toItem(InterMineObject obj, Model model) {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("Id of object was null (" + obj.toString() + ")");
        }

        String className = XmlHelper.getClassName(obj, model);

        Item item = new Item();
        item.setIdentifier(obj.getId().toString());
        item.setClassName(className.equals("") ? "" : model.getNameSpace()
                          + TypeUtil.unqualifiedName(XmlHelper.getClassName(obj, model)));
        item.setImplementations(getImplements(obj, model));

        try {
            Map infos = TypeUtil.getFieldInfos(obj.getClass());
            Iterator iter = infos.keySet().iterator();
            while (iter.hasNext()) {
                // If Reference, value is id of referred-to object
                // If Attribute, value is field value
                // If Collection, contains list of ids of objects in collection

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
                        ReferenceList refList = new ReferenceList(fieldname);
                        for (Iterator j = col.iterator(); j.hasNext();) {
                            refList.addRefId(((InterMineObject) j.next()).getId().toString());
                        }
                        item.addCollection(refList);
                    }
                } else if (value instanceof InterMineObject) {
                    Reference ref = new Reference();
                    ref.setName(fieldname);
                    ref.setRefId(((InterMineObject) value).getId().toString());
                    item.addReference(ref);
                } else {
                    if (!fieldname.equalsIgnoreCase("id")) {
                        Attribute attr = new Attribute();
                        attr.setName(fieldname);
                        attr.setValue(TypeUtil.objectToString(value));
                        item.addAttribute(attr);
                    }
                }
            }
        } catch (IllegalAccessException e) {
        }
        return item;
    }

    /**
     * Render the given Item as XML
     * @param item the Item to render
     * @return an XML representation of the Item
     */
    public static String render(Item item) {
        if (item.getIdentifier() == null) {
            throw new IllegalArgumentException("Item has null Identifier");
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<item id=\"" + item.getIdentifier() + "\"")
          .append(" class=\"")
          .append(item.getClassName() == null ? "" : item.getClassName())
          .append("\"");
        if (item.getImplementations() != null && !item.getImplementations().equals("")) {
            sb.append(" implements=\"" + item.getImplementations() + "\"");
        }
        sb.append(">" + ENDL);

        TreeSet attrs = new TreeSet(new SimpleComparator());
        attrs.addAll(item.getAttributes());
        for (Iterator i = attrs.iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            sb.append("<attribute name=\"" + attr.getName() + "\" value=\""
                + attr.getValue() + "\"/>" + ENDL);
        }

        TreeSet refs = new TreeSet(new SimpleComparator());
        refs.addAll(item.getReferences());
        for (Iterator i = refs.iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            sb.append("<reference name=\"" + ref.getName() + "\" ref_id=\""
                + ref.getRefId() + "\"/>" + ENDL);
        }

        TreeSet cols = new TreeSet(new SimpleComparator());
        cols.addAll(item.getCollections());
        for (Iterator i = cols.iterator(); i.hasNext();) {
            ReferenceList refList = (ReferenceList) i.next();
            sb.append("<collection name=\"" + refList.getName() + "\">" + ENDL);
            for (Iterator j = refList.getRefIds().iterator(); j.hasNext();) {
                sb.append("<reference ref_id=\"" + j.next() + "\"/>" + ENDL);
            }
            sb.append("</collection>" + ENDL);
        }
        sb.append("</item>" + ENDL);
        return sb.toString();
    }

    /**
     * Return the Full XML file header
     *
     * @return the header
     */
    public static String getHeader() {
        return "<items>";
    }

    /**
     * Return the Full XML file footer
     *
     * @return the footer
     */
    public static String getFooter() {
        return "</items>";
    }

    /**
     * Get all interfaces that an object implements.
     *
     * @param obj the object
     * @param model the parent model
     * @return space separated list of extended/implemented classes/interfaces
     */
    protected static String getImplements(InterMineObject obj, Model model) {
        StringBuffer sb = new StringBuffer();

        Class [] interfaces = obj.getClass().getInterfaces();
        Arrays.sort(interfaces, new SimpleComparator());

        for (int i = 0; i < interfaces.length; i++) {
            ClassDescriptor cld = model.getClassDescriptorByName(interfaces[i].getName());
            if (cld != null && cld.isInterface()
                    && !cld.getName().equals("org.intermine.model.InterMineObject")) {
                sb.append(model.getNameSpace().toString()
                          + TypeUtil.unqualifiedName(interfaces[i].getName()))
                    .append(" ");
            }
        }
        return sb.toString().trim();
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
            if (a instanceof Class && b instanceof Class) {
                return compare((Class) a, (Class) b);
            } else if (a instanceof Item && b instanceof Item) {
                return compare((Item) a, (Item) b);
            } else if (a instanceof Attribute && b instanceof Attribute) {
                return compare((Attribute) a, (Attribute) b);
            } else if (a instanceof Reference && b instanceof Reference) {
                return compare((Reference) a, (Reference) b);
            } else if (a instanceof ReferenceList && b instanceof ReferenceList) {
                return compare((ReferenceList) a, (ReferenceList) b);
            } else {
                throw new IllegalArgumentException("Cannot compare: " + a.getClass().getName()
                                                   + " and " + b.getClass().getName());
            }
        }

        private int compare(Class a, Class b) {
            return a.getName().compareTo(b.getName());
        }

        private int compare(Item a, Item b) {
            return a.getIdentifier().compareTo(b.getIdentifier());
        }

        private int compare(Attribute a, Attribute b) {
            return a.getName().compareTo(b.getName());
        }

        private int compare(Reference a, Reference b) {
            return a.getName().compareTo(b.getName());
        }

        private int compare(ReferenceList a, ReferenceList b) {
            return a.getName().compareTo(b.getName());
        }
    }
}
