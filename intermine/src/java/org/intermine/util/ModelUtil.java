package org.flymine.util;

import java.util.Collection;
import java.util.StringTokenizer;
//import java.util.Iterator;
//import java.util.ArrayList;
//import java.util.Set;
import java.util.HashSet;
//import java.util.Map;
//import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Provides utility methods for working with data models
 *
 * @author Mark Woodbridge
 */
public class ModelUtil
{
    /** Collection type */
    public static final int COLLECTION = 0;
    /** Attribute type */
    public static final int ATTRIBUTE = 1;
    /** Reference type */
    public static final int REFERENCE = 2;

    private ModelUtil() {
    }

    /**
     * Returns the type of a Field of an Class given the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant Field
     * @return the type of the Field
     */ 
    public static int getFieldType(Class c, String fieldName) {
        Field f = TypeUtil.getField(c, fieldName);
        Class type = f.getType();
        if (isCollection(type)) {
            return COLLECTION;
        }
        if (isAttribute(type)) {
            return ATTRIBUTE;
        }
        if (isReference(type)) {
            return REFERENCE;
        }
        return -1;
    }

    /**
     * Checks whether a Class represents a Collection
     *
     * @param c the Class
     * @return whether the Field is a Collection
     */ 
    public static boolean isCollection(Class c) {
        return Collection.class.isAssignableFrom(c);
    }
    /**
     * Checks whether a Class represents an Attribute
     *
     * @param c the Class
     * @return whether the Field is an Attribute
     */ 
    public static boolean isAttribute(Class c) {
        return c.isPrimitive() || c.getName().startsWith("java");
    }

    /**
     * Checks whether a Field represents a Reference
     *
     * @param c the Class
     * @return whether the Field is a Reference
     */ 
    public static boolean isReference(Class c) {
        return !(isCollection(c) || isAttribute(c));
    }
    
    /**
     * Returns a Collection of Strings which is a list of the primary key fields of this object
     *
     * @param o the Object
     * @return the list of keys
     */
    public static Collection getKey(Object o) {
        Class c = o.getClass();
        Collection col = new HashSet();
        try {
            do {
                Field f = TypeUtil.getField(c, "key");
                f.setAccessible(true);
                StringTokenizer st = new StringTokenizer((String) f.get(o), ", ");
                while (st.hasMoreTokens()) {
                    col.add(st.nextToken());
                }
            } while ((c = c.getSuperclass()) != null);
        } catch (Exception e) {
        }
        return col;
    }
}
