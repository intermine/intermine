package org.intermine.dataconversion;

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

import org.intermine.model.InterMineObject;
import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;

/**
 * Stores Items in an objectstore.
 *
 * @author Matthew Wakeling
 * @author Mark Woodbridge
 */
public class ObjectStoreItemWriter implements ItemWriter
{
    private ObjectStoreWriter osw;
    private int transactionCounter = 0;
    private static final int TRANSACTION_BATCH_SIZE = 10000;
    /**
     * Constructs the ItemWriter with an ObjectStoreWriter.
     *
     * @param osw the ObjectStoreWriter in which to store the Items
     * @throws ObjectStoreException if the ObjectStore is already in a transaction
     */
    public ObjectStoreItemWriter(ObjectStoreWriter osw) throws ObjectStoreException {
        this.osw = osw;
        osw.beginTransaction();
    }

    /**
     * @see ItemWriter#store
     */
    public void store(Item item) throws ObjectStoreException {
        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            osw.store((InterMineObject) i.next());
            transactionCounter++;
        }
        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            osw.store((InterMineObject) i.next());
            transactionCounter++;
        }
        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            osw.store((InterMineObject) i.next());
            transactionCounter++;
        }
        osw.store(item);
        transactionCounter++;
        if (transactionCounter >= TRANSACTION_BATCH_SIZE) {
            osw.commitTransaction();
            osw.beginTransaction();
            transactionCounter = 0;
        }
    }

    /**
     * @see ItemWriter#storeAll
     */
    public void storeAll(Collection items) throws ObjectStoreException {
        Iterator i = items.iterator();
        while (i.hasNext()) {
            store((Item) i.next());
            //i.remove(); ?
        }
    }

    /**
     * @see ItemWriter#close
     */
    public void close() throws ObjectStoreException {
        osw.commitTransaction();
        osw.close();
    }
}


