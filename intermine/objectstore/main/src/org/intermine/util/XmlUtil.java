package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayOutputStream;

/**
 * General-purpose methods for manipulating URIs and XML schema types
 * @author Mark Woodbridge
 */
public class XmlUtil
{
    /**
     * XSD namespace.
     */
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

    /**
     * If a given namespace uri does not end in a '#' add one, removing trailing '/' if present.
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
     * Return the namespace portion of URI string (i.e. everything up to and including a #).
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
     * Return an XML datatype given a java string describing a java type.
     * @param javaType string describing a fully qualified java type.
     * @return a string describing and XML data type
     */
    public static String javaToXmlType(String javaType) {
        if (javaType.equals("java.lang.String")) {
            return XSD_NAMESPACE + "string";
        } else if (javaType.equals("java.lang.Integer") || javaType.equals("int")) {
            return XSD_NAMESPACE + "integer";
        } else if (javaType.equals("java.lang.Short") || javaType.equals("short")) {
            return XSD_NAMESPACE + "short";
        } else if (javaType.equals("java.lang.Long") || javaType.equals("long")) {
            return XSD_NAMESPACE + "long";
        } else if (javaType.equals("java.lang.Double") || javaType.equals("double")) {
            return XSD_NAMESPACE + "double";
        } else if (javaType.equals("java.lang.Float") || javaType.equals("float")) {
            return XSD_NAMESPACE + "float";
        } else if (javaType.equals("java.lang.Boolean") || javaType.equals("boolean")) {
            return XSD_NAMESPACE + "boolean";
        } else if (javaType.equals("java.lang.Byte") || javaType.equals("byte")) {
            return XSD_NAMESPACE + "byte";
        } else if (javaType.equals("java.net.URL")) {
            return XSD_NAMESPACE + "anyURI";
        } else if (javaType.equals("java.util.Date")) {
            return (XSD_NAMESPACE + "dateTime");
        } else if (javaType.equals("java.math.BigDecimal")) {
            return (XSD_NAMESPACE + "bigDecimal");
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
            return "java.net.URL";
        } else if (xmlType.equals("date")) {
            return "java.util.Date";
        } else if (xmlType.equals("dateTime")) {
            return "java.util.Date";
        } else if (xmlType.equals("bigDecimal")) {
            return "java.math.BigDecimal";
        } else if (xmlType.equals("ID")) {
            return "java.lang.String";
        } else if (xmlType.equals("NMTOKEN")) {
            return "java.lang.String";
        } else {
            throw new IllegalArgumentException("Unrecognised XML data type: " + xmlType);
        }
    }
    
    /**
     * Apply some indentiation to some XML. This method is not very sophisticated and will
     * not cope well with anything but the simplest XML (no CDATA etc). The algorithm used does
     * not look at element names and does not actually parse the XML. It also assumes that the
     * forward slash and greater-than at the end of a self-terminating tag and not seperated by
     * ant whitespace.
     * 
     * @param xmlString input XML fragment
     * @return indented XML fragment
     */
    public static String indentXmlSimple(String xmlString) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int indent = 0;
        char bytes[] = xmlString.toCharArray();
        int i = 0;
        while (i < bytes.length) {
            if (bytes[i] == '<' && bytes[i + 1] == '/') {
                os.write('\n');
                writeIndentation(os, --indent);
            } else if (bytes[i] == '<') {
                if (i > 0) {
                    os.write('\n');
                }
                writeIndentation(os, indent++);
            } else if (bytes[i] == '/' && bytes[i + 1] == '>') {
                indent--;
            } else if (bytes[i] == '>') {
                
            }
            os.write(bytes[i++]);
        }
        return os.toString();
    }
    
    private static void writeIndentation(ByteArrayOutputStream os, int indent) {
        for (int j = 0; j < indent; j++) {
            os.write(' ');
            os.write(' ');
        }
    }
}