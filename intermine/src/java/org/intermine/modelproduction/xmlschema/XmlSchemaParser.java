package org.intermine.modelproduction.xmlschema;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

// This code is based on Castor's SourceGenerator to autobuild Java
// classes from XML-Schema.  This code carries the following license:

/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Intalio, Inc.  For written permission,
 *    please contact info@exolab.org.
 *
 * 4. Products derived from this Software may not be called "Exolab"
 *    nor may "Exolab" appear in their names without prior written
 *    permission of Intalio, Inc. Exolab is a registered
 *    trademark of Intalio, Inc.
 *
 * 5. Due credit should be given to the Exolab Project
 *    (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY INTALIO, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * INTALIO, INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 1999-2003 (C) Intalio, Inc. All Rights Reserved.
 *
 *
 * $Id$
 */

import java.io.Reader;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.XMLType;
import org.exolab.castor.xml.schema.ModelGroup;
import org.exolab.castor.xml.schema.*;
import org.exolab.castor.xml.schema.reader.Sax2ComponentReader;
import org.exolab.castor.xml.schema.reader.SchemaUnmarshaller;

import org.intermine.modelproduction.ModelParser;
import org.intermine.metadata.*;
import org.intermine.ontology.OntologyUtil;
import org.intermine.util.StringUtil;

import org.apache.log4j.Logger;

/**
 * Translates an XML Schema definition into an InterMine model definition.
 *
 * @author Richard Smith
 * @author Andrew Varley
 */
public class XmlSchemaParser implements ModelParser
{
    protected static final Logger LOG = Logger.getLogger(XmlSchemaParser.class);

    protected String nameSpace = null;
    protected String pkgName;
    protected String modelName;

    protected Set classes;
    protected Map attributes;
    protected Map references;
    protected Map collections;
    protected Stack clsStack;

    /**
     * Constructor that takes the modelName and pkgName
     *
     * @param pkgName name of package to generation Java code in
     * @param modelName the name of the model to produce
     */
    public XmlSchemaParser(String modelName, String pkgName) {
        this.pkgName = pkgName;
        this.modelName = modelName;
    }

    /**
     * Read source model information in XML Schema format and
     * construct an InterMine Model object.
     *
     * @param reader the source XMI file to parse
     * @return the InterMine Model created
     * @throws Exception if Model not created successfully
     */
    public Model process(Reader reader) throws Exception {
        classes = new HashSet();
        attributes = new HashMap();
        references = new HashMap();
        collections = new HashMap();
        clsStack = new Stack();

        SchemaUnmarshaller schemaUnmarshaller = null;
        schemaUnmarshaller = new SchemaUnmarshaller();

        Sax2ComponentReader handler = new Sax2ComponentReader(schemaUnmarshaller);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser saxParser = factory.newSAXParser();
        Parser parser = saxParser.getParser();

        parser.setDocumentHandler(handler);
        parser.parse(new InputSource(reader));
        Schema schema = schemaUnmarshaller.getSchema();

        process(schema);

        Model m = new Model(modelName, OntologyUtil.correctNamespace(nameSpace), classes);
        return m;
    }



    /**
     * Create an InterMine model from an in-memory XML-Schema description.
     * @param schema and XML-Schema description
     * @throws Exception if anything goes wrong
     */
    protected void process(Schema schema) throws Exception {
        if (schema == null) {
            throw new IllegalArgumentException("schema was null");
        }

        // make sure the XML Schema is valid
        try {
            schema.validate();
        } catch (ValidationException ve) {
            String err = "The schema:" + schema.getSchemaLocation() + " is not valid.\n";
            err += ve.getMessage();
            throw new IllegalArgumentException("Schema (" + schema.getSchemaLocation()
                                               + ") is not valid because: " + ve.getMessage());
        }

        nameSpace = schema.getTargetNamespace();
        if (nameSpace == null) {
            nameSpace = "";
        }

        // Process all top level element declarations
        Enumeration structures = schema.getElementDecls();

        while (structures.hasMoreElements()) {
            ElementDecl e = (ElementDecl) structures.nextElement();
            processElementDecl((ElementDecl) e);
        }

        structures = schema.getComplexTypes();
        while (structures.hasMoreElements()) {
            ComplexType struct = (ComplexType) structures.nextElement();
            processComplexType((ComplexType) struct);
        }

        // Don't need to process simple types
        //structures = schema.getSimpleTypes();
        //while (structures.hasMoreElements()) {
        //    processSimpleType((SimpleType) structures.nextElement());
        //}

        structures = schema.getModelGroups();
        while (structures.hasMoreElements()) {
            processGroup((ModelGroup) structures.nextElement());
        }
    }


    /**
     * Process an ElementDecl, if an anonymouse complex type call processComplexType
     * to create a ClassDescriptor.  Need to return classname if possible so
     * ReferenceDescriptors can be built correctly (called from processContentModel)
     * @param eDecl element to process
     * @return name of class if possible or null
     */
    protected String processElementDecl(ElementDecl eDecl) {

        // only want to produce classes for elements with anonymous complex types
        // named complex types will be processed elsewhere
        XMLType xmlType = eDecl.getType();
        if (eDecl.isReference()) {
            return eDecl.getReference().getName();
        } else if ((xmlType != null) && (xmlType.getName() != null)) {
            return xmlType.getName();
        }

        String clsName = null;
        if (xmlType == null) {
            return null;
        } else if (xmlType.isComplexType()) {
            ComplexType complexType = (ComplexType) xmlType;
            if (!clsStack.empty()) {
                clsName = uniqueClassName(eDecl.getName(), (String) clsStack.peek());
            } else {
                clsName = eDecl.getName();
            }
            LOG.debug("pushing: " + clsName);
            clsStack.push(clsName);
            processComplexType(complexType);
            if (complexType.isTopLevel()) {
                return null;
            }
        } else {
            // SimpleType or AnyType - no processing needed
        }

        if (clsName == null && !clsStack.empty()) {
            clsName = (String) clsStack.peek();
        }
        return clsName;
    }

    /**
     * Filter out empty groups then call processContentModel
     * @param group a group to process
     */
    protected void processGroup(Group group) {
        if (group == null) {
            return;
        }

        // don't generate classes for empty groups
        if (group.getParticleCount() == 0) {
            if (group instanceof ModelGroup) {
                ModelGroup mg = (ModelGroup) group;
                if (mg.isReference() && (mg.getReference().getParticleCount() == 0)) {
                    return;
                }
            } else {
                return;
            }
        }

        processContentModel(group);
    }


    /**
     * Given a toplevel complex type or an un-named element declaration
     * deal with attributes and nested groups (possible recursion) then
     * create a new ClassDescriptor.
     * @param complexType complex type to process
     */
    protected void processComplexType(ComplexType complexType) {
        // a ComplexType we haven't seen before -> create ClassDescriptor

        if (complexType == null) {
            return;
        }

        // push classname on stack, will be popped when create ClassDescriptor
        if (complexType.getName() != null) {
            LOG.debug("pushing: " + complexType.getName());
            clsStack.push(complexType.getName());
        }

        // process attributes
        processAttributes(complexType);

        // process groups - e.g. sequences
        processContentModel(complexType);

        String clsName = (String) clsStack.pop();
        LOG.debug("popped: " + clsName);

        // look for immediate super class
        String baseType = null;
        if (complexType.getBaseType() != null && !complexType.getBaseType().isSimpleType()) {
            baseType = complexType.getBaseType().getName();
            LOG.debug(clsName + " has super " + baseType);
        }

        LOG.debug("creating new cld: " + StringUtil.capitalise(clsName));
        ClassDescriptor cld = new ClassDescriptor(StringUtil.capitalise(clsName), baseType, true,
                                                  getFieldSetForClass(attributes, clsName),
                                                  getFieldSetForClass(references, clsName),
                                                  getFieldSetForClass(collections, clsName));
        classes.add(cld);
    }


    /**
     * Given a complex type extract any attributes and create AttributeDesriptors
     * for them.
     * @param complexType complex type to process
     */
    protected void processAttributes(ComplexType complexType) {
        if (complexType == null) {
            return;
        }

        Enumeration enum = complexType.getAttributeDecls();
        while (enum.hasMoreElements()) {
            AttributeDecl attribute = (AttributeDecl) enum.nextElement();
            String clsName = (String) clsStack.peek();
            LOG.debug("creating atd (" + attribute.getName() + ","
                      + attribute.getSimpleType().getName() + ")" + " for class: " + clsName);
            AttributeDescriptor atd = new AttributeDescriptor(generateJavaName(attribute.getName()),
                            OntologyUtil.xmlToJavaType(attribute.getSimpleType().getName()));
            Set atds = getFieldSetForClass(attributes, clsName);
            atds.add(atd);
        }
    }


    /**
     * Iterate through particles of a group and create attribute, reference
     * and collection descriptors as appropriate.  Recurse as necessary for
     * nested groups.
     * @param cmGroup a group to process
     */
    protected void processContentModel(ContentModelGroup cmGroup) {
        if (cmGroup == null) {
            return;
        }

        String clsName = (String) clsStack.peek();

        Enumeration enum = cmGroup.enumerate();
        while (enum.hasMoreElements()) {
            Structure struct = (Structure) enum.nextElement();
            switch (struct.getStructureType()) {
            case Structure.ELEMENT:
                ElementDecl eDecl = (ElementDecl) struct;
                if (eDecl.isReference()) {
                    continue;
                }
                XMLType xmlType = eDecl.getType();
                if (xmlType.isComplexType() && Particle.UNBOUNDED == eDecl.getMaxOccurs()) {
                    // collection
                    String colName = null;
                    if (xmlType.getName() == null) {
                        colName = StringUtil.decapitalise(
                            StringUtil.pluralise(generateJavaName(eDecl.getName())));
                    } else {
                        colName = generateJavaName(eDecl.getName());
                    }
                    String refType =  processElementDecl(eDecl);
                    LOG.debug("creating cod (" + colName + "," + refType + ")"
                              + " for class: " + clsName);
                    CollectionDescriptor cod = new CollectionDescriptor(colName, refType,
                                                                        null, true);
                    HashSet cods = getFieldSetForClass(collections, clsName);
                    cods.add(cod);
                } else if (xmlType.isComplexType()) {
                    // reference
                    String refType =  processElementDecl(eDecl);
                    LOG.debug("creating rfd (" + eDecl.getName() + "," + refType + ")"
                              + " for class: " + clsName);
                    ReferenceDescriptor rfd = new ReferenceDescriptor(
                                                      generateJavaName(eDecl.getName()),
                                                      refType, null);
                    HashSet rfds = getFieldSetForClass(references, clsName);
                    rfds.add(rfd);
                } else if (xmlType.isSimpleType()) {
                    // is a SimpleType -> attribute
                    SimpleType simpleType = (SimpleType) xmlType;
                    String type = null;
                    if (simpleType.hasFacet(Facet.ENUMERATION)) {
                        type = simpleType.getBaseType().getName();
                    } else {
                        type = simpleType.getName();
                    }
                    LOG.debug("creating atd (" + eDecl.getName() + "," + type + ")"
                              + " for class: " + clsName);
                    AttributeDescriptor atd = new AttributeDescriptor(
                                    generateJavaName(eDecl.getName()),
                                    OntologyUtil.xmlToJavaType(type));
                    Set atds = getFieldSetForClass(attributes, (String) clsStack.peek());
                    atds.add(atd);
                }

                break;
            case Structure.GROUP:
                processContentModel((Group) struct);
                //handle nested groups
                if (!((cmGroup instanceof ComplexType)
                       || (cmGroup instanceof ModelGroup))) {
                        processGroup((Group) struct);
                    }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Get or create a set of FieldsDescriptors in the given map for a class.
     * @param fieldMap the map to search/create field set in
     * @param className local name of class
     * @return set of FieldDescriptors
     */
    protected HashSet getFieldSetForClass(Map fieldMap, String className) {
        HashSet fields = (HashSet) fieldMap.get(className);
        if (fields == null) {
            fields = new HashSet();
            fieldMap.put(className, fields);
        }
        return fields;
    }

    private String uniqueClassName(String clsName, String encName) {
        return clsName + "_" + encName;
    }


    private String generateJavaName(String name) {
        if ("id".equals(name)) {
            return "identifier";
        }
        return name;
    }


}
