package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.InterMineException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.ontology.SubclassRestriction;
import org.intermine.ontology.SubclassRestrictionComparator;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;

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
    protected Map equivMap = new HashMap();       // src URI to equivalent tgt URI
    protected Map templateMap = new HashMap();    // src class URI to SubclassRestriction templates
    protected Map restrictionMap = new HashMap(); // SubclassRestrictions to target class URI
    protected String tgtNs = null;
    protected ItemFactory itemFactory = null;
    protected Map aliases = new HashMap();        // item aliases for classnames
    protected int nextClsId = 0;

    /**
     * Empty constructor.
     */
    private DataTranslator() {
    }

    /**
     * Construct with a srcItemStore to read from an ontology model.
     * Use model to set up src/tgt maps required during translation.
     * @param srcItemReader the ItemReader from which to retrieve the source Items
     * @param mergeSpec the merge specification
     * @param srcModel the source model
     * @param tgtModel the target model
     */
    public DataTranslator(ItemReader srcItemReader, Properties mergeSpec, Model srcModel,
                          Model tgtModel) {
        //note this does not verify that:
        //  src items conform to the source model (slow? unnecessary?)
        //  tgt paths in the merge spec are valid
        //  the equivalences in the merge spec are type-safe
        //  any new tgt items conform to the tgt model
        //or handle equivalences for paths longer than:
        //  srcClass.fieldName = tgtClass.fieldName
        //which can't be expressed as srcURI === tgtURI, so this will have to be changed once we
        //want to do more that just represent the old merge_specs in the new format
        this.srcItemReader = srcItemReader;

        if (tgtModel != null) {
            tgtNs = tgtModel.getNameSpace().toString();
        }

        itemFactory = new ItemFactory(tgtModel, "-1_");

        buildTranslationMaps(buildEquivalences(mergeSpec, srcModel, tgtNs), srcModel);
    }

    /**
     * Parse the equivalences properties to Equivalence objects
     * Each line of the file should either be of the form:
     * tgtClsName = srcClsName [path=value]*, srcClsName [path=value]* ...
     * or:
     * tgtClsName.fieldName = srcClsName.fieldName, srcClsName.fieldName ...
     * @param mergeSpec the properties
     * @param srcModel the source model
     * @param tgtNs the target namespace
     * @return a set of Equivalences
     */
    private static Set buildEquivalences(Properties mergeSpec, Model srcModel, String tgtNs) {
        Set equivalences = new HashSet();
        for (Iterator i = mergeSpec.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            Equivalence equivalence = new Equivalence(tgtNs + ((String) entry.getKey())
                                                      .replaceAll("[.]", "__"));
            for (Iterator j = Arrays.asList(((String) entry.getValue())
                                            .split("\\s*,\\s*")).iterator(); j.hasNext();) {
                String s = (String) j.next();
                String path = s.split("\\[")[0].trim();
                verifyPath(path, srcModel, false);
                String srcURI = srcModel.getNameSpace() + path.replaceAll("[.]", "__");
                equivalence.getSrcURIs().put(srcURI, null);
                Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(s);
                while (matcher.find()) {
                    String[] restriction = matcher.group(1).split("\\s*=\\s*");
                    verifyPath(path + "." + restriction[0], srcModel, true);
                    Map restrictions = (Map) equivalence.getSrcURIs().get(srcURI);
                    if (restrictions == null) {
                        restrictions = new HashMap();
                        equivalence.getSrcURIs().put(srcURI, restrictions);
                    }
                    restrictions.put(restriction[0], restriction[1]);
                }
            }
            equivalences.add(equivalence);
        }
        return equivalences;
    }

    /**
     * Verify that a path of the form clsName.refName.refName.attName is valid for a given model
     * @param path the path
     * @param model the model
     * @param attribute whether the path must point at an attribute field
     */
    private static void verifyPath(String path, Model model, boolean attribute) {
        String[] parts = path.split("[.]");
        String clsName = parts[0];
        ClassDescriptor cld = model.getClassDescriptorByName(model.getPackageName() + "."
                                                             + clsName);
        if (cld == null) {
            throw new RuntimeException("Unable to resolve path '" + path + "': class '" + clsName
                                       + "' not found in model '" + model.getName() + "'");
        }
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                FieldDescriptor fld = cld.getFieldDescriptorByName(parts[i]);
                if (fld == null) {
                    throw new RuntimeException("Unable to resolve path '" + path + "': field '"
                                               + parts[i] + "' of class '" + cld.getName()
                                               + "' not found in model '" + model.getName() + "'");
                }
                if (i < parts.length - 1) {
                    if (!fld.isReference()) {
                         throw new RuntimeException("Unable to resolve path '" + path + "': field '"
                                                    + parts[i] + "' of class '"
                                                    + cld.getName()
                                                    + "' is not a reference field in model '"
                                                    + model.getName() + "'");
                    }
                    cld = ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
                } else {
                    if (attribute && !fld.isAttribute()) {
                         throw new RuntimeException("Unable to resolve path '" + path
                                                    + "': field '" + parts[i] + "' of class '"
                                                    + cld.getName()
                                                    + "' is not an attribute field in model '"
                                                    + model.getName() + "'");
                    }
                }
            }
        }
    }

    /**
     * Build the "traditional" data translator maps from our set of Equivalences
     * @param equivalences the set of Equivalences
     * @param srcModel the source model
     */
    private void buildTranslationMaps(Set equivalences, Model srcModel) {
        for (Iterator i = equivalences.iterator(); i.hasNext();) {
            Equivalence equivalence = (Equivalence) i.next();
            String tgtURI = equivalence.getTgtURI();
            for (Iterator j = equivalence.getSrcURIs().keySet().iterator(); j.hasNext();) {
                String srcURI = (String) j.next();
                Map restrictions = (Map) equivalence.getSrcURIs().get(srcURI);
                if (restrictions == null) { //property or class
                    equivMap.put(srcURI, tgtURI);
                    if (tgtURI.indexOf("__") != -1) { //property
                        String srcCls = srcURI.split("#")[1].split("__")[0];
                        String srcFld = srcURI.split("__")[1];
                        ClassDescriptor srcCld = srcModel
                            .getClassDescriptorByName(srcModel.getPackageName() + "." + srcCls);
                        if (srcModel.getAllSubs(srcCld) != null) {
                            for (Iterator k = srcModel.getAllSubs(srcCld).iterator();
                                 k.hasNext();) {
                                ClassDescriptor cld = (ClassDescriptor) k.next();
                                equivMap.put(srcURI.split("#")[0] + "#" + cld.getUnqualifiedName()
                                             + "__" + srcFld, tgtURI);
                            }
                        }
                    }
                } else { //class
                    SubclassRestriction sr1 = new SubclassRestriction();
                    SubclassRestriction sr2 = new SubclassRestriction();
                    for (Iterator k = restrictions.keySet().iterator(); k.hasNext();) {
                        String path = (String) k.next();
                        String value = (String) restrictions.get(path);
                        String srcCls = srcURI.split("#")[1];
                        sr1.addRestriction(srcCls + "." + path, null);
                        sr2.addRestriction(srcCls + "." + path, value);
                    }
                    Set templates = (Set) templateMap.get(srcURI);
                    if (templates == null) {
                        templates = new TreeSet(new SubclassRestrictionComparator());
                        templateMap.put(srcURI, templates);
                    }
                    templates.add(sr1);
                    restrictionMap.put(sr2, tgtURI);
                }
            }
        }
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
        String tgtClsName = null;

        // see if there are any SubclassRestriction template for this class
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
            if (tgtClsName == null || !XmlUtil.getNamespaceFromURI(tgtClsName).equals(tgtNs)) {
                return null;
            }
        }

        Item tgtItem = itemFactory.makeItem(srcItem.getIdentifier());
        tgtItem.setIdentifier(srcItem.getIdentifier());
        tgtItem.setClassName(tgtClsName);
        // no need to set implementations as not dynamic classes

        //attributes
        for (Iterator i = srcItem.getAttributes().iterator(); i.hasNext();) {
            Attribute att = (Attribute) i.next();
            if (!att.getName().equals("nonUniqueId")) {
                String attSrcURI = srcItem.getClassName() + "__" + att.getName();
                String attTgtURI = (String) equivMap.get(attSrcURI);
                if (attTgtURI != null && XmlUtil.getNamespaceFromURI(attTgtURI).equals(tgtNs)) {
                    tgtItem.addAttribute(new Attribute(attTgtURI.split("__")[1], att.getValue()));
                }
            }
        }

        //references
        for (Iterator i = srcItem.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            String refSrcURI = srcItem.getClassName() + "__" + ref.getName();
            String refTgtURI = (String) equivMap.get(refSrcURI);
            if (refTgtURI != null && XmlUtil.getNamespaceFromURI(refTgtURI).equals(tgtNs)) {
                tgtItem.addReference(new Reference(refTgtURI.split("__")[1], ref.getRefId()));
            }
        }

        //collections
        for (Iterator i = srcItem.getCollections().iterator(); i.hasNext();) {
            ReferenceList col = (ReferenceList) i.next();
            String colSrcURI = srcItem.getClassName() + "__" + col.getName();
            String colTgtURI = (String) equivMap.get(colSrcURI);
            if (colTgtURI != null && XmlUtil.getNamespaceFromURI(colTgtURI).equals(tgtNs)) {
                tgtItem.addCollection(new ReferenceList(colTgtURI.split("__")[1], col.getRefIds()));
            }
        }

        return Collections.singleton(tgtItem);
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
            if (tokenizer.hasMoreTokens() && item.hasReference(fieldName)) {
                return buildRestriction(tokenizer, getReference(item, fieldName));
            } else if (item.hasAttribute(fieldName)) {
                return item.getAttribute(fieldName).getValue();
            }
        }
        return null;
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
     * Uniquely alias a className
     * @param className the class name
     * @return the alias
     */
    protected String alias(String className) {
        String alias = (String) aliases.get(className);
        if (alias != null) {
            return alias;
        }
        String nextIndex = "" + (nextClsId++);
        aliases.put(className, nextIndex);
        LOG.info("Aliasing className " + className + " to index " + nextIndex);
        return nextIndex;
    }

    /**
     * Create a new item and assign it an id.
     * @param className class name of new item
     * @param implementations implementations string for new item
     * @return the new item
     */
    protected Item createItem(String className, String implementations) {
        return itemFactory.makeItem(null, className, implementations);
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
     * @throws ObjectStoreException if an object store error occurs
     */
    protected Iterator getCollection(Item item, String refListName) throws ObjectStoreException {
        ReferenceList refList = item.getCollection(refListName);
        Iterator idIter = refList.getRefIds().iterator();
        StringBuffer refIds = new StringBuffer();
        boolean needComma = false;
        while (idIter.hasNext()) {
            String identifier = (String) idIter.next();
            if (needComma) {
                refIds.append(" ");
            }
            needComma = true;
            refIds.append(identifier);
        }
        Set description = Collections.singleton(new FieldNameAndValue(
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER, refIds.toString(), true));
        List fulldataItems = srcItemReader.getItemsByDescription(description);
        List results = new ArrayList();
        Iterator itemIter = fulldataItems.iterator();
        while (itemIter.hasNext()) {
            org.intermine.model.fulldata.Item i = (org.intermine.model.fulldata.Item) itemIter
                .next();
            results.add(ItemHelper.convert(i));
        }
        return results.iterator();
        //return (refList == null ? null : new ItemIterator(refList.getRefIds()));
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
     * eg. A-[B1,B2] + B1-[C1,C2] + B2-[C3] -&gt; A'-[C1, C2, C3]
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
     * Used to fetch a single src item via a suitable item path.
     *
     * @param sourceItem Our starting point.
     * @param itemPath The path to follow to the item we want.
     * @param sourceItemReader Where we can get our src item from.
     *
     * @return an Item or a null if we can't find it.
     *
     * @throws ObjectStoreException if there is some kind of problem reading from the database.
     * */
    protected org.intermine.xml.full.Item getItemViaItemPath(
            org.intermine.xml.full.Item sourceItem, org.intermine.dataconversion.ItemPath itemPath,
            org.intermine.dataconversion.ItemReader sourceItemReader) throws ObjectStoreException {

        //Have to convert from 'org.intermine.xml.full.Item' to 'org.intermine.model.fulldata.Item'
        org.intermine.model.fulldata.Item modelItem = ItemHelper.convert(sourceItem);

        org.intermine.model.fulldata.Item targetItemToConvert =
                sourceItemReader.getItemByPath(itemPath, modelItem);

        org.intermine.xml.full.Item targetItemToReturn = null;

        if (targetItemToConvert != null) {
            targetItemToReturn = ItemHelper.convert(targetItemToConvert);
        }

        return targetItemToReturn;
    }

    /**
     * Used to fetch a list of src items via a suitable item path.
     *
     * @param sourceItem Our starting point.
     * @param itemPath The path to follow to the items we want.
     * @param sourceItemReader Where we can get our src items from.
     *
     * @return a List of Items or a null if we can't find any.
     *
     * @throws ObjectStoreException if there is some kind of problem reading from the database.
     * */
    protected java.util.List getItemsViaItemPath(
            org.intermine.xml.full.Item sourceItem, org.intermine.dataconversion.ItemPath itemPath,
            org.intermine.dataconversion.ItemReader sourceItemReader) throws ObjectStoreException {

        //Have to convert from 'org.intermine.xml.full.Item' to
        // 'org.intermine.model.fulldata.Item' and back again!!!
        org.intermine.model.fulldata.Item modelItem = ItemHelper.convert(sourceItem);

        java.util.List targetItemListToConvert
                = sourceItemReader.getItemsByPath(itemPath, modelItem);

        java.util.List itemList = new ArrayList();

        if (targetItemListToConvert != null) {

            for (Iterator itemIterator
                    = targetItemListToConvert.iterator(); itemIterator.hasNext();) {
                itemList.add(ItemHelper.convert((org.intermine.model.fulldata.Item)
                        itemIterator.next()));
            }
        }
        return itemList;
    }


    /**
     * Move a property from one item to another
     * @param fromItem an item to move property from
     * @param toItem an item to move property to
     * @param oldFieldName name of field in fromItem
     * @param newFieldName desired name of field in target item
     */
    protected static void moveField(Item fromItem, Item toItem, String oldFieldName,
                                    String newFieldName) {
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
    protected static void addReferencedItem(Item tgtItem, Item newItem, String fwdRefName,
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
        if (revRefName != null && revRefName.length() > 0) {
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
     * Add an element to a collection field of an Item
     * @param item the item
     * @param refListName the collection name
     * @param element the element
     */
    protected static void addToCollection(Item item, String refListName, Item element) {
        ReferenceList refList = item.getCollection(refListName);
        if (refList == null) {
            refList = new ReferenceList(refListName);
            item.addCollection(refList);
        }
        if (!refList.getRefIds().contains(element.getIdentifier())) {
            refList.addRefId(element.getIdentifier());
        }
    }

    /**
     * Check if a ReferenceList contains exactly one element.
     * @param col a ReferenceList
     * @return true if contains exactly one element
     */
    protected static boolean isSingleElementCollection(ReferenceList col) {
        return (col.getRefIds().size() == 1);
    }

    /**
     * Return the first identifier in a collection or null if empty.
     * @param col a ReferenceList
     * @return first identifier in collection or null if empty
     */
    protected static String getFirstId(ReferenceList col) {
        if (col.getRefIds().size() > 0) {
            return (String) col.getRefIds().get(0);
        }
        return null;
    }

    /**
     * Simple structure to represent the link between a class or property in the target model and
     * several classes or properties in the the source model. The values in the srcURIs map will
     * usually be null, unless this equivalence represents a "restricted subclass" in which case
     * the value will be map of "restrictions" ie. a map of paths to string values.
     */
    static class Equivalence
    {
        String tgtURI;
        Map srcURIs = new HashMap();

        /**
         * Constructor
         * @param tgtURI the target URI
         */
        Equivalence(String tgtURI) {
            this.tgtURI = tgtURI;
        }

        /**
         * Get the value of tgtURI
         * @return the target URI
         */
        String getTgtURI() {
            return tgtURI;
        }

        /**
         * Get the value of srcURIs
         * @return the Map of source URIs
         */
        Map getSrcURIs() {
            return srcURIs;
        }
    }
}
