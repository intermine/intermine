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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.model.fulldata.Item;

/**
 * Represents a description of which Items are likely to be needed for the translation of a given
 * item.
 *
 * @author Matthew Wakeling
 */
public class ItemPrefetchDescriptor
{
    private Set constraints; // A set of ItemPrefetchConstraint objects
    private Set nextPath; // A set of ItemPrefetchDescriptor objects for the next level of the path
    private String name;

    /**
     * Constructor.
     *
     * @param name a name which will be included in the toString output, for debug and logging
     * purposes only
     */
    public ItemPrefetchDescriptor(String name) {
        constraints = new HashSet();
        nextPath = new HashSet();
        this.name = name;
    }

    /**
     * Adds a constraint to the constraint list.
     *
     * @param constraint an ItemPrefetchConstraint
     */
    public void addConstraint(ItemPrefetchConstraint constraint) {
        constraints.add(constraint);
    }

    /**
     * Adds an element to the next path set.
     *
     * @param path an ItemPrefetchDescriptor
     */
    public void addPath(ItemPrefetchDescriptor path) {
        nextPath.add(path);
    }

    /**
     * Creates a constraint such as would be used by a DataTranslator to fetch an Item by
     * description, from an Item.
     *
     * @param item an Item
     * @return a Set of FieldNameAndValue objects
     */
    public Set getConstraint(Item item) {
        Set retval = new HashSet();
        Iterator iter = constraints.iterator();
        while (iter.hasNext()) {
            ItemPrefetchConstraint con = (ItemPrefetchConstraint) iter.next();
            retval.add(con.getConstraint(item));
        }
        return retval;
    }

    /**
     * Creates a constraint such as would be used by a DataTranslator to fetch an Item by
     * description, from an Item that would be returned by such a method call.
     *
     * @param item an Item
     * @return a Set of FieldNameAndValue objects
     */
    public Set getConstraintFromTarget(Item item) {
        Set retval = new HashSet();
        Iterator iter = constraints.iterator();
        while (iter.hasNext()) {
            ItemPrefetchConstraint con = (ItemPrefetchConstraint) iter.next();
            retval.add(con.getConstraintFromTarget(item));
        }
        return retval;
    }

    /**
     * Returns true if the given FieldNameAndValue object is present in the Set of constraints.
     *
     * @param f the FieldNameAndValue object
     * @return a boolean
     */
    public boolean isStatic(FieldNameAndValue f) {
        return constraints.contains(f);
    }

    /**
     * Returns the set of paths from this path.
     *
     * @return a Set of ItemPrefetchDescriptor objects
     */
    public Set getPaths() {
        return nextPath;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return name + constraints.toString();
    }
}
