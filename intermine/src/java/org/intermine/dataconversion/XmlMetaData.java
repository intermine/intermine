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
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;

import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.exolab.castor.xml.schema.reader.Sax2ComponentReader;
import org.exolab.castor.xml.schema.reader.SchemaUnmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.exolab.castor.xml.schema.*;

import org.intermine.util.StringUtil;

import org.apache.log4j.Logger;

/**
 * Derive information about an XML Schema required when converting the schema
 * to an InterMine model and when converting XML conforming to the schema
 * into InterMine fulldata XML.  Public methods to access class names and
 * key/reference pair information.
 *
 * @author Richard Smith
 * @author Andrew Varley
 */
public class XmlMetaData
{
    protected static final Logger LOG = Logger.getLogger(XmlMetaData.class);

    protected Stack paths;
    protected Map idFields;
    protected Map refFields;
    protected Map refIdPaths;
    protected Map clsNameMap;

    /**
     * Construct with a reader for the XML Schema
     * @param xsdReader reader pointing ant an XML Schema
     * @throws Exception if anything goes wrong
     */
    public XmlMetaData(Reader xsdReader) throws Exception {
        paths = new Stack();
        idFields = new HashMap();
        refFields = new HashMap();
        refIdPaths = new HashMap();
        clsNameMap = new HashMap();

        SchemaUnmarshaller schemaUnmarshaller = null;
        schemaUnmarshaller = new SchemaUnmarshaller();

        Sax2ComponentReader handler = new Sax2ComponentReader(schemaUnmarshaller);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser saxParser = factory.newSAXParser();
        Parser parser = saxParser.getParser();

        parser.setDocumentHandler(handler);
        parser.parse(new InputSource(xsdReader));
        Schema schema = schemaUnmarshaller.getSchema();

        buildRefsMap(schema);
    }

    /**
     * Construct with a Castor Schema object describing and XML Schema
     * @param schema descrption of XML Schema
     * @throws Exception if anyhting goes wrong
     */
    public XmlMetaData(Schema schema) throws Exception {
        paths = new Stack();
        idFields = new HashMap();
        refFields = new HashMap();
        refIdPaths = new HashMap();
        clsNameMap = new HashMap();
        buildRefsMap(schema);
    }

    /**
     * Return true if the given element path is defined as a keyref
     * @param refPath the path to examine
     * @return true if path is a reference
     */
    public boolean isReference(String refPath) {
        return refFields.containsKey(refPath);
    }

    /**
     * Get the attribute of a reference element which contains the
     * reference text.
     * @param refPath the path to examine
     * @return name of the reference attribute
     */
    public String getReferenceField(String refPath) {
        return (String) refFields.get(refPath);
    }

    /**
     * For a path that is defined as a keyref find the corresponding path
     * for the id attribute.
     * @param refPath the path to examine
     * @return the id path
     */
    public String getIdPath(String refPath) {
        return (String) refIdPaths.get(refPath);
    }

    /**
     * Return true if the given element path is defined as a key
     * @param idPath the path to examine
     * @return true if path is a key
     */
    public boolean isId(String idPath) {
        return idFields.containsKey(idPath);
    }

    /**
     * Get the attribute of an element which contains the
     * id text.
     * @param idPath the path to examine
     * @return name of the id attribute
     */
    public String getIdField(String idPath) {
        return (String) idFields.get(idPath);
    }


    /**
     * Return a classname for the element ant specified by xpath.
     * Will possible have _EnclosingClass... according to element
     * nesting.
     * @param xpath the path to examine
     * @return the generated class name
     */
    public String getClsNameFromXPath(String xpath) {
        if (isReference(xpath)) {
            return (String) clsNameMap.get(getIdPath(xpath));
        }
        return (String) clsNameMap.get(xpath);
    }



    private void buildRefsMap(Schema schema) throws Exception {
        Enumeration structures = schema.getElementDecls();

        while (structures.hasMoreElements()) {
            ElementDecl e = (ElementDecl) structures.nextElement();
            processElementDecl((ElementDecl) e);
        }
    }


    private void processElementDecl(ElementDecl eDecl) throws Exception {
        String path = eDecl.getName();
        if (!paths.empty()) {
            path = (String) paths.peek() + "/" + path;
        }
        LOG.debug("pushing paths: " + path);
        paths.push(path);

        String clsName = null;
        XMLType xmlType = eDecl.getType();
        if (eDecl.isReference()) {
            // nothing needs to be done to name
            clsName = eDecl.getReference().getName();
        } else if ((xmlType != null) && (xmlType.getName() != null)) {
            // named complex type
            clsName = xmlType.getName();
        } else if (xmlType != null && xmlType.isComplexType() && (xmlType.getName() != null)) {
            LOG.debug("named complex type");
            clsName = xmlType.getName();
        } else if (xmlType != null && xmlType.isComplexType()) {
            LOG.debug("anon complex type");
            // anon complex type
            String encPath = null;
            if (path.indexOf('/') >= 0) {
                encPath = path.substring(0, path.lastIndexOf('/'));
                clsName = eDecl.getName() + "_" + (String) clsNameMap.get(encPath);
            } else {
                clsName = eDecl.getName();
            }
        }
        if (clsName != null) {
            LOG.debug("clsName = " + clsName);
            clsNameMap.put(path, StringUtil.capitalise(clsName));
        }

        findRefs(eDecl);

        if (eDecl.getType().isComplexType()) {
            ComplexType complexType = (ComplexType) eDecl.getType();
            LOG.debug("processContentModel");
            processContentModelGroup(complexType);
            if (complexType.getBaseType() != null && complexType.getBaseType().isComplexType()) {
                LOG.error("processContentModel(parent)");
                processContentModelGroup((ComplexType) complexType.getBaseType());
            }
        }

        path = (String) paths.pop();
        LOG.debug("popped paths: " + path);
    }


    private void processContentModelGroup(ContentModelGroup cmGroup) throws Exception {
        Enumeration enum = cmGroup.enumerate();
        while (enum.hasMoreElements()) {
            Structure struct = (Structure) enum.nextElement();
            switch (struct.getStructureType()) {
            case Structure.ELEMENT:
                LOG.debug("process element");
                processElementDecl((ElementDecl) struct);
                break;
            case Structure.GROUP:
                LOG.debug("process group");
                //handle nested groups
                processContentModelGroup((Group) struct);
                break;
            default:
                break;
            }
        }
    }


    private void findRefs(ElementDecl eDecl) throws Exception {
        Map keys = new HashMap();
        Map keyrefs = new HashMap();

        Enumeration idCons = eDecl.getIdentityConstraints();
        while (idCons.hasMoreElements()) {
            IdentityConstraint idCon = (IdentityConstraint) idCons.nextElement();
            if (idCon.getStructureType() == Structure.KEYREF) {
                keyrefs.put(idCon.getName(), idCon);
            } else if (idCon.getStructureType() == Structure.KEY) {
                keys.put(idCon.getName(), idCon);
            }
        }

        Map keyNames = new HashMap();
        Iterator iter = keys.values().iterator();
        while (iter.hasNext()) {
            Key key = (Key) iter.next();
            String path = paths.peek() + "/" + key.getSelector().getXPath();
            Enumeration enum = key.getFields();
            String field = ((IdentityField) enum.nextElement()).getXPath();
            if (enum.hasMoreElements()) {
                throw new Exception("Unable to deal with Keys on more than one field");
            }
            if (field.startsWith("@")) {
                field = field.substring(field.indexOf('@') + 1);
            }
            idFields.put(path, field);
            keyNames.put(key.getName(), path);
        }

        iter = keyrefs.values().iterator();
        while (iter.hasNext()) {
            KeyRef keyref = (KeyRef) iter.next();
            String path = paths.peek() + "/" + keyref.getSelector().getXPath();
            Enumeration enum = keyref.getFields();
            String field = ((IdentityField) enum.nextElement()).getXPath();
            if (enum.hasMoreElements()) {
                throw new Exception("Unable to deal with KeyRefs on more than one field");
            }
            if (field.startsWith("@")) {
                field = field.substring(field.indexOf('@') + 1);
            }
            refFields.put(path, field);
            refIdPaths.put(path, keyNames.get(keyref.getRefer()));
        }
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        String endl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        sb.append("idFields: " + idFields + endl)
            .append("refFields: " + refFields + endl)
            .append("refIdPaths: " + refIdPaths + endl)
            .append("clsNameMap: " + clsNameMap + endl);
        return sb.toString();
    }

}
