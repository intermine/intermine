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
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;

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
        return render(new ItemFactory(model).makeItem(obj));
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
            items.add(new ItemFactory(model).makeItem(obj));
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

        TreeSet attrs = new TreeSet(new RendererComparator());
        attrs.addAll(item.getAttributes());
        for (Iterator i = attrs.iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            String value = attr.getValue();
            
            sb.append("<attribute name=\"" + attr.getName() + "\" value=\""
                      + attr.getValue().replaceAll("\"", "&quot;").replaceAll("'", "&apos;")
                      + "\"/>" + ENDL);
        }

        TreeSet refs = new TreeSet(new RendererComparator());
        refs.addAll(item.getReferences());
        for (Iterator i = refs.iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            sb.append("<reference name=\"" + ref.getName() + "\" ref_id=\""
                + ref.getRefId() + "\"/>" + ENDL);
        }

        TreeSet cols = new TreeSet(new RendererComparator());
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
}
