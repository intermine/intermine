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

import java.util.Collection;
import java.util.Iterator;

import org.intermine.model.InterMineObject;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Stores Items in an objectstore.
 *
 * @author Matthew Wakeling
 * @author Mark Woodbridge
 */
public class ObjectStoreItemWriter implements ItemWriter
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreItemWriter.class);

    private ObjectStoreWriter osw;
    private int transactionCounter = 0;
    private static final int TRANSACTION_BATCH_SIZE = 10000000;

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
     * {@inheritDoc}
     */
    public Integer store(Item item) throws ObjectStoreException {
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
        if (StringUtils.isEmpty(item.getClassName())
            && StringUtils.isEmpty(item.getImplementations())) {
            throw new RuntimeException("className not set for item: " + item.getIdentifier());
        }
        osw.store(item);
        incrementTransaction();
        return item.getId();
    }

    /**
     * {@inheritDoc}
     */
    public void store(ReferenceList refList, Integer itemId) throws ObjectStoreException {
        ProxyReference proxy = new ProxyReference(osw.getObjectStore(), itemId, Item.class); 
        refList.proxyItem(proxy);
        osw.store(refList);
        incrementTransaction();
    }

    /**
     * {@inheritDoc}
     */
    public void store(Reference ref, Integer itemId) throws ObjectStoreException {
        ProxyReference proxy = new ProxyReference(osw.getObjectStore(), itemId, Item.class); 
        ref.proxyItem(proxy);
        osw.store(ref);
        incrementTransaction();
    }

    /**
     * {@inheritDoc}
     */
    public void storeAll(Collection<Item> items) throws ObjectStoreException {
        Iterator<Item> i = items.iterator();
        int count = 0;
        Item item = new Item();
        while (i.hasNext()) {
            item = i.next();
            store(item);
            count++;
            if (count % 1000 == 0) {
                LOG.info("transactionCounter has size of " + transactionCounter
                         + " is now on storing " + item.getClassName());
            }
            //i.remove(); ?
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        if (osw.isInTransaction()) {
            osw.commitTransaction();
        }
    }

    private void incrementTransaction() throws ObjectStoreException {
        transactionCounter++;
        if (transactionCounter >= TRANSACTION_BATCH_SIZE) {
            LOG.info("Committing transaction");
            osw.commitTransaction();
            osw.beginTransaction();
            transactionCounter = 0;
        }
    }
}
