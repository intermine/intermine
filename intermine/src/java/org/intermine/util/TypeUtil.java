package org.flymine.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.lang.reflect.Field;

/**
 * Provides utility methods for working with Java types and reflection
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class TypeUtil
{
    private TypeUtil() {
    }

    /**
     * Returns the value of a public or protected Field of an Object given the field name
     *
     * @param o the Object
     * @param fieldName the name of the relevant Field
     * @return the value of the Field
     * @throws IllegalAccessException if the field is inaccessible
     */ 
    public static Object getFieldValue(Object o, String fieldName) throws IllegalAccessException {
        Field f  = getField(o.getClass(), fieldName);
        f.setAccessible(true);
        return f.get(o);
    }

    /**
     * Returns the Field object of a Class given the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the Field, or null if the field is not found
     */ 
    public static Field getField(Class c, String fieldName) {
        Field f = null;
        boolean found = false;
        do {
            try {
                f = c.getDeclaredField(fieldName);
                found = true;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        } while(c != null && !found);    
        return f;
    }

    /**
     * Gets the Fields of a Class
     *
     * @param c the Class
     * @return the fields in this class
     */
    public static Collection getFields(Class c) {
        Collection fields = new HashSet();
        do {
            for (int i = 0; i < c.getDeclaredFields().length; i++) {
                fields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
        } while ((c = c.getSuperclass()) != null);
        return fields;
    }

    /**
     * Get the type of the elements of a collection
     *
     * @param col the collection
     * @return the Class of the elements of the collection
     */
    public static Class getElementType(Collection col) {
        if (col == null) {
            throw new NullPointerException("Collection cannot be null");
        }
        if (col.size() == 0) {
            throw new NoSuchElementException("Collection cannot be empty");
        }
        return col.iterator().next().getClass();
    }

    /**
     * Returns the container class for a given primitive type
     *
     * @param c one of the 8 primitive types
     * @return the corresponding container class
     */
    public static Class toContainerType(Class c) {
        if (c.equals(Integer.TYPE)) {
            return Integer.class;
        }
        if (c.equals(Boolean.TYPE)) {
            return Boolean.class;
        }
        if (c.equals(Double.TYPE)) {
            return Double.class;
        }
        if (c.equals(Float.TYPE)) {
            return Float.class;
        }
        if (c.equals(Long.TYPE)) {
            return Long.class;
        }
        if (c.equals(Short.TYPE)) {
            return Short.class;
        }
        if (c.equals(Byte.TYPE)) {
            return Byte.class;
        }
        if (c.equals(Character.TYPE)) {
            return Character.class;
        }
        return c;
    }
}
