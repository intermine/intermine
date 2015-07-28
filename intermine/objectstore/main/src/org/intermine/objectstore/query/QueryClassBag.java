package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;

/**
 * An element that can appear in the FROM clause of a query
 *
 * @author Matthew Wakeling
 */
public class QueryClassBag implements FromElement
{
    private Class<? extends InterMineObject> type;
    private Set<Integer> ids;
    private Collection<?> bag;
    private ObjectStoreBag osb;

    /**
     * Constructs a QueryClassBag representing the specified Java class and bag of objects.
     *
     * @param type the Java class
     * @param bag the Collection of objects
     */
    public QueryClassBag(Class<? extends InterMineObject> type, Collection<?> bag) {
        this.type = type;
        this.bag = bag;
        ids = convertToIds(bag, this.type);
        this.osb = null;
    }

    /**
     * Constructs a QueryClassBag representing the specified Java class and ObjectStoreBag.
     *
     * @param type the Java class
     * @param osb the ObjectStoreBag
     */
    public QueryClassBag(Class<? extends InterMineObject> type, ObjectStoreBag osb) {
        this.type = type;
        this.osb = osb;
        this.ids = null;
        this.bag = null;
    }

    private static Set<Integer> convertToIds(Collection<?> bag,
            Class<? extends InterMineObject> type) {
        if (bag == null) {
            return null;
        }
        Set<Integer> ids = new HashSet<Integer>();
        for (Object o : bag) {
            if (type.isInstance(o)) {
                ids.add(((InterMineObject) o).getId());
            }
        }
        return ids;
    }

    /**
     * Gets the Java class represented by this QueryClassBag.
     *
     * @return the Class
     */
    public Class<? extends InterMineObject> getType() {
        return type;
    }

    /**
     * Returns the bag of objects.
     *
     * @return a Collection
     */
    public Collection<?> getBag() {
        return bag;
    }

    /**
     * Returns the ObjectStoreBag.
     *
     * @return an ObjectStoreBag
     */
    public ObjectStoreBag getOsb() {
        return osb;
    }

    /**
     * Returns the Set of object IDs present in the bag that correspond to the type specified.
     *
     * @return a Set of Integers
     */
    public Set<Integer> getIds() {
        return ids;
    }

    /**
     * Returns a String representation. This method is used by IqlQuery to generate iql, so don't
     * change the output.
     *
     * @return a String
     */
    @Override
    public String toString() {
        Set<Class<?>> classes = Util.decomposeClass(type);
        StringBuffer retval = new StringBuffer();
        if (osb != null) {
            retval.append("BAG(" + osb.getBagId() + ")::");
        } else if (bag != null) {
            retval.append("?::");
        } else {
            retval.append("!::");
        }
        if (classes.size() == 1) {
            retval.append(type.getName());
        } else {
            boolean needComma = false;
            for (Class<?> clazz : classes) {
                retval.append(needComma ? ", " : "(");
                needComma = true;
                retval.append(clazz.getName());
            }
            retval.append(")");
        }
        return retval.toString();
    }
}
