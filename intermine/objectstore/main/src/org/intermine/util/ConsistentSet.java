package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


/**
 * A Set that allows for its member objects to be changed whilst still
 * belonging to the set. Normally, this would result in unspecified
 * behavior, as the hashCode() for the objects may change once
 * added. If elements are changed so that two elements are made equal,
 * behaviour is unspecified.
 *
 * @author Andrew Varley
 */
public class ConsistentSet extends AbstractSet
{
    private List list;

    /**
     * Constructor.
     */
    public ConsistentSet() {
        super();
        list = new ArrayList();
    }


    /**
     * Add an object to the set. If an equivalent object already
     * exists, nothing happens.
     *
     * @param obj the object to be added
     * @return true if the set was altered
     * @see Set#add
     */
    public boolean add(Object obj) {
        int index = list.indexOf(obj);
        if (index != -1) {
            //list.set(index, obj);
            return false;
        }
        return list.add(obj);
    }

    /**
     * Add a whole Collection of objects to the set.
     *
     * @param col the Collection of objects to be added
     * @return true if the set was altered
     * @see Set#addAll
     */
    public boolean addAll(Collection col) {
        boolean retval = false;
        HashSet set = new HashSet(list);
        Iterator iter = col.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (!set.contains(obj)) {
                set.add(obj);
                list.add(obj);
                retval = true;
            }
        }
        return retval;
    }

    /**
     * Return the number of elements in this Set.
     *
     * @return the number of elements in the Set.
     */
    public int size() {
        return list.size();
    }

    /**
     * Return an iterator for the elements in the Set.
     *
     * @return an iterator
     */
    public Iterator iterator() {
        return list.iterator();
    }



}
