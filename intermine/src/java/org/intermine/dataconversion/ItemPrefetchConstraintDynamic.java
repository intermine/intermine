package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;

import org.intermine.util.StringUtil;

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
    protected Map idToFnavs = new HashMap();

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
        if (nearFieldName == ObjectStoreItemPathFollowingImpl.IDENTIFIER) {
            return new FieldNameAndValue(farFieldName, item.getIdentifier(), true);
        } else if (nearFieldName == ObjectStoreItemPathFollowingImpl.CLASSNAME) {
            return new FieldNameAndValue(farFieldName, item.getClassName(), false);
        } else if (farFieldName == ObjectStoreItemPathFollowingImpl.IDENTIFIER) {
            Iterator iter = item.getReferences().iterator();
            while (iter.hasNext()) {
                Reference ref = (Reference) iter.next();
                if (nearFieldName.equals(ref.getName())) {
                    FieldNameAndValue retval = new FieldNameAndValue(
                            ObjectStoreItemPathFollowingImpl.IDENTIFIER, ref.getRefId(), false);
                    Set fnavs = (Set) idToFnavs.get(ref.getRefId());
                    if (fnavs == null) {
                        fnavs = new HashSet();
                        idToFnavs.put(ref.getRefId(), fnavs);
                    }
                    fnavs.add(retval);
                    return retval;
                }
            }
            iter = item.getCollections().iterator();
            while (iter.hasNext()) {
                ReferenceList ref = (ReferenceList) iter.next();
                if (nearFieldName.equals(ref.getName())) {
                    FieldNameAndValue retval = new FieldNameAndValue(
                            ObjectStoreItemPathFollowingImpl.IDENTIFIER, ref.getRefIds(), true);
                    String[] ids = StringUtil.split(ref.getRefIds(), " ");
                    for (int i = 0; i < ids.length; i++) {
                        Set fnavs = (Set) idToFnavs.get(ids[i]);
                        if (fnavs == null) {
                            fnavs = new HashSet();
                            idToFnavs.put(ids[i], fnavs);
                        }
                        fnavs.add(retval);
                    }
                    return retval;
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
     * Returns a Set of FieldNameAndValue objects that describe this constraint with respect to a
     * particular target Item. In the case that this is a constraint on IDENTIFIER being a certain
     * value, there is the possibility that the value may originate from a referencelist instead of
     * a reference. This is the case where the returned Set may contain more than one entry. The
     * entries returned in that case will be those FieldNameAndValue objects which have ever been
     * returned that have contained an IDENTIFIER equal to the identifier in the given Item.
     *
     * @param item the Item
     * @return a Set of FieldNameAndValue objects (usually 1)
     */
    public Set getConstraintFromTarget(Item item) {
        if (farFieldName == ObjectStoreItemPathFollowingImpl.IDENTIFIER) {
            return (Set) idToFnavs.get(item.getIdentifier());
        } else if (farFieldName == ObjectStoreItemPathFollowingImpl.CLASSNAME) {
            return Collections.singleton(new FieldNameAndValue(farFieldName, item.getClassName(),
                        false));
        } else if (nearFieldName == ObjectStoreItemPathFollowingImpl.IDENTIFIER) {
            Iterator iter = item.getReferences().iterator();
            while (iter.hasNext()) {
                Reference ref = (Reference) iter.next();
                if (farFieldName.equals(ref.getName())) {
                    return Collections.singleton(new FieldNameAndValue(farFieldName,
                                ref.getRefId(), true));
                }
            }
            throw new IllegalArgumentException("Reference " + farFieldName + " not present in "
                    + item);
        } else {
            Iterator iter = item.getAttributes().iterator();
            while (iter.hasNext()) {
                Attribute att = (Attribute) iter.next();
                if (farFieldName.equals(att.getName())) {
                    return Collections.singleton(new FieldNameAndValue(farFieldName,
                                att.getValue(), false));
                }
            }
            throw new IllegalArgumentException("Attribute " + farFieldName + " not present in "
                    + item);
        }
    }


    /**
     * Clear map of item identifiers to contraints when identifiers no longer
     * needed.  Need to keep memory usage sensible.
     */
    public void clearIdToFnavs() {
        idToFnavs = new HashMap();
    }


    /**
     * @see Object#toString
     */
    public String toString() {
        return "(near." + nearFieldName + " = far." + farFieldName + ")";
    }
}
