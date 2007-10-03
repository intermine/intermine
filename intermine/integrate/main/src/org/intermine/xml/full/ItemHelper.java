package org.intermine.xml.full;

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.RandomAccess;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.StringUtil;

/**
* Class providing Item utility methods
* @author Mark Woodbridge
*/
public class ItemHelper
{
    /**
     * Convert an XML item to a data model one
     * @param item the XML item
     * @return an equivalent data model item
     */
    public static org.intermine.model.fulldata.Item convert(Item item) {
        org.intermine.model.fulldata.Item newItem = new org.intermine.model.fulldata.Item();

        newItem.setIdentifier(item.getIdentifier());
        newItem.setClassName(item.getClassName());
        newItem.setImplementations(item.getImplementations());

        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            org.intermine.model.fulldata.Attribute newAttr
                = new org.intermine.model.fulldata.Attribute();
            newAttr.setName(attr.getName());
            newAttr.setValue(attr.getValue());
            newItem.getAttributes().add(newAttr);
            newAttr.setItem(newItem);
        }

        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            org.intermine.model.fulldata.Reference newRef
                = new org.intermine.model.fulldata.Reference();
            newRef.setName(ref.getName());
            newRef.setRefId(ref.getRefId());
            newItem.getReferences().add(newRef);
            newRef.setItem(newItem);
        }

        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            ReferenceList refs = (ReferenceList) i.next();
            org.intermine.model.fulldata.ReferenceList newRefs
                = new org.intermine.model.fulldata.ReferenceList();
            newRefs.setName(refs.getName());
            newRefs.setRefIds(makeFulldataRefIds(refs));
            newItem.getCollections().add(newRefs);
            newRefs.setItem(newItem);
        }

        return newItem;
    }

    /**
     * Get the item ids from a ReferenceList and make a String for the fulldata ReferenceList.
     */
    private static String makeFulldataRefIds(ReferenceList refs) {
        return StringUtil.join(refs.getRefIds(), " ");
    }

    /**
     * Convert a xml ReferenceList to a fulldata ReferenceList that can then be stored with
     * ItemWriter
     * @param refList the input ReferenceList
     * @return a fulldata object
     */
    public static org.intermine.model.fulldata.ReferenceList convert(ReferenceList refList) {
        org.intermine.model.fulldata.ReferenceList newRefList =
            new org.intermine.model.fulldata.ReferenceList();
        newRefList.setName(refList.getName());
        newRefList.setRefIds(makeFulldataRefIds(refList));
        return newRefList;
    }

    /**
     * Convert a xml Reference to a fulldata Reference that can then be stored with
     * ItemWriter
     * @param ref the input Reference
     * @return a fulldata object
     */
    public static org.intermine.model.fulldata.Reference convert(Reference ref) {
        org.intermine.model.fulldata.Reference newRef =
            new org.intermine.model.fulldata.Reference();
        newRef.setName(ref.getName());
        newRef.setRefId(ref.getRefId());
        return newRef;
    }

    /**
     * Convert a xml Attribute to a fulldata Attribute that can then be stored with
     * ItemWriter
     * @param ref the input Attribute
     * @return a fulldata Attribute
     */
    public static org.intermine.model.fulldata.Attribute convert(Attribute att) {
        org.intermine.model.fulldata.Attribute newAtt =
            new org.intermine.model.fulldata.Attribute();
        newAtt.setName(att.getName());
        newAtt.setValue(att.getValue());
        return newAtt;
    }

    /**
     * Convert a list of XML items to a list of data model items.
     * @param items of XML item
     * @return an equivalent list data model items
     */
    public static List convertToFullDataItems(List items) {
        List results;
        if (items instanceof RandomAccess) {
            results = new LinkedList();
        } else {
            results = new ArrayList();
        }
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            results.add(convert(item));
        }
        return results;
    }

    /**
     * Convert a data model item to an XML one
     * @param item the data model Item
     * @return an equivalent XML Item
     */
    public static Item convert(org.intermine.model.fulldata.Item item) {
        Item newItem = new Item();
        newItem.setIdentifier(item.getIdentifier());
        newItem.setClassName(item.getClassName());
        newItem.setImplementations(item.getImplementations());

        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            org.intermine.model.fulldata.Attribute attr =
                (org.intermine.model.fulldata.Attribute) i.next();
            Attribute newAttr = new Attribute();
            newAttr.setName(attr.getName());
            newAttr.setValue(attr.getValue());
            newItem.addAttribute(newAttr);
        }

        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            org.intermine.model.fulldata.Reference ref =
                (org.intermine.model.fulldata.Reference) i.next();
            Reference newRef = new Reference();
            newRef.setName(ref.getName());
            newRef.setRefId(ref.getRefId());
            newItem.addReference(newRef);
        }

        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            org.intermine.model.fulldata.ReferenceList refs
                = (org.intermine.model.fulldata.ReferenceList) i.next();
            ReferenceList newRefs = new ReferenceList(refs.getName(),
                                                      StringUtil.tokenize(refs.getRefIds()));
            newItem.addCollection(newRefs);
        }

        return newItem;
    }

    /**
     * Convert a list of full data items to a list of XML items.
     * @param items in data model format
     * @return an equivalent list of XML items
     */
    public static List convertFromFullDataItems(List items) {
        List results;
        if (items instanceof RandomAccess) {
            results = new LinkedList();
        } else {
            results = new ArrayList();
        }
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            org.intermine.model.fulldata.Item item
                = (org.intermine.model.fulldata.Item) iter.next();
            results.add(convert(item));
        }
        return results;
    }

    /**
     * org.intermine.model.fulldata.Item is auto-generated code and does not have a sensible
     * constructor.  Define one here.
     * @param identifier identifier of Item
     * @param className classname of Item
     * @param implementations space separated string of fully qualified class/interface names
     * @return the new Item
     */
    public static org.intermine.model.fulldata.Item createFulldataItem(String identifier,
                                                                       String className,
                                                                       String implementations) {
        org.intermine.model.fulldata.Item item = new org.intermine.model.fulldata.Item();
        item.setIdentifier(identifier);
        item.setClassName(className);
        item.setImplementations(implementations);
        return item;
    }

    /**
     * org.intermine.model.fulldata.Attribute is auto-generated code and does not have a sensible
     * constructor.  Define one here.
     * @param name field name of the attribute
     * @param value the attribute value
     * @return the new Attribute
     */
    public static org.intermine.model.fulldata.Attribute createFulldataAttribute(String name,
                                                                                 String value) {
        org.intermine.model.fulldata.Attribute att =
            new org.intermine.model.fulldata.Attribute();
        att.setName(name);
        att.setValue(value);
        return att;
    }

    /**
     * org.intermine.model.fulldata.Reference is auto-generated code and does not have a sensible
     * constructor.  Define one here.
     * @param name field name of the reference
     * @param refid identifer of referenced Item
     * @return the new Reference
     */
    public static org.intermine.model.fulldata.Reference createFulldataReference(String name,
                                                                                 String refid) {
        org.intermine.model.fulldata.Reference ref =
            new org.intermine.model.fulldata.Reference();
        ref.setName(name);
        ref.setRefId(refid);
        return ref;
    }

    /**
     * org.intermine.model.fulldata.ReferenceList is auto-generated code and does not have a
     * sensible constructor.  Define one here.
     * @param name field name of the collection
     * @param refids space separated string of identifiers in collection
     * @return the new ReferenceList
     */
    public static org.intermine.model.fulldata.ReferenceList createFulldataReferenceList(
                                                                String name, String refids) {
        org.intermine.model.fulldata.ReferenceList col =
            new org.intermine.model.fulldata.ReferenceList();
        col.setName(name);
        col.setRefIds(refids);
        return col;
    }
}
