package org.intermine.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;

/**
 * Provides utility methods for working with Java types and reflection
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public final class TypeUtil
{
    private TypeUtil() {
        // empty
    }

    private static Map<Class<?>, Map<String, FieldInfo>> classToFieldnameToFieldInfo
        = new HashMap<Class<?>, Map<String, FieldInfo>>();

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
     * Sets the value of a public or protected Field of an Object given the field name.
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
                    + DynamicUtil.getFriendlyName(o.getClass()) + "." + fieldName + "\""
                    + (type == null ? "" : " (a " + type + ")")
                    + " to \"" + fieldValue + "\" (a " + fieldValue.getClass().getName() + ")");
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Adds an element to a public or protected collection of an Object given the field name.
     *
     * @param o the Object
     * @param fieldName the name of the relevant collection
     * @param element the element to add to the collection
     */
    public static void addCollectionElement(Object o, String fieldName, Object element) {
        try {
            getAdder(o.getClass(), fieldName).invoke(o, element);
        } catch (Exception e) {
            String type = null;
            try {
                type = getFieldInfo(o.getClass(), fieldName).getElementType().getName();
            } catch (Exception e3) {
                IllegalArgumentException e2 = new IllegalArgumentException("Couldn't add element to"
                        + " collection \"" + DynamicUtil.getFriendlyName(o.getClass()) + "."
                        + fieldName + "\"" + " - not an accessible collection");
                e2.initCause(e);
                throw e2;
            }
            IllegalArgumentException e2 = new IllegalArgumentException("Couldn't add element to"
                    + " collection \"" + DynamicUtil.getFriendlyName(o.getClass()) + "."
                    + fieldName + "\"" + " (a " + type + ") with \"" + element + "\" (a "
                    + element.getClass().getName() + ")");
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Returns the Method object that is the getter for the field name.
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the Getter, or null if the field is not found
     */
    public static Method getGetter(Class<?> c, String fieldName) {
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
    public static Method getSetter(Class<?> c, String fieldName) {
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
    public static Method getProxySetter(Class<?> c, String fieldName) {
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
    public static Method getProxyGetter(Class<?> c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getProxyGetter();
        }
        return null;
    }

    /**
     * Returns the Method object that is the adder for the field name
     *
     * @param c the Class
     * @param fieldName the name of the relevant collection
     * @return the adder, or null if it is not present or if the field is not found
     */
    public static Method getAdder(Class<?> c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getAdder();
        }
        return null;
    }

    /**
     * Returns the type of a field given the field name.
     *
     * @param c the Class
     * @param fieldName the name of the relevant field
     * @return the class of the field, or null if the field is not found
     */
    public static Class<?> getFieldType(Class<?> c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            return info.getType();
        }
        return null;
    }

    /**
     * Returns the element type of a collection given the field name.
     *
     * @param c the Class
     * @param fieldName the name of the relevant collection
     * @return the class of the field, or null if the field is not found
     * @throws IllegalArgumentException if the field is not a collection
     */
    public static Class<? extends FastPathObject> getElementType(Class<?> c, String fieldName) {
        FieldInfo info = getFieldInfo(c, fieldName);
        if (info != null) {
            try {
                return info.getElementType();
            } catch (NullPointerException e) {
                IllegalArgumentException e2 = new IllegalArgumentException("Field "
                        + DynamicUtil.getFriendlyName(c) + "." + fieldName
                        + " is not a collection");
                e2.initCause(e);
                throw e2;
            }
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
    public static Map<String, FieldInfo> getFieldInfos(Class<?> c) {
        Map<String, FieldInfo> infos = null;
        synchronized (classToFieldnameToFieldInfo) {
            infos = classToFieldnameToFieldInfo.get(c);

            if (infos == null) {
                infos = new TreeMap<String, FieldInfo>();

                Map<String, Method> methods = new HashMap<String, Method>();
                Method[] methodArray = c.getMethods();

                for (int i = 0; i < methodArray.length; i++) {
                    String methodName = methodArray[i].getName();
                    methods.put(methodName, methodArray[i]);
                }

                for (String getterName : methods.keySet()) {
                    if (getterName.startsWith("get")) {
                        String setterName = "set" + getterName.substring(3);
                        String proxySetterName = "proxy" + getterName.substring(3);
                        String proxyGetterName = "proxGet" + getterName.substring(3);
                        String adderName = "add" + getterName.substring(3);
                        if (methods.containsKey(setterName)) {
                            Method getter = methods.get(getterName);
                            Method setter = methods.get(setterName);
                            Method proxySetter = methods.get(proxySetterName);
                            Method proxyGetter = methods.get(proxyGetterName);
                            Method adder = methods.get(adderName);
                            String fieldName = getterName.substring(3);
                            fieldName = StringUtil.reverseCapitalisation(fieldName).intern();

                            // cglib Factory interface has getCallBack() and getCallBacks() methods
                            if ((!"getClass".equals(getter.getName()))
                                    && (!"getCallback".equals(getter.getName()))
                                    && (!"getCallbacks".equals(getter.getName()))
                                    && (!"getoBJECT".equals(getter.getName()))
                                    && (!"getFieldValue".equals(getter.getName()))
                                    && (!"getFieldProxy".equals(getter.getName()))
                                    && (!"getFieldType".equals(getter.getName()))
                                    && (!"getElementType".equals(getter.getName()))) {
                                FieldInfo info = new FieldInfo(fieldName, getter, setter,
                                        proxySetter, proxyGetter, adder);
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
    public static FieldInfo getFieldInfo(Class<?> c, String fieldname) {
        return getFieldInfos(c).get(fieldname);
    }

    /**
     * Gets the getter methods for the bean properties of a class
     *
     * @param c the Class
     * @return an array of the getter methods
     * @throws IntrospectionException if an error occurs
     */
    public static Method[] getGetters(Class<?> c) throws IntrospectionException {
        PropertyDescriptor[] pd = Introspector.getBeanInfo(c).getPropertyDescriptors();
        Collection<Method> getters = new HashSet<Method>();
        for (int i = 0; i < pd.length; i++) {
            Method getter = pd[i].getReadMethod();
            if ((!"getClass".equals(getter.getName()))
                    && (!"getoBJECT".equals(getter.getName()))
                    && (!"getCallback".equals(getter.getName()))
                    && (!"getCallbacks".equals(getter.getName()))
                    && (!"getFieldValue".equals(getter.getName()))
                    && (!"getFieldProxy".equals(getter.getName()))
                    && (!"getFieldType".equals(getter.getName()))
                    && (!"getElementType".equals(getter.getName()))) {
                getters.add(getter);
            }
        }
        return getters.toArray(new Method[] {});
    }

    /**
     * Make all nested objects top-level in returned collection
     *
     * @param obj a top-level object or collection of such objects
     * @return a set of objects
     * @throws Exception if a problem occurred during flattening
     */
    public static List<Object> flatten(Object obj) throws Exception {
        Collection<?> c;
        if (obj instanceof Collection<?>) {
            c = (Collection<?>) obj;
        } else {
            c = Arrays.asList(new Object[] {obj});
        }
        try {
            List<Object> toStore = new ArrayList<Object>();
            for (Object i : c) {
                flatten(i, toStore);
            }
            return toStore;
        } catch (Exception e) {
            throw new Exception("Problem occurred flattening object", e);
        }
    }

    private static void flatten(Object o, Collection<Object> c) throws Exception {
        if (o == null || c.contains(o)) {
            return;
        }
        c.add(o);
        Method[] getters = TypeUtil.getGetters(o.getClass());
        for (int i = 0; i < getters.length; i++) {
            Method getter = getters[i];
            Class<?> returnType = getter.getReturnType();
            if (Collection.class.isAssignableFrom(returnType)) {
                for (Object obj : (Collection<?>) getter.invoke(o, new Object[] {})) {
                    flatten(obj, c);
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
    public static Class<?> instantiate(String type) {
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
        Class<?> cls = null;
        try {
            cls = Class.forName(type);
        } catch (Exception e) {
        }
        return cls;
    }

    private static final DateFormat DATE_TIME_FORMAT;
    private static final DateFormat DATE_FORMAT;

    static {
        DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Returns an object for a given String
     *
     * @param clazz the class to convert to
     * @param value the value to convert
     * @return the corresponding Class
     */
    public static Object stringToObject(Class<?> clazz, String value) {
        if (clazz.equals(Integer.class) || clazz.equals(Integer.TYPE)) {
            return Integer.valueOf(value.replace(",", ""));
        }
        if (clazz.equals(Boolean.class) || clazz.equals(Boolean.TYPE)) {
            if ("NULL".equals(value)) {
                return "NULL";
            } else {
                return Boolean.valueOf(value);
            }
        }
        if (clazz.equals(Double.class) || clazz.equals(Double.TYPE)) {
            return Double.valueOf(value.replace(",", ""));
        }
        if (clazz.equals(Float.class) || clazz.equals(Float.TYPE)) {
            return Float.valueOf(value.replace(",", ""));
        }
        if (clazz.equals(Long.class)  || clazz.equals(Long.TYPE)) {
            return Long.valueOf(value.replace(",", ""));
        }
        if (clazz.equals(Short.class) || clazz.equals(Short.TYPE)) {
            return Short.valueOf(value.replace(",", ""));
        }
        if (clazz.equals(Byte.class) || clazz.equals(Byte.TYPE)) {
            return Byte.valueOf(value.replace(",", ""));
        }
        if (clazz.equals(Character.class) || clazz.equals(Character.TYPE)) {
            return new Character(value.charAt(0));
        }
        if (clazz.equals(Date.class)) {
            if (value.matches("^\\d+$")) {
                return new Date(Long.parseLong(value));
            } else {
                try {
                    return DATE_TIME_FORMAT.parse(value);
                } catch (Exception e) {
                    // probably ParseException, try a simpler format
                    try {
                        return DATE_FORMAT.parse(value);
                    } catch (Exception e1) {
                        throw new RuntimeException("Failed to parse " + value + " as a Date", e);
                    }
                }
            }
        }
        if (clazz.equals(BigDecimal.class)) {
            return new BigDecimal(value.replace(",", ""));
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
        if (clazz.equals(ClobAccess.class)) {
        //    String[] parts = value.split(",");
        //    if (parts.length == 1) {
        //        return new ClobAccess(os, new Clob(Integer.parseInt(parts[0])));
        //    } else {
        //        return new ClobAccess(os, new Clob(Integer.parseInt(parts[0])))
        //            .subSequence(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        //    }
            throw new IllegalStateException("Cannot convert - we need an ObjectStore");
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
        } else if (value instanceof ClobAccess) {
            return ((ClobAccess) value).getDbDescription();
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
     * Generate the full class name, eg. org.intermine.bio.SequenceFeature from a SO term and a
     * package name.
     *
     * @param packageName namespace, eg. org.intermine.bio
     * @param className so term name, eg. sequence_feature
     * @return full name of class, eg. org.intermine.bio.SequenceFeature
     */
    public static String generateClassName(String packageName, String className) {
        return packageName + "." + javaiseClassName(className);
    }

    /**
     * Return true if and only if the object is an instance of the class given by the className.
     * @param object the object to test
     * @param className the super class name to test for
     * @return true if object is an instance of className
     * @exception ClassNotFoundException if the class given by className cannot be located
     */
    public static boolean isInstanceOf(FastPathObject object, String className)
        throws ClassNotFoundException {
        Set<Class<?>> classes = DynamicUtil.decomposeClass(object.getClass());
        Class<?> testClass = Class.forName(className);
        for (Class<?> objectClass: classes) {
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
        private Method adder;

        /**
         * Construct a new FieldInfo object.
         *
         * @param name the field name
         * @param getter the getter Method to retrieve the value
         * @param setter the setter Method to alter the value
         * @param proxySetter the setter Method to set the value to a ProxyReference
         * @param proxyGetter the getter Method to get the value without dereferencing
         * ProxyReferences
         * @param adder the adder Method to add elements to a collection
         */
        public FieldInfo(String name, Method getter, Method setter, Method proxySetter,
                Method proxyGetter, Method adder) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.proxySetter = proxySetter;
            this.proxyGetter = proxyGetter;
            this.adder = adder;
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
         * Returns the adder Method.
         *
         * @return an adder Method
         */
        public Method getAdder() {
            return adder;
        }

        /**
         * Returns the type of the field.
         *
         * @return a Class object
         */
        public Class<?> getType() {
            return getter.getReturnType();
        }

        /**
         * Returns the collection element type of the field.
         *
         * @return a Class
         */
        public Class<? extends FastPathObject> getElementType() {
            @SuppressWarnings("unchecked") Class<? extends FastPathObject> retval =
                (Class) adder.getParameterTypes()[0];
            return retval;
        }
    }

    /**
     * Instantiate a class by unqualified name
     * The name should be "Date" or that of a primitive container class such as "Integer"
     * @param className the name of the class
     * @return the relevant Class
     */
    public static Class<?> getClass(String className) {
        Class<?> cls = instantiate(className);
        if (cls == null) {
            if ("Date".equals(className)) {
                cls = Date.class;
            } else {
                if ("BigDecimal".equals(className)) {
                    cls = BigDecimal.class;
                } else {
                    try {
                        cls = Class.forName("java.lang." + className);
                    } catch (Exception e) {
                        throw new RuntimeException("unknown class: " + className);
                    }
                }
            }
        }
        return cls;
    }

    /**
     * Instantiate a class by unqualified name
     * The name should be "InterMineObject" or the name of class in the model provided
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the relevant Class
     * @throws ClassNotFoundException if the class name is not in the model
     */
    public static Class<?> getClass(String className, Model model)
        throws ClassNotFoundException {
        if ("InterMineObject".equals(className)) {
            className = "org.intermine.model.InterMineObject";
        } else {
            className = model.getPackageName() + "." + className;
        }
        return Class.forName(className);
    }

    /**
     * Filter a string to remove illegal characters and join the rest in lower case.
     *
     * @param s e.g. modMine_TEST-2.r
     * @return a string with no special character such as space, "_" or others, e.g. modminetest2r
     */
    public static String javaisePackageName(String s) {

        String normalRegex = "[A-Za-z0-9]*";
        String illRegex = "[. _#$%&()*+,\"'/:;<=>?@\\^`{|}~-]";

        if (Pattern.matches(normalRegex, s)) {
            return s.toLowerCase();
        } else {
            String[] splitedStr = s.split(illRegex);
            StringBuffer sb = new StringBuffer();
            for (String str : splitedStr) {
                sb.append(str.toLowerCase());
            }
            return sb.toString();
        }
    }
}
