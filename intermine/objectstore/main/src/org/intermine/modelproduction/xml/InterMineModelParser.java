package org.intermine.modelproduction.xml;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;
import java.util.Set;
import java.io.Reader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.modelproduction.ModelParser;
import org.intermine.metadata.*;
import org.intermine.util.SAXParser;

import org.apache.log4j.Logger;

/**
 * Parse InterMine metadata XML to produce a InterMine Model
 *
 * @author Mark Woodbridge
 */
public class InterMineModelParser implements ModelParser
{
    private static final Logger LOG = Logger.getLogger(InterMineModelParser.class);

    /**
     * Read source model information in InterMine XML format and
     * construct a InterMine Model object.
     *
     * @param reader the source model to parse
     * @return the InterMine Model created
     * @throws Exception if Model not created successfully
     */
    public Model process(Reader reader) throws Exception {
        ModelHandler handler = new ModelHandler();
        SAXParser.parse(new InputSource(reader), handler);
        Model model = new Model(handler.modelName, handler.modelNameSpace, handler.classes);
        return model;
    }

    /**
     * Read source model information in InterMine XML format and
     * create a set of ClassDescriptors.
     *
     * @param reader the source model to parse
     * @return a set of
     * @throws Exception if Model not created successfully
     */
    public Set<ClassDescriptor> generateClassDescriptors(Reader reader) throws Exception {
        ModelHandler handler = new ModelHandler();
        SAXParser.parse(new InputSource(reader), handler);
        return handler.classes;
    }

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class ModelHandler extends DefaultHandler
    {
        String modelName;
        String modelNameSpace;
        Set<ClassDescriptor> classes = new LinkedHashSet<ClassDescriptor>();
        SkeletonClass cls;

        /**
         * @see DefaultHandler#startElement(String, String, String, Attributes)
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.equals("model")) {
                modelName = attrs.getValue("name");
                modelNameSpace = attrs.getValue("namespace");
            } else if (qName.equals("class")) {
                String name = attrs.getValue("name");
                String supers = attrs.getValue("extends");
                boolean isInterface = Boolean.valueOf(attrs.getValue("is-interface"))
                    .booleanValue();
                cls = new SkeletonClass(name, supers, isInterface);
            } else if (qName.equals("attribute")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("type");
                cls.attributes.add(new AttributeDescriptor(name, type));
            } else if (qName.equals("reference")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("referenced-type");
                String reverseReference = attrs.getValue("reverse-reference");
                cls.references.add(new ReferenceDescriptor(name, type,
                                                           reverseReference));
            } else if (qName.equals("collection")) {
                String name = attrs.getValue("name");
                String type = attrs.getValue("referenced-type");
                if (attrs.getValue("ordered") != null) {
                    LOG.error("Deprecated \"ordered\" attribute on collection " + cls.name
                            + "." + name);
                }
                String reverseReference = attrs.getValue("reverse-reference");
                cls.collections.add(new CollectionDescriptor(name, type,
                                                             reverseReference));
            }
        }

        /**
         * @see DefaultHandler#endElement(String, String, String)
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("class")) {
                classes.add(new ClassDescriptor(cls.name, cls.supers,
                                                cls.isInterface, cls.attributes, cls.references,
                                                cls.collections));
            }
        }
    }

    /**
     * Semi-constructed ClassDescriptor
     */
    static class SkeletonClass
    {
        String name, supers;
        boolean isInterface;
        Set<AttributeDescriptor> attributes = new LinkedHashSet<AttributeDescriptor>();
        Set<ReferenceDescriptor> references = new LinkedHashSet<ReferenceDescriptor>();
        Set<CollectionDescriptor> collections = new LinkedHashSet<CollectionDescriptor>();
        
        /**
         * Constructor
         * @param name the fully qualified name of the described class
         * @param supers a space string of fully qualified class names
         * @param isInterface true if describing an interface
         */
        SkeletonClass(String name, String supers, boolean isInterface) {
            this.name = name;
            this.supers = supers;
            this.isInterface = isInterface;
        }
    }
}
