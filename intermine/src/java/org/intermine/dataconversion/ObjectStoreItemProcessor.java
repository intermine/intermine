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

import org.flymine.xml.full.Item;
import org.flymine.objectstore.ObjectStoreWriter;

/**
* ItemProcessor that stores Items in an ObjectStore
* @author Mark Woodbridge
*/
public class ObjectStoreItemProcessor extends ItemProcessor
{
    protected ItemStore itemStore;

    /**
    * Constructor
    * @param osw the ObjectStoreWriter used to store the items
    */
    public ObjectStoreItemProcessor(ObjectStoreWriter osw) {
        itemStore = new ItemStore(osw);
    }

    /**
    * @see ItemProcessor#process
    */
    public void process(Item item) throws Exception {
        itemStore.store(item);
    }
}
