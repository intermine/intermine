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

import java.util.Iterator;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;

/**
 * Provides an object that describes a constraint that is part of a path for the
 * ObjectStoreItemPathFollowingImpl to follow.
 *
 * @author Matthew Wakeling
 */
public class ItemPrefetchConstraintDynamic implements ItemPrefetchConstraint
{
    private String nearFieldName;
    private String farFieldName;

    /**
     * Constructs a new instance of ItemPrefetchConstraint.
     *
     * @param nearFieldName the name of the field on the near object
     * @param farFieldName the name of the field on the referenced object
     */
    public ItemPrefetchConstraintDynamic(String nearFieldName, String farFieldName) {
        this.nearFieldName = nearFieldName;
        this.farFieldName = farFieldName;
    }

    /**
     * Returns a FieldNameAndValue object that describes this constraint with respect to a
     * particular Item.
     *
     * @param item the Item
     * @return a FieldNameAndValue object
     */
    public FieldNameAndValue getConstraint(Item item) {
        if (nearFieldName.equals("identifier")) {
            return new FieldNameAndValue(farFieldName, item.getIdentifier(), true);
        } else if (nearFieldName.equals("className")) {
            return new FieldNameAndValue(farFieldName, item.getClassName(), false);
        } else if (farFieldName.equals("identifier")) {
            Iterator iter = item.getReferences().iterator();
            while (iter.hasNext()) {
                Reference ref = (Reference) iter.next();
                if (nearFieldName.equals(ref.getName())) {
                    return new FieldNameAndValue("identifier", ref.getRefId(), false);
                }
            }
            throw new IllegalArgumentException("Reference " + nearFieldName + " not present in "
                    + item);
        } else {
            Iterator iter = item.getAttributes().iterator();
            while (iter.hasNext()) {
                Attribute att = (Attribute) iter.next();
                if (nearFieldName.equals(att.getName())) {
                    return new FieldNameAndValue(farFieldName, att.getValue(), false);
                }
            }
            throw new IllegalArgumentException("Attribute " + nearFieldName + " not present in "
                    + item);
        }
    }

    /**
     * Returns a FieldNameAndValue object that describes this constraint with respect to a
     * particular target Item.
     *
     * @param item the Item
     * @return a FieldNameAndValue object
     */
    public FieldNameAndValue getConstraintFromTarget(Item item) {
        if (farFieldName.equals("identifier")) {
            return new FieldNameAndValue(farFieldName, item.getIdentifier(), false);
        } else if (farFieldName.equals("className")) {
            return new FieldNameAndValue(farFieldName, item.getClassName(), false);
        } else if (nearFieldName.equals("identifier")) {
            Iterator iter = item.getReferences().iterator();
            while (iter.hasNext()) {
                Reference ref = (Reference) iter.next();
                if (farFieldName.equals(ref.getName())) {
                    return new FieldNameAndValue(farFieldName, ref.getRefId(), true);
                }
            }
            throw new IllegalArgumentException("Reference " + farFieldName + " not present in "
                    + item);
        } else {
            Iterator iter = item.getAttributes().iterator();
            while (iter.hasNext()) {
                Attribute att = (Attribute) iter.next();
                if (farFieldName.equals(att.getName())) {
                    return new FieldNameAndValue(farFieldName, att.getValue(), false);
                }
            }
            throw new IllegalArgumentException("Attribute " + farFieldName + " not present in "
                    + item);
        }
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "(near." + nearFieldName + " = far." + farFieldName + ")";
    }
}
