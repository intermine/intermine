package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;

import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.util.TypeUtil;

/**
 * General purpose ontology methods.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class OntologyUtil
{
    /**
     * the XML namespace
     */
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";
    /**
     * OWL namespace.
     */
    public static final String OWL_NAMESPACE = "http://www.w3.org/2002/07/owl#";
    /**
     * RDF namespace.
     */
    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    /**
     * RDFS namespace
     */
    public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";


    private OntologyUtil() {
    }

    /**
     * Generate a name for a property in OntModel, this takes the form:
     * <namespace>#<classname>__<fieldname>.
     * @param fld field to create property name for
     * @return the new property name
     */
    public static String generatePropertyName(FieldDescriptor fld) {
        ClassDescriptor cld = fld.getClassDescriptor();
        return cld.getModel().getNameSpace()
            + TypeUtil.unqualifiedName(cld.getName()) + "__" + fld.getName();
    }


    /**
     * Strip <classname>_ from the beginning of a property name, if not present then
     * return name as is.
     * @param prop property to generate field name for
     * @param domain the domain of this property
     * @return the new field name
     */
    public static String generateFieldName(OntProperty prop, OntResource domain) {
        String name = prop.getLocalName();
        if (name.indexOf("__") > 0) {
            String start = name.substring(0, name.indexOf("__"));
            if (start.equals(domain.getLocalName())) {
                return name.substring(name.indexOf("__") + 2);
            }
        }
        return name;
    }


    /**
     * Return an XML datatype given a java string describing a java type.
     * @param javaType string describing a fully qualified java type.
     * @return a string describing and XML data type
     */
    public static String javaToXmlType(String javaType) {
        if (javaType.equals("java.lang.String")) {
            return OntologyUtil.XSD_NAMESPACE + "string";
        } else if (javaType.equals("java.lang.Integer") || javaType.equals("int")) {
            return OntologyUtil.XSD_NAMESPACE + "integer";
        } else if (javaType.equals("java.lang.Short") || javaType.equals("short")) {
            return OntologyUtil.XSD_NAMESPACE + "short";
        } else if (javaType.equals("java.lang.Long") || javaType.equals("long")) {
            return OntologyUtil.XSD_NAMESPACE + "long";
        } else if (javaType.equals("java.lang.Double") || javaType.equals("double")) {
            return OntologyUtil.XSD_NAMESPACE + "double";
        } else if (javaType.equals("java.lang.Float") || javaType.equals("float")) {
            return OntologyUtil.XSD_NAMESPACE + "float";
        } else if (javaType.equals("java.lang.Boolean") || javaType.equals("boolean")) {
            return OntologyUtil.XSD_NAMESPACE + "boolean";
        } else if (javaType.equals("java.lang.Byte") || javaType.equals("byte")) {
            return OntologyUtil.XSD_NAMESPACE + "byte";
        } else if (javaType.equals("java.net.URI")) {
            return OntologyUtil.XSD_NAMESPACE + "anyURI";
        } else if (javaType.equals("java.util.Date")) {
            return (OntologyUtil.XSD_NAMESPACE + "dateTime");
        } else if (javaType.equals("java.math.BigDecimal")) {
            return (OntologyUtil.XSD_NAMESPACE + "bigDecimal");
        } else {
            throw new IllegalArgumentException("Unrecognised Java type: " + javaType);
        }
    }


    /**
     * Convert an XML xsd: type to a fully qualified class name of a java type.
     * @param xmlType the local name of an XML type
     * @return a string representing a java class name
     * @throws IllegalArgumentException if XML datatype unrecognised
     */
    public static String xmlToJavaType(String xmlType) throws IllegalArgumentException {
        if (xmlType.equals("string") || xmlType.equals("normalizedString")
            || xmlType.equals("language") || xmlType.equals("Name") || xmlType.equals("NCName")) {
            return "java.lang.String";
        } else if (xmlType.equals("positiveInteger") || xmlType.equals("negativeInteger")
                   || xmlType.equals("int") || xmlType.equals("nonNegativeInteger")
                   || xmlType.equals("unsignedInt") || xmlType.equals("integer")
                   || xmlType.equals("nonPositiveInteger")) {
            return "java.lang.Integer";
        } else if (xmlType.equals("short") || xmlType.equals("unsignedShort")) {
            return "java.lang.Short";
        } else if (xmlType.equals("long") || xmlType.equals("unsignedLong")) {
            return "java.lang.Long";
        } else if (xmlType.equals("byte") || xmlType.equals("unsignedByte")) {
            return "java.lang.Byte";
        } else if (xmlType.equals("float") || xmlType.equals("decimal")) {
            return "java.lang.Float";
        }  else if (xmlType.equals("double")) {
            return "java.lang.Double";
        } else if (xmlType.equals("boolean")) {
            return "java.lang.Boolean";
        } else if (xmlType.equals("anyURI")) {
            return "java.net.URI";
        } else if (xmlType.equals("dateTime")) {
            return "java.util.Date";
        } else if (xmlType.equals("bigDecimal")) {
            return "java.math.BigDecimal";
        } else {
            throw new IllegalArgumentException("Unrecognised XML data type: " + xmlType);
        }
    }

    /**
     * Return true if there is a maxCardinalityRestriction of 1 on this property for
     * given domain (note that properties can have more than one domain).
     * @param model the Ontolgoy model
     * @param prop the property to check for cardinality restriction
     * @param domain the specific domain of the restriction
     * @return true if maxCardinality restriction 1 exists
     */
    public static boolean hasMaxCardinalityOne(OntModel model, OntProperty prop,
                                               OntResource domain) {
        Iterator iter = model.listRestrictions();
        while (iter.hasNext()) {
            Restriction res = (Restriction) iter.next();
            if (res.hasSubClass(domain) && res.onProperty(prop)
                && res.isMaxCardinalityRestriction()) {
                return true;
            }
        }
        return false;
    }


    /**
     * For the given OntClass get a set of subclasses with a hasValue restriction
     * on a particular DatatypeProperty.  May have to follow ObjectProperties to
     * other classes to find the restricted DatatypeProperty.  srcCls may be from a
     * different OntModel, use this class to find properties, look in given model for
     * subclasses.  If srcCls is from given model then this is no different.
     * @param model the ontology model to search for subclasses in
     * @param srcCls the OntClass to find restricted subclasses for
     * @return a set of subclasses with a hasValue Restriction
     */
    public static Set findRestrictedSubclasses(OntModel model, OntClass srcCls) {
        return findRestrictedSubclasses(model, srcCls, null);
    }


    /**
     * For the given OntClass get a set of subclasses with a hasValue restriction
     * on a particular DatatypeProperty.  May have to follow ObjectProperties to
     * other classes to find the restricted DatatypeProperty, if so method is recursed
     * but retains original subclass in 'top' parameter.  srcCls may be from a different
     * OntModel, use this class to find properties, look in given model for subclasses.
     * If srcCls is from given model then this is no different.
     * @param model the ontology model to search for subclasses in
     * @param srcCls the OntClass to find restricted subclasses for
     * @param top if nested call made to this method, the highest level subclass found,
     *            null on first call.
     * @return a set of subclasses with a hasValue Restriction
     */
    protected static Set findRestrictedSubclasses(OntModel model, OntClass srcCls, OntClass top) {
        OntClass tgtCls = model.getOntClass(srcCls.getURI());
        Set subclasses = new HashSet();
        Iterator i = tgtCls.listSubClasses();
        while (i.hasNext()) {
            OntClass subcls = (OntClass) i.next();

            Iterator j = model.listRestrictions();
            while (j.hasNext()) {
                Restriction res = (Restriction) j.next();
                if (res.hasSubClass(subcls) && res.isHasValueRestriction()) {
                    Iterator propIter = srcCls.listDeclaredProperties(false);
                    while (propIter.hasNext()) {
                        OntProperty prop = (OntProperty) propIter.next();
                        if (res.onProperty(prop) && isDatatypeProperty(prop)) {
                            if (top != null) {
                                subclasses.add(top);
                            } else {
                                subclasses.add(subcls);
                            }
                        } else if (res.onProperty(prop) && isObjectProperty(prop)) {
                            OntResource range = prop.getRange();
                            if (range.canAs(OntClass.class)
                                && range.getNameSpace().equals(srcCls.getNameSpace())) {
                                if (top != null) {
                                    subclasses.addAll(findRestrictedSubclasses(model,
                                                                     range.asClass(), top));
                                } else {
                                    subclasses.addAll(findRestrictedSubclasses(model,
                                                                     range.asClass(), subcls));
                                }
                            }
                        }
                    }
                }
            }
        }
        return subclasses;
    }


    /**
     * Build a map of resources in source namespaces to the equivalent resources
     * in target namespace.
     * @param model an OWL model specifying mapping
     * @return mappings between source and target namespaces
     */
    public static Map buildEquivalenceMap(OntModel model) {
        return buildEquivalenceMap(model, null);
    }

    /**
     * Build a map of resource URIs in source namespaces to equivalent resources
     * in target namespace if defined in model.  Only include equivalence to srcNs
     * if parameter is not null.
     * @param model an OWL model specifying mapping
     * @param srcNs only include statements in this namespace, can be null
     * @return mappings between source and target namespaces
     */
    public static Map buildEquivalenceMap(OntModel model, String srcNs) {
        Map equivMap = new HashMap();

        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getPredicate().getLocalName().equals("equivalentClass")
                || stmt.getPredicate().getLocalName().equals("equivalentProperty")
                || stmt.getPredicate().getLocalName().equals("sameAs")) {
                Resource res = stmt.getResource();
                if (srcNs == null) {
                    equivMap.put(res.getURI(), stmt.getSubject());
                } else if (res.getNameSpace().equals(srcNs)) {
                    equivMap.put(res.getURI(), stmt.getSubject());
                }
// uncomment this code for mapping one source class to more than one target
//                if (equivMap.containsKey(res.getURI())) {
//                     Object obj = equivMap.get(res.getURI());
//                     if (!(obj instanceof HashSet)) {
//                         obj = new HashSet();
//                         ((Set) obj).add(equivMap.get(res.getURI()));
//                         equivMap.put(res.getURI(), obj);
//                     }
//                     ((Set) obj).add(stmt.getSubject().getURI());
            }
        }
        return equivMap;
    }

    /**
     * Test whether a OntProperty is a datatype property - if type of property
     * is not owl:DatatypeProperty checks if object is a literal or a Resource
     * that is an xml datatype.
     * @param prop the property in question
     * @return true if is a DatatypeProperty
     */
    public static boolean isDatatypeProperty(Property prop) {
        if (prop instanceof OntProperty && ((OntProperty) prop).isDatatypeProperty()) {
            return true;
        }
        Statement stmt = (Statement) getStatementsFor((OntModel) prop.getModel(), prop,
            RDFS_NAMESPACE + "range").iterator().next();
        if (stmt.getObject() instanceof Literal) {
            return true;
        } else if (stmt.getObject() instanceof Resource) {
            Resource res = (Resource) stmt.getObject();
            if (res.getNameSpace().equals(XSD_NAMESPACE)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Test whether a given property is an object property.  If type of property
     * is not owl:ObjectProperty establishes whether it is a DatatypeProperty.
     * @param prop the property in question
     * @return true if this is an ObejctProperty
     */
    public static boolean isObjectProperty(Property prop) {
        if (prop instanceof OntProperty && ((OntProperty) prop).isObjectProperty()) {
            return true;
        }
        return !isDatatypeProperty(prop);
    }


    /**
     * Return a set of jena rdf statement objects from model for given subject and predicate.
     * @param model the OntModel to search for statements
     * @param subject subject of the desired statements
     * @param predicate predicate of the desired statements
     * @return a set of Statement objects
     */
    public static Set getStatementsFor(OntModel model, Resource subject, String predicate) {
        Set statements = new HashSet();
        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getSubject().equals(subject)
                && stmt.getPredicate().getURI().equals(predicate)) {
                statements.add(stmt);
            }
        }
        return statements;
    }


    /**
     * Return the namespace portion of URI string (i.e. everything before a #).
     * @param uri a uri string
     * @return the namespace or original uri if no # present
     */
    public static String getNamespaceFromURI(String uri) {
        if (uri.indexOf('#') > 0) {
            return uri.substring(0, uri.indexOf('#') + 1);
        }
        return uri;
    }

    /**
     * Return the fragment portion of a URI string (i.e. everything after a #).
     * @param uri a uri string
     * @return the fragment or original uri if no # present
     */
    public static String getFragmentFromURI(String uri) {
        if (uri.indexOf('#') > 0) {
            return uri.substring(uri.indexOf('#') + 1);
        }
        return uri;
    }


    /**
     * If a given namespace uri does not end in a '#' add one, rmoving trailing '/' if present.
     * @param ns the namespace uri
     * @return the corrected namespace
     */
    public static String correctNamespace(String ns) {
        if (ns.indexOf('#') >= 0) {
            return ns.substring(0, ns.indexOf('#') + 1);
        } else if (ns.endsWith("/")) {
            return ns.substring(0, ns.length() - 1) + "#";
        } else {
            return ns + "#";
        }
    }


    /**
     * Return a valid resource given a resource name fragment, currently just replaces
     * spaces with underscores.
     * @param fragment fragment part of a resource name uri
     * @return a valid resource name fragment
     */
    public static String validResourceName(String fragment) {
        return fragment.replace(' ', '_');
    }

}
