package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
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
import org.intermine.modelproduction.xmlschema.XmlMetaData;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.SAXParser;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;

import org.apache.log4j.Logger;

/**
 * Convert XML format data conforming to given InterMine model to fulldata
 * Items, requires an XML Schema to understand references.
 *
 * @author Andrew Varley
 * @author Richard Smith
 * @author Thomas Riley
 */
public class XmlConverter extends DataConverter
{
    static final Logger LOG = Logger.getLogger(XmlConverter.class);

    protected Model model;
    protected XmlMetaData xmlInfo;
    protected Reader xmlReader;
    protected Reader xsdReader;
    private int id = 1;

    long count = 0;
    long start, time, times[];

    /**
     * Construct with model, reader for xml data, reader for schema and an ItemWriter
     * @param model an Intermine model that describes XML format
     * @param xsdReader reader pointing at SML-Schema describing data
     * @param writer write items produced
     * @throws Exception if anything goes wrong
     */
    public XmlConverter(Model model, Reader xsdReader, ItemWriter writer)
        throws Exception {
        super(writer);
        this.model = model;
        this.xmlInfo = new XmlMetaData(xsdReader);

        start = System.currentTimeMillis();
        time = start;
        times = new long[20];
        for (int i = 0; i < 20; i++) {
            times[i] = -1;
        }
    }

    /**
    * Perform conversion form src model XML to InterMine fulldata items
    * @param xmlReader reader pointing at XML data
    * @throws Exception if an error occurs during processing
    */
    public void process(Reader xmlReader) throws Exception {
        SAXParser.parse(new InputSource(xmlReader), new XmlHandler(model));
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
        Stack elements = new Stack();
        Stack items = new Stack();
        Stack paths = new Stack();
        String attributeName = null;
        StringBuffer attValue = new StringBuffer();
        Map identifiers = new HashMap();
        boolean isAttribute = false;
        ItemFactory itemFactory;

        /**
         * {@inheritDoc}
         * @param model the Model to use when creating items
         */
        public XmlHandler(Model model) {
            itemFactory = new ItemFactory(model);
        }

        /**
         * {@inheritDoc}
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
                items.push(itemFactory.makeItem(getIdentifier(), itemName(clsName), ""));
                elements.push(qName);
                pushPaths(qName);
            } else {
                Item item = (Item) items.peek();
                ClassDescriptor cld = model.getClassDescriptorByName(clsName(item.getClassName()));
                String fieldName = ("id".equals(qName) ? "identifier" : qName);
                fieldName = StringUtil.decapitalise(fieldName);
                if (cld.getAttributeDescriptorByName(fieldName, true) != null) {
                    // Attribute
                    attributeName = fieldName;
                    isAttribute = true;
                } else if (cld.getReferenceDescriptorByName(fieldName, true) != null) {
                    // Reference
                    if (item.hasReference(fieldName)) {
                        throw new SAXException("Field (" + cld.getName() + "." + fieldName
                                               + ") is defined in model as"
                                               + " a reference but attempted to add two values");
                    }
                    ReferenceDescriptor rfd = cld.getReferenceDescriptorByName(fieldName, true);
                    String clsName = rfd.getReferencedClassDescriptor().getName();
                    String path = pushPaths(qName);
                    String identifier = null;
                    if (xmlInfo.isReferenceElement(path)) {
                        String refField = xmlInfo.getReferenceElementField(path);
                        String key = xmlInfo.getReferencingKeyName(path, refField);
                        String clsPath = xmlInfo.getKeyPath(key);
                        identifier = getReferenceIdentifier(clsPath //xmlInfo.getIdPath(path)
                                                            + attrs.getValue(refField));
                        LOG.debug("found reference element identifier " + identifier);
                    } else if (!xmlInfo.getKeyFieldsForPath(path).isEmpty()) {
                        String idField = (String) xmlInfo.getKeyFieldsForPath(path).iterator()
                                                                                            .next();
                        path = xmlInfo.getKeyXPathMatchingPath(path);
                        String referencedId =
                            getReferenceIdentifier(path + attrs.getValue(idField));
                        Item newItem = itemFactory.makeItem(referencedId, itemName(clsName), "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                        LOG.debug("creating new item with key field and identifier "
                                  + newItem.getIdentifier());
                    } else {
                        Item newItem = itemFactory.makeItem(getIdentifier(), itemName(clsName), "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                        LOG.debug("creating new item " + newItem.getIdentifier());
                    }
                    item.addReference(new Reference(fieldName, identifier));
                    elements.push(qName);
                } else if (cld.getCollectionDescriptorByName(StringUtil.pluralise(fieldName), true)
                           != null) {
                    CollectionDescriptor cod
                        = cld.getCollectionDescriptorByName(StringUtil.pluralise(fieldName), true);
                    String clsName = cod.getReferencedClassDescriptor().getName();
                    String path = pushPaths(qName);
                    String identifier = null;
                    if (xmlInfo.isReferenceElement(path)) {
                        String refField = xmlInfo.getReferenceElementField(path);
                        String key = xmlInfo.getReferencingKeyName(path, refField);
                        String clsPath = xmlInfo.getKeyPath(key);
                        identifier = getReferenceIdentifier(clsPath
                                                            + attrs.getValue(refField));
                        LOG.debug("found reference element identifier " + identifier
                                  + " class:" + clsName);
                    } else if (!xmlInfo.getKeyFieldsForPath(path).isEmpty()) {
                        String idField = (String) xmlInfo.getKeyFieldsForPath(path).iterator()
                                                                                        .next();
                        path = xmlInfo.getKeyXPathMatchingPath(path);
                        String referencedId =
                            getReferenceIdentifier(path + attrs.getValue(idField));
                        Item newItem = itemFactory.makeItem(referencedId, itemName(clsName), "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                        LOG.debug("creating new item with key field and identifier "
                                  + newItem.getIdentifier() + " class:" + clsName + " path:" + path
                                  + attrs.getValue(idField));
                    } else {
                        Item newItem = itemFactory.makeItem(getIdentifier(), itemName(clsName), "");
                        identifier = newItem.getIdentifier();
                        items.push(newItem);
                        LOG.debug("creating new item " + newItem.getIdentifier()
                                  + " class:" + clsName);
                    }
                    if (!item.hasCollection(StringUtil.pluralise(fieldName))) {
                        item.addCollection(new ReferenceList(StringUtil.pluralise(fieldName),
                                                             new ArrayList()));
                    }
                    item.getCollection(StringUtil.pluralise(fieldName)).addRefId(identifier);
                    elements.push(qName);
                } else {
                    throw new SAXException("Field (" + fieldName + "[s]) not found in class ("
                                           + cld.getName() + ")");
                }
            }
            // create attributes specified within tag - but don't process attributes of root element
            // expect these to be namespaces, version, etc and not in InterMine model
            if (attrs.getLength() > 0) {
                Item item = (Item) items.peek();
                String path = (String) paths.peek();
                String refField = "";
                if (xmlInfo.isReferenceElement(path)) {
                    refField = xmlInfo.getReferenceElementField(path);
                }
                for (int i = 0; i < attrs.getLength(); i++) {
                    String attrName = attrs.getQName(i);
                    if (!attrName.equals(refField)) {
                        ClassDescriptor cld =
                                    model.getClassDescriptorByName(clsName(item.getClassName()));
                        if (cld.getReferenceDescriptorByName(attrName, true) != null) {
                            // Reference
                            if (item.hasReference(attrName)) {
                                throw new SAXException("Field (" + cld.getName() + "." + attrName
                                                   + ") is defined in model as"
                                                 + " a reference but attempted to add two values");
                            }
                            ReferenceDescriptor rfd =
                                cld.getReferenceDescriptorByName(attrName, true);
                            String clsName = rfd.getReferencedClassDescriptor().getName();
                            path = xmlInfo.getKeyXPathMatchingPath(path);
                            String referencedId =
                                getReferenceIdentifier(path + attrs.getValue(attrName));
                            Item newItem =
                                itemFactory.makeItem(referencedId, itemName(clsName), "");
                            String identifier = newItem.getIdentifier();
                            item.addReference(new Reference(attrName, identifier));
                        } else if (!xmlInfo.getReferenceFields(path).contains(attrName)
                            && !xmlInfo.getKeyFieldsForPath(path).contains(attrName)) {
                            if (cld.getAttributeDescriptorByName(attrName, true) != null) {
                                item.addAttribute(new Attribute(attrName, attrs.getValue(i)));
                            }
                        } else {
                           // skip
                        }
                    }
                }
            }
        }


        /**
         * {@inheritDoc}
         */
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            // DefaultHandler may call this method more than once for a single
            // attribute content -> hold text & create attribute in endElement
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
                Item item = (Item) items.peek();
                ClassDescriptor cld = model.getClassDescriptorByName(clsName(item.getClassName()));
                if (attributeName == null) {
                    // If no attribute name set, will be name og last element
                    String path = (String) paths.peek();
                    attributeName = path.substring(path.lastIndexOf('/') + 1);
                    attributeName = ("id".equals(attributeName) ? "identifier" : attributeName);

                    if (cld.getAttributeDescriptorByName(attributeName) == null) {
                        throw new SAXException("Class (" + cld.getName() + ") does not have"
                                               + " default content attribute named: "
                                               + attributeName);
                    }
                }
                attValue.append(s);
            }
        }


        /**
         * {@inheritDoc}
         */
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // if qName is top of element stack and is item/ref/col
            // -> store item

            if (attributeName != null) {
                // create attributes specified in content of tags
                Item item = (Item) items.peek();

                if (item.hasAttribute(attributeName)) {
                    throw new SAXException("A value has already been set for attribute ("
                                           + attributeName + ") in item: " + item.getClassName()
                                           + " [" + item.getIdentifier() + "]");
                } else {
                    item.addAttribute(new Attribute(attributeName, attValue.toString()));
                }
                attValue = new StringBuffer();
                attributeName = null;
            }
            if (!isAttribute) {
                String path = (String) paths.pop();
                try {
                    if (!xmlInfo.isReferenceElement(path)) {
                        getItemWriter().store(ItemHelper.convert((Item) items.pop()));
                        count++;
                        if (count % 10000 == 0) {
                            long now = System.currentTimeMillis();
                            if (times[(int) ((count / 10000) % 20)] == -1) {
                                LOG.info("Processed " + count + " objects - running at "
                                        + (600000000L / (now - time)) + " (avg "
                                        + ((60000L * count) / (now - start))
                                        + ") objects per minute");
                            } else {
                                LOG.info("Processed " + count + " objects - running at "
                                        + (600000000L / (now - time)) + " (200000 avg "
                                        + (12000000000L / (now - times[(int) ((count / 10000)
                                                    % 20)]))
                                        + ") (avg " + ((60000L * count) / (now - start))
                                        + ") objects per minute");
                            }
                            time = now;
                            times[(int) ((count / 10000) % 20)] = now;
                        }
                    }
                } catch (ObjectStoreException e) {
                    try {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        pw.close();
                        sw.close();
                        throw new SAXException(sw.toString());
                    } catch (IOException e2) {
                        throw new SAXException(e);
                    }
                }
            }
            isAttribute = false;
        }


        private String getReferenceIdentifier(String key) {
            String identifier = (String) identifiers.get(key);
            if (identifier == null) {
                identifier = getIdentifier();
                LOG.debug("created identifier " + identifier + " for key " + key);
                identifiers.put(key, identifier);
            } else {
                LOG.debug("found identifier " + identifier + " for key " + key);
            }
            return identifier;
        }

        private String pushPaths(String qname) {
            String path = qname;
            if (!paths.empty()) {
                path = (String) paths.peek() + "/" + path;
            }
            LOG.debug("pushing " + path);
            paths.push(path);
            return path;
        }

        private String itemName(String clsName) {
            return model.getNameSpace() + TypeUtil.unqualifiedName(clsName);
        }

        private String clsName(String itemName) {
            return model.getPackageName() + "." + XmlUtil.getFragmentFromURI(itemName);
        }
    }
 }
