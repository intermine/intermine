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
     * @param reference true if fieldName is a reference, false if it is an Attribute
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
     * @see Object#equals
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
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * fieldName.hashCode() + 5 * value.hashCode() + (reference ? 7 : 0);
    }

    /**
     * @see Object#toString
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
     * Returns this object.
     *
     * @param item ignored
     * @return this
     */
    public FieldNameAndValue getConstraintFromTarget(Item item) {
        return this;
    }

    /**
     * Returns true if this object matches the given Item.
     *
     * @param item the item to compare
     * @return a boolean
     */
    public boolean matches(Item item) {
        if (reference) {
            Iterator refIter = item.getReferences().iterator();
            while (refIter.hasNext()) {
                Reference ref = (Reference) refIter.next();
                if (ref.getName().equals(fieldName)) {
                    return ref.getRefId().equals(value);
                }
            }
            return false;
        } else {
            if (fieldName.equals("identifier")) {
                return value.equals(item.getIdentifier());
            } else if (fieldName.equals("className")) {
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
}
