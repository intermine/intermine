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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Statement;

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
     * <namespace>#<classname>_<fieldname>.
     * @param fld field to create property name for
     * @return the new property name
     */
    public static String generatePropertyName(FieldDescriptor fld) {
        ClassDescriptor cld = fld.getClassDescriptor();
        return cld.getModel().getNameSpace()
            + TypeUtil.unqualifiedName(cld.getName()) + "_" + fld.getName();
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
        if (name.indexOf("_") > 0) {
            String start = name.substring(0, name.indexOf("_"));
            if (start.equals(domain.getLocalName())) {
                return name.substring(name.indexOf("_") + 1);
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
        } else {
            throw new IllegalArgumentException("Unrecognised Java type");
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
        } else {
            throw new IllegalArgumentException("Unrecognised XML data type");
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
     * Return a set of jena rdf statement objects from model for given subject and predicate.
     * @param model the OntModel to search for statements
     * @param subject subject of the desired statements
     * @param predicate predicate of the desired statements
     * @return a set of Statement objects
     */
    public static Set getStatementsFor(OntModel model, OntResource subject, String predicate) {
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

}
