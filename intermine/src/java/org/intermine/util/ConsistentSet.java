package org.flymine.util;

import java.util.AbstractSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


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
     * exists, it will be be replaced by this one.
     *
     * @param obj the object to be added
     * @return true if the set was altered
     * @see AbstractSet#add
     */
    public boolean add(Object obj) {
        int index = list.indexOf(obj);
        if (index != -1) {
            list.set(index, obj);
            return true;
        }
        return list.add(obj);
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
