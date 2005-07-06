package org.intermine.ontology;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.metadata.*;
import org.intermine.util.XmlUtil;

/**
 * Converts an OWL description of a business model to a Java business model
 * (described by org.intermine.metadata classes).
 *
 * @author Richard Smith
 */

public class Owl2InterMine
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
    public Owl2InterMine(String modelName, String pkg) {
        this.modelName = modelName;
        this.pkg = pkg;
    }


    /**
     * Generate a InterMine metadata model from an OWL ontology.  Can handle attributes,
     * references and collections, reverse references.  Will not handle
     * ordered collections (how to represent in OWL?).  Also will not do anything
     * about individuals in OWL.
     * @param ontModel jena java representation of OWL ontology
     * @param tgtNs the namespace merged ontology model
     * @return a InterMine metadata model
     * @throws Exception if anything goes wrong
     */
    public Model process(OntModel ontModel, String tgtNs) throws Exception {
        tgtNs = XmlUtil.correctNamespace(tgtNs);
        attributes = new HashMap();
        references = new HashMap();
        collections = new HashMap();

        // Deal with properties, place in maps of class_name/set_of fields
        for (Iterator i = ontModel.listOntProperties(); i.hasNext(); ) {
            OntProperty prop = (OntProperty) i.next();
            if (!prop.getNameSpace().equals(tgtNs)) {
                continue;
            }

            processProperty(ontModel, prop, tgtNs, false);
            if (prop.hasInverse()) {
                processProperty(ontModel, (OntProperty) prop.listInverse().next(),
                                tgtNs, true);
            }
        }

        // Deal with  classes
        Set classes = new HashSet();
        Iterator i = ontModel.listClasses();
        while (i.hasNext()) {
            OntClass cls = (OntClass) i.next();
            if (cls.getNameSpace() != null && cls.getNameSpace().equals(tgtNs)) {
                String clsName = pkg + "." + cls.getLocalName();
                StringBuffer superClasses = new StringBuffer();
                for (Iterator j = cls.listSuperClasses(true); j.hasNext(); ) {
                    OntClass superCls = (OntClass) j.next();
                    if (superCls.getNameSpace() != null
                        && superCls.getNameSpace().equals(tgtNs)) {
                        superClasses.append(pkg + "." + superCls.getLocalName());
                        if (j.hasNext()) {
                            superClasses.append(" ");
                        }
                    }
                }
                String superClassList = null;
                if (superClasses.length() != 0) {
                    superClassList = superClasses.toString().trim();
                }
                classes.add(new ClassDescriptor(clsName, superClassList, true,
                                                getFieldSetForClass(attributes, cls.getLocalName()),
                                                getFieldSetForClass(references, cls.getLocalName()),
                                                getFieldSetForClass(collections,
                                                                    cls.getLocalName())));
            }
        }
        return new Model(modelName, tgtNs, classes);
    }



    /**
     * Builds a FieldDescriptor for the given OntProperty and adds to the appropriate
     * field map.  isInverse should be true if this property is defined as owl:inversOf
     * to prevent isDatatypeProperty being called and attempting to examine a null domain.
     * @param ontModel the Jena ontology model
     * @param prop the OntProperty to convert to a FieldDescriptor
     * @param tgtNs namespace within model that we are interested in
     * @param isInverse true if this property is an inverse
     * @throws Exception if error occurs processing property
     */
    protected void processProperty(OntModel ontModel, OntProperty prop, String tgtNs,
                                   boolean isInverse)
        throws Exception {

        if (!prop.getNameSpace().equals(tgtNs)) {
            return;
        }
        Iterator r = prop.listRange();
        Iterator d = prop.listDomain();
        String reverseRef = null;
        OntProperty invProp = null;

        Iterator inverse = prop.listInverse();
        if (inverse.hasNext()) {
            // only set invProp if inverse is in target namespace
            OntProperty tmpProp = (OntProperty) inverse.next();
            if (tmpProp.getNameSpace().equals(tgtNs)) {
                invProp = tmpProp;
                while (inverse.hasNext()) {
                    OntProperty nextInvProp = (OntProperty) inverse.next();
                    if (nextInvProp.getNameSpace().equals(tgtNs)) {
                        throw new Exception("Property: " + prop.getURI().toString()
                                            + " has more than one inverse property.");
                    }
                }
            }
        }
        Iterator inverseOf = prop.listInverseOf();
        if (inverseOf.hasNext()) {
            // find the top level property that is in the target namespace
            invProp = ultimateSuperProperty((OntProperty) inverseOf.next(), tgtNs);

            // check that all inverse properties are in in the same inheritance path
            while (inverseOf.hasNext()) {
                if (!invProp.equals(ultimateSuperProperty((OntProperty) inverseOf.next(), tgtNs))) {
                    throw new Exception("Property: " + prop.getURI().toString()
                                        + " has more than one inverse property.");
                }
            }

            r = invProp.listDomain();
            d = invProp.listRange();
        }

        // we want properties to have exactly one domain in the target namespace
        OntResource domain = null;
        while (d.hasNext()) {
            OntResource or = (OntResource) d.next();
            if (or.getNameSpace() != null && or.getNameSpace().equals(tgtNs)) {
                if (domain == null) {
                    domain = or;
                } else {
                    throw new Exception("Property: " + prop.getURI().toString()
                                        + " has more than one defined domain in tgtNs.");
                }
            }
        }
        if (domain == null) {
            // property does not have a defined domain in the target namespace
            return;
        }

        // if this is an inherited duplicate property ignore it, in InterMine model property
        // will get inherited ... Jena makes everything subPropertyOf itself -> look through
        // all super properties
        Iterator s = prop.listSuperProperties();
        while (s.hasNext()) {
            OntProperty superProp = (OntProperty) s.next();
            if (superProp != null && !superProp.getURI().equals(prop.getURI())
                && domain.canAs(OntClass.class)) {
                OntClass superDomain = null;
                if (superProp.getInverseOf() != null) {
                    superDomain = (OntClass) superProp.getInverseOf().getRange().as(OntClass.class);
                } else {
                    superDomain = (OntClass) superProp.getDomain().as(OntClass.class);
                }
                if (((OntClass) domain.as(OntClass.class)).hasSuperClass(superDomain, false)) {
                    return;
                }
            }
        }

        // we don't want heterogeneous collections/enumerations in our java so check
        // there is only one valid range
        OntResource range = null;
        while (r.hasNext()) {
            OntResource or = (OntResource) r.next();
            if (or.getNameSpace() != null && (or.getNameSpace().equals(tgtNs)
                                  || or.getNameSpace().equals(XmlUtil.XSD_NAMESPACE)
                                  || or.getURI().equals(OntologyUtil.RDFS_NAMESPACE + "Literal"))) {
                if (range == null) {
                    range = or;
                } else {
                    throw new Exception("Property: " + prop.getURI().toString()
                                        + " has more than one defined range.");
                }
            }
        }
        if (range == null) {
            // ignore properties that don't have a range in a valid namespace
            return;
        }

        if (invProp != null) {
            reverseRef = OntologyUtil.generateFieldName(invProp, range);
        }

        if (!isInverse && (invProp == null) && OntologyUtil.isDatatypeProperty((Property) prop)) {
            String javaType;
            if (range.getURI().equals(OntologyUtil.RDFS_NAMESPACE + "Literal")) {
                javaType = "java.lang.String";
            } else if (range.getNameSpace().equals(XmlUtil.XSD_NAMESPACE)) {
                javaType = XmlUtil.xmlToJavaType(range.getLocalName());
            } else {
                throw new Exception("DatatypeProperty: " + prop.getURI().toString()
                                    + " has a range that is not a datatype ("
                                    + range.getURI().toString() + ")");
            }
            AttributeDescriptor atd
                = new AttributeDescriptor(OntologyUtil.generateFieldName(prop, domain),
                        javaType);
            HashSet atds = getFieldSetForClass(attributes, domain.getLocalName());
            atds.add(atd);
        } else if (isInverse || (invProp != null)
                   || OntologyUtil.isObjectProperty((Property) prop)) {
            // TODO set package correctly if java type not business object in collection
            String referencedType = pkg + "." + range.getLocalName();

            if (OntologyUtil.hasMaxCardinalityOne(ontModel, prop, domain)) {
                ReferenceDescriptor rfd
                    = new ReferenceDescriptor(OntologyUtil.generateFieldName(prop, domain),
                            referencedType, reverseRef);
                HashSet rfds = getFieldSetForClass(references, domain.getLocalName());
                rfds.add(rfd);
            } else {
                // TODO collection - cannot handle unordered
                CollectionDescriptor cod
                    = new CollectionDescriptor(OntologyUtil.generateFieldName(prop, domain),
                            referencedType, reverseRef);
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
     * Find top level of property inheritance hierarchy.
     * @param prop the property to find to level
     * @param tgtNs target namespace
     * @return top level of property inheritance
     */
    protected OntProperty ultimateSuperProperty(OntProperty prop, String tgtNs) {
        Set superProps = new HashSet();
        Iterator i = prop.listSuperProperties();
        while (i.hasNext()) {
            OntProperty sup = (OntProperty) i.next();
            if (!sup.equals(prop) && sup.getNameSpace().equals(tgtNs)) {
                superProps.add(sup);
            }
        }

        i = superProps.iterator();
        while (i.hasNext()) {
            return ultimateSuperProperty((OntProperty) i.next(), tgtNs);
        }
        return prop;
    }

    /**
     * Main method to convert OWL to InterMine model XML.
     * @param args srcFilename, RDF format, tgtFilename, modelname, package, tgtNs
     * @throws Exception if anything goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException("Usage: InterMine2Owl source_owl format"
                                               + " target_xml model_name package namespace");
        }

        String srcFilename = args[0];
        String format = args[1];
        String tgtFilename = args[2];
        String tgtNs = args[5];

        Owl2InterMine o2i = new Owl2InterMine(args[3], args[4]);
        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(srcFilename)), null, format);
        Model tgt = o2i.process(model, tgtNs);
        FileWriter writer = new FileWriter(new File(tgtFilename));
        writer.write(tgt.toString());
        writer.close();
    }
}
