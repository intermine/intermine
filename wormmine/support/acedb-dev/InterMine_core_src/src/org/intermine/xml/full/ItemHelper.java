package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.util.StringUtil;
import org.intermine.util.XmlUtil;

/**
* Class providing Item utility methods
* @author Mark Woodbridge
* @author Richard Smith
*/
public final class ItemHelper
{
    private ItemHelper() {
    }

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

        for (Attribute attr : item.getAttributes()) {
            org.intermine.model.fulldata.Attribute newAttr = convert(attr);
            newItem.getAttributes().add(newAttr);
            newAttr.setItem(newItem);
        }

        for (Reference ref : item.getReferences()) {
            org.intermine.model.fulldata.Reference newRef = convert(ref);
            newItem.getReferences().add(newRef);
            newRef.setItem(newItem);
        }

        for (ReferenceList refs : item.getCollections()) {
            org.intermine.model.fulldata.ReferenceList newRefs = convert(refs);
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
     * @param att the input Attribute
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
     * Convert a data model item to an XML one
     * @param item the data model Item
     * @return an equivalent XML Item
     */
    public static Item convert(org.intermine.model.fulldata.Item item) {
        Item newItem = new Item(item.getIdentifier(), item.getClassName(),
                item.getImplementations());

        for (org.intermine.model.fulldata.Attribute attr : item.getAttributes()) {
            newItem.setAttribute(attr.getName(), attr.getValue());
        }

        for (org.intermine.model.fulldata.Reference ref : item.getReferences()) {
            newItem.setReference(ref.getName(), ref.getRefId());
        }

        for (org.intermine.model.fulldata.ReferenceList refs : item.getCollections()) {
            newItem.setCollection(refs.getName(), StringUtil.tokenize(refs.getRefIds()));
        }

        return newItem;
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
        for (String s : StringUtil.tokenize(classNames)) {
            sb.append(model.getPackageName() + "." + XmlUtil.getFragmentFromURI(s + " "));
        }
        return sb.toString().trim();
    }
}
