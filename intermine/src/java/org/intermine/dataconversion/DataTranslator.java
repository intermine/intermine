package org.flymine.dataconversion;

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
import java.util.StringTokenizer;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Comparator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import org.flymine.FlyMineException;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.ontology.OntologyUtil;
import org.flymine.ontology.SubclassRestriction;
import org.flymine.util.StringUtil;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Convert data in FlyMine Full XML format conforming to a source OWL definition
 * to FlyMine Full XML conforming to FlyMine OWL definition.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class DataTranslator
{

    protected ItemStore srcItemStore;
    protected OntModel model;
    protected Map equivMap;       // lookup equivalent resources - excludes restricted subclass info
    protected Map templateMap;    // map of src class URI/SubclassRestriction templates possible
    protected Map restrictionMap; // map of SubclassRestrictions to target restricted subclass URI
    protected Map clsPropMap;     // map src -> tgt property URI for each restricted subclass URI

    /**
     * Construct with a srcItemStore to read from an ontology model.
     * Use model to set up src/tgt maps required during translation.
     * @param srcItemStore and ItemStore to run getItems() on
     * @param model the merged ontology model
     */
    public DataTranslator(ItemStore srcItemStore, OntModel model) {
        this.srcItemStore = srcItemStore;
        this.model = model;
        this.templateMap = OntologyUtil.getRestrictionSubclassTemplateMap(model);
        this.restrictionMap = OntologyUtil.getRestrictionSubclassMap(model);
        buildPropertiesMap();
        buildEquivalenceMap();  // use local version instead of OntologyUtil
    }


    /**
     * Convert all items in srcItemStore ant write resulting items to tgtItemStore.
     * Mapping between source and target models contained in ontology model.
     * @param tgtItemStore and ItemStore to write converted target items to
     * @throws ObjectStoreException if error reading/writing an item
     * @throws FlyMineException if no target class/property name can be found
     */
    public void translate(ItemStore tgtItemStore) throws ObjectStoreException, FlyMineException {
        translateItems(srcItemStore.getItems(), tgtItemStore);
    }


    private void translateItems(Iterator itemIter, ItemStore tgtItemStore)
        throws ObjectStoreException, FlyMineException {
        while (itemIter.hasNext()) {
            tgtItemStore.store(translateItem((Item) itemIter.next()));
        }
    }

    /**
     * Convert an Item in source format to an item conforming
     * to target OWL, preforms transformation by restricted subclass
     * and equivalence.
     * @param srcItem item to convert
     * @return converted item
     * @throws ObjectStoreException if error reading/writing an item
     * @throws FlyMineException if no target class/property name can be found
     */
    protected Item translateItem(Item srcItem) throws ObjectStoreException, FlyMineException {
        String ns = OntologyUtil.getNamespaceFromURI(srcItem.getClassName());
        Item tgtItem = new Item();
        tgtItem.setIdentifier(srcItem.getIdentifier());

        // see if there are any SubclassRestriction template for this class
        Set templates = new TreeSet(new SubclassRestrictionComparator());
        Set tmp = (Set) templateMap.get(srcItem.getClassName());
        if (tmp != null) {
            templates.addAll(tmp);
        }

        String tgtClsName = null;
        if (templates != null && templates.size() > 0) {
            Iterator i = templates.iterator();
            while (i.hasNext() && tgtClsName == null) {
                SubclassRestriction sr = buildSubclassRestriction(srcItem,
                                                         (SubclassRestriction) i.next());
                tgtClsName = (String) restrictionMap.get(sr);
            }
        }
        if (tgtClsName == null) {
            tgtClsName = (String) equivMap.get(srcItem.getClassName());
            if (tgtClsName == null) {
                // should perhaps log error and ignore this item?
                throw new FlyMineException("Could not find a target class name for class: "
                                           + srcItem.getClassName());
            }
        }
        tgtItem.setClassName(tgtClsName);

        // is tgt item subclass of anything else?

        //implementations
        String imps = srcItem.getImplementations();
        String newImps = "";
        if (imps != null) {
            for (Iterator i = StringUtil.tokenize(imps).iterator(); i.hasNext();) {
                newImps += (String) equivMap.get((String) i.next()) + " ";
            }
        }
        tgtItem.setImplementations(newImps.trim());

        //attributes
        for (Iterator i = srcItem.getAttributes().iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            Attribute newAttr = new Attribute();
            newAttr.setName(OntologyUtil.getFragmentFromURI(
                getTargetFieldURI(tgtClsName, ns + attr.getName())));
            newAttr.setValue(attr.getValue());
            tgtItem.addAttribute(newAttr);
        }

        //references
        for (Iterator i = srcItem.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            Reference newRef = new Reference();
            newRef.setName(OntologyUtil.getFragmentFromURI(
                getTargetFieldURI(tgtClsName, ns + ref.getName())));
            newRef.setRefId(ref.getRefId());
            tgtItem.addReference(newRef);
        }

        //collections
        for (Iterator i = srcItem.getCollections().iterator(); i.hasNext();) {
            ReferenceList col = (ReferenceList) i.next();
            ReferenceList newCol = new ReferenceList();
            newCol.setName(OntologyUtil.getFragmentFromURI(
                getTargetFieldURI(tgtClsName, ns + col.getName())));
            newCol.setRefIds(col.getRefIds());
            tgtItem.addCollection(newCol);
        }
        return tgtItem;
    }


    /**
     * Given an item in src format and a template (a list of path expressions) create
     * a SubclassRestriction object with attribute values filled in if present.
     * If any attribute has a null value or a reference is not present return null.
     * @param item an Item in source format
     * @param template a SubclassRestriction with path expressions bu null values
     * @return a SubclassRestriction with attribute values filled in or null
     * @throws ObjectStoreException if error reading an item
     */
    protected SubclassRestriction buildSubclassRestriction(Item item, SubclassRestriction template)
        throws ObjectStoreException {
        SubclassRestriction sr = new SubclassRestriction();
        Iterator i = template.getRestrictions().keySet().iterator();
        while (i.hasNext()) {
            String path = (String) i.next();
            StringTokenizer tokenizer = new StringTokenizer(path, ".");
            String clsName = tokenizer.nextToken();
            if (item.getClassName().indexOf(clsName) >= 0) {
                String value = buildRestriction(tokenizer, item);
                if (value != null) {
                    sr.addRestriction(path, value);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return sr;
    }


    /**
     * Given a StringTokenizer over a path expression recurse through references of item
     * to find described attribute value.  Return null if attribute or reference
     * not found.  Needs access to source ItemStore.
     * @param tokenizer tokenizer over a path expression excluding toplevel class name
     * @param item item to examine
     * @return the value of the attribute or null if not found
     * @throws ObjectStoreException if error reading an item
     */
    protected String buildRestriction(StringTokenizer tokenizer, Item item)
        throws ObjectStoreException {
        if (tokenizer.hasMoreTokens()) {
            String fieldName = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()  && item.hasReference(fieldName)) {
                Reference ref = item.getReference(fieldName);
                return buildRestriction(tokenizer,
                         srcItemStore.getItemByIdentifier(ref.getRefId()));
            } else if (item.hasAttribute(fieldName)) {
                return item.getAttribute(fieldName).getValue();
            }
        }
        return null;
    }

    /**
     * Get URI of a target property given parent class and source property URI.
     * First examines restricted subclasses then uses equivalence map if no value found.
     * @param clsURI URI of property domain in source model
     * @param srcPropURI URI of property in source model
     * @return corresponding URI for property in target model
     */
    protected String getTargetFieldURI(String clsURI, String srcPropURI) {
        String tgtPropURI = null;
        if (clsPropMap.containsKey(clsURI)) {
            Map propMap = (Map) clsPropMap.get(clsURI);
            tgtPropURI = (String) propMap.get(srcPropURI);
        }
        if (tgtPropURI == null) {
            // change to storing strings for performance
            tgtPropURI = (String) equivMap.get(srcPropURI);
        }
        return tgtPropURI;
    }


    /**
     * Build a map from restricted subclass URI (target namespace) to a map of src
     * property URI to tgt property URI.  Contructed using prebuilt restrictionMap
     * and equivalence statements in model.
     */
    protected void buildPropertiesMap() {
        clsPropMap = new HashMap();

        // restrictionMap.values() is set of all restricted subclasses in target namespace
        Iterator clsIter = restrictionMap.values().iterator();
        while (clsIter.hasNext()) {
            OntClass cls = model.getOntClass((String) clsIter.next());
            Iterator propIter = model.listOntProperties();
            Map propMap = new HashMap();
            while (propIter.hasNext()) {
                OntProperty prop = (OntProperty) propIter.next();
                if (prop.hasDomain(cls)) {
                    propMap.put(prop.getEquivalentProperty().getURI(), prop.getURI());
                }
            }
            if (!propMap.isEmpty()) {
                clsPropMap.put(cls.getURI(), propMap);
            }
        }
    }


    /**
     * Build a map of src/tgt resource URI.  Does not include resources
     * that are restricted subclasses or restrictions thereof.
     */
    protected void buildEquivalenceMap() {

        // build a set of all restricted subclass URIs and their properties
        Set subs = new HashSet(restrictionMap.values());
        Iterator i = clsPropMap.values().iterator();
        while (i.hasNext()) {
            subs.addAll(((Map) i.next()).values());
        }

        // build equiv map excluding restricted subclass data
        equivMap = new HashMap();

        Iterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getPredicate().getLocalName().equals("equivalentClass")
                || stmt.getPredicate().getLocalName().equals("equivalentProperty")
                || stmt.getPredicate().getLocalName().equals("sameAs")) {
                Resource res = stmt.getResource();
                String tgtURI = stmt.getSubject().getURI();
                if (!subs.contains(tgtURI)) {
                    equivMap.put(res.getURI(), stmt.getSubject().getURI());
                }
            }
        }
    }


    /**
     * Compare two SubclassRestrictions by number of restrictions, more
     * restrictions comes first.  Used to ensure most detailed restrictions
     * are examied first, others may be a subset of this.
     */
    protected class SubclassRestrictionComparator implements Comparator
    {
        /**
         * Compare two SubclassRestrictions by number of restrictions, more
         * restrictions comes first.
         * @param a an object to compare
         * @param b an object to compare
         * @return integer result of comparason
         */
        public int compare(Object a, Object b) {
            if (a instanceof SubclassRestriction && b instanceof SubclassRestriction) {
                return compare ((SubclassRestriction) a, (SubclassRestriction) b);
            } else {
                throw new IllegalArgumentException("Cannot compare: " + a.getClass().getName()
                                                   + " and " + b.getClass().getName());
            }
        }

        // reverse natural order
        private int compare(SubclassRestriction a, SubclassRestriction b) {
            return -(new Integer(a.getRestrictions().size())
                      .compareTo(new Integer(b.getRestrictions().size())));
        }
    }
}
