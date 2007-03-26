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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.model.fulldata.Item;

/**
 * Represents a description of which Items are likely to be needed for the translation of a given
 * item.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
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
     * Creates a set of constraints such as would be used by a DataTranslator to fetch an Item by
     * description, from an Item that would be returned by such a method call. This has to be a set,
     * because there is a ItemPrefetchConstraintDynamic that is capable of having multiple different
     * constraints all fetching the same Item. This is particularly the case with collection fetches
     * (where the field name is IDENTIFIER, the value contains a space-separated list of item
     * identifiers, and the "reference" boolean is true).
     *
     * @param item an Item
     * @return a Set of constraints (each a Set of FieldNameAndValue objects)
     */
    public Set getConstraintFromTarget(Item item) {
        Set retval = new HashSet();
        List constraintSets = new ArrayList();
        Iterator iter = constraints.iterator();
        while (iter.hasNext()) {
            ItemPrefetchConstraint con = (ItemPrefetchConstraint) iter.next();
            constraintSets.add(con.getConstraintFromTarget(item));
        }
        // Now, we have a List of sets of Constraints. We need to produce the combinational product
        // of these sets of Constraints. This requires recursion, so we use another method to help.
        buildCombinationalProduct(retval, constraintSets, 0, new HashSet());
        return retval;
    }


    /**
     * Builds a combinational product of a load of constraints.
     *
     * @param retval this will have constraints (each a Set of FieldNameAndValue objects) added to
     * it
     * @param constraintSets this is a List of Sets of FieldNameAndValue objects. The Sets are not
     * called constraints in this case, because each set represents a different constraint-fragment
     * that has been used by different constraints to refer to the given target Item
     * @param position the position number of recursion in the constraintSets. This should range
     * between 0 and constraintSets.size() - 1, inclusive. When position is equal to
     * constraintSets.size() - 1 then objects will be added to retval
     * @param soFar the incompletely-build objects to be added to retval. This should be a Set that
     * contains one element from each position in constraintSets up to the current position
     * exclusive
     */
    protected void buildCombinationalProduct(Set retval, List constraintSets, int position,
            Set soFar) {
        Iterator fnavIter = ((Set) constraintSets.get(position)).iterator();
        while (fnavIter.hasNext()) {
            FieldNameAndValue fnav = (FieldNameAndValue) fnavIter.next();
            Set newSoFar = soFar;
            if (fnavIter.hasNext()) {
                newSoFar = new HashSet(soFar);
            }
            newSoFar.add(fnav);
            if (position >= constraintSets.size() - 1) {
                retval.add(newSoFar);
            } else {
                buildCombinationalProduct(retval, constraintSets, position + 1, newSoFar);
            }
        }
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
     * Return deep clone of this object, releies on calling deepClone of all constituent
     * objects.
     * @return the deep cloned ItemPrefetchDescriptor
     */
    public ItemPrefetchDescriptor deepClone() {
        ItemPrefetchDescriptor clone = new ItemPrefetchDescriptor(this.name);

        Iterator iter = this.constraints.iterator();
        while (iter.hasNext()) {
            ItemPrefetchConstraint cloneConstraint
                = ((ItemPrefetchConstraint) iter.next()).deepClone();
            clone.addConstraint(cloneConstraint);
        }

        iter = this.nextPath.iterator();
        while (iter.hasNext()) {
            ItemPrefetchDescriptor cloneDescriptor
                = ((ItemPrefetchDescriptor) iter.next()).deepClone();
            clone.addPath(cloneDescriptor);
        }
        return clone;
    }

    /**
     * Get the display name of this descriptor.
     * 
     * @return the arbitrary display name
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return name + constraints.toString();
    }
}
