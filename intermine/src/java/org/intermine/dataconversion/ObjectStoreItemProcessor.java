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

import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.Attribute;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.util.StringUtil;

/**
* ItemProcessor that stores Items in an ObjectStore
* @author Mark Woodbridge
*/
public class ObjectStoreItemProcessor extends ItemProcessor
{
    protected ObjectStoreWriter osw;

    /**
    * Constructor
    * @param osw the ObjectStoreWriter used to store the items
    */
    public ObjectStoreItemProcessor(ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
    * @see ItemProcessor#process
    */
    public void process(Item item) throws Exception {
        org.flymine.model.fulldata.Item newItem = new org.flymine.model.fulldata.Item();
        newItem.setIdentifier(item.getIdentifier());
        newItem.setClassName(item.getClassName());
        newItem.setImplementations(item.getImplementations());
        
        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            org.flymine.model.fulldata.Attribute newAttr 
                = new org.flymine.model.fulldata.Attribute();
            newAttr.setName(attr.getName());
            newAttr.setValue(attr.getValue());
            newItem.getAttributes().add(newAttr);
            osw.store(newAttr);
        }

        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            org.flymine.model.fulldata.Reference newRef
                = new org.flymine.model.fulldata.Reference();
            newRef.setName(ref.getName());
            newRef.setRefId(ref.getRefId());
            newItem.getReferences().add(newRef);
            osw.store(newRef);
        }

        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            ReferenceList refs = (ReferenceList) i.next();
            org.flymine.model.fulldata.ReferenceList newRefs
                = new org.flymine.model.fulldata.ReferenceList();
            newRefs.setName(refs.getName());
            newRefs.setRefIds(StringUtil.join(refs.getRefIds(), " "));
            newItem.getCollections().add(newRefs);
            osw.store(newRefs);
        }

        osw.store(newItem);
    }
}
