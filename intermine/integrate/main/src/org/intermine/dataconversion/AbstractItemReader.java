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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Implements getItemByPath and getItemsByPath.
 * 
 * @author Thomas Riley
 */
public abstract class AbstractItemReader implements ItemReader
{
    private static final Logger LOG = Logger.getLogger(AbstractItemReader.class);
    
    /**
     * @see ItemReader#getItemsByPath(ItemPath, Item, Object[])
     */
    public Item getItemByPath(ItemPath path, Item startingPoint, Object variables[])
            throws ObjectStoreException {
        ItemPrefetchDescriptor ipd = path.getItemPrefetchDescriptor();
        List items = getItemsByPath(path, ipd, startingPoint, variables);
        
        if (items == null || items.size() == 0) {
            return null;
        } else if (items.size() > 1) {
            throw new ObjectStoreException("expected one item at the end of path "
                        + path.getItemPrefetchDescriptor().getDisplayName() + " but found "
                        + items.size() + " items");
        } else {
            return (Item) items.get(0);
        }
    }
    
    /**
     * @see ItemReader#getItemsByPath(ItemPath, Item)
     */
    public Item getItemByPath(ItemPath path, Item startingPoint)
            throws ObjectStoreException {
        return getItemByPath(path, startingPoint, new Object[0]);
    }
    
    /**
     * Generalised method for traversing a path and returning the items found at the 
     * end of the path.
     * 
     * @param path the ItemPath being traversed
     * @param ipd the root ItemPrefetchDescriptor
     * @param startingPoint the item to start traversing from
     * @param variables variable values, must not be null
     * @return list of Items found at the end of the path
     * @throws ObjectStoreException if something goes wrong
     */
    private List getItemsByPath(ItemPath path, ItemPrefetchDescriptor ipd,
                                Item startingPoint, Object variables[])
            throws ObjectStoreException {
        Item currentItem = startingPoint;
        
        while (ipd != null) {
            Set constraints;
            try {
                constraints = new HashSet(ipd.getConstraint(currentItem));
            } catch (IllegalArgumentException err) {
                LOG.debug("caught IllegalArgumentException in getItemsByPath:", err);
                return null;
            }
            constraints.addAll(path.getFieldValueConstrainsts(ipd, variables));
            List items = getItemsByDescription(constraints);
            
            Set subPathConstraints = path.getSubItemPathConstraints(ipd);
            for (Iterator iiter = items.iterator(); iiter.hasNext(); ) {
                Item start = (Item) iiter.next();
                for (Iterator citer = subPathConstraints.iterator(); citer.hasNext(); ) {
                    ItemPrefetchDescriptor subipd = (ItemPrefetchDescriptor) citer.next();
                    List itemsFound = getItemsByPath(path, subipd, start, variables);
                    if (itemsFound == null || itemsFound.size() == 0) {
                        iiter.remove();
                        break;
                    }
                }
            }
            
            if (items.size() == 1 && ipd.getPaths().size() > 0) {
                currentItem = (Item) items.get(0);
            } else if (items.size() == 0) {
                return null;
            } else if (ipd.getPaths().size() > 0) {
                throw new ObjectStoreException("expected one item following prefetch descriptor "
                        + ipd.getDisplayName() + " from item " + currentItem + " but found "
                        + items.size() + " items");
            } else {
                return items;
            }
            
            if (ipd.getPaths().size() > 0) {
                ipd = (ItemPrefetchDescriptor) ipd.getPaths().iterator().next();
            } else {
                ipd = null;
            }
        }
        
        throw new IllegalStateException("should not get here");
    }
    
    /**
     * @see ItemReader#getItemsByPath(ItemPath, Item, Object[])
     */
    public List getItemsByPath(ItemPath path, Item startingPoint, Object variables[])
            throws ObjectStoreException {
        ItemPrefetchDescriptor ipd = path.getItemPrefetchDescriptor();
        return getItemsByPath(path, ipd, startingPoint, variables);
    }
    
    /**
     * @see ItemReader#getItemsByPath(ItemPath, Item)
     */
    public List getItemsByPath(ItemPath path, Item startingPoint)
            throws ObjectStoreException {
        return getItemsByPath(path, startingPoint, new Object[0]);
    }
}
