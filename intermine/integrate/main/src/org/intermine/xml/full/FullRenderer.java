package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
     * Render an Object as Xml in Full Data format.
     * @param obj an object to render
     * @param model the parent model
     * @return the XML for object
     */
    public static String render(Object obj, Model model) {
        return render(new ItemFactory(model).makeItem(obj));
    }

    /**
     * Convert a collection of Objects to Item format.
     * @param objects objects to convert
     * @param model the parent model
     * @return a list of Full Data Items
     */
    public static List toItems(Collection objects, Model model) {
        List items = new ArrayList();

        Iterator objIter = objects.iterator();
        while (objIter.hasNext()) {
            Object obj = (Object) objIter.next();
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
    public static String render(Collection<Item> items) {
        StringBuffer sb = new StringBuffer();
        sb.append(getHeader()).append(ENDL);
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            sb.append(render((Item) iter.next()));
        }
        sb.append(getFooter()).append(ENDL);

        return sb.toString();
    }

    /**
     * Render the given Item as XML using an XMLStreamWriter
     * @param writer to XMLStreamWriter to write to
     * @param item the Item to render
     */
    public static void render(XMLStreamWriter writer, Item item) {
        renderImpl(writer, item, true);
    }

    /**
     * Render the given Item as XML using an XMLStreamWriter
     * @param writer to XMLStreamWriter to write to
     * @param item the Item to render
     * @param renderCollections render the collections if and only if this is true
     */
    public static void renderImpl(XMLStreamWriter writer, Item item,
                                  boolean renderCollections) {
        try {
            writer.writeStartElement("item");
            if (item.getIdentifier() != null) {
                writer.writeAttribute("id", item.getIdentifier());
            }
            writer.writeAttribute("class", item.getClassName() == null ? "" : item.getClassName());

            if (item.getImplementations() != null && !item.getImplementations().equals("")) {
                writer.writeAttribute("implements", item.getImplementations());
            }
            writer.writeCharacters(ENDL);
            TreeSet attrs = new TreeSet(new RendererComparator());
            attrs.addAll(item.getAttributes());
            for (Iterator i = attrs.iterator(); i.hasNext();) {
                Attribute attr = (Attribute) i.next();
                writer.writeEmptyElement("attribute");
                writer.writeAttribute("name", attr.getName());
                writer.writeAttribute("value", attr.getValue());
                writer.writeCharacters(ENDL);
            }

            TreeSet refs = new TreeSet(new RendererComparator());
            refs.addAll(item.getReferences());
            for (Iterator i = refs.iterator(); i.hasNext();) {
                Reference ref = (Reference) i.next();
                writer.writeEmptyElement("reference");
                writer.writeAttribute("name", ref.getName());
                writer.writeAttribute("ref_id", ref.getRefId());
                writer.writeCharacters(ENDL);
            }

            if (renderCollections) {
                TreeSet cols = new TreeSet(new RendererComparator());
                cols.addAll(item.getCollections());
                for (Iterator i = cols.iterator(); i.hasNext();) {
                    ReferenceList refList = (ReferenceList) i.next();
                    writer.writeStartElement("collection");
                    writer.writeAttribute("name", refList.getName());

                    for (Iterator j = refList.getRefIds().iterator(); j.hasNext();) {
                        writer.writeEmptyElement("reference");
                        writer.writeAttribute("ref_id", (String) j.next());
                    }
                    writer.writeEndElement();
                    writer.writeCharacters(ENDL);
                }
            }
            writer.writeEndElement();
            writer.writeCharacters(ENDL);
        } catch (XMLStreamException e) {
            throw new RuntimeException("unexpected exception while accessing a XMLStreamWriter", e);
        }
    }

    /**
     * Render the given Item as XML
     * @param item the Item to render
     * @return an XML representation of the Item
     */
    public static String render(Item item) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        XMLStreamWriter writer;
        try {
            writer = factory.createXMLStreamWriter(sw);
            renderImpl(writer, item, true);
        } catch (XMLStreamException e) {
            throw new RuntimeException("unexpected failure while creating Item XML", e);
        }

        return sw.toString();
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
