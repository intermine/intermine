package org.intermine.modelproduction.xmlschema;

/*
 * Copyright (C) 2002-2007 FlyMine
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
 */

import java.io.Reader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
import org.intermine.util.XmlUtil;
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

    protected String nameSpace;
    protected String pkgName;
    protected String modelName;

    protected Set classes;
    protected Set processed;
    protected Map attributes;
    protected Map references;
    protected Map collections;
    protected Map fieldNamesMap;
    protected Stack clsStack;
    protected Stack refsStack;
    protected Stack paths;
    protected XmlMetaData xmlInfo;

    protected Set namedTypesAlreadyDone = new HashSet();

    /** Map from class name to Set of key field names. */
    protected Map keyFieldSets = new HashMap();

    /**
     * Constructor that takes the modelName and pkgName
     *
     * @param pkgName name of package to generation Java code in
     * @param modelName the name of the model to produce
     * @param nameSpace namespace of the target model
     */
    public XmlSchemaParser(String modelName, String pkgName, String nameSpace) {
        this.pkgName = pkgName;
        this.modelName = modelName;
        this.nameSpace = new StringBuffer().append(nameSpace).
                append("/").append(modelName).append("#").toString();
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
        classes = new LinkedHashSet();
        processed = new HashSet();
        attributes = new LinkedHashMap();
        references = new LinkedHashMap();
        collections = new LinkedHashMap();
        fieldNamesMap = new HashMap();
        clsStack = new Stack();
        refsStack = new Stack();
        paths = new Stack();

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

        LOG.info("ModelName = " + modelName + ", nameSpace = " + nameSpace + ", classes = "
                + classes);
        Model m = new Model(modelName, nameSpace, classes);
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

        xmlInfo = new XmlMetaData(schema);
        // make sure the XML Schema is valid
        try {
            schema.validate();
        } catch (ValidationException ve) {
            String err = "The schema:" + schema.getSchemaLocation() + " is not valid.\n";
            err += ve.getMessage();
            throw new IllegalArgumentException("Schema (" + schema.getSchemaLocation()
                                               + ") is not valid because: " + ve.getMessage());
        }

        // Process all top level element declarations
        Enumeration structures = schema.getElementDecls();

        while (structures.hasMoreElements()) {
            ElementDecl e = (ElementDecl) structures.nextElement();
            processElementDecl(e);
        }
    }


    /**
     * Process an ElementDecl, if an anonymous complex type call processComplexType
     * to create a ClassDescriptor.  Need to return classname if possible so
     * ReferenceDescriptors can be built correctly (called from processContentModel)
     * @param eDecl element to process
     * @return name of class if possible or null
     */
    protected String processElementDecl(ElementDecl eDecl) {

        if (eDecl == null) {
            return null;
        }

        // Three cases possible processing elements
        // 1. named complex type - add element name to path and put on stack
        //                       - process the complex type
        // 2. anon complex type - create class name from element name -> clsStack
        //                      - add element name to path and put on stack
        //                      - process the complex type
        // 3. element is a keyref - find out classname of target and return

        String clsName = null;
        String path = eDecl.getName();
        boolean generatedName = false;
        if (!paths.empty()) {
            path = (String) paths.peek() + "/" + path;
        }

        if (eDecl.isReference()) {
            return eDecl.getReferenceName();
        }

        XMLType xmlType = eDecl.getType();

        if (xmlType != null && xmlType.isComplexType()) {

            // check whether this element is actually a reference
            if (xmlInfo.isReferenceElement(path)) {
                //String field = xmlInfo.getReferenceElementField(path);
                //String key = xmlInfo.getReferencingKeyName(path, field);
                //String clsPath = xmlInfo.getKeyPath(key);
                clsName = (String) xmlInfo.getClsNameFromXPath(path);
                LOG.debug("found reference element at: " + path + " referencing class: " + clsName);
                return clsName;

            } else if (xmlType.getName() == null) {
                if (!clsStack.empty()) {
                    clsName = StringUtil.capitalise(uniqueClassName(eDecl.getName(),
                                                     (String) clsStack.peek()));
                } else {
                    clsName = StringUtil.capitalise(eDecl.getName());
                }
                LOG.debug("(no name) processElementDecl pushing " + clsName + " onto clsStack "
                                                                + clsStack);
                clsStack.push(clsName);
                generatedName = true;
            }

            LOG.debug("pushing " + path + " onto paths " + paths);
            paths.push(path);
            clsName = processComplexType((ComplexType) xmlType);
            path = (String) paths.pop();
            LOG.debug("popped path: " + path);

            if (generatedName) {
                clsName = (String) clsStack.pop();
                LOG.debug("(no name) Popped clsName " + clsName + " from clsStack " + clsStack);
            }
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
                } else {
                    processContentModel(mg.getReference(), false);
                }
            } else {
                return;
            }
        }
        LOG.debug("processGroup() calling processContentModel(\"" + group.getName() + "\")");
        processContentModel(group, false);
    }


    /**
     * Given a toplevel complex type or an un-named element declaration
     * deal with attributes and nested groups (possible recursion) then
     * create a new ClassDescriptor.
     * @param complexType complex type to process
     * @return the class name of the complex type or null
     */
    protected String processComplexType(ComplexType complexType) {
        if (complexType == null) {
            LOG.debug("Entering and leaving processComplexType(null)");
            return null;
        }

        LOG.debug("Entering processComplexType(" + complexType.getName() + ")");
        // Two possibilities processing complex types
        // 1. named complex type - create class name from complex type name -> clsStack
        //                       - recurse in to content elements
        //                       - create new ClassDescriptor
        // 2. anon complex type - class name is already on clsStack
        //                      - recurse in to content elements
        //                      - create new ClassDescriptor


        // 1. named complex type - put class name on clsStack
        if (complexType.getName() != null) {
            String clsName = StringUtil.capitalise(complexType.getName());
            LOG.debug("processComplexType pushing " + clsName + " onto clsStack " + clsStack);
            if (namedTypesAlreadyDone.contains(clsName)) {
                LOG.debug("Leaving processComplexType(" + complexType.getName() + ") - clsName "
                        + clsName + " has aleady been processed.");
                return clsName;
            }
            namedTypesAlreadyDone.add(clsName);
            clsStack.push(clsName);
        }

        if (complexType.isSimpleContent() && complexType.getAttributeDecls().hasMoreElements()) {
            String path = (String) paths.peek();
            String attrName = path.substring(path.lastIndexOf('/') + 1);
            String typeName = simpleTypeToBuiltInTypeName((SimpleType) complexType.getBaseType());
            AttributeDescriptor atd = new AttributeDescriptor(attrName,
                                        XmlUtil.xmlToJavaType(typeName));
            Set atds = getFieldSetForClass(attributes, (String) clsStack.peek());
            atds.add(atd);
        }

        // create AttributeDescrptors for class
        processAttributes(complexType);

        // recurse into sub elements
        processContentModel(complexType, false);

        // now create ClassDescriptor
        String clsName;
        if (complexType.getName() != null) {
            clsName = (String) clsStack.pop();
            LOG.debug("popped clsStack: " + clsName);
        } else {
            clsName = (String) clsStack.peek();
            LOG.debug("peeked clsStack: " + clsName);
        }

        // look for immediate super class
        String baseType = null;
        if (complexType.getBaseType() != null && !complexType.getBaseType().isSimpleType()) {
            processComplexType((ComplexType) complexType.getBaseType());
            baseType = this.pkgName + "."
                + StringUtil.capitalise(complexType.getBaseType().getName());
            LOG.debug(clsName + " has super " + baseType);
        }

        if (!processed.contains(clsName)) {
            LOG.debug("creating new cld: " + this.pkgName + "." + clsName);
            ClassDescriptor cld = new ClassDescriptor(this.pkgName + "." + clsName, baseType, true,
                                                      getFieldSetForClass(attributes, clsName),
                                                      getFieldSetForClass(references, clsName),
                                                      getFieldSetForClass(collections, clsName));
            classes.add(cld);
            processed.add(clsName);
            // Record primary keys
            Set keys = (Set) keyFieldSets.get(clsName);
            if (keys == null) {
                keys = new HashSet();
                keyFieldSets.put(clsName, keys);
            }
            keys.addAll(xmlInfo.getKeyFields((String) paths.peek()));
        }
        LOG.debug("Leaving processComplexType(" + complexType.getName() + ") normally");

        return clsName;
    }


    /**
     * Given a complex type extract any attributes and create AttributeDesriptors
     * for them.
     * @param complexType complex type to process
     */
    protected void processAttributes(ComplexType complexType) {
        if (complexType == null) {
            LOG.debug("Entering and leaving processAttributes(null)");
            return;
        }
        LOG.debug("Entering processAttributes(" + complexType.getName() + ")");

        String path = (String) paths.peek();
        Enumeration declEnum = complexType.getAttributeDecls();
        while (declEnum.hasMoreElements()) {
            AttributeDecl attribute = (AttributeDecl) declEnum.nextElement();
            String clsName = (String) clsStack.peek();
            String fieldName = attribute.getName();
            // Check for references
            if (xmlInfo.isReferenceField(path, fieldName)) {
                // Need to determine the type using the keyref and key
                String key = xmlInfo.getReferencingKeyName(path, fieldName);
                String clsPath = xmlInfo.getKeyPath(key);
                String refType = xmlInfo.getClsNameFromXPath(clsPath);
                fieldName = StringUtil.decapitalise(fieldName);
                LOG.debug("creating reference attribute at: " + path + " field:" + fieldName
                      + " referencing: " + clsPath);
                ReferenceDescriptor rfd = new ReferenceDescriptor(fieldName,
                                                          this.pkgName + "." + refType, null);
                HashSet rfds = getFieldSetForClass(references, clsName);
                rfds.add(rfd);
            } else {
                LOG.debug("creating atd (" + fieldName + ","
                      + attribute.getSimpleType().getName() + ")" + " for class: " + clsName);
                String attributeTypeName = simpleTypeToBuiltInTypeName(attribute.getSimpleType());
                AttributeDescriptor atd
                      = new AttributeDescriptor(generateJavaName(attribute.getName()),
                                                XmlUtil.xmlToJavaType(attributeTypeName));
                Set atds = getFieldSetForClass(attributes, clsName);
                atds.add(atd);
            }
        }
        LOG.debug("Leaving processAttributes(" + complexType.getName() + ") normally");
    }


    /**
     * Iterate through particles of a group and create attribute, reference
     * and collection descriptors as appropriate.  Recurse as necessary for
     * nested groups.
     * @param cmGroup a group to process
     * @param isCollection if a containing group/choice/sequence declares multiple cardinality
     */
    protected void processContentModel(ContentModelGroup cmGroup, boolean isCollection) {
        if (cmGroup == null) {
            LOG.debug("processContentModel(null, " + isCollection + ") called");
            return;
        }
        boolean origIsCollection = isCollection;
        String cmGroupDescription = cmGroup.toString();
        if (cmGroup instanceof Group) {
            Group cmG = (Group) cmGroup;
            cmGroupDescription = cmG.getOrder().toString() + ": " + cmG.getName()
                + ", maxOccurs = " + cmG.getMaxOccurs();
            if ((cmG.getMaxOccurs() < 0) || (cmG.getMaxOccurs() > 1)) {
                isCollection = true;
            }
        } else if (cmGroup instanceof ComplexType) {
            cmGroupDescription = ((ComplexType) cmGroup).getName();
        }


        LOG.debug("Entering processContentModel(" + cmGroupDescription + ", "
                + origIsCollection + ")");

        if (clsStack.isEmpty()) {
            LOG.warn("processContentModel called with empty clsStack");
            LOG.debug("Leaving processContentModel(" + cmGroupDescription + ", "
                    + origIsCollection + ")");
            return;
        }
        String clsName = (String) clsStack.peek();

        Enumeration cmGroupEnum = cmGroup.enumerate();
        while (cmGroupEnum.hasMoreElements()) {
            Structure struc = (Structure) cmGroupEnum.nextElement();
            switch (struc.getStructureType()) {
            case Structure.ELEMENT:
                ElementDecl eDecl = (ElementDecl) struc;
                LOG.debug("Found ElementDecl with maxOccurs " + eDecl.getMaxOccurs());
                XMLType xmlType = eDecl.getType();
                String refType = null;
                String fieldName = null;
                if (xmlType.isComplexType() || eDecl.isReference()) {
                    refType = StringUtil.capitalise(processElementDecl(eDecl));
                    if (refType == null) {
                        LOG.debug("processElementDecl returned null for clsName:" + clsName
                            + " element name:" + eDecl.getName());
                        continue;
                    }
                }
                boolean declIsCollection = isCollection || (eDecl.getMaxOccurs() < 0)
                    || (eDecl.getMaxOccurs() > 1);
                if (xmlType.isComplexType()) {
                    if (declIsCollection) {
                        // collection
                        fieldName = StringUtil.decapitalise(
                                     StringUtil.pluralise(generateJavaName(eDecl.getName())));
                        LOG.debug("creating collection (" + fieldName + "," + refType
                                + ") for class: " + clsName);
                        CollectionDescriptor cod
                            = new CollectionDescriptor(fieldName, this.pkgName + "." + refType,
                                                       null);
                        HashSet cods = getFieldSetForClass(collections, clsName);
                        cods.add(cod);
                    } else {
                        // reference
                        fieldName = eDecl.getName();
                        LOG.debug("creating reference (" + fieldName + "," + refType
                                + ") for class: " + clsName);
                        ReferenceDescriptor rfd = new ReferenceDescriptor(
                                                          StringUtil.decapitalise(fieldName),
                                                          this.pkgName + "." + refType, null);
                        HashSet rfds = getFieldSetForClass(references, clsName);
                        rfds.add(rfd);
                    }
                } else if (xmlType.isSimpleType()) {
                    SimpleType simpleType = (SimpleType) xmlType;
                    String type = null;
                    if (simpleType.hasFacet(Facet.ENUMERATION)) {
                        type = simpleType.getBaseType().getName();
                    } else {
                        type = simpleTypeToBuiltInTypeName(simpleType);
                    }
                    fieldName = generateJavaName(eDecl.getName());
                    if (declIsCollection) {
                        // SimpleType collection - need to make up a class to put in the collection.
                        LOG.debug("Creating collection (" + eDecl.getName() + ", " + type
                                + ") for class: " + clsName);
                        String subClassName = uniqueClassName(StringUtil.capitalise(fieldName),
                                clsName);
                        CollectionDescriptor cod = new CollectionDescriptor(
                                StringUtil.decapitalise(StringUtil.pluralise(fieldName)),
                                this.pkgName + "." + subClassName, null);
                        AttributeDescriptor atd = new AttributeDescriptor(
                                generateJavaName(fieldName), XmlUtil.xmlToJavaType(type));
                        ClassDescriptor cld = new ClassDescriptor(this.pkgName + "." + subClassName,
                                null, false, Collections.singleton(atd), Collections.EMPTY_SET,
                                Collections.EMPTY_SET);
                        classes.add(cld);
                        processed.add(subClassName);
                        Set cods = getFieldSetForClass(collections, clsName);
                        cods.add(cod);
                    } else {
                        // is a SimpleType -> attribute
                        LOG.debug("creating atd (" + eDecl.getName() + ", " + type
                                + ") for class: " + clsName);
                        AttributeDescriptor atd = new AttributeDescriptor(
                                        generateJavaName(eDecl.getName()),
                                        XmlUtil.xmlToJavaType(type));
                        Set atds = getFieldSetForClass(attributes, clsName);
                        atds.add(atd);
                    }
                }

                break;
            case Structure.GROUP:
                processContentModel((Group) struc, isCollection);
                //handle nested groups
                if (!((cmGroup instanceof ComplexType)
                       || (cmGroup instanceof ModelGroup))) {
                        processGroup((Group) struc);
                    }
                break;
            case Structure.MODELGROUP:
                processContentModel(((ModelGroup) struc).getReference(), isCollection);
                break;
            default:
                break;
            }
        }
        LOG.debug("Leaving processContentModel(" + cmGroupDescription + ", " + origIsCollection
                + ") normally");
    }

    /**
     * Get or create a set of FieldsDescriptors in the given map for a class.
     * @param fieldMap the map to search/create field set in
     * @param className local name of class
     * @return set of FieldDescriptors
     */
    private HashSet getFieldSetForClass(Map fieldMap, String className) {
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

    private String simpleTypeToBuiltInTypeName(SimpleType type) {
        String typeName = null;
        if (!type.isBuiltInType() && type.getBuiltInBaseType() != null) {
            typeName = type.getBuiltInBaseType().getName();
        } else {
            typeName = type.getName();
        }
        if (typeName == null) {
            LOG.error("Null simple type " + type);
            typeName = "string";
        }
        if (typeName.equals("timeStampType")) {
            typeName = "string";
        }
        return typeName;
    }

    /**
     * Given a model class name, return a Set of key field names.
     * @param clsName model class name
     * @return set of field names
     */
    public Set getKeyFieldsForClass(String clsName) {
        return (Set) keyFieldSets.get(clsName);
    }
}
