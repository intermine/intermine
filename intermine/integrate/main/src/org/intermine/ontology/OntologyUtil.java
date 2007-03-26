package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.MaxCardinalityRestriction;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * General purpose ontology methods.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class OntologyUtil
{
    /**
     * XSD namespace.
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

    private static final Logger LOG = Logger.getLogger(OntologyUtil.class);

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
     * Generate a name for a property in OntModel, this takes the form:
     * <namespace>#<classname>__<propertyname>.
     * @param prop property to generate name for
     * @param domain the domain of this property
     * @return the new property name
     */
    public static String generatePropertyName(OntProperty prop, Resource domain) {
        String propName = prop.getLocalName();
        if (propName.indexOf("__") > 0) {
            propName = propName.substring(propName.indexOf("__") + 2);
        }
        if (domain == null) {
            throw new NullPointerException("domain is null: prop = \"" + prop.getLocalName()
                    + "\"");
        }
        return domain.getLocalName() + "__" + propName;
    }

     /**
     * Generate a name for a property given namespace, clasname and property name strings
     * <namespace>#<classname>__<propertyname>.
     * @param ns namespace
     * @param domain the domain name of this property
     * @param prop the property name
     * @return the new property name
     */
    public static String generatePropertyName(String ns, String domain, String prop) {
        return XmlUtil.correctNamespace(ns) + domain + "__" + prop;
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
        ExtendedIterator i = tgtCls.listSubClasses();
        while (i.hasNext()) {
            OntClass subcls = (OntClass) i.next();

            ExtendedIterator j = subcls.listSuperClasses(true);
            while (j.hasNext()) {
                OntResource tmp = (OntResource) j.next();
                Restriction res = null;
                if (tmp.canAs(Restriction.class)) {
                    res = (Restriction) tmp.as(Restriction.class);
                }
                if (res != null && res.isHasValueRestriction()) {
                    ExtendedIterator propIter = srcCls.listDeclaredProperties(false);
                    while (propIter.hasNext()) {
                        OntProperty prop = (OntProperty) propIter.next();
                        if (res.onProperty(prop) && isDatatypeProperty(prop)) {
                            if (top != null && !top.isAnon()) {
                                subclasses.add(top);
                            } else if (!subcls.isAnon()) {
                                subclasses.add(subcls);
                            }
                        } else if (res.onProperty(prop) && isObjectProperty(prop)) {
                            OntResource range = prop.getRange();
                            if (range.canAs(OntClass.class)
                                && (range.getNameSpace().equals(srcCls.getNameSpace())
                                    || range.getNameSpace().equals(tgtCls.getNameSpace()))) {
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
                    propIter.close();
                }
            }
            j.close();
        }
        i.close();
        return subclasses;
    }

    /**
     * Create a map of class URI -> restricted subclass URIs
     * @param model the model to process
     * @return restricted subclass map
     */
    public static Map getRestrictedSubclassMap(OntModel model) {
        long time = System.currentTimeMillis();

        // find all hasValue restrictions in model
        Set restrictions = new HashSet();
        ExtendedIterator resIter = model.listRestrictions();
        while (resIter.hasNext()) {
            Restriction res = (Restriction) resIter.next();
            if (res.isHasValueRestriction()) {
                restrictions.add(res);
            }
        }
        resIter.close();
        LOG.info("built restrictions set in " + (System.currentTimeMillis() - time) + " ms, size = "
                 + restrictions.size());
        time = System.currentTimeMillis();

        // find all classes that subclass a hasValue restriction
        Set hasValueClasses = new HashSet();
        ExtendedIterator clsIter = model.listClasses();
        while (clsIter.hasNext()) {
            OntClass cls = (OntClass) clsIter.next();
            if (!cls.isAnon()) {
                ExtendedIterator superIter = cls.listSuperClasses(true); // direct
                boolean added = false;
                while (superIter.hasNext() && !added) {
                    if (restrictions.contains((Resource) superIter.next())) {
                        hasValueClasses.add(cls);
                        added = true;
                    }
                }
                superIter.close();
            }
        }
        clsIter.close();
        LOG.info("built hasValueClasses set in " + (System.currentTimeMillis() - time)
                 + " ms, size = " + hasValueClasses.size());
        time = System.currentTimeMillis();

        // candidates for restricted subclasses are superclasses of classes
        // that also subclass a hasValue restriction
        Set candidates = new HashSet();
        Iterator candIter = hasValueClasses.iterator();
        while (candIter.hasNext()) {
            OntClass cls = (OntClass) candIter.next();
            ExtendedIterator superIter = cls.listSuperClasses(true); // direct
            while (superIter.hasNext()) {
                Resource candidate = (Resource) superIter.next();
                if (!candidate.isAnon() && candidate.canAs(OntClass.class)) {
                    // faster - correct? && !cls.getNameSpace().equals(candidate.getNameSpace())) {
                    candidates.add((OntClass) candidate.as(OntClass.class));
                }
            }
            superIter.close();
        }
        LOG.info("built candidates set in " + (System.currentTimeMillis() - time) + " ms, size = "
                 + candidates.size());
        time = System.currentTimeMillis();

        Map classesMap = new HashMap();
        Iterator i = candidates.iterator();
        while (i.hasNext()) {
            OntClass cls = (OntClass) i.next();
            Set subs = OntologyUtil.findRestrictedSubclasses(model, cls);
            if (subs.size() > 0) {
                classesMap.put(cls, subs);
            }
        }
        LOG.info("found restricted subclasses in " + (System.currentTimeMillis() - time)
                 + " ms, size = " + classesMap.keySet().size());
        return classesMap;
    }

    /**
     * Use model to build a map from class URI to all possible SubclassRestriction templates
     * (where a template is defined as a SubclassRestriction with null values for
     * each path expression.
     * @param model the model to examine
     * @param classesMap restricted subclass map
     * @return a map of class URI to possible SubclassRestriction templates
     */
    public static Map getRestrictionSubclassTemplateMap(OntModel model, Map classesMap) {

        Map srMap = new HashMap();

        Iterator i = classesMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            OntClass cls = (OntClass) e.getKey();
            Iterator j = ((Set) e.getValue()).iterator();
            Set srs = new TreeSet(new SubclassRestrictionComparator());
            while (j.hasNext()) {
                OntClass sub = (OntClass) j.next();
                // no values in SubclassRestrictions so any that operate on same paths
                // will be .equals()
                srs.add(createSubclassRestriction(model, sub, cls.getLocalName(), null, false));
                srMap.put(cls.getURI(), srs);
            }
        }
        return srMap;
    }

    /**
     * Build a map from SubclassRetriction objects (with values filled in) to
     * URI of restricted subclass that this defines.
     * @param model the ontology model to process
     * @param classesMap restricted subclass map
     * @return map from SubclassRestriction to class URI
     */
    public static Map getRestrictionSubclassMap(OntModel model, Map classesMap) {

        // create a map of SubclassRestriction objects to restricted subclass URIs
        Map restrictionMap = new HashMap();

        Iterator i = classesMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            OntClass cls = (OntClass) e.getKey();
            Iterator j = ((Set) e.getValue()).iterator();
            while (j.hasNext()) {
                OntClass sub = (OntClass) j.next();
                SubclassRestriction sr = createSubclassRestriction(model, sub,
                                                      cls.getLocalName(), null, true);
                if (sr.hasRestrictions()) {
                    restrictionMap.put(sr, sub.getURI());
                }
            }
        }
        return restrictionMap;
    }

    /**
     * Examine ontology model to create a SubclassRestriction object describing reference/attribute
     * values that defined the given restriced subclass.  Recurses to follow nested retrictions.
     * @param model the model to process
     * @param cls a restricted subclass to create description of
     * @param path expression descriping path from superclass to current class
     * @param sr partially created SubclassRestriction (when recursing) null initially
     * @param values if true fill in values of attributes, otherwise just create template
     * @return a description of the given restricted subclass
     */
    protected static SubclassRestriction createSubclassRestriction(OntModel model, OntClass cls,
                                           String path, SubclassRestriction sr, boolean values) {
        if (sr == null) {
            sr = new SubclassRestriction();
        }
        ExtendedIterator i = model.listRestrictions();
        while (i.hasNext()) {
            Restriction res = (Restriction) i.next();
            if (res.hasSubClass(cls, true) && res.isHasValueRestriction()) {
                ExtendedIterator j = cls.listDeclaredProperties(true);
                while (j.hasNext()) {
                    OntProperty prop = (OntProperty) j.next();
                    if (res.onProperty(prop)) {
                        if (prop.getLocalName().indexOf("__") == -1) {
                            throw new IllegalArgumentException("Attribute name '"
                                                               + prop.getLocalName()
                                                               + "' is not of form "
                                                               + "'className__propertyName'");
                        }
                        String newPath = path + "." + prop.getLocalName().split("__")[1];
                        if (isDatatypeProperty(prop)) {
                            // add path/value restriction to SubclassRestriction
                            RDFNode node = ((HasValueRestriction) res.as(HasValueRestriction.class))
                                .getHasValue();
                            sr.addRestriction(newPath, values ? ((Literal) node.as(Literal.class))
                                              .getValue() : null);
                        } else if (isObjectProperty(prop)) {
                            OntResource range = prop.getRange();
                            if (range.canAs(OntClass.class)) {
                                RDFNode node = ((HasValueRestriction) res
                                                .as(HasValueRestriction.class)).getHasValue();
                                OntClass hv = (OntClass) node.as(OntClass.class);
                                createSubclassRestriction(model, hv, newPath, sr, values);
                            }
                        }
                    }
                }
                j.close();
            }
        }
        i.close();
        return sr;
    }

    /**
     * Prepare format of OWL properties for generation of a InterMine model:
     * a) change names of properties to be <domain>__<property>.
     * b) if multiple ranges that inherit from one another choose correct one
     * @param model the model to alter proerties in
     * @param ns the namespace within model that we are interested in
     * @throws Exception if property has invalid range
     */
    public static void reorganiseProperties(OntModel model, String ns) throws Exception {

        // get set of all properties, excluding those defined as owl:inverseOf
        Set props = new HashSet();
        Iterator propIter = model.listOntProperties();
        while (propIter.hasNext()) {
            OntProperty prop = (OntProperty) propIter.next();
            if (prop.getNameSpace().equals(ns) && prop.getInverseOf() == null) {
                props.add(prop);
            }
        }


        propIter = props.iterator();
        while (propIter.hasNext()) {
            OntProperty prop = (OntProperty) propIter.next();
            OntProperty newProp = null;
            String newPropName = ns + generatePropertyName(prop, prop.getDomain());
            if (newPropName.equals(prop.getURI())) {
                newProp = prop;
                prop.setRange(pickRange(prop));
            } else {
                newProp = renameProperty(prop, prop.getDomain(), model, ns);
                prop.remove();
            }
        }

//         propIter = props.iterator();
//         while (propIter.hasNext()) {
//             OntProperty prop = (OntProperty) propIter.next();
//             OntProperty newProp = renameProperty(prop, prop.getDomain(), model, ns);

//             if (!newProp.getURI().equals(prop.getURI())) {
//                 prop.remove();
//             }
//         }
    }

    /**
     * Change the name of a property to be <domainName>__<propertyName> and
     * update any references to the property (e.g. Restrictions) with respect
     * to change.  Apply property (recurse) to any direct subclasses of domain.
     * @param prop the property to change name of
     * @param domain the domain of the property
     * @param model parent OntModel
     * @param ns namespace to create property name in
     * @return the renamed property
     * @throws Exception if property has invalid range
     */
    protected static OntProperty renameProperty(OntProperty prop, OntResource domain,
                                         OntModel model, String ns) throws Exception {

        OntProperty newProp;
        if (isObjectProperty(prop)) {
            newProp = model.createObjectProperty(ns + generatePropertyName(prop, domain));
        } else {
            newProp = model.createDatatypeProperty(ns + generatePropertyName(prop, domain));
        }


        newProp.setDomain(domain);
        if (prop.getInverseOf() != null) {
            newProp.setRange(prop.getInverseOf().getDomain());
        } else {
            newProp.setRange(pickRange(prop));
        }
        transferEquivalenceStatements(prop, newProp, model);
        Iterator labelIter = prop.listLabels(null);
        while (labelIter.hasNext()) {
            newProp.addLabel(((Literal) labelIter.next()).getString(), null);
        }

        // deal with restrictions on property
        Set restrictions = new HashSet();
        Iterator r = ((OntClass) prop.getDomain().as(OntClass.class)).listSuperClasses(true);
        while (r.hasNext()) {
            OntResource res = (OntResource) r.next();
            if (res.canAs(Restriction.class)
                && ((Restriction) res.as(Restriction.class)).onProperty(prop)) {
                restrictions.add((Restriction) res.as(Restriction.class));
            }
        }
        ((ExtendedIterator) r).close();

        r = restrictions.iterator();
        while (r.hasNext()) {
            Restriction res = (Restriction) r.next();

            // if just changing name of property just set onProperty
            if (domain.equals(prop.getDomain())) {
                res.setOnProperty(newProp);
            } else if (res.isMaxCardinalityRestriction()
                       && !hasMaxCardinalityOne(model, newProp, domain)) {
                Restriction newRes = model.createMaxCardinalityRestriction(null, newProp,
                  ((MaxCardinalityRestriction) res.as(MaxCardinalityRestriction.class))
                                                                           .getMaxCardinality());
                ((OntClass) domain.as(OntClass.class)).addSuperClass(newRes);
            }
        }

        // apply property to direct subclasses
        //if (domain.canAs(OntClass.class)) {
        //    Iterator subIter = ((OntClass) domain.as(OntClass.class)).listSubClasses(true);
        //    while (subIter.hasNext()) {
        //        newProp.addSubProperty(renameProperty(newProp, (OntResource) subIter.next(),
        //                                              model, ns));
        //    }
        //}

        return newProp;
    }

    /**
     * Return true if the given OntProperty has more than one defined domain.
     * @param prop the OntProperty to examine
     * @return true if prop has more than one domain
     */
    public static boolean hasMultipleDomains(OntProperty prop) {
        Iterator i = prop.listDomain();
        i.next();
        return i.hasNext();
    }

    /**
     * Return true if the given OntProperty has more than one defined range.
     * @param prop the OntProperty to examine
     * @return true if prop has more than one range
     */
    public static boolean hasMultipleRanges(OntProperty prop) {
        Iterator i = prop.listRange();
        i.next();
        return i.hasNext();
    }

    /**
     * If property has multiple ranges that inherit from one another return the highest
     * in inheritance hierarchy.  If ranges do not all inherit from one another,
     * throw an Exception.
     * @param prop property to examine
     * @return the chosen range
     * @throws Exception if ranges are invalid
     */
    public static Resource pickRange(OntProperty prop) throws Exception {
        if (hasMultipleRanges(prop)) {
            OntClass cls = null;
            ExtendedIterator i = prop.listRange();
            while (i.hasNext()) {
                RDFNode node = (RDFNode) i.next();
                if (node instanceof Resource && ((Resource) node).canAs(OntClass.class)) {
                    OntClass current = (OntClass) node.as(OntClass.class);
                    if (cls == null) {
                        cls = current;
                    } else if (current.hasSubClass(cls, false)) {
                        cls = current;
                    } else if (!(current.equals(cls)) && !(cls.hasSubClass(current, false))) {
                        throw new Exception("Property (" + prop.getURI()
                                            + ") has ranges that are not compatible.");
                    }
                } else {
                    throw new Exception("Property (" + prop.getURI() + ") has more than one range");
                }
            }
            i.close();
            return cls;
        }
        return prop.getRange();
    }

    /**
     * Move equivalence statements from one property to another, removes statements
     * from original property.
     * @param prop proprty to transfer statements from
     * @param newProp target property to transfer statements to
     * @param model the parent OntModel
     */
    protected static void transferEquivalenceStatements(OntProperty prop, OntProperty
                                                        newProp, OntModel model) {
        List statements = new ArrayList();
        Iterator i = model.listStatements();
        while (i.hasNext()) {
            Statement stmt = (Statement) i.next();
            if (!stmt.getSubject().isAnon() && stmt.getSubject().getURI().equals(prop.getURI())
                && stmt.getPredicate().getURI().equals(OWL_NAMESPACE + "equivalentProperty")) {
                statements.add(model.createStatement(newProp, stmt.getPredicate(),
                                                     stmt.getObject()));
            }
        }
        model.add(statements);
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
                    equivMap.put(res.getURI(), stmt.getSubject().getURI());
                } else if (res.getNameSpace().equals(srcNs)) {
                    equivMap.put(res.getURI(), stmt.getSubject().getURI());
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
     * Given a fully qualified class name return the appropriate model name space.
     *
     * @param className a full qualified class name
     * @return a name space
     */
    public static String getNamespaceFromClassName(String className) {
        String nameSpace = "http://www.";

        if (className.startsWith("org")) {

            String [] packageParts = className.split("\\.");

            if (packageParts.length > 2) {

                nameSpace += packageParts[1] + "." + packageParts[0];

                for (int partsIndex = 2; partsIndex < packageParts.length - 1; partsIndex++) {
                    nameSpace += "/" + packageParts[partsIndex];
                }

                nameSpace += "#";

                nameSpace += packageParts[packageParts.length - 1];
            } else {
                throw new IllegalArgumentException("className too short: " + className);
            }

            return nameSpace;
        }

        throw new IllegalArgumentException("method only accepts org packages, not: " + className);
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

    /**
     * Generate a package qualified class name within the specified model from a space separated
     * list of namespace qualified names
     *
     * @param classNames the list of namepace qualified names
     * @param model the relevant model
     * @return the package qualified names
     */
    public static String generateClassNames(String classNames, Model model) {
        if (classNames == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator i = StringUtil.tokenize(classNames).iterator(); i.hasNext();) {
            sb.append(model.getPackageName() + "."
                      + XmlUtil.getFragmentFromURI((String) i.next()) + " ");
        }
        return sb.toString().trim();
    }
}
