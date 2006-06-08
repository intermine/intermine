package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Bag of intermine ids as Integers.
 * 
 * @author Thomas Riley
 */
public class InterMineIdBag extends InterMineBag
{
    protected static final Logger LOG = Logger.getLogger(InterMineIdBag.class);

    /** 
     * Constructs a new InterMineIdBag to be lazily-loaded from the userprofile database.
     *
     * @param userId the id of the user, matching the userprofile database
     * @param name the name of the bag, matching the userprofile database
     * @param size the size of the bag
     * @param os the ObjectStore to use to retrieve the contents of the bag
     */
    public InterMineIdBag(Integer userId, String name, int size, ObjectStore os) {
        super(userId, name, size, os);
    }

    /** 
     * Constructs a new InterMineIdBag with certain contents.
     *
     * @param userId the Id of the user, to be saved in the userprofile database
     * @param name the name of the bag, to be saved in the userprofile database
     * @param os the ObjectStore to use to store the contents of the bag
     * @param c the new bag contents
     */
    public InterMineIdBag(Integer userId, String name, ObjectStore os, Collection c) {
        super(userId, name, os, c);
    }

   
    /**
     * Add an id.
     * @param id intermine id
     */
    public void add(int id) {
        add(new Integer(id));
    }
    
    /**
     * Remove an id.
     * @param id an intermine id
     */
    public void remove(int id) {
        remove(new Integer(id));
    }

    /**
     * Return a collection of InterMineObjects corresponding to the ids in the bag.
     *
     * @return collection of InterMineObjects
     */
    public Collection toObjectCollection() {
        List list = new ArrayList();
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Integer id = (Integer) iter.next();
            try {
                list.add(os.getObjectById(id));
            } catch (ObjectStoreException err) {
                LOG.error("Failed to load object by id " + id);
            }
        }
        return list;
    }
}
