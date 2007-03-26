package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Represents a value with an unknown type. The value is stored as a String representation - the
 * representation used in IQL. It is presumed that these unknown type values are converted into
 * typed values before the Query is finished constructing. Therefore, these objects are merely a
 * temporary placeholder for values before their type is inferred.
 *
 * @author Matthew Wakeling
 */
public class UnknownTypeValue
{
    /** A value that indicates that the value represents a number. */
    public static final int TYPE_NUMBER = 0;
    /** A value that indicates that the value represents a Boolean. */
    public static final int TYPE_BOOLEAN = 1;
    /** A value that indicates that the value represents a String or Date. */
    public static final int TYPE_STRING = 2;
    
    private String value;

    /**
     * Constructor.
     *
     * @param value the value
     */
    public UnknownTypeValue(String value) {
        this.value = value;
    }

    /**
     * Returns the value.
     * 
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the value, converted (if possible) into the object type given by the input class.
     *
     * @param cls a Class into which to attempt to convert the value
     * @return the converted value
     */
    public Object getConvertedValue(Class cls) {
        try {
            if (cls.equals(Boolean.class)) {
                if ("true".equals(value)) {
                    return Boolean.TRUE;
                } else if ("false".equals(value)) {
                    return Boolean.FALSE;
                }
            } else if (cls.equals(String.class)) {
                if ((value.charAt(0) == '\'') && (value.charAt(value.length() - 1) == '\'')) {
                    return value.substring(1, value.length() - 1);
                }
            } else if (cls.equals(Date.class)) {
                if ((value.charAt(0) == '\'') && (value.charAt(value.length() - 1) == '\'')) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    return format.parse(value.substring(1, value.length() - 1));
                }
            } else if (cls.equals(Short.class)) {
                return Short.valueOf(value);
            } else if (cls.equals(Integer.class)) {
                return Integer.valueOf(value);
            } else if (cls.equals(Long.class)) {
                return Long.valueOf(value);
            } else if (cls.equals(Float.class)) {
                return Float.valueOf(value);
            } else if (cls.equals(Double.class)) {
                return Double.valueOf(value);
            } else if (cls.equals(BigDecimal.class)) {
                return new BigDecimal(value);
            }
        } catch (ParseException e) {
            // Fall through to general exception
        } catch (NumberFormatException e) {
            // Fall through to general exception
        }
        throw new ClassCastException("Cannot parse value \"" + value + "\" into " + cls.toString());
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return value;
    }

    /**
     * Returns the approximate type of the argument.
     *
     * @return an int
     */
    public int getApproximateType() {
        if ((value.charAt(0) == '\'') && (value.charAt(value.length() - 1) == '\'')) {
            return TYPE_STRING;
        } else if ("true".equals(value) || "false".equals(value)) {
            return TYPE_BOOLEAN;
        } else {
            try {
                new BigDecimal(value);
                return TYPE_NUMBER;
            } catch (NumberFormatException e) {
            }
        }
        throw new ClassCastException("Value \"" + value + "\" has no sane type");
    }

    /**
     * Converts a Class into an approximate type.
     *
     * @param cls the Class to convert
     * @return an int
     */
    public static int classToType(Class cls) {
        if (String.class.equals(cls)) {
            return TYPE_STRING;
        } else if (Boolean.class.equals(cls)) {
            return TYPE_BOOLEAN;
        } else if (Number.class.isAssignableFrom(cls)) {
            return TYPE_NUMBER;
        }
        throw new ClassCastException(cls.toString() + " has no associated type");
    }
}
