package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

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
            newRefs.setRefIds(StringUtil.join(refs.getRefIds(), " "));
            newItem.getCollections().add(newRefs);
            newRefs.setItem(newItem);
        }

        return newItem;
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
}
