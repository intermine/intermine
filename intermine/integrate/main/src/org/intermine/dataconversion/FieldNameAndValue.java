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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;

/**
 * Provides an object that describes a field name with a corresponding value.
 *
 * @author Matthew Wakeling
 */
public class FieldNameAndValue implements ItemPrefetchConstraint
{
    private String fieldName;
    private String value;
    private boolean reference;

    /**
     * Constructs a new instance of FieldNameAndValue.
     *
     * @param fieldName a String
     * @param value a String
     * @param reference true if fieldName is a reference, false if it is an Attribute.
     * Alternatively, if fieldName is ObjectStoreItemPathFollowingImpl.IDENTIFIER, then false if
     * the value is from a reference, true if it is from a referencelist.
     */
    public FieldNameAndValue(String fieldName, String value, boolean reference) {
        this.fieldName = fieldName;
        this.value = value;
        this.reference = reference;
    }

    /**
     * Returns the field name.
     *
     * @return a String
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the field value.
     *
     * @return a String
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the reference boolean.
     *
     * @return a boolean
     */
    public boolean isReference() {
        return reference;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof FieldNameAndValue) {
            FieldNameAndValue f = (FieldNameAndValue) o;
            return (f.fieldName.equals(fieldName) && f.value.equals(value))
                && (f.reference == reference);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 3 * fieldName.hashCode() + 5 * value.hashCode() + (reference ? 7 : 0);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return (reference ? "(reference " : "(") + fieldName + " = " + value + ")";
    }

    /**
     * Returns this object.
     *
     * @param item ignored
     * @return this
     */
    public FieldNameAndValue getConstraint(Item item) {
        return this;
    }

    /**
     * Returns this object in a singleton.
     *
     * @param item ignored
     * @return this, in a singleton
     */
    public Set getConstraintFromTarget(Item item) {
        return Collections.singleton(this);
    }

    /**
     * Returns true if this object matches the given Item.
     *
     * @param item the item to compare
     * @return a boolean
     */
    public boolean matches(Item item) {
        if (reference) {
            if (fieldName == ObjectStoreItemPathFollowingImpl.IDENTIFIER) {
                String id = item.getIdentifier();
                int idLen = id.length();
                if (value == null) {
                    return false;
                }
                int valLen = value.length();
                if (valLen < idLen) {
                    return false;
                } else if (id.equals(value.substring(0, idLen)) && (valLen == idLen
                                     || Character.isWhitespace(value.charAt(idLen)))) {
                    return true;
                } else if (value.indexOf(" " + id + " ") != -1) {
                    return true;
                } else if ((valLen > idLen)
                           && (" " + id).equals(value.substring(valLen - idLen - 1))) {
                    return true;
                }
                return false;
            } else {
                Iterator refIter = item.getReferences().iterator();
                while (refIter.hasNext()) {
                    Reference ref = (Reference) refIter.next();
                    if (ref.getName().equals(fieldName)) {
                        return ref.getRefId().equals(value);
                    }
                }
                return false;
            }
        } else {
            if (fieldName == ObjectStoreItemPathFollowingImpl.IDENTIFIER) {
                return value.equals(item.getIdentifier());
            } else if (fieldName == ObjectStoreItemPathFollowingImpl.CLASSNAME) {
                return value.equals(item.getClassName());
            } else {
                Iterator attIter = item.getAttributes().iterator();
                while (attIter.hasNext()) {
                    Attribute att = (Attribute) attIter.next();
                    if (att.getName().equals(fieldName)) {
                        return att.getValue().equals(value);
                    }
                }
                return false;
            }
        }
    }

    /**
     * Perform a deep clone of this object.  Needed to allow ObjectStoreItemPathFollowingImpl
     * to manage memory.
     * @return the cloned object
     */
    public ItemPrefetchConstraint deepClone() {
        FieldNameAndValue clone = new FieldNameAndValue(this.fieldName, this.value, this.reference);
        return clone;
    }
}
