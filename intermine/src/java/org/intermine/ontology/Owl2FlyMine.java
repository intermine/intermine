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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;

import org.flymine.metadata.*;

/**
 * Converts an OWL description of a business model to a Java business model
 * (described by org.flymine.metadata classes).
 *
 * @author Richard Smith
 */

public class Owl2FlyMine
{
    /**
     * the XML namespace
     */
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

    private String pkg;
    private String modelName;

    protected Map clds = new HashMap(); // class name -> cld


    /**
     * Construct with a model name and the name of the package within model.
     * @param modelName name of model to be created
     * @param pkg name of package within model
     */
    public Owl2FlyMine(String modelName, String pkg) {
        this.modelName = modelName;
        this.pkg = pkg;
    }


    // INITIAL VERSION NOT DESIGNED TO COPE WITH INDIVIDUALS

    /**
     * Generate a FlyMine metadata model from an OWL ontology.  Can handle attributes,
     * references and collections but not: reverseRef, primary keys (easy to implement)
     * or ordered collections (how to represent in OWL?).  Also will not do anyhting
     * about individuals in OWL.
     * @param ontModel jena java representation of OWL ontology
     * @param tgtNamespace the namespace merged ontology model
     * @return a FlyMine metadata model
     * @throws Exception if anything goes wrong
     */
    public Model process(OntModel ontModel, String tgtNamespace) throws Exception {
        Map attributes = new HashMap();
        Map references = new HashMap();
        Map collections = new HashMap();

        // TODO if we want primary keys examine functionalProperties

        // Deal with properties, place in maps of class_name:set_of fields
        for (Iterator i = ontModel.listOntProperties(); i.hasNext(); ) {
            OntProperty prop = (OntProperty) i.next();
            if (!prop.getNameSpace().equals(tgtNamespace)
                || prop.getDomain() == null) {
                continue;
            }
            Iterator r = prop.listRange();
            if (!r.hasNext()) {
                throw new Exception("Property: " + prop.getURI().toString()
                                    + " does not have a defined range.");
            }
            OntResource range = (OntResource) r.next();
            // we don't want heterogeneous collections/enumerations in our java
            if (r.hasNext()) {
                throw new Exception("Property: " + prop.getURI().toString()
                                    + " has more than one defined range.");
            }
            if (prop.isDatatypeProperty()) {
                // TODO method to read xml datatype and give back appropriate java type
                String javaType;
                if (range.getNameSpace().equals(MergeOwl.RDFS_NAMESPACE)
                    && range.getLocalName().equals("Literal")) {
                    javaType = "java.lang.String";
                } else if (range.getNameSpace().equals(XSD_NAMESPACE)) {
                    javaType = xmlToJavaType(range.getLocalName());
                } else {
                    throw new Exception("DatatypeProperty: " + prop.getURI().toString()
                                        + " has a range that is not an datataye ("
                                        + range.getURI().toString() + ")");
                }
                AttributeDescriptor atd = new AttributeDescriptor(prop.getLocalName(),
                                                                  false,   // primary key
                                                                  javaType);
                for (Iterator j = prop.listDomain(); j.hasNext(); ) {
                    OntResource domain = (OntResource) j.next();
                    if (domain.getNameSpace().equals(tgtNamespace)) {
                        HashSet atds = getFieldSetForClass(attributes, domain.getLocalName());
                        atds.add(atd);
                    }
                }
            } else {  // some refs/cols may be ObjectProperties but the may not -> look at subject
                // TODO set package correctly if java type not business object in collection
                String referencedType = pkg + "." + range.getLocalName();
                for (Iterator j = prop.listDomain(); j.hasNext(); ) {

                    OntResource domain = (OntResource) j.next();
                    if (domain.getNameSpace().equals(tgtNamespace)) {
                        if (hasMaxCardinalityOne(ontModel, prop, domain)) {
                            // reference - cannot handle reverse references yet - how?
                            ReferenceDescriptor rfd = new ReferenceDescriptor(prop.getLocalName(),
                                                                              false,
                                                                              referencedType,
                                                                              null);
                            HashSet rfds = getFieldSetForClass(references, domain.getLocalName());
                            rfds.add(rfd);
                        } else {
                            // collection - cannot handle reverese references or ordered
                            CollectionDescriptor cod = new CollectionDescriptor(prop.getLocalName(),
                                                                                false,
                                                                                referencedType,
                                                                                null,
                                                                                false);

                            HashSet cods = getFieldSetForClass(collections, domain.getLocalName());
                            cods.add(cod);
                        }
                    }
                }
            }
        }

        // Deal with  classes
        Set classes = new HashSet();
        for (Iterator i = ontModel.listClasses(); i.hasNext(); ) {
            OntClass cls = (OntClass) i.next();
            if (cls.getNameSpace().equals(tgtNamespace)) {
                String clsName = pkg + "." + cls.getLocalName();
                StringBuffer superClasses = new StringBuffer();
                for (Iterator j = cls.listSuperClasses(true); j.hasNext(); ) {
                    OntClass superCls = (OntClass) j.next();
                    if (superCls.getNameSpace().equals(tgtNamespace)) {
                        superClasses.append(pkg + "." + superCls.getLocalName());
                        if (j.hasNext()) {
                            superClasses.append(" ");
                        }
                    }
                }
                String superClassList = null;
                if (superClasses.length() != 0) {
                    superClassList = superClasses.toString();
                }
                classes.add(new ClassDescriptor(clsName, superClassList, true,
                                                getFieldSetForClass(attributes, cls.getLocalName()),
                                                getFieldSetForClass(references, cls.getLocalName()),
                                                getFieldSetForClass(collections,
                                                                    cls.getLocalName())));
            }
        }
        return new Model(modelName, tgtNamespace, classes);
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


    /**
     * Return true if there is a maxCardinalityRestriction of 1 on this property for
     * given domain (note that properties can have more than one domain).
     * @param model the Ontolgoy model
     * @param prop the property to check for cardinality restriction
     * @param domain the specific domain of the restriction
     * @return true if maxCardinality restriction 1 exists
     */
    protected boolean hasMaxCardinalityOne(OntModel model, OntProperty prop, OntResource domain) {
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
     * Convert an XML xsd: type to a fully qualified class name of a java type.
     * @param xmlType the local name of an XML type
     * @return a string representing a java class name
     * @throws Exception if XML datatype unrecognised
     */
    protected String xmlToJavaType(String xmlType) throws Exception {
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
        } else {
            throw new Exception("Unrecognised XML data type");
        }


    }

}
