package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.*;
import org.intermine.util.StringUtil;
import org.intermine.util.SAXParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;

import org.apache.log4j.Logger;

/**
 * Convert XML format data conforming to given InterMine model to fulldata
 * Items, requires an XML Schema to understand references.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
 public class XmlConverter extends DataConverter
{
    private static final Logger LOG = Logger.getLogger(DataConverter.class);

    protected Model model;
    protected XmlMetaData xmlInfo;
    protected Reader xmlReader;
    protected Reader xsdReader;
    private int id = 1;


    /**
     * Construct with model, reader for xml data, reader for schema and an ItemWriter
     * @param model an Intermine model that describes XML format
     * @param xmlReader reader pointing at XML data
     * @param xsdReader reader pointing at SML-Schema describing data
     * @param writer write items produced
     * @throws Exception if anything goes wrong
     */
    public XmlConverter(Model model, Reader xmlReader, Reader xsdReader, ItemWriter writer)
        throws Exception {
        super(writer);
        this.model = model;
        this.xmlReader = xmlReader;
        this.xmlInfo = new XmlMetaData(xsdReader);
        LOG.debug(xmlInfo.toString());
    }

    /**
    * Perform conversion form src model XML to InterMine fulldata items
    * @throws Exception if an error occurs during processing
    */
    public void process() throws Exception {
        XmlHandler handler = new XmlHandler();
        try {
            SAXParser.parse(new InputSource(xmlReader), handler);
        } finally {
            writer.close();
        }
    }

    private String getIdentifier() {
        return "0_" + id++;
    }

    /**
     * Extend SAX DefaultHandler to process arbitrary XML conforming to given model
     * to InterMine fulldata XML.
     */
    class XmlHandler extends DefaultHandler
    {
        String modelName;
        String modelNameSpace;
        Stack elements = new Stack();
        Stack items = new Stack();
        Stack paths = new Stack();
        String attributeName = null;
        Map identifiers = new HashMap();


        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {

            // read an element
            // look at stack - are we currently in a class
            //   if no - look up class name in model
            //         - create item, put on stack
            //   if yes - look up field type (att, ref, col)
            //          if att - add to item on stack
            //          if ref/col add to item on stack, process next item

            if (items.empty()) {
                String clsName = model.getPackageName() + "." + StringUtil.capitalise(qName);
                if (!model.hasClassDescriptor(clsName)) {
                    throw new SAXException("Classname (" + clsName + ") not found in model "
                                           + "for element: " + qName);
                }
                items.push(new Item(getIdentifier(), clsName, ""));
                elements.push(qName);
                LOG.debug("pushed elements: " + qName);
                pushPaths(qName);
            } else {
                Item item = (Item) items.peek();
                ClassDescriptor cld = model.getClassDescriptorByName(item.getClassName());
                if (cld.getAttributeDescriptorByName(qName) != null) {
                    attributeName = qName;
                } else if (cld.getReferenceDescriptorByName(qName) != null) {
                    if (item.hasReference(qName)) {
                        throw new SAXException("Field (" + cld.getName() + "." + qName
                                               + ") is defined in model as"
                                               + " a reference but attempted to add two values");
                    }
                    ReferenceDescriptor rfd = cld.getReferenceDescriptorByName(qName);
                    String clsName = rfd.getReferencedClassDescriptor().getName();
                    String path = pushPaths(qName);
                    String identifier = null;
                    if (xmlInfo.isReference(path)) {
                        String refField = xmlInfo.getReferenceField(path);
                        identifier = getReferenceIdentifier(xmlInfo.getIdPath(path)
                                                            + attrs.getValue(refField));
                    } else if (xmlInfo.isId(path)) {
                        String idField = xmlInfo.getIdField(path);
                        Item newItem = new Item(getReferenceIdentifier(path
                                                           + attrs.getValue(idField)), clsName, "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                    } else {
                        Item newItem = new Item(getIdentifier(), clsName, "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                    }
                    item.addReference(new Reference(qName, identifier));
                    elements.push(qName);
                    LOG.debug("pushed elements: " + qName);
                } else if (cld.getCollectionDescriptorByName(StringUtil.pluralise(qName)) != null) {
                    CollectionDescriptor cod
                        = cld.getCollectionDescriptorByName(StringUtil.pluralise(qName));
                    String clsName = cod.getReferencedClassDescriptor().getName();
                    String path = pushPaths(qName);
                    String identifier = null;
                    if (xmlInfo.isReference(path)) {
                        String refField = xmlInfo.getReferenceField(path);
                        identifier = getReferenceIdentifier(xmlInfo.getIdPath(path)
                                                            + attrs.getValue(refField));
                    } else if (xmlInfo.isId(path)) {
                        String idField = xmlInfo.getIdField(path);
                        Item newItem = new Item(getReferenceIdentifier(path
                                                         + attrs.getValue(idField)), clsName, "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                    } else {
                        Item newItem = new Item(getIdentifier(), clsName, "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                    }
                    if (!item.hasCollection(StringUtil.pluralise(qName))) {
                        item.addCollection(new ReferenceList(StringUtil.pluralise(qName),
                                                             new ArrayList()));
                    }
                    item.getCollection(StringUtil.pluralise(qName)).addRefId(identifier);
                    elements.push(qName);
                    LOG.debug("pushed elements: " + qName);
                } else {
                    throw new SAXException("Field (" + qName + "[s]) not found in class ("
                                           + cld.getName() + ")");
                }
            }
            if (attrs.getLength() > 0) {
                Item item = (Item) items.peek();
                String path = (String) paths.peek();
                String refField = "";
                if (xmlInfo.isReference(path)) {
                    refField = xmlInfo.getReferenceField(path);
                }
                for (int i = 0; i < attrs.getLength(); i++) {
                    if (!attrs.getQName(i).equals(refField)) {
                        item.addAttribute(new Attribute(attrs.getQName(i), attrs.getValue(i)));
                    }
                }
            }
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            while (length > 0) {
                boolean whitespace = false;
                switch(ch[start]) {
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    whitespace = true;
                    break;
                default:
                    break;
                }
                if (!whitespace) {
                    break;
                }
                ++start;
                --length;
            }

            if (length > 0) {
                StringBuffer s = new StringBuffer();
                s.append(ch, start, length);
                LOG.debug("attributeName = " + attributeName + " ch: " + s.toString());
                // if attribute_name not null - create string and set to named att
                // else create string and set to 'content' field of current item
                Item item = (Item) items.peek();
                ClassDescriptor cld = model.getClassDescriptorByName(item.getClassName());
                if (attributeName == null) {
                    // TODO possibly look up xmlInfo for defined name for 'content' attribute
                    // currently possibility of a clash
                    if (cld.getAttributeDescriptorByName("content") == null) {
                        throw new SAXException("Class (" + cld.getName() + ") does not have"
                                               + " default 'content' attribute");
                    }
                    attributeName = "content";
                }
                if (item.hasAttribute(attributeName)) {
                    throw new SAXException("A value has already been set for attribute ("
                                           + attributeName + ") in item: " + item.getClassName()
                                           + " [" + item.getIdentifier() + "]");
                } else {
                    StringBuffer sb = new StringBuffer(length);
                    sb.append(ch, start, length);
                    item.addAttribute(new Attribute(attributeName, sb.toString()));
                }
            }
        }


        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // if qName is top of element stack and is item/ref/col
            // -> store item

            if (attributeName == null) {
                String tmp = (String) elements.pop();
                LOG.debug("popped elements: " + tmp);
                String path = (String) paths.pop();
                LOG.debug("popped paths: " + path);
                try {
                    if (!xmlInfo.isReference(path)) {
                        writer.store((ItemHelper.convert((Item) items.pop())));
                    }
                } catch (ObjectStoreException e) {
                    throw new SAXException(e);
                }
            }
            attributeName = null;
        }


        private String getReferenceIdentifier(String key) {
            String identifier = (String) identifiers.get(key);
            if (identifier == null) {
                identifier = getIdentifier();
                identifiers.put(key, identifier);
            }
            return identifier;
        }

        private String pushPaths(String qname) {
            String path = qname;
            if (!paths.empty()) {
                path = (String) paths.peek() + "/" + path;
            }
            LOG.debug("pushing paths: " + path);
            paths.push(path);
            return path;
        }
    }
 }
