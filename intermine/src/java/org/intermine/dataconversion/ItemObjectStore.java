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
import java.util.Iterator;

import org.flymine.metadata.Model;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.util.TypeUtil;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.ItemHelper;

/**
* Far-from-finished objectstore-ish thing to transparently objectify items
* @author Mark Woodbridge
*/
public class ItemObjectStore
{
    ItemStore itemStore;
    
    /**
    * Method that is a bit like LiteParser.convertToObject and FullParser.realiseObjects
    * @param item the item to objectify
    * @param model the metadata relating to the target object
    * @param os an objectstore used to build proxies
    * @return a business object
    * @throws Exception if something goes wrong
    */
    //This is rather like LiteParser.convertToObject)
    protected Object realiseObject(Item item, Model model, ObjectStore os) throws Exception {
        Object obj = ItemHelper.instantiateObject(item, model);
        
        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            Class attrClass = TypeUtil.getFieldInfo(obj.getClass(), attr.getName()).getType();
            if (!attr.getName().equalsIgnoreCase("id")) {
                Object value = TypeUtil.stringToObject(attrClass, attr.getValue());
                TypeUtil.setFieldValue(obj, attr.getName(), value);
            }
        }
        
        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            Integer identifier = new Integer(ref.getRefId());
            // note that this isn't the right id with which to create the proxy
            TypeUtil.setFieldValue(obj, ref.getName(), new ProxyReference(os, identifier));
        }
        
        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            ReferenceList refs = (ReferenceList) i.next();
            Collection col = (Collection) TypeUtil.getFieldValue(obj, refs.getName());
            for (Iterator j = refs.getRefIds().iterator(); i.hasNext();) {
                i.next();
                //col.add(objMap.get(i.next()));
            }
        }
        
        return obj;
    }
}
