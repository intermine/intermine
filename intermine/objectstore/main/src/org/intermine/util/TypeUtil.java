package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;

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

    private static Map<Class, Map<String, FieldInfo>> classToFieldnameToFieldInfo
        = new HashMap<Class, Map<String, FieldInfo>>();

    /**
     * Returns the package name from a fully qualified class name
     *
     * @param className the fully qualified class name
     * @return the package name
     */
    public static String packageName(String className) {
        if (className.lastIndexOf(".") >= 0) {
            return className.substring(0, className.lastIndexOf("."));
        } else {
            return "";
        }
    }

    /**
     * Returns the unqualified class name from a fully qualified class name
     *
     * @param className the fully qualified class name
     * @return the unqualified name
     */
    public static String unqualifiedName(String className) {
        if (className.lastIndexOf(".") >= 0) {
            return className.substring(className.lastIndexOf(".") + 1);
        } else {
            return className;
        }
    }

    /**
     * Returns the value of a public or protected Field of an Object given the field name
     *
     * @param o the Object
     * @param fieldName the name of the relevant Field
     * @return the value of the Field
     * @throws IllegalAccessException if the field is inaccessible
     */
    public static Object getFieldValue(Object o, String fieldName)
        throws IllegalAccessException {
        try {
            return getGetter(o.getClass(), fieldName).invoke(o, new Object[] {});
        } catch (Exception e) {
            String type = "";
            try {
                type = " (a " + getFieldInfo(o.getClass(), fieldName).getGetter().getReturnType()
                    .getName() + ")";
            } catch (Exception e3) {
                type = " (available fields are " + getFieldInfos(o.getClass()).keySet() + ")";
            }
            IllegalAccessException e2 = new IllegalAccessException("Couldn't get field \""
                    + DynamicUtil.decomposeClass(o.getClass()) + "." + fieldName + "\""
                    + type);
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Returns the value of a public or protected Field of an Object given by the field name without
     * dereferencing any ProxyReference objects.
     *
     * @param o the Object
     * @param fieldName the name of the relevant Field
     * @return the value of the field, without dereferencing ProxyReferences
     * @throws IllegalAccessException if the field is inaccessible
     */
    public static Object getFieldProxy(Object o, String fieldName) throws IllegalAccessException {
        try {
            Method proxyGetter = getProxyGetter(o.getClass(), fieldName);
            if (proxyGetter == null) {
                proxyGetter = getGetter(o.getClass(), fieldName);
            }
            return proxyGetter.invoke(o, new Object[] {});
        } catch (Exception e) {
            String type = null;
            try {
                type = getFieldInfo(o.getClass(), fieldName).getGetter().getReturnType().getName();
            } catch (Exception e3) {
                // ignore
            }
            IllegalAccessException e2 = new IllegalAccessException("Couldn't proxyGet field \""
                    + o.getClass().getName() + "." + fieldName + "\""
                    + (type == null ? "" : " (a " + type + ")"));
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Sets the value of a public or protected Field of an Object given the field name
     *
     * @param o the Object
     * @param fieldName the name of the relevant Field
     * @param fieldValue the value of the Field
     */
    public static void setFieldValue(Object o, String fieldName, Object fieldValue) {
        try {
            if (fieldValue instanceof ProxyReference) {
                getProxySetter(o.getClass(), fieldName).invoke(o, new Object[] {fieldValue});
            } else {
                getSetter(o.getClass(), fieldName).invoke(o, new Object[] {fieldValue});
            }
        } catch (Exception e) {
            String type = null;
            try {
                type = getFieldInfo(o.getClass(), fieldName).getGetter().getReturnType().getName();
            } catch (Exception e3) {
                // ignore
            }
            IllegalArgumentException e2 = new IllegalArgumentException("Couldn't set field \""
                    + DynamicUtil.decomposeClass(o.getClass()) + "." + fieldName + "\""
                    + (type == null ? "" : " (a " + type + ")")
                    + " to \"" + fieldValue + "\" (a " + fieldValue.getClass().getName() + ")");
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Returns the Method object that is the getter for the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the Getter, or null if the field is not found
     */
    public static Method getGetter(Class c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getGetter();
        }
        return null;
    }

    /**
     * Returns the Method object that is the setter for the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the setter, or null if the field is not found
     */
    public static Method getSetter(Class c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getSetter();
        }
        return null;
    }

    /**
     * Returns the Method object that is the proxySetter for the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the proxySetter, or null if it is not present or the field is not found
     */
    public static Method getProxySetter(Class c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getProxySetter();
        }
        return null;
    }

    /**
     * Returns the Method object that is the proxyGetter for the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the proxyGetter, or null if it is not present or the field is not found
     */
    public static Method getProxyGetter(Class c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getProxyGetter();
        }
        return null;
    }

    /**
     * Returns the Map from field name to TypeUtil.FieldInfo objects for all the fields in a
     * given class.
     *
     * @param c the Class
     * @return a Map from field name to FieldInfo object
     */
    public static Map<String, FieldInfo> getFieldInfos(Class c) {
        Map<String, FieldInfo> infos = null;
        synchronized (classToFieldnameToFieldInfo) {
            infos = classToFieldnameToFieldInfo.get(c);

            if (infos == null) {
                infos = new TreeMap();

                Map<String, Method> methods = new HashMap();
                Method methodArray[] = c.getMethods();
                for (int i = 0; i < methodArray.length; i++) {
                    String methodName = methodArray[i].getName();
                    methods.put(methodName, methodArray[i]);
                }

                for (String getterName : methods.keySet()) {
                    if (getterName.startsWith("get")) {
                        String setterName = "set" + getterName.substring(3);
                        String proxySetterName = "proxy" + getterName.substring(3);
                        String proxyGetterName = "proxGet" + getterName.substring(3);
                        if (methods.containsKey(setterName)) {
                            Method getter = methods.get(getterName);
                            Method setter = methods.get(setterName);
                            Method proxySetter = methods.get(proxySetterName);
                            Method proxyGetter = methods.get(proxyGetterName);
                            String fieldName = getterName.substring(3);
                            fieldName = StringUtil.reverseCapitalisation(fieldName);
                            
                            // cglib Factory interface has getCallBack() and getCallBacks() methods
                            if (!getter.getName().equals("getClass")
                                && !getter.getName().startsWith("getCallback")) {
                                FieldInfo info = new FieldInfo(fieldName, getter, setter,
                                        proxySetter, proxyGetter);
                                infos.put(fieldName, info);
                            }
                        }
                    }
                }

                classToFieldnameToFieldInfo.put(c, infos);
            }
        }
        return infos;
    }

    /**
     * Returns a FieldInfo object for the given class and field name.
     *
     * @param c the Class
     * @param fieldname the fieldname
     * @return a FieldInfo object, or null if the fieldname is not found
     */
    public static FieldInfo getFieldInfo(Class c, String fieldname) {
        return getFieldInfos(c).get(fieldname);
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
     * Make all nested objects top-level in returned collection
     *
     * @param obj a top-level object or collection of such objects
     * @return a set of objects
     * @throws Exception if a problem occurred during flattening
     */
    public static List flatten(Object obj) throws Exception {
        Collection c;
        if (obj instanceof Collection) {
            c = (Collection) obj;
        } else {
            c = Arrays.asList(new Object[] {obj});
        }
        try {
            List toStore = new ArrayList();
            Iterator i = c.iterator();
            while (i.hasNext()) {
                flatten(i.next(), toStore);
            }
            return toStore;
        } catch (Exception e) {
            throw new Exception("Problem occurred flattening object", e);
        }
    }

    private static void flatten(Object o, Collection c) throws Exception {
        if (o == null || c.contains(o)) {
            return;
        }
        c.add(o);
        Method[] getters = TypeUtil.getGetters(o.getClass());
        for (int i = 0; i < getters.length; i++) {
            Method getter = getters[i];
            Class returnType = getter.getReturnType();
            if (Collection.class.isAssignableFrom(returnType)) {
                Iterator iter = ((Collection) getter.invoke(o, new Object[] {})).iterator();
                while (iter.hasNext()) {
                    flatten(iter.next(), c);
                }
            } else if (!returnType.isPrimitive() && !returnType.getName().startsWith("java")) {
                flatten(getter.invoke(o, new Object[] {}), c);
            }
        }
    }

    /**
     * Returns the Class for a given name (promoting primitives to their container class)
     *
     * @param type a classname
     * @return the corresponding Class
     */
    public static Class instantiate(String type) {
        if (type.equals(Integer.TYPE.toString())) {
            return Integer.class;
        }
        if (type.equals(Boolean.TYPE.toString())) {
            return Boolean.class;
        }
        if (type.equals(Double.TYPE.toString())) {
            return Double.class;
        }
        if (type.equals(Float.TYPE.toString())) {
            return Float.class;
        }
        if (type.equals(Long.TYPE.toString())) {
            return Long.class;
        }
        if (type.equals(Short.TYPE.toString())) {
            return Short.class;
        }
        if (type.equals(Byte.TYPE.toString())) {
            return Byte.class;
        }
        if (type.equals(Character.TYPE.toString())) {
            return Character.class;
        }
        Class cls = null;
        try {
            cls = Class.forName(type);
        } catch (Exception e) {
        }
        return cls;
    }


    /**
     * Returns an object for a given String
     *
     * @param clazz the class to convert to
     * @param value the value to convert
     * @return the corresponding Class
     */
    public static Object stringToObject(Class clazz, String value) {
        if (clazz.equals(Integer.class) || clazz.equals(Integer.TYPE)) {
            return Integer.valueOf(value);
        }
        if (clazz.equals(Boolean.class) || clazz.equals(Boolean.TYPE)) {
            if (value.equals("NULL")) {
                return "NULL";
            } else {
                return Boolean.valueOf(value);
            }
        }
        if (clazz.equals(Double.class) || clazz.equals(Double.TYPE)) {
            return Double.valueOf(value);
        }
        if (clazz.equals(Float.class) || clazz.equals(Float.TYPE)) {
            return Float.valueOf(value);
        }
        if (clazz.equals(Long.class)  || clazz.equals(Long.TYPE)) {
            return Long.valueOf(value);
        }
        if (clazz.equals(Short.class) || clazz.equals(Short.TYPE)) {
            return Short.valueOf(value);
        }
        if (clazz.equals(Byte.class) || clazz.equals(Byte.TYPE)) {
            return Byte.valueOf(value);
        }
        if (clazz.equals(Character.class) || clazz.equals(Character.TYPE)) {
            return new Character(value.charAt(0));
        }
        if (clazz.equals(Date.class)) {
            return new Date(Long.parseLong(value));
        }
        if (clazz.equals(BigDecimal.class)) {
            return new BigDecimal(value);
        }
        if (clazz.equals(String.class)) {
            return value;
        }
        if (clazz.equals(URL.class)) {
            try {
                return new URL(value);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }

    /**
     * Returns a String for a given object
     *
     * @param value the value to convert
     * @return the string representation
     */
    public static String objectToString(Object value) {
        if (value instanceof Date) {
            return "" + ((Date) value).getTime();
        } else {
            return value.toString();
        }
    }

    /**
     * Filter a URI fragment to remove illegal characters
     * @param s the relevant string
     * @return the filtered string
     */
    public static String javaiseClassName(String s) {
        String filtered = s;
         StringBuffer sb = new StringBuffer();
         for (StringTokenizer st = new StringTokenizer(filtered, " _-"); st.hasMoreTokens();) {
             sb.append(StringUtil.capitalise(st.nextToken().replaceAll("\\W", "")));
         }
         filtered = sb.toString();
         return filtered;
    }

    /**
     * Return true if and only if the object is an instance of the class given by the className.
     * @param object the object to test
     * @param className the super class name to test for
     * @return true if object is an instance of className
     * @exception ClassNotFoundException if the class given by className cannot be located
     */
    public static boolean isInstanceOf(InterMineObject object, String className)
        throws ClassNotFoundException {
        Set<Class> classes = DynamicUtil.decomposeClass(object.getClass());
        for (Class objectClass: classes) {
            Class testClass = Class.forName(className);
            if (testClass.isAssignableFrom(objectClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inner class to hold info on a field.
     *
     * @author Matthew Wakeling
     * @author Andrew Varley
     */
    public static class FieldInfo
    {
        private String name;
        private Method getter;
        private Method setter;
        private Method proxySetter;
        private Method proxyGetter;

        /**
         * Construct a new FieldInfo object.
         *
         * @param name the field name
         * @param getter the getter Method to retrieve the value
         * @param setter the setter Method to alter the value
         * @param proxySetter the setter Method to set the value to a ProxyReference
         * @param proxyGetter the getter Method to get the value without dereferencing
         * ProxyReferences
         */
        public FieldInfo(String name, Method getter, Method setter, Method proxySetter,
                Method proxyGetter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.proxySetter = proxySetter;
            this.proxyGetter = proxyGetter;
        }

        /**
         * Returns the field name
         *
         * @return a String
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the getter Method.
         *
         * @return a getter Method
         */
        public Method getGetter() {
            return getter;
        }

        /**
         * Returns the setter Method.
         *
         * @return a setter Method
         */
        public Method getSetter() {
            return setter;
        }

        /**
         * Returns the proxySetter Method.
         *
         * @return a proxySetter Method
         */
        public Method getProxySetter() {
            return proxySetter;
        }

        /**
         * Returns the proxyGetter Method.
         *
         * @return a proxyGetter Method
         */
        public Method getProxyGetter() {
            return proxyGetter;
        }

        /**
         * Returns the type of the field.
         *
         * @return a Class object
         */
        public Class getType() {
            return getter.getReturnType();
        }
    }
}
