package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;

/**
 * Render an object in InterMine Full XML format
 *
 * @author Andrew Varley
 */
public final class FullRenderer
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
    public static String render(Collection<FastPathObject> objects, Model model) {
        return render(toItems(objects, model));
    }

    /**
     * Render an Object as Xml in Full Data format.
     * @param obj an object to render
     * @param model the parent model
     * @return the XML for object
     */
    public static String render(FastPathObject obj, Model model) {
        return render(new ItemFactory(model).makeItem(obj));
    }

    /**
     * Convert a collection of Objects to Item format.
     * @param objects objects to convert
     * @param model the parent model
     * @return a list of Full Data Items
     */
    public static List<Item> toItems(Collection<FastPathObject> objects, Model model) {
        List<Item> items = new ArrayList<Item>();

        for (FastPathObject obj : objects) {
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
        for (Item item : items) {
            sb.append(render(item));
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
        renderImpl(writer, item);
    }

    /**
     * Render the given Item as XML using an XMLStreamWriter
     * @param writer to XMLStreamWriter to write to
     * @param item the Item to render
     */
    public static void renderImpl(XMLStreamWriter writer, Item item) {
        try {
            writer.writeStartElement("item");
            if (item.getIdentifier() != null) {
                writer.writeAttribute("id", item.getIdentifier());
            }
            writer.writeAttribute("class", item.getClassName() == null ? "" : item.getClassName());

            if (item.getImplementations() != null && !"".equals(item.getImplementations())) {
                writer.writeAttribute("implements", item.getImplementations());
            }
            writer.writeCharacters(ENDL);
            TreeSet<Attribute> attrs = new TreeSet<Attribute>(new RendererComparator());
            attrs.addAll(item.getAttributes());
            for (Attribute attr : attrs) {
                if (!"".equals(attr.getValue())) {
                    writer.writeEmptyElement("attribute");
                    writer.writeAttribute("name", attr.getName());
                    writer.writeAttribute("value", attr.getValue());
                    writer.writeCharacters(ENDL);
                }
            }

            TreeSet<Reference> refs = new TreeSet<Reference>(new RendererComparator());
            refs.addAll(item.getReferences());
            for (Reference ref : refs) {
                writer.writeEmptyElement("reference");
                writer.writeAttribute("name", ref.getName());
                writer.writeAttribute("ref_id", ref.getRefId());
                writer.writeCharacters(ENDL);
            }

            TreeSet<ReferenceList> cols = new TreeSet<ReferenceList>(new RendererComparator());
            cols.addAll(item.getCollections());
            for (ReferenceList refList : cols) {
                writer.writeStartElement("collection");
                writer.writeAttribute("name", refList.getName());

                for (String ref : refList.getRefIds()) {
                    writer.writeEmptyElement("reference");
                    writer.writeAttribute("ref_id", ref);
                }
                writer.writeEndElement();
                writer.writeCharacters(ENDL);
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
            renderImpl(writer, item);
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
