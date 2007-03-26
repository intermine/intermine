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

import java.io.Reader;
import java.util.Set;
import java.util.Stack;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

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
 * @author Thomas Riley
 */
public class XmlMetaData
{
    protected static final Logger LOG = Logger.getLogger(XmlMetaData.class);

    protected Stack paths;
    /** path -&gt; Set of key fields */
    protected Map keyFields;
    /** key name -&gt; path */
    protected Map keyNameToPath;
    /** key name -&gt; field name */
    protected Map keyNameToField;
    /** path -&gt; Set of referring fields */
    protected Map keyrefFields;
    /** path+"/"+field -&gt; key name */
    protected Map keyrefFieldToKey;
    /** key/keyref xpath -&gt; to regex pattern */
    protected Map xpathToRegex;
    /** element path -&gt; field name. */
    protected Map referenceElements;

    protected Map keyNames;
    protected Map clsNameMap;

    /**
     * Construct with a reader for the XML Schema
     * @param xsdReader reader pointing ant an XML Schema
     * @throws Exception if anything goes wrong
     */
    public XmlMetaData(Reader xsdReader) throws Exception {
        paths = new Stack();
        keyFields = new HashMap();
        keyNameToPath = new HashMap();
        keyNameToField = new HashMap();
        keyrefFields = new HashMap();
        keyrefFieldToKey = new HashMap();
        clsNameMap = new HashMap();
        keyNames = new HashMap();
        xpathToRegex = new HashMap();
        referenceElements = new HashMap();

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
        filterReferenceElements();
        LOG.debug("clsNameMap: " + clsNameMap);
        LOG.debug(toString());
    }

    /**
     * Construct with a Castor Schema object describing and XML Schema
     * @param schema descrption of XML Schema
     * @throws Exception if anyhting goes wrong
     */
    public XmlMetaData(Schema schema) throws Exception {
        paths = new Stack();
        keyFields = new HashMap();
        keyNameToPath = new HashMap();
        keyNameToField = new HashMap();
        keyrefFields = new HashMap();
        keyrefFieldToKey = new HashMap();
        clsNameMap = new HashMap();
        keyNames = new HashMap();
        xpathToRegex = new HashMap();
        referenceElements = new HashMap();

        buildRefsMap(schema);
        filterReferenceElements();
        LOG.debug("clsNameMap: " + clsNameMap);
        LOG.debug(toString());
    }

    /**
     * Return true if the given field on the given element path is a key.
     *
     * @param path  the path to examine
     * @param field  the field name
     * @return true if field is a key field
     */
    public boolean isKeyField(String path, String field) {
        return getKeyFields(path).contains(field);
    }

    /**
     * Given a path, return a Set of field names that are keys. If the path
     * has no key fields, an empty Set will be returned.
     *
     * @param path  path
     * @return  Set of field names
     */
    public Set getKeyFields(String path) {
        Set set = (Set) keyFields.get(path);
        if (set == null) {
            return new TreeSet();
        } else {
            return set;
        }
    }

    /**
     * Return true if the given field on the given element path is a reference.
     *
     * @param path  the path to examine
     * @param field  the field name
     * @return true if field is a reference field
     */
    public boolean isReferenceField(String path, String field) {
        return getReferenceFields(path).contains(field);
    }

    /**
     * Given a path, return a List of field names that are references.
     *
     * @param path  path
     * @return  List of field names
     */
    public Set getReferenceFields(String path) {
        Set set = (Set) keyrefFields.get(path);
        if (set == null) {
            return new TreeSet();
        } else {
            return set;
        }
    }

    /**
     * Return the name of the key referenced by the given reference field.
     *
     * @param  path path to element with reference field
     * @param  field  the name of the reference field
     * @return  name of key referenced
     */
    public String getReferencingKeyName(String path, String field) {
        return (String) keyrefFieldToKey.get(path + "/" + field);
    }

    /**
     * Return the element path associated with the given key.
     *
     * @param key  the key name
     * @return  the associated element path
     * @see #getKeyField
     */
    public String getKeyPath(String key) {
        return (String) keyNameToPath.get(key);
    }

    /**
     * Return the field name associated with the given key.
     *
     * @param key  the key name
     * @return  the associated field name
     * @see #getKeyPath
     */
    public String getKeyField(String key) {
        return (String) keyNameToField.get(key);
    }

    /**
     * Return true if given path refers to an element which contains
     * a single reference attribute. In which case the element as a
     * whole is treated as a reference.
     *
     * @param path  path to element
     * @return  true if element is reference (single reference attribute)
     */
    public boolean isReferenceElement(String path) {
        return referenceElements.containsKey(path);
    }

    /**
     * Given a path that refers to a reference element - it has a single
     * reference attribute - return the name of that reference attribute.
     *
     * @param path  path to reference element
     * @return  name of element's single reference attribute
     */
    public String getReferenceElementField(String path) {
        return (String) referenceElements.get(path);
    }

    /**
     * Return a classname for the element ant specified by xpath.
     * Will possible have _EnclosingClass... according to element
     * nesting.
     * @param xpath the path to examine
     * @return the generated class name
     */
    public String getClsNameFromXPath(String xpath) {
        // If xpath is a reference element, then we resolve the target
        // path using the referring key and call this method again
        if (isReferenceElement(xpath)) {
            String field = getReferenceElementField(xpath);
            String key = getReferencingKeyName(xpath, field);
            String xpath2 = getKeyPath(key);
            LOG.debug("getClsNameFromXPath(\"" + xpath + "\") - is reference element, "
                      + "recursing with path \"" + xpath2 + "\"");
            return getClsNameFromXPath(xpath2);
        }
        // Try and map directly
        String name = (String) clsNameMap.get(xpath);
        if (name != null) {
            return name;
        }
        // If we don't map directly, then try matching
        // via regular expression
        String regex = (String) xpathToRegex.get(xpath);
        if (regex != null) {
            LOG.debug("getClsNameFromXPath(\"" + xpath + "\") - matching with regex \""
                      + regex + "\"");
            Pattern pattern = Pattern.compile(regex);
            Iterator iter = clsNameMap.keySet().iterator();
            while (iter.hasNext()) {
                String path = (String) iter.next();
                if (pattern.matcher(path).matches()) {
                    name = (String) clsNameMap.get(path);
                    break;
                }
            }
        }
        LOG.debug("getClsNameFromXPath(\"" + xpath + "\") - returning \"" + name + "\"");
        return name;
    }

    /**
     * For a given full element path, return the set of key field names. This method
     * finds the xpath that keys the key field map via <code>getKeyXPathMatchingPath</code>.
     *
     * @param path the full element path
     * @return set of key field names
     */
    public Set getKeyFieldsForPath(String path) {
        String xpath = getKeyXPathMatchingPath(path);
        if (!xpath.equals(path)) {
            LOG.debug("getKeyFieldsForPath() found matching xpath " + xpath
                                 + " for path " + path);
        }
        return this.getKeyFields(xpath);
    }

    /**
     * For a given path, return the xpath that keys the key field set associated with
     * this path. Use this method when you have constructed your own path.
     *
     * @param path a full path
     * @return xpath that keys key field set for element at given path
     */
    public String getKeyXPathMatchingPath(String path) {
        Iterator iter = keyFields.keySet().iterator();
        while (iter.hasNext()) {
            String xpath = (String) iter.next();
            String regex = (String) xpathToRegex.get(xpath);
            if (regex != null) {
                // compile regex and try to match path against it
                Pattern pattern = Pattern.compile(regex);
                if (pattern.matcher(path).matches()) {
                    // path matched this key - get keyfields for key
                    LOG.debug("getKeyXPathMatchingPath() found matching xpath " + xpath
                             + " for path " + path);
                    return xpath;
                }
            }
        }
        return path;
    }



    private void buildRefsMap(Schema schema) throws Exception {
        Enumeration structures = schema.getElementDecls();

        while (structures.hasMoreElements()) {
            ElementDecl e = (ElementDecl) structures.nextElement();
            processElementDecl(e, false);
        }
    }


    private void processElementDecl(ElementDecl eDecl, boolean isCollection) throws Exception {
        String path = eDecl.getName();
        if (!paths.empty()) {
            path = (String) paths.peek() + "/" + path;
        }
        if (path.length() > 1000) {
            // TODO: Infinite recursion
            return;
        }
        LOG.debug("pushing path: " + path);
        paths.push(path);

        if (eDecl.isReference()) {
            processElementDecl(eDecl.getReference(), isCollection);
        }

        String clsName = null;
        XMLType xmlType = eDecl.getType();

        // Record those elements that are actually references
        if (xmlType != null && xmlType.isComplexType()) {
            Enumeration e1 = ((ComplexType) xmlType).getAttributeDecls();
            AttributeDecl attrib = null;
            if (e1.hasMoreElements() && ((attrib = (AttributeDecl) e1.nextElement()) != null)
                && !e1.hasMoreElements()
                /*&& xmlInfo.isReferenceField(path, attrib.getName())*/) {
                // Add as candidate, these are checked at the end of the parse
                referenceElements.put(path, attrib.getName());
            }
        }

        isCollection = isCollection || (eDecl.getMaxOccurs() < 0) || (eDecl.getMaxOccurs() > 1);
        LOG.debug("Processing path: " + path + ", isCollection = " + isCollection
                + (xmlType == null ? "" : ", isComplexType = " + xmlType.isComplexType()
                    + ", isSimpleType = " + xmlType.isSimpleType() + ", xmlType.getName() = "
                    + xmlType.getName()) + ", isReference = " + eDecl.isReference());
        if (eDecl.isReference()) {
            // nothing needs to be done to name
            clsName = eDecl.getReference().getName();
        } else if ((xmlType != null) && xmlType.isSimpleType() && isCollection) {
            clsName = eDecl.getName() + "_" + ((String) clsNameMap.get(path.substring(0,
                            path.lastIndexOf('/'))));
        } else if ((xmlType != null) && (xmlType.getName() != null)) {
            // named complex type
            clsName = xmlType.getName();
        } else if (xmlType != null && xmlType.isComplexType() && (xmlType.getName() != null)) {
            LOG.debug("named complex type");
            clsName = xmlType.getName();
        } else if (xmlType != null && (xmlType.isComplexType()
                    || (xmlType.isSimpleType() && isCollection))) {
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
            processContentModelGroup(complexType, false);
            if (complexType.getBaseType() != null && complexType.getBaseType().isComplexType()) {
                LOG.debug("processContentModel(parent)");
                processContentModelGroup((ComplexType) complexType.getBaseType(), false);
            }
        }

        path = (String) paths.pop();
        LOG.debug("popped path: " + path);
    }


    private void processContentModelGroup(ContentModelGroup cmGroup, boolean isCollection)
        throws Exception {
        if (cmGroup instanceof Group) {
            if ((((Group) cmGroup).getMaxOccurs() < 0) || (((Group) cmGroup).getMaxOccurs() > 1)) {
                isCollection = true;
            }
        }
        Enumeration cmGroupEnum = cmGroup.enumerate();
        while (cmGroupEnum.hasMoreElements()) {
            Structure struc = (Structure) cmGroupEnum.nextElement();
            switch (struc.getStructureType()) {
            case Structure.ELEMENT:
                LOG.debug("process element");
                processElementDecl((ElementDecl) struc, isCollection);
                break;
            case Structure.GROUP:
                LOG.debug("process group");
                //handle nested groups
                processContentModelGroup((Group) struc, isCollection);
                break;
            case Structure.MODELGROUP:
                processContentModelGroup(((ModelGroup) struc).getReference(), isCollection);
                break;
            default:
                break;
            }
        }
    }

    private void filterReferenceElements() {
        Iterator iter = this.referenceElements.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String path = (String) entry.getKey();
            String field = (String) entry.getValue();
            if (!isReferenceField(path, field)) {
                iter.remove();
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


        Iterator iter = keys.values().iterator();
        while (iter.hasNext()) {
            Key key = (Key) iter.next();
            String selector = key.getSelector().getXPath();
            String selectors[] = StringUtils.split(selector, '|');
            for (int i = 0; i < selectors.length; i++) {
                String path = null;
                String regex = null;
                if (selectors[i].equals(".")) {
                    regex = "^" + (String) paths.peek() + "$";
                    path = (String) paths.peek();
                } else if (selectors[i].startsWith(".//")) {
                    // all descendents
                    regex = "^" + paths.peek() + "/.+/" + selectors[i].substring(3) + "$";
                    regex += "|^" + paths.peek() + "/" + selectors[i].substring(3) + "$";
                    path = paths.peek() + "/" + selectors[i];
                } else {
                    regex = "^" + paths.peek() + "/" + selectors[i] + "$";
                    path = paths.peek() + "/" + selectors[i];
                }

                Enumeration keyFieldEnum = key.getFields();
                String field = ((IdentityField) keyFieldEnum.nextElement()).getXPath();
                if (keyFieldEnum.hasMoreElements()) {
                    //throw new Exception("Unable to deal with Keys on more than one field");
                    LOG.debug("skipping key " + key.getName() + " on more than one field");
                    continue;
                }
                if (field.startsWith("@")) {
                    field = field.substring(field.indexOf('@') + 1);
                }
                Set fields = (Set) keyFields.get(path);
                if (fields == null) {
                    fields = new TreeSet();
                    keyFields.put(path, fields);
                }
                fields.add(field);
                xpathToRegex.put(path, regex);
                keyNameToPath.put(key.getName(), path);
                keyNameToField.put(key.getName(), field);
                LOG.debug("found key name:" + key.getName() + " path:" + path + " field:" + field);
            }
        }

        iter = keyrefs.values().iterator();
        while (iter.hasNext()) {
            KeyRef keyref = (KeyRef) iter.next();
            String path = null;
            String selector = keyref.getSelector().getXPath();
            if (selector.equals(".")) {
                path = (String) paths.peek();
            } else if (selector.startsWith(".//")) {
                // all descendents
                path = paths.peek() + "/" + selector.substring(3);
            } else {
                path = paths.peek() + "/" + selector;
            }

            Enumeration keyrefEnum = keyref.getFields();
            String field = ((IdentityField) keyrefEnum.nextElement()).getXPath();
            if (keyrefEnum.hasMoreElements()) {
                throw new Exception("Unable to deal with KeyRefs on more than one field");
            }
            if (field.startsWith("@")) {
                field = field.substring(field.indexOf('@') + 1);
            }
            Set fields = (Set) keyrefFields.get(path);
            if (fields == null) {
                fields = new TreeSet();
                keyrefFields.put(path, fields);
            }
            fields.add(field);
            keyrefFieldToKey.put(path + "/" + field, keyref.getRefer());
            LOG.debug("keyref path:" + path + " field:" + field + " refer:" + keyref.getRefer()
                      + " refid:" + keyNames.get(keyref.getRefer()));
        }
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        String endl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        Iterator iter = keyFields.keySet().iterator();
        sb.append("\n\n ========= keyFields ========= " + endl);
        while (iter.hasNext()) {
            Object path = iter.next();
            sb.append(path + endl);
            sb.append("\t\t" + keyFields.get(path) + endl);
        }
        iter = keyrefFields.keySet().iterator();
        sb.append("\n\n ========= keyrefFields ========= " + endl);
        while (iter.hasNext()) {
            Object path = iter.next();
            sb.append(path + endl);
            Iterator fields = getReferenceFields((String) path).iterator();
            while (fields.hasNext()) {
                String field = (String) fields.next();
                sb.append("\t\t" + field + "  ->  " + getReferencingKeyName((String) path, field)
                    + endl);
            }
        }
        iter = referenceElements.keySet().iterator();
        sb.append("\n\n ======== reference elements ======== " + endl);
        while (iter.hasNext()) {
            String path = (String) iter.next();
            String field = (String) referenceElements.get(path);
            sb.append("\t\t" + path + "  ->  " + field + endl);
        }
       // sb.append("idFields: " + idFields + endl + endl)
       //     .append("refFields: " + refFields + endl + endl)
       //     .append("refIdPaths: " + refIdPaths + endl + endl)
       //     .append("clsNameMap: " + clsNameMap + endl + endl);
        return sb.toString();
    }

}
