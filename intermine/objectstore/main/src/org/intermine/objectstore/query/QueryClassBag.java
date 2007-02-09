package org.intermine.objectstore.query;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;

/**
 * An element that can appear in the FROM clause of a query
 *
 * @author Matthew Wakeling
 */
public class QueryClassBag implements FromElement
{
    private Class type;
    private Set ids;
    private Collection bag;
    private ObjectStoreBag osb;

    /**
     * Constructs a QueryClassBag representing the specified Java class and bag of objects.
     *
     * @param type the Java class
     * @param bag the Collection of objects
     */
    public QueryClassBag(Class type, Collection bag) {
        this.type = type;
        this.bag = bag;
        ids = convertToIds(bag, this.type);
        this.osb = null;
    }

    /**
     * Constructs a QueryClass representing the specified set of classes and bag of objects.
     *
     * @param types the Set of classes
     * @param bag the Collection of objects
     */
    public QueryClassBag(Set types, Collection bag) {
        if (types.size() == 1) {
            this.type = (Class) types.iterator().next();
        } else {
            this.type = DynamicUtil.composeClass(types);
        }
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
    public QueryClassBag(Class type, ObjectStoreBag osb) {
        this.type = type;
        this.osb = osb;
        this.ids = null;
        this.bag = null;
    }

    /**
     * Constructs a QueryClass representing the specified set of classes and ObjectStoreBag.
     *
     * @param types the Set of classes
     * @param osb the ObjectStoreBag
     */
    public QueryClassBag(Set types, ObjectStoreBag osb) {
        if (types.size() == 1) {
            this.type = (Class) types.iterator().next();
        } else {
            this.type = DynamicUtil.composeClass(types);
        }
        this.osb = osb;
        this.ids = null;
        this.bag = null;
    }

    private static Set convertToIds(Collection bag, Class type) {
        Set ids = new HashSet();
        Iterator iter = bag.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
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
    public Class getType() {
        return type;
    }

    /**
     * Returns the bag of objects.
     *
     * @return a Collection
     */
    public Collection getBag() {
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
    public Set getIds() {
        return ids;
    }

    /**
     * Returns a String representation. This method is used by IqlQuery to generate iql, so don't
     * change the output.
     *
     * @return a String
     */
    public String toString() {
        Set classes = DynamicUtil.decomposeClass(type);
        StringBuffer retval = new StringBuffer();
        if (bag == null) {
            retval.append("BAG(" + osb.getBagId() + ")::");
        } else {
            retval.append("?::");
        }
        if (classes.size() == 1) {
            retval.append(type.getName());
        } else {
            boolean needComma = false;
            Iterator classIter = classes.iterator();
            while (classIter.hasNext()) {
                retval.append(needComma ? ", " : "(");
                needComma = true;
                retval.append(((Class) classIter.next()).getName());
            }
            retval.append(")");
        }
        return retval.toString();
    }
}
