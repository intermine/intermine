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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import org.flymine.FlyMineException;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.ontology.OntologyUtil;
import org.flymine.ontology.SubclassRestriction;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.StringUtil;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.xml.full.ItemHelper;

import org.apache.log4j.Logger;

/**
 * Convert data in FlyMine Full XML format conforming to a source OWL definition
 * to FlyMine Full XML conforming to FlyMine OWL definition.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class DataTranslator
{

    protected ItemReader srcItemReader;
    protected Map equivMap;       // lookup equivalent resources - excludes restricted subclass info
    protected Map templateMap;    // map of src class URI/SubclassRestriction templates possible
    protected Map restrictionMap; // map of SubclassRestrictions to target restricted subclass URI
    protected Map clsPropMap;     // map src -> tgt property URI for each restricted subclass URI
    protected Map impMap;         // map class URI -> implementation string
    protected String tgtNs;
    protected int newItemId = -1;


    protected static final Logger LOG = Logger.getLogger(DataTranslator.class);
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
     * @throws FlyMineException if no target class/property name can be found
     */


    public void translate(ItemWriter tgtItemWriter) throws ObjectStoreException, FlyMineException {
        long count = 0;
        long start = System.currentTimeMillis();
        long time = System.currentTimeMillis();
        for (Iterator i = srcItemReader.itemIterator(); i.hasNext();) {
            Item srcItem = ItemHelper.convert((org.flymine.model.fulldata.Item) i.next());
            Collection translated = translateItem(srcItem);
            if (translated != null) {
                for (Iterator j = translated.iterator(); j.hasNext();) {
                    if (count % 1000 == 0) {
                        LOG.error("processed " + count + " objects at "
                                  + ((60000L * count) / ((System.currentTimeMillis() - start) + 1))
                                  + " per min - current: "
                                  + 60000000L / ((System.currentTimeMillis() - time) + 1)
                                  + " (" + srcItem.getClassName() + ")");
                        time = System.currentTimeMillis();
                    }
                    Object obj = j.next();
                    tgtItemWriter.store(ItemHelper.convert((Item) obj));
                    count++;
                }
            }
        }
    }

    /**
     * Convert an Item in source format to an item conforming
     * to target OWL, performs transformation by restricted subclass
     * and equivalence.
     * @param srcItem item to convert
     * @return converted items
     * @throws ObjectStoreException if error reading/writing an item
     * @throws FlyMineException if no target class/property name can be found
     */
    protected Collection translateItem(Item srcItem) throws ObjectStoreException, FlyMineException {
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
                throw new FlyMineException("Could not find a target class name for class: "
                                           + srcItem.getClassName());
            }
        }

        // if class is not in target namespace then don't bother translating it
        if (!OntologyUtil.getNamespaceFromURI(tgtClsName).equals(tgtNs)) {
            return null;
        }

        Item tgtItem = new Item();
        tgtItem.setIdentifier(srcItem.getIdentifier());
        tgtItem.setClassName(tgtClsName);
        tgtItem.setImplementations((String) impMap.get(tgtClsName));

        //attributes
        for (Iterator i = srcItem.getAttributes().iterator(); i.hasNext();) {
            Attribute att = (Attribute) i.next();
            String attSrcURI = srcItem.getClassName() + "__" + att.getName();
            String attTgtURI = getTargetFieldURI(tgtClsName, attSrcURI);
            if (attTgtURI == null) {
                throw new FlyMineException("no target attribute found for " + attSrcURI
                                           + " in class " + tgtClsName);
            }
            if (OntologyUtil.getNamespaceFromURI(attTgtURI).equals(tgtNs)) {
                Attribute newAtt = new Attribute();
                newAtt.setName(attTgtURI.split("__")[1]);
                newAtt.setValue(StringUtil.duplicateQuotes(att.getValue()));
                tgtItem.addAttribute(newAtt);
            }
        }

        //references
        for (Iterator i = srcItem.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            String refSrcURI = srcItem.getClassName() + "__" + ref.getName();
            String refTgtURI = getTargetFieldURI(tgtClsName, refSrcURI);
            if (refTgtURI == null) {
                throw new FlyMineException("no target reference found for " + refSrcURI
                                           + " in class " + tgtClsName);
            }
            if (OntologyUtil.getNamespaceFromURI(refTgtURI).equals(tgtNs)) {
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
            String colTgtURI = getTargetFieldURI(tgtClsName, colSrcURI);
            if (colTgtURI == null) {
                throw new FlyMineException("no target collection found for " + colSrcURI
                                           + " in class " + tgtClsName);
            }
            if (OntologyUtil.getNamespaceFromURI(colTgtURI).equals(tgtNs)) {
                ReferenceList newCol = new ReferenceList();
                newCol.setName(colTgtURI.split("__")[1]);
                newCol.setRefIds(col.getRefIds());
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
            ReferenceList col = new ReferenceList();
            col.setName(newFieldName);
            col.setRefIds(fromItem.getCollection(oldFieldName).getRefIds());
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
                                        Collections.singletonList(newItem.getIdentifier()));
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
                                            Collections.singletonList(tgtItem.getIdentifier()));
                    newItem.addCollection(col);
                }
            } else {
                Reference revRef = new Reference(revRefName, tgtItem.getIdentifier());
                newItem.addReference(revRef);
            }
        }
    }


    /**
     * Create a new item and assign it an id.
     * @param className class name of new item
     * @param implementations implementations string for new item
     * @return the new item
     */
    protected Item createItem(String className, String implementations) {
        Item item = new Item();
        item.setIdentifier("" + (newItemId--));
        item.setClassName(className);
        item.setImplementations(implementations);
        return item;
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
                org.flymine.xml.full.Reference ref = item.getReference(fieldName);
                return buildRestriction(tokenizer,
                         ItemHelper.convert(srcItemReader.getItemById(ref.getRefId())));
            } else if (item.hasAttribute(fieldName)) {
                return item.getAttribute(fieldName).getValue();
            }
        }
        return null;
    }

    /**
     * Get URI of a target property given parent class and source property URI.
     * First examines class/property then uses equivalence map if no value found.
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
            tgtPropURI = (String) equivMap.get(srcPropURI);
        }
        return tgtPropURI;
    }


    /**
     * Classes in target OWL model specifically inherit superclass properties (inherited
     * properties are subPropertyOf parent property).  Build a map of class/
     * src_property_uri/tgt_property_uri.
     * @param model the OntModel
     */
    protected void buildPropertiesMap(OntModel model) {
        clsPropMap = new HashMap();
        impMap = new HashMap();

        ExtendedIterator clsIter = model.listClasses();
        while (clsIter.hasNext()) {
            OntClass cls = (OntClass) clsIter.next();
            if (!cls.isAnon() && cls.getNameSpace().equals(tgtNs)) {
                Map propMap = new HashMap();
                ExtendedIterator propIter = cls.listDeclaredProperties(false);
                while (propIter.hasNext()) {
                    OntProperty prop = (OntProperty) propIter.next();
                    if (prop.getNameSpace().equals(tgtNs) && prop.getSuperProperty() != null) {
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
                String imps = "";
                ExtendedIterator superIter = cls.listSuperClasses(false);
                while (superIter.hasNext()) {
                    OntClass sup = (OntClass) superIter.next();
                    if (!sup.isAnon() && sup.getNameSpace().equals(cls.getNameSpace())) {
                        imps += sup.getURI() + " ";
                    }
                }
                superIter.close();
                impMap.put(cls.getURI(), imps.trim());
            }
        }
        clsIter.close();
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
                if (!subs.contains(tgtURI)) {
                    equivMap.put(res.getURI(), stmt.getSubject().getURI());
                }
            }
        }
        stmtIter.close();
    }

    private void buildImplementationsMap(OntModel model) {
        impMap = new HashMap();
        ExtendedIterator clsIter = model.listClasses();
        while (clsIter.hasNext()) {
            String imps = "";
            OntClass cls = (OntClass) clsIter.next();
            ExtendedIterator superIter = cls.listSuperClasses(false);
            while (superIter.hasNext()) {
                OntClass sup = (OntClass) superIter.next();
                if (!sup.isAnon() && sup.getNameSpace().equals(cls.getNameSpace())) {
                    imps += sup.getURI() + " ";
                }
            }
            superIter.close();
            impMap.put(cls.getURI(), imps.trim());
        }
        clsIter.close();
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
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new BufferedItemWriter(new ObjectStoreItemWriter(oswTgt));

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new DataTranslator(srcItemReader, model, namespace);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }
}
