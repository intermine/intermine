package org.flymine.util;

import java.util.Set;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Iterator;
//import java.util.Set;
import java.util.LinkedHashSet;
//import java.util.Map;
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
     * @return the type of the Field, or -1 if the field is not found
     */
    public static int getFieldType(Class c, String fieldName) {
        Field f = TypeUtil.getField(c, fieldName);
        if (f == null) {
            return -1;
        }
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
     * Returns a Set of Strings which is a list of the primary key fields of this Class
     *
     * @param c the Class
     * @return the list of keys
     */
    public static Set getKey(Class c) {
        Set set = new LinkedHashSet();
        try {
            do {
                Field f = TypeUtil.getField(c, "key");
                f.setAccessible(true);
                StringTokenizer st = new StringTokenizer((String) f.get(null), ", ");
                while (st.hasMoreTokens()) {
                    set.add(st.nextToken());
                }
            } while ((c = c.getSuperclass()) != null);
        } catch (Exception e) {
        }
        return set;
    }

    /**
     * Checks that an object has its primary keys set
     *
     * @param obj the Object to check
     * @return true if primary keys set, false otherwise
     * @throws IllegalAccessException if one of the fields is inaccessible
     */
    public static boolean hasValidKey(Object obj) throws IllegalAccessException {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }

        Class clazz = obj.getClass();
        Set keys = getKey(clazz);

        Iterator keysIter = keys.iterator();

        try {
            while (keysIter.hasNext()) {
                String field = (String) keysIter.next();

                Object value = TypeUtil.getFieldValue(obj, field);

                if (value == null) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return true;
    }


//     public static void makeMap(Object o, Map m) throws Exception {
//         if (o == null) {
//             return;
//         }

//         Class c = o.getClass();
//         if (m.keySet().contains(c)) {
//             ((Set) m.get(c)).add(o);
//         } else {
//             Set s = new HashSet();
//             s.add(o);
//             m.put(c, s);
//         }

//         Iterator fields = getKey(c).iterator();
//         while (fields.hasNext()) {
//             String field = (String) fields.next();
//             int fieldType = getFieldType(c, field);
//             Object fieldValue = TypeUtil.getFieldValue(o, field);
//             if (COLLECTION == fieldType) {
//                 Iterator iter = ((Collection) fieldValue).iterator();
//                 while (iter.hasNext()) {
//                         makeMap(iter.next(), m);
//                 }
//             } else if (REFERENCE == fieldType) {
//                 makeMap(fieldValue, m);
//             }
//         }
//     }

//     public static void match(Collection c1, Collection c2) throws Exception {
//         Iterator i1 = c1.iterator();
//         while (i1.hasNext()) {
//             Object o1 = i1.next();
//             Iterator i2 = c2.iterator();
//             while (i2.hasNext()) {
//                 Object o2 = i2.next();
//                 if (equal(o1, o2)) {
//                     TypeUtil.setFieldValue(o2, "id", TypeUtil.getFieldValue(o1, "id"));
//                     i2.remove();
//                     break;
//                 }
//             }
//         }
//     }

//      public static boolean equal(Object o1, Object o2) {
//         Class c = o1.getClass();
//         if (!c.equals(o2.getClass())) {
//             return false;
//         }
//         try {
//             Iterator iter = getKey(c).iterator();
//             while (iter.hasNext()) {
//                 String field = (String) iter.next();
//                 String o1FieldValue = (String) TypeUtil.getFieldValue(o1, field);
//                 String o2FieldValue = (String) TypeUtil.getFieldValue(o2, field);
//             if (!o1FieldValue.equals(o2FieldValue)) {
//                 return false;
//             }
//             }
//         } catch (Exception e) {
//             return false;
//         }
//         return true;
//     }
}
