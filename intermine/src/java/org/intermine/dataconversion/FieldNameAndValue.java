package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Provides an object that describes a field name with a corresponding value.
 *
 * @author Matthew Wakeling
 */
public class FieldNameAndValue
{
    private String fieldName;
    private Object value;

    /**
     * Constructs a new instance of FieldNameAndValue.
     *
     * @param fieldName a String
     * @param value an Object
     */
    public FieldNameAndValue(String fieldName, Object value) {
        this.fieldName = fieldName;
        this.value = value;
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
     * @return an Object
     */
    public Object getValue() {
        return value;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof FieldNameAndValue) {
            FieldNameAndValue f = (FieldNameAndValue) o;
            return (f.fieldName.equals(fieldName) && f.value.equals(value));
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * fieldName.hashCode() + 5 * value.hashCode();
    }
}
