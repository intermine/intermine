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

import java.util.Collection;

import org.intermine.objectstore.ObjectStore;

/**
 * Bag of primitive wrapper objects.
 * 
 * @author tom
 */
public class InterMinePrimitiveBag extends InterMineBag
{
    /** 
     * Constructs a new InterMinePrimitiveBag to be lazily-loaded from the userprofile database.
     *
     * @param userId the id of the user, matching the userprofile database
     * @param name the name of the bag, matching the userprofile database
     * @param size the size of the bag
     * @param os the ObjectStore to use to retrieve the contents of the bag
     */
    public InterMinePrimitiveBag(Integer userId, String name, int size, ObjectStore os) {
        super(userId, name, size, os);
    }

    /** 
     * Constructs a new InterMinePrimitiveBag with certain contents.
     *
     * @param userId the id of the user, to be saved in the userprofile database
     * @param name the name of the bag, to be saved in the userprofile database
     * @param os the ObjectStore to use to store the contents of the bag
     * @param c the new bag contents
     */
    public InterMinePrimitiveBag(Integer userId, String name, ObjectStore os, Collection c) {
        super(userId, name, os, c);
    }

    /**
     * @see InterMineBag#toObjectCollection
     */
    public Collection toObjectCollection() {
        return this;
    }
}
