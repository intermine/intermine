package org.intermine.dataconversion;

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
import java.io.FileReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import org.intermine.InterMineException;
import org.intermine.ontology.OntologyUtil;
import org.intermine.ontology.SubclassRestriction;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;

import org.apache.log4j.Logger;

/**
 * Convert data in InterMine Full XML format conforming to a source OWL definition
 * to InterMine Full XML conforming to InterMine OWL definition.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class DataTranslator
{
    private static final Logger LOG = Logger.getLogger(DataTranslator.class);

    protected ItemReader srcItemReader;
    protected Map equivMap;       // lookup equivalent resources - excludes restricted subclass info
    protected Map templateMap;    // map of src class URI/SubclassRestriction templates possible
    protected Map restrictionMap; // map of SubclassRestrictions to target restricted subclass URI
    protected Map clsPropMap;     // map src -> tgt property URI for each restricted subclass URI
    protected Map impMap;         // map class URI -> implementation string
    protected String tgtNs;
    protected int newItemId = 1;

    /**
     * Empty constructor.
     */
    public DataTranslator() {
    }

    /**
     * Construct with a srcItemStore to read from an ontology model.
     * Use model to set up src/tgt maps required during translation.
     * @param srcItemReader the ItemReader from which to retrieve the source Items
     * @param model the merged ontology model
     * @param tgtNs the target namespace in model
     */
    public DataTranslator(ItemReader srcItemReader, OntModel model, String tgtNs) {
        this.tgtNs = tgtNs;
        this.srcItemReader = srcItemReader;
        Map subMap = OntologyUtil.getRestrictedSubclassMap(model);
        this.templateMap = OntologyUtil.getRestrictionSubclassTemplateMap(model, subMap);
        this.restrictionMap = OntologyUtil.getRestrictionSubclassMap(model, subMap);
        buildPropertiesMap(model);
        buildEquivalenceMap(model);  // use local version instead of OntologyUtil
    }

    /**
     * Convert all items in srcItemStore ant write resulting items to tgtItemStore.
     * Mapping between source and target models contained in ontology model.
     * @param tgtItemWriter the ItemWriter used to store target items
     * @throws ObjectStoreException if error reading/writing an item
     * @throws InterMineException if no target class/property name can be found
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        long opCount = 0;
        long time = System.currentTimeMillis();
        long start = time;
        long times[] = new long[20];
        for (int i = 0; i < 20; i++) {
            times[i] = -1;
        }
        for (Iterator i = getItemIterator(); i.hasNext();) {
            Item srcItem = ItemHelper.convert((org.intermine.model.fulldata.Item) i.next());
            Collection translated = translateItem(srcItem);
            if (translated != null) {
                for (Iterator j = translated.iterator(); j.hasNext();) {
                    Object obj = j.next();
                    tgtItemWriter.store(ItemHelper.convert((Item) obj));
                    opCount++;
                    if (opCount % 1000 == 0) {
                        long now = System.currentTimeMillis();
                        if (times[(int) ((opCount / 1000) % 20)] == -1) {
                            LOG.info("Translated " + opCount + " objects - running at "
                                    + (60000000 / (now - time)) + " (avg "
                                    + ((60000L * opCount) / (now - start))
                                    + ") objects per minute -- now on "
                                    + srcItem.getClassName());
                        } else {
                            LOG.info("Translated " + opCount + " objects - running at "
                                    + (60000000 / (now - time)) + " (20000 avg "
                                    + (1200000000 / (now - times[(int) ((opCount / 1000) % 20)]))
                                    + ") (avg " + ((60000L * opCount) / (now - start))
                                    + ") objects per minute -- now on "
                                    + srcItem.getClassName());
                        }
                        time = now;
                        times[(int) ((opCount / 1000) % 20)] = now;
                    }
                }
            }
        }
    }

    /**
     * Returns the Iterator over Items that the DataTranslator will translate.
     *
     * @return an Iterator
     * @throws ObjectStoreException if something goes wrong
     */
    public Iterator getItemIterator() throws ObjectStoreException {
        return srcItemReader.itemIterator();
    }

    /**
     * Convert an Item in source format to an item conforming
     * to target OWL, performs transformation by restricted subclass
     * and equivalence.
     * @param srcItem item to convert
     * @return converted items
     * @throws ObjectStoreException if error reading/writing an item
     * @throws InterMineException if no target class/property name can be found
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        // see if there are any SubclassRestriction template for this class
        String tgtClsName = null;
        Set templates = (Set) templateMap.get(srcItem.getClassName());
        if (templates != null) {
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
                throw new InterMineException("Could not find a target class name for class: "
                                           + srcItem.getClassName());
            }
        }

        // if class is not in target namespace then don't bother translating it
        if (!XmlUtil.getNamespaceFromURI(tgtClsName).equals(tgtNs)) {
            return null;
        }

        Item tgtItem = new Item();
        tgtItem.setIdentifier(srcItem.getIdentifier());
        tgtItem.setClassName(tgtClsName);
        // no need to set implementations as not dynamic classes
        //tgtItem.setImplementations((String) impMap.get(tgtClsName));

        //attributes
        for (Iterator i = srcItem.getAttributes().iterator(); i.hasNext();) {
            Attribute att = (Attribute) i.next();
            if (!att.getName().equals("nonUniqueId")) {
                String attSrcURI = srcItem.getClassName() + "__" + att.getName();
                String attTgtURI = getTargetFieldURI(srcItem.getClassName(), att.getName());
                if (attTgtURI == null) {
                    throw new InterMineException("no target attribute found for " + attSrcURI
                                               + " in class " + tgtClsName);
                }
                if (XmlUtil.getNamespaceFromURI(attTgtURI).equals(tgtNs)) {
                    Attribute newAtt = new Attribute();
                    newAtt.setName(attTgtURI.split("__")[1]);
                    newAtt.setValue(att.getValue());
                    tgtItem.addAttribute(newAtt);
                }
            }
        }

        //references
        for (Iterator i = srcItem.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            String refSrcURI = srcItem.getClassName() + "__" + ref.getName();
            //            String refTgtURI = getTargetFieldURI(tgtClsName, refSrcURI);
            String refTgtURI = getTargetFieldURI(srcItem.getClassName(), ref.getName());
            if (refTgtURI == null) {
                throw new InterMineException("no target reference found for " + refSrcURI
                                           + " in class " + tgtClsName);
            }
            if (XmlUtil.getNamespaceFromURI(refTgtURI).equals(tgtNs)) {
                Reference newRef = new Reference();
                newRef.setName(refTgtURI.split("__")[1]);
                newRef.setRefId(ref.getRefId());
                tgtItem.addReference(newRef);
            }
        }

        //collections
        for (Iterator i = srcItem.getCollections().iterator(); i.hasNext();) {
            ReferenceList col = (ReferenceList) i.next();
            String colSrcURI = srcItem.getClassName() + "__" + col.getName();
            //String colTgtURI = getTargetFieldURI(tgtClsName, colSrcURI);
            String colTgtURI = getTargetFieldURI(srcItem.getClassName(), col.getName());
            if (colTgtURI == null) {
                throw new InterMineException("no target collection found for " + colSrcURI
                                           + " in class " + tgtClsName);
            }
            if (XmlUtil.getNamespaceFromURI(colTgtURI).equals(tgtNs)) {
                ReferenceList newCol = new ReferenceList(colTgtURI.split("__")[1], col.getRefIds());
                tgtItem.addCollection(newCol);
            }
        }
        return Collections.singleton(tgtItem);
    }

    /**
     * Set an item property to value from within a referenced item.
     * @param tgtItem a target item to set property in
     * @param srcItem a source item to get reference from
     * @param fieldName name of field to set in tgtItem
     * @param refName name of the reference in srcItem to other item
     * @param refFieldName name of field within referenced item to get value from
     * @throws ObjectStoreException if error retrieving referenced item
     */
    protected void promoteField(Item tgtItem, Item srcItem, String fieldName, String refName,
                                String refFieldName)
        throws ObjectStoreException {
        if (srcItem.getReference(refName) != null) {
            Item refItem = ItemHelper.convert(srcItemReader
                                       .getItemById(srcItem.getReference(refName).getRefId()));
            moveField(refItem, tgtItem, refFieldName, fieldName);
        }
    }

    /**
     * "Flattens" a collection by removing intermediate items
     * eg. A-[B1,B2] + B1-[C1,C2] + B2-[C3] -> A'-[C1, C2, C3]
     * @param srcItem the source item eg. A
     * @param colName the collection name eg. Bs
     * @param colFieldName the other collection name eg. Cs
     * @param tgtItem the target item eg. A'
     * @param fieldName the target collection name eg. Cs
     * @throws ObjectStoreException if error retrieving referenced item
     */
    protected void promoteCollection(Item srcItem, String colName, String colFieldName,
                                     Item tgtItem, String fieldName)
        throws ObjectStoreException {
        ReferenceList refList = new ReferenceList(fieldName);
        for (Iterator i = srcItem.getCollection(colName).getRefIds().iterator(); i.hasNext(); ) {
            Item colItem = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
            for (Iterator j = colItem.getCollection(colFieldName).getRefIds().iterator();
                 j.hasNext();) {
                refList.addRefId((String) j.next());
            }
        }
        tgtItem.addCollection(refList);
    }

    /**
     * Move a property from one item to another
     * @param fromItem an item to move property from
     * @param toItem an item to move property to
     * @param oldFieldName name of field in fromItem
     * @param newFieldName desired name of field in target item
     */
    protected void moveField(Item fromItem, Item toItem, String oldFieldName, String newFieldName) {
        if (fromItem.hasAttribute(oldFieldName)) {
            Attribute att = new Attribute();
            att.setName(newFieldName);
            att.setValue(fromItem.getAttribute(oldFieldName).getValue());
            toItem.addAttribute(att);
        } else if (fromItem.hasReference(oldFieldName)) {
            Reference ref = new Reference();
            ref.setName(newFieldName);
            ref.setRefId(fromItem.getReference(oldFieldName).getRefId());
            toItem.addReference(ref);
        } else if (fromItem.hasCollection(oldFieldName)) {
            ReferenceList col = new ReferenceList(newFieldName,
                                                  fromItem.getCollection(oldFieldName).getRefIds());
            toItem.addCollection(col);
        }
    }

    /**
     * Add a reference from tgtItem to a newItem and set reverse reference in newItem as
     * appropriate.
     * @param tgtItem item to reference newItem
     * @param newItem newly created item to be referenced from tgtItem
     * @param fwdRefName name of reference in tgtItem that points to newItem
     * @param fwdIsMany true if fwdRef is a collection
     * @param revRefName name of field in newItem that points back to tgtItem (may be null)
     * @param revIsMany true if reference from newItem to tgtItem is a collection
     */
    protected void addReferencedItem(Item tgtItem, Item newItem, String fwdRefName,
                                     boolean fwdIsMany, String revRefName, boolean revIsMany) {
        if (fwdIsMany) {
            ReferenceList col = tgtItem.getCollection(fwdRefName);
            if (col != null) {
                col.addRefId(newItem.getIdentifier());
            } else {
                col = new ReferenceList(fwdRefName,
                                        new ArrayList(Collections.singletonList(newItem
                                                                             .getIdentifier())));
                tgtItem.addCollection(col);
            }
        } else {
            Reference fwdRef = new Reference(fwdRefName, newItem.getIdentifier());
            tgtItem.addReference(fwdRef);
        }

        // if a reverse reference supplied add to newItem
        if (!revRefName.equals("")) {
            if (revIsMany) {
                ReferenceList col = newItem.getCollection(revRefName);
                if (col != null) {
                    col.addRefId(tgtItem.getIdentifier());
                } else {
                    col = new ReferenceList(revRefName,
                                     new ArrayList(Collections.singletonList(tgtItem
                                                                             .getIdentifier())));
                    newItem.addCollection(col);
                }
            } else {
                Reference revRef = new Reference(revRefName, tgtItem.getIdentifier());
                newItem.addReference(revRef);
            }
        }
    }

    /**
     * Convenience method to create an item in the target namespace
     * @param className the (un-namespaced) class name
     * @return the new item
     */
    protected Item createItem(String className) {
        return createItem(tgtNs + className, "");
    }

    /**
     * Create a new item and assign it an id.
     * @param className class name of new item
     * @param implementations implementations string for new item
     * @return the new item
     */
    protected Item createItem(String className, String implementations) {
        Item item = new Item();
        item.setIdentifier("-1_" + (newItemId++));
        item.setClassName(className);
        item.setImplementations(implementations);
        return item;
    }

    /**
     * Check if a ReferenceList contains exactly one element.
     * @param col a ReferenceList
     * @return true if contains exactly one element
     */
    protected boolean isSingleElementCollection(ReferenceList col) {
        return (col.getRefIds().size() == 1);
    }

    /**
     * Return the first identifier in a collection or null if empty.
     * @param col a ReferenceList
     * @return first identifier in collection or null if empty
     */
    protected String getFirstId(ReferenceList col) {
        if (col.getRefIds().size() > 0) {
            return (String) col.getRefIds().get(0);
        }
        return null;
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
                org.intermine.xml.full.Reference ref = item.getReference(fieldName);
                return buildRestriction(tokenizer,
                         ItemHelper.convert(srcItemReader.getItemById(ref.getRefId())));
            } else if (item.hasAttribute(fieldName)) {
                return item.getAttribute(fieldName).getValue();
            }
        }
        return null;
    }

    /**
     * Get name of a target property given parent class and source property URI.
     * First examines class/property then uses equivalence map if no value found.
     * @param srcClsURI URI of property domain in source model
     * @param srcPropName Name of property in source model
     * @return corresponding URI for property in target model
     */
    protected String getTargetFieldURI(String srcClsURI, String srcPropName) {
        String tgtPropURI = null;
        if (clsPropMap.containsKey(srcClsURI)) {
            Map propMap = (Map) clsPropMap.get(srcClsURI);
            if (srcPropName.indexOf("__") > 0) {
                srcPropName = srcPropName.split("__")[1];
            }
            tgtPropURI = (String) propMap.get(srcPropName);
        }
        if (tgtPropURI == null) {
            tgtPropURI = (String) equivMap.get(srcClsURI + "__" + srcPropName);
        }
        return tgtPropURI;
    }

    /**
     * Classes in target OWL model specifically inherit superclass properties (inherited
     * properties are subPropertyOf parent property).  Build a map of class/
     * src_property_localname/tgt_property_localname.
     * @param model the OntModel
     */
    protected void buildPropertiesMap(OntModel model) {
        clsPropMap = new HashMap();
        impMap = new HashMap();

        ExtendedIterator clsIter = model.listClasses();
        while (clsIter.hasNext()) {
            OntClass cls = (OntClass) clsIter.next();
            if (!cls.isAnon() && cls.getNameSpace().equals(tgtNs)) {
                ExtendedIterator propIter = cls.listDeclaredProperties(false);
                while (propIter.hasNext()) {
                    OntProperty prop = (OntProperty) propIter.next();
                    //if (prop.getNameSpace().equals(tgtNs)) {
                        ExtendedIterator equivIter = prop.listEquivalentProperties();
                        while (equivIter.hasNext()) {
                            OntProperty srcProp = (OntProperty) equivIter.next();
                            OntClass srcCls = getPropertyDomain(srcProp);
                            addToClsPropMap(srcCls.getURI(), srcProp.getLocalName(), prop.getURI());

                            // now apply this property equivalence to all subclasses of srcCls
                            // that exist in the src namespace
                            ExtendedIterator subIter = srcCls.listSubClasses(false); // all
                            while (subIter.hasNext()) {
                                OntClass subCls = (OntClass) subIter.next();
                                if (!subCls.isAnon()
                                    && subCls.getNameSpace().equals(srcCls.getNameSpace())) {
                                    addToClsPropMap(subCls.getURI(), srcProp.getLocalName(),
                                                    prop.getURI());
                                }
                            }
                            subIter.close();
                        }
                        equivIter.close();
                        //}
                }
                propIter.close();
            }
        }
        clsIter.close();
    }

    private OntClass getPropertyDomain(OntProperty prop) {
        OntClass domain = null;
        if (prop.getInverseOf() != null) {
            domain = (OntClass) prop.getInverseOf().getRange().as(OntClass.class);
        } else {
            domain = (OntClass) prop.getDomain().as(OntClass.class);
        }
        return domain;
    }


    /**
     * Classes in target OWL model specifically inherit superclass properties (inherited
     * properties are subPropertyOf parent property).  Build a map of class/
     * src_property_uri/tgt_property_uri.
     * @param model the OntModel
     */
    protected void buildPropertiesMapOld(OntModel model) {
        clsPropMap = new HashMap();
        impMap = new HashMap();

        ExtendedIterator clsIter = model.listClasses();
        while (clsIter.hasNext()) {
            OntClass cls = (OntClass) clsIter.next();
            if (!cls.isAnon() && cls.getNameSpace().equals(tgtNs)) {
                Set subclasses = new HashSet();

                Map propMap = new HashMap();
                ExtendedIterator propIter = cls.listDeclaredProperties(false);
                while (propIter.hasNext()) {
                    OntProperty prop = (OntProperty) propIter.next();
                    //if (prop.getNameSpace().equals(tgtNs) && prop.getSuperProperty() != null) {
                    if (prop.getNameSpace().equals(tgtNs)) {
                        ExtendedIterator equivIter = prop.listEquivalentProperties();
                        while (equivIter.hasNext()) {
                            propMap.put(((OntProperty) equivIter.next()).getURI(), prop.getURI());
                        }
                        equivIter.close();
                    }
                }
                propIter.close();


                if (!propMap.isEmpty()) {
                    clsPropMap.put(cls.getURI(), propMap);
                }

                // build implementations map
                StringBuffer imps = new StringBuffer();
                ExtendedIterator superIter = cls.listSuperClasses(true);
                while (superIter.hasNext()) {
                    OntClass sup = (OntClass) superIter.next();
                    if (!sup.isAnon() && sup.getNameSpace().equals(cls.getNameSpace())) {
                        imps.append(sup.getURI() + " ");
                    }
                }
                superIter.close();
                impMap.put(cls.getURI(), imps.toString().trim());
            }
        }
        clsIter.close();
    }


    private void addToClsPropMap(String srcClsName, String srcPropName, String tgtPropName) {
        Map propMap = (Map) clsPropMap.get(srcClsName);
        if (propMap == null) {
            propMap = new HashMap();
        }
        propMap.put(srcPropName.split("__")[1], tgtPropName);
        clsPropMap.put(srcClsName, propMap);
    }

    /**
     * Build a map of src/tgt resource URI.  Does not include resources
     * that are restricted subclasses or restrictions thereof.
     * @param model the OntModel
     */
    protected void buildEquivalenceMap(OntModel model) {
        // build a set of all restricted subclass URIs and their properties
        Set subs = new HashSet(restrictionMap.values());
        Iterator i = clsPropMap.values().iterator();
        while (i.hasNext()) {
            subs.addAll(((Map) i.next()).values());
        }

        // build equiv map excluding restricted subclass data
        equivMap = new HashMap();

        ExtendedIterator stmtIter = model.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = (Statement) stmtIter.next();
            if (stmt.getPredicate().getLocalName().equals("equivalentClass")
                || stmt.getPredicate().getLocalName().equals("equivalentProperty")
                || stmt.getPredicate().getLocalName().equals("sameAs")) {
                Resource res = stmt.getResource();
                String tgtURI = stmt.getSubject().getURI();
                // no longer cascade properties to subclasses, if a subproperty is mentioned
                // specifically in merge spec it will be in clsPropMap and so will be accessed
                // first, otherwise the superclass property will be found in equivMap
                //if (!subs.contains(tgtURI)) {
                equivMap.put(res.getURI(), stmt.getSubject().getURI());
                //}
            }
        }
        stmtIter.close();
    }

    /**
     * Retrieve a reference of an Item
     * @param item the Item
     * @param refName the name of the reference
     * @return the referenced Item
     * @throws ObjectStoreException if an error occurs
     */
    protected Item getReference(Item item, String refName) throws ObjectStoreException {
        Reference ref = item.getReference(refName);
        return (ref == null ? null : ItemHelper.convert(srcItemReader.getItemById(ref.getRefId())));
    }

    /**
     * Retrieve an Iterator over the elements of a collection field of an Item
     * @param item the Item
     * @param refListName the name of the collection
     * @return the Iterator
     * @throws ObjectStoreException if an error occurs
     */
    protected Iterator getCollection(Item item, String refListName) throws ObjectStoreException {
        ReferenceList refList = item.getCollection(refListName);
        return (refList == null ? null : new ItemIterator(refList.getRefIds()));
    }

    private class ItemIterator implements Iterator
    {
        Iterator i;
        public ItemIterator(Collection ids) {
            i = ids.iterator();
        }
        public Object next() {
            try {
                return ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
        }
        public boolean hasNext() {
            return i.hasNext();
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Add an element to a collection field of an Item
     * @param item the item
     * @param refListName the collection name
     * @param element the element
     */
    protected void addToCollection(Item item, String refListName, Item element) {
        ReferenceList refList = item.getCollection(refListName);
        if (refList == null) {
            refList = new ReferenceList("comments");
            item.addCollection(refList);
        }
        refList.addRefId(element.getIdentifier());
    }

    /**
     * Main method
     * @param args command line arguments
     * @throws Exception if something goes wrong
     */
    public static void main (String[] args) throws Exception {
        String srcOsName = args[0];
        String tgtOswName = args[1];
        String modelName = args[2];
        String format = args[3];
        String namespace = args[4];

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ItemReader sourceItemReader = new ObjectStoreItemReader(osSrc);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new ObjectStoreItemWriter(oswTgt);

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new DataTranslator(sourceItemReader, model, namespace);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
        oswTgt.close();
    }
}
