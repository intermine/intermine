package org.flymine.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;

/**
 * Provides utility methods for working with Java types and reflection
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class TypeUtil
{
    private TypeUtil() {
    }

    private static Map classToFieldToGetter = new HashMap();
    private static Map classToFieldToSetter = new HashMap();
    
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
     * Sets the value of a public or protected Field of an Object given the field name
     *
     * @param o the Object
     * @param fieldName the name of the relevant Field
     * @param fieldValue the value of the Field
     * @throws IllegalAccessException if the field is inaccessible
     */ 
    public static void setFieldValue(Object o, String fieldName, Object fieldValue) 
        throws IllegalAccessException {
        Field f  = getField(o.getClass(), fieldName);
        f.setAccessible(true);
        f.set(o, fieldValue);
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
     * Gets the getter methods for the bean properties of a class
     *
     * @param c the Class
     * @return an array of the getter methods
     * @throws IntrospectionException if an error occurs
     */
    public static Method[] getGetters(Class c) throws IntrospectionException {
        PropertyDescriptor[] pd = Introspector.getBeanInfo(c).getPropertyDescriptors();
        Collection getters = new HashSet();
        for (int i = 0; i < pd.length; i++) {
            Method getter = pd[i].getReadMethod();
            if (!getter.getName().equals("getClass")) {
                getters.add(getter);
            }
        }
        return (Method[]) getters.toArray(new Method[] {});
    }

    /**
     * Gets a map from field to getter for a class. Fields that do not have a getter do not appear
     * in this map.
     *
     * @param c the Class
     * @return a mapping from field to the getter used to read that field
     * @throws IntrospectionException if an error occurs
     */
    public static Map getFieldToGetter(Class c) throws IntrospectionException {
        synchronized (classToFieldToGetter) {
            Map retval = (Map) classToFieldToGetter.get(c);
            if (retval == null) {
                PropertyDescriptor[] pd = Introspector.getBeanInfo(c).getPropertyDescriptors();
                retval = new HashMap();
                Map fieldToSetter = new HashMap();
                for (int i = 0; i < pd.length; i++) {
                    try {
                        Method getter = pd[i].getReadMethod();
                        if ((!getter.getName().equals("getClass"))
                                && (!getter.getName().equals("getId"))) {
                            Field field = c.getDeclaredField(pd[i].getName());
                            retval.put(field, getter);
                            Method setter = pd[i].getWriteMethod();
                            fieldToSetter.put(field, setter);
                        }
                    } catch (NoSuchFieldException e) {
                    }
                }
                classToFieldToGetter.put(c, retval);
                classToFieldToSetter.put(c, fieldToSetter);
            }
            return retval;
        }
    }
                
    /**
     * Gets a map from field to setter for a class.
     *
     * @param c the Class
     * @return a mappin from field to the setter used to write to that field
     * @throws IntrospectionException if an error occurs
     */
    public static Map getFieldToSetter(Class c) throws IntrospectionException {
        synchronized (classToFieldToGetter) {
            if (!classToFieldToSetter.containsKey(c)) {
                getFieldToGetter(c);
            }
            return (Map) classToFieldToSetter.get(c);
        }
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
