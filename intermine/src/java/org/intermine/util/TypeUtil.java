package org.flymine.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
     * Returns the Field object of a Class given the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the Field
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
