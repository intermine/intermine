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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Resource;

import org.flymine.metadata.*;

/**
 * Converts an OWL description of a business model to a Java business model
 * (described by org.flymine.metadata classes).
 *
 * @author Richard Smith
 */

public class Owl2FlyMine
{
    private String pkg;
    private String modelName;
    private Map attributes;
    private Map references;
    private Map collections;

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
        tgtNamespace = OntologyUtil.correctNamespace(tgtNamespace);
        attributes = new HashMap();
        references = new HashMap();
        collections = new HashMap();

        // TODO if we want primary keys examine inverseFunctionalProperties

        // Deal with properties, place in maps of class_name:set_of fields
        for (Iterator i = ontModel.listOntProperties(); i.hasNext(); ) {
            OntProperty prop = (OntProperty) i.next();
            if (!prop.getNameSpace().equals(tgtNamespace)) {
                 continue;
            }

            processProperty(ontModel, prop, false);
            if (prop.hasInverse()) {
                processProperty(ontModel, (OntProperty) prop.listInverse().next(), true);
            }
        }

        // Deal with  classes
        Set classes = new HashSet();
        Iterator i = ontModel.listClasses();
        while (i.hasNext()) {
            OntClass cls = (OntClass) i.next();
            if (cls.getNameSpace() != null && cls.getNameSpace().equals(tgtNamespace)) {
                String clsName = pkg + "." + cls.getLocalName();
                StringBuffer superClasses = new StringBuffer();
                for (Iterator j = cls.listSuperClasses(true); j.hasNext(); ) {
                    OntClass superCls = (OntClass) j.next();
                    if (superCls.getNameSpace() != null
                        && superCls.getNameSpace().equals(tgtNamespace)) {
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
     * Builds a FieldDescriptor for the given OntProperty and adds to the appropriate
     * field map.  isInverse should be true if this property is defined as owl:inversOf
     * to prevent isDatatypeProperty being called and attempting to examine a null domain.
     * @param ontModel the Jena ontology model
     * @param prop the OntProperty to convert to a FieldDescriptor
     * @param isInverse true if this property is an inverse
     * @throws Exception if error occurs processing property
     */
    protected void processProperty(OntModel ontModel, OntProperty prop, boolean isInverse)
        throws Exception {

        Iterator r = prop.listRange();
        Iterator d = prop.listDomain();
        String reverseRef = null;
        OntProperty invProp = null;

        Iterator inverse = prop.listInverse();
        if   (inverse.hasNext()) {
            invProp = (OntProperty) inverse.next();
            if (inverse.hasNext()) {
                throw new Exception("Property: " + prop.getURI().toString()
                                    + " has more than one inverse property.");
            }
        }
        Iterator inverseOf = prop.listInverseOf();
        if (inverseOf.hasNext()) {
            invProp = (OntProperty) inverseOf.next();
            if (inverseOf.hasNext()) {
                throw new Exception("Property: " + prop.getURI().toString()
                                    + " has more than one inverse property.");
            }
            r = invProp.listDomain();
            d = invProp.listRange();
        }

        // we want properties to have exactly one domain
        if (!d.hasNext()) {
            throw new Exception("Property: " + prop.getURI().toString()
                                + " does not have a defined domain.");
        }
        OntResource domain = (OntResource) d.next();
        if (d.hasNext()) {
            throw new Exception("Property: " + prop.getURI().toString()
                                + " has more than one defined domain.");
        }

        //if (!domain.getNameSpace().equals(tgtNamespace)) {
        //    continue;
        //}

        // we don't want heterogeneous collections/enumerations in our java
        if (!r.hasNext()) {
            throw new Exception("Property: " + prop.getURI().toString()
                                + " does not have a defined range.");
        }
        OntResource range = (OntResource) r.next();
        if (r.hasNext()) {
            throw new Exception("Property: " + prop.getURI().toString()
                                + " has more than one defined range.");
        }

        if (invProp != null) {
            reverseRef = OntologyUtil.generateFieldName(invProp, range);
        }

        if (!isInverse && isDatatypeProperty(prop)) {
            String javaType;
            if (range.getNameSpace().equals(OntologyUtil.RDFS_NAMESPACE)
                && range.getLocalName().equals("Literal")) {
                javaType = "java.lang.String";
            } else if (range.getNameSpace().equals(OntologyUtil.XSD_NAMESPACE)) {
                javaType = OntologyUtil.xmlToJavaType(range.getLocalName());
            } else {
                throw new Exception("DatatypeProperty: " + prop.getURI().toString()
                                    + " has a range that is not a datatype ("
                                    + range.getURI().toString() + ")");
            }
            AttributeDescriptor atd
                = new AttributeDescriptor(OntologyUtil.generateFieldName(prop, domain),
                                          false,   // primary key
                                          javaType);
            HashSet atds = getFieldSetForClass(attributes, domain.getLocalName());
            atds.add(atd);
        } else if (isInverse || isObjectProperty(prop)) {
            // TODO set package correctly if java type not business object in collection
            String referencedType = pkg + "." + range.getLocalName();

            if (OntologyUtil.hasMaxCardinalityOne(ontModel, prop, domain)) {
                ReferenceDescriptor rfd
                    = new ReferenceDescriptor(OntologyUtil.generateFieldName(prop, domain),
                                              false,
                                              referencedType,
                                              reverseRef);
                HashSet rfds = getFieldSetForClass(references, domain.getLocalName());
                rfds.add(rfd);
            } else {
                // TODO collection - cannot handle ordered
                CollectionDescriptor cod
                    = new CollectionDescriptor(OntologyUtil.generateFieldName(prop, domain),
                                               false,
                                               referencedType,
                                               reverseRef,
                                               false);
                HashSet cods = getFieldSetForClass(collections, domain.getLocalName());
                cods.add(cod);
            }
        } else {
            throw new Exception("Was neither datatype or object property");
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


    /**
     * Test whether a OntProperty is a datatype property - if type of property
     * is not owl:DatatypeProperty checks if object is a literal or a Resource
     * that is an xml datatype.
     * @param prop the property in question
     * @return true if is a DatatypeProperty
     */
    protected boolean isDatatypeProperty(OntProperty prop) {
        if (prop.isDatatypeProperty()) {
            return true;
        }
        Statement stmt = (Statement) OntologyUtil.getStatementsFor((OntModel) prop.getModel(), prop,
            OntologyUtil.RDFS_NAMESPACE + "range").iterator().next();
        if (stmt.getObject() instanceof Literal) {
            return true;
        } else if (stmt.getObject() instanceof Resource) {
            Resource res = (Resource) stmt.getObject();
            if (res.getNameSpace().equals(OntologyUtil.XSD_NAMESPACE)) {
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
    protected boolean isObjectProperty(OntProperty prop) {
        if (prop.isObjectProperty()) {
            return true;
        }
        return !isDatatypeProperty(prop);
    }
}
