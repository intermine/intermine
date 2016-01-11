package org.intermine.metadata;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.log4j.Logger;


/**
 * Generic utility functions.
 *
 * @author Matthew Wakeling
 */
public final class Util
{

    private static final Logger LOG = Logger.getLogger(Util.class);
    /**
     * Name of the key under which to store the serialized version of the key definitions
     */
    public static final String KEY_DEFINITIONS = "keyDefs";
    /**
     * The name of the key to use to store the class_keys.properties file.
     */
    private static final String CLASS_KEYS = "class_keys";
    /**
     * Name of the key under which to store the serialized version of the model
     */
    public static final String MODEL = "model";



    private Util() {
        // don't
    }

    /**
     * Compare two objects, using their .equals method, but comparing null to null as equal.
     *
     * @param a one Object
     * @param b another Object
     * @return true if they are equal or both null
     */
    public static boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * Return a zero hashCode if the object is null, otherwise return the real hashCode
     *
     * @param obj an object
     * @return the hashCode, or zero if the object is null
     */
    public static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
    }

    /**
     * Takes an Exception, and returns an Exception of similar type with all available information
     * in the message.
     *
     * @param e an Exception
     * @return a String
     */
    public static Exception verboseException(Exception e) {
        boolean needComma = false;
        StringWriter message = new StringWriter();
        PrintWriter pMessage = new PrintWriter(message);
        Class<? extends Exception> c = e.getClass();
        while (e != null) {
            if (needComma) {
                pMessage.println("\n---------------NEXT EXCEPTION");
            }
            needComma = true;
            e.printStackTrace(pMessage);
            if (e instanceof SQLException) {
                e = ((SQLException) e).getNextException();
            } else {
                e = null;
            }
        }
        try {
            Constructor<? extends Exception> cons = c.getConstructor(new Class[] {String.class});
            Exception toThrow = cons.newInstance(new Object[] {message.toString()});
            return toThrow;
        } catch (NoSuchMethodException e2) {
            throw new RuntimeException("NoSuchMethodException thrown while handling " + c.getName()
                    + ": " + message.toString());
        } catch (InstantiationException e2) {
            throw new RuntimeException("InstantiationException thrown while handling "
                    + c.getName() + ": " + message.toString());
        } catch (IllegalAccessException e2) {
            throw new RuntimeException("IllegalAccessException thrown while handling "
                    + c.getName() + ": " + message.toString());
        } catch (InvocationTargetException e2) {
            throw new RuntimeException("InvocationTargetException thrown while handling "
                    + c.getName() + ": " + message.toString());
        }
    }

    /**
     * Takes two integers, and returns the greatest common divisor, using euclid's algorithm.
     *
     * @param a an integer
     * @param b an integer
     * @return the gcd of a and b
     */
    public static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    /**
     * Takes two integers, and returns the lowest common multiple.
     *
     * @param a an integer
     * @param b an integer
     * @return the lcm of a and b
     */
    public static int lcm(int a, int b) {
        return (a / gcd(a, b)) * b;
    }

    /**
     * Convert an SQL LIKE/NOT LIKE expression to a * wildcard expression. See
     * wildcardUserToSql method for more information.
     * @param exp  the wildcard expression
     * @return     the SQL LIKE parameter
     * @deprecated I don't think this is used anymore?
     */
    @Deprecated
    public static String wildcardSqlToUser(String exp) {
        StringBuffer sb = new StringBuffer();

        // Java needs backslashes to be backslashed in strings.
        for (int i = 0; i < exp.length(); i++) {
            String substring = exp.substring(i);
            if (substring.startsWith("%")) {
                sb.append("*");
            } else {
                if (substring.startsWith("_")) {
                    sb.append("?");
                } else {
                    if (substring.startsWith("\\%")) {
                        sb.append("%");
                        i++;
                    } else {
                        if (substring.startsWith("\\_")) {
                            sb.append("_");
                            i++;
                        } else {
                            if (substring.startsWith("*")) {
                                sb.append("\\*");
                            } else {
                                if (substring.startsWith("?")) {
                                    sb.append("\\?");
                                } else {
                                    // a single '\' as in Dpse\GA10108
                                    if (substring.startsWith("\\\\")) {
                                        i++;
                                        sb.append("\\");
                                    } else {
                                        sb.append(substring.charAt(0));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * Turn a user supplied wildcard expression with * into an SQL LIKE/NOT LIKE
     * expression with %'s and other special characters. Please note that constraint
     * value is saved in created java object (constraint) in form with '%' and in
     * this form is saved in xml as well.
     *
     * @param exp  the SQL LIKE parameter
     * @return     the equivalent wildcard expression
     */
    public static String wildcardUserToSql(String exp) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < exp.length(); i++) {
            String substring = exp.substring(i);
            if (substring.startsWith("*")) {
                sb.append("%");
            } else if (substring.startsWith("?")) {
                sb.append("_");
            } else if (substring.startsWith("\\*")) {
                sb.append("*");
                i++;
            } else if (substring.startsWith("\\?")) {
                sb.append("?");
                i++;
            } else if (substring.startsWith("%")) {
                sb.append("\\%");
            } else if (substring.startsWith("_")) {
                sb.append("\\_");
            } else if (substring.startsWith("\\")) {
                sb.append("\\\\");
            } else {
                sb.append(substring.charAt(0));
            }
        }

        return sb.toString();
    }



    /**
     * @param sequence sequence to be encoded
     * @return encoded sequence, set to lowercase
     */
    public static String getMd5checksum(String sequence) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        byte[] buffer = sequence.getBytes();
        md5.update(buffer);
        byte[] bits = md5.digest();
        StringBuilder checksum = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            checksum.append(Integer.toHexString((0x000000ff & bits[i]) | 0xffffff00).substring(6));
        }
        return checksum.toString().toLowerCase();
    }

    /**
     * Returns the class (not primitive) associated with the given String type.
     *
     * @param type the String type name
     * @return a Class
     * @throws IllegalArgumentException if the String is an invalid name
     */
    public static Class<?> getClassFromString(String type) {
        if ("short".equals(type) || "java.lang.Short".equals(type)) {
            return Short.class;
        } else if ("int".equals(type) || "java.lang.Integer".equals(type)) {
            return Integer.class;
        } else if ("long".equals(type) || "java.lang.Long".equals(type)) {
            return Long.class;
        } else if ("java.lang.String".equals(type)) {
            return String.class;
        } else if ("boolean".equals(type)) {
            return boolean.class;
        } else if ("java.lang.Boolean".equals(type)) {
            return Boolean.class;
        } else if ("float".equals(type) || "java.lang.Float".equals(type)) {
            return Float.class;
        } else if ("double".equals(type) || "java.lang.Double".equals(type)) {
            return Double.class;
        } else if ("java.util.Date".equals(type)) {
            return java.util.Date.class;
        } else if ("java.math.BigDecimal".equals(type)) {
            return java.math.BigDecimal.class;
        } else if ("org.intermine.objectstore.query.ClobAccess".equals(type)) {
            return String.class;
        } else {
            throw new IllegalArgumentException("Unknown type \"" + type + "\"");
        }
    }

    /**
     * Add values to a Map from keys to Set of values, creating the value list
     * as needed.
     * @param <K> The type of the key of the map.
     * @param <V> The type of the values in the sets of the map.
     *
     * @param map the Map
     * @param key the key
     * @param newValues the set of values
     */
    public static <K, V> void addToSetMap(Map<K, Set<V>> map, K key, Collection<V> newValues) {
        if (map == null) {
            throw new IllegalArgumentException("invalid map");
        }
        if (key == null) {
            throw new IllegalArgumentException("invalid map key");
        }
        if (newValues == null) {
            throw new IllegalArgumentException("invalid new values");
        }
        Set<V> values = map.get(key);
        if (values == null) {
            values = new HashSet<V>();
            map.put(key, values);
        }
        values.addAll(newValues);
    }


    /**
     * Add a value to a Map from keys to Set of values, creating the value list
     * as needed.
     * @param <K> The type of the key of the map.
     * @param <V> The type of the values in the sets of the map.
     *
     * @param map the Map
     * @param key the key
     * @param value the value
     */
    public static <K, V> void addToSetMap(Map<K, Set<V>> map, K key, V value) {
        if (map == null) {
            throw new IllegalArgumentException("invalid map");
        }
        if (key == null) {
            throw new IllegalArgumentException("invalid map key");
        }
        Set<V> values = map.get(key);
        if (values == null) {
            values = new HashSet<V>();
            map.put(key, values);
        }
        values.add(value);
    }

    /**
     * Add a value to a Map from keys to Set of values, creating the value list
     * as needed.
     * @param <K> The type of the key of the map.
     * @param <V> The type of the values in the lists of the map.
     *
     * @param map the Map
     * @param key the key
     * @param value the value
     */
    public static <K, V> void addToListMap(Map<K, List<V>> map, K key, V value) {
        if (map == null) {
            throw new IllegalArgumentException("invalid map");
        }
        if (key == null) {
            throw new IllegalArgumentException("invalid map key");
        }
        List<V> valuesList = map.get(key);
        if (valuesList == null) {
            valuesList = new ArrayList<V>();
            map.put(key, valuesList);
        }
        valuesList.add(value);
    }

    /**
     * Returns a list of tokens delimited by whitespace in String str (useful when handling XML)
     *
     * @param str the String to tokenize
     * @return the String tokens
     * @throws NullPointerException if  str is null
     */
    protected static List<String> tokenize(String str) {
        if (str == null) {
            throw new NullPointerException("Cannot pass null arguments to tokenize");
        }

        List<String> l = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {
            l.add(st.nextToken());
        }
        return l;
    }

    /**
     * Returns the unqualified class name from a fully qualified class name
     *
     * @param className the fully qualified class name
     * @return the unqualified name
     */
    protected static String unqualifiedName(String className) {
        if (className.lastIndexOf(".") >= 0) {
            return className.substring(className.lastIndexOf(".") + 1);
        } else {
            return className;
        }
    }

    /**
     * Returns a String formed by the delimited results of calling toString over a collection.
     *
     * @param c the collection to stringify
     * @param delimiter the character to join on
     * @return the string representation
     */
    protected static String join(Collection<?> c, String delimiter) {
        StringBuffer sb = new StringBuffer();
        boolean needComma = false;
        for (Object o : c) {
            if (needComma) {
                sb.append(delimiter);
            }
            needComma = true;
            sb.append(o.toString());
        }
        return sb.toString();
    }

    /**
     * Returns the Class for a given name (promoting primitives to their container class)
     *
     * @param type a classname
     * @return the corresponding Class
     */
    protected static Class<?> instantiate(String type) {
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
            // do nothing
        }
        return cls;
    }


    /**
     * Returns all Properties in props that begin with str
     *
     * @param str the String that the returned properties should start with
     * @param props the Properties to search through
     * @return a Properties object containing the subset of props
     */
    protected static Properties getPropertiesStartingWith(String str, Properties props) {
        if (str == null) {
            throw new NullPointerException("str cannot be null, props param: " + props);
        }
        if (props == null) {
            throw new NullPointerException("props cannot be null, str param: " + str);
        }

        Properties subset = new Properties();
        Enumeration<Object> propertyEnum = props.keys();
        while (propertyEnum.hasMoreElements()) {
            String propertyName = (String) propertyEnum.nextElement();
            if (propertyName.startsWith(str)) {
                subset.put(propertyName, props.get(propertyName));
            }
        }
        return subset;
    }

    /**
     * <p>Strips the given string off the keys of the given
     * Properties, and returns a new set of properties. The
     * original properties are not altered.<br/>
     * For example, given the property:<br/>
     * <ul><li><code>database.name=production</code></li></ul>
     * a call to <code>stripStart("database", props)</code> will produce:<br/>
     * <ul><li><code>name=production</code></li></ul>
     * Note that a dot will be added to the prefix.</p>
     *
     * @param prefix the String to strip off - a "." will be appended to this string.
     * @param props the Properties object to change
     * @return a Properties object containing the same properties with
     * the initial string + "." stripped off the keys
     */
    protected static Properties stripStart(String prefix, Properties props) {
        if (prefix == null) {
            throw new NullPointerException("prefix cannot be null");
        }
        if (props == null) {
            throw new NullPointerException("props cannot be null");
        }
        Properties ret = new Properties();
        Enumeration<Object> propertyEnum = props.keys();
        while (propertyEnum.hasMoreElements()) {
            String propertyName = (String) propertyEnum.nextElement();
            if (propertyName.startsWith(prefix + ".")) {
                ret.put(propertyName.substring(prefix.length() + 1), props.get(propertyName));
            }
        }

        return ret;
    }

    /**
     * Load a specified properties file
     * @param filename the filename of the properties file
     * @return the corresponding Properties object
     */
    protected static Properties loadProperties(String filename) {
        Properties props = new NonOverrideableProperties();
        try {
            ClassLoader loader = Util.class.getClassLoader();
            InputStream is = loader.getResourceAsStream(filename);
            if (is == null) {
                LOG.error("Could not find file " + filename + " from " + loader);
                return null;
            }
            props.load(is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load :" + filename, e);
        }
        return props;
    }

    /**
     * Given  a key and model name, return filename for reading/writing.
     * @param key key name
     * @param modelName the name of the model
     * @return name of file
     */
    protected static String getFilename(String key, String modelName) {
        String filename;
        if (modelName == null) {
            filename = key;
        } else {
            filename = modelName + "_" + key;
        }
        if (MODEL.equals(key)) {
            return filename + ".xml";
        } else if (KEY_DEFINITIONS.equals(key)
                   || CLASS_KEYS.equals(key)
                   /* || CLASS_DESCRIPTIONS.equals(key)*/) {
            return filename + ".properties";
        }
        throw new IllegalArgumentException("Unrecognised key '" + key + "'");
    }

    /**
     * Load the key definitions file for the named model from the classpath
     * @param modelName the model name
     * @return the key definitions
     */
    public static Properties loadKeyDefinitions(String modelName) {
        return loadProperties(getFilename(KEY_DEFINITIONS, modelName));
    }

    private static HashMap<Class<?>, Set<Class<?>>> decomposeMap = new HashMap<Class<?>,
            Set<Class<?>>>();
    private static Map<Class<?>, String> friendlyNameMap = new HashMap<Class<?>, String>();

    /**
     * Convert a dynamic Class into a Set of Class objects that comprise it.
     *
     * @param clazz the Class to decompose
     * @return a Set of Class objects
     */
    public static synchronized Set<Class<?>> decomposeClass(Class<?> clazz) {
        Set<Class<?>> retval = decomposeMap.get(clazz);
        if (retval == null) {
            if (net.sf.cglib.proxy.Factory.class.isAssignableFrom(clazz)) {
                // Decompose
                retval = new TreeSet<Class<?>>(new ClassNameComparator());
                retval.add(clazz.getSuperclass());
                Class<?>[] interfs = clazz.getInterfaces();
                for (int i = 0; i < interfs.length; i++) {
                    Class<?> inter = interfs[i];
                    if (net.sf.cglib.proxy.Factory.class != inter) {
                        boolean notIn = true;
                        Iterator<Class<?>> inIter = retval.iterator();
                        while (inIter.hasNext() && notIn) {
                            Class<?> in = inIter.next();
                            if (in.isAssignableFrom(inter)) {
                                // That means that the one already in the return value is more
                                // general than the one we are about to put in, so we can get rid
                                // of the one already in.
                                inIter.remove();
                            }
                            if (inter.isAssignableFrom(in)) {
                                // That means that the one already in the return value is more
                                // specific than the one we would have added, so don't bother.
                                notIn = false;
                            }
                        }
                        if (notIn) {
                            retval.add(inter);
                        }
                    }
                }
            } else if (org.intermine.model.ShadowClass.class.isAssignableFrom(clazz)) {
                try {
                    retval = new TreeSet<Class<?>>(new ClassNameComparator());
                    retval.add((Class<?>) clazz.getField("shadowOf").get(null));
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("ShadowClass " + clazz.getName() + " has no "
                            + "shadowOf method", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(clazz.getName()
                            + ".shadowOf method is inaccessible", e);
                }
            } else {
                // Normal class - return it.
                retval = new TreeSet<Class<?>>(new ClassNameComparator());
                retval.add(clazz);
            }
            decomposeMap.put(clazz, retval);
        }
        return retval;
    }

    /**
     * Creates a friendly name for a given class.
     *
     * @param clazz the class
     * @return a String describing the class, without package names
     */
    public static synchronized String getFriendlyName(Class<?> clazz) {
        String retval = friendlyNameMap.get(clazz);
        if (retval == null) {
            retval = "";
            Iterator<Class<?>> iter = decomposeClass(clazz).iterator();
            boolean needComma = false;
            while (iter.hasNext()) {
                Class<?> constit = iter.next();
                retval += needComma ? "," : "";
                needComma = true;
                retval += constit.getName().substring(constit.getName().lastIndexOf('.') + 1);
            }
            friendlyNameMap.put(clazz, retval);
        }
        return retval;
    }

    private static class ClassNameComparator implements Comparator<Class<?>>
    {
        @Override
        public int compare(Class<?> a, Class<?> b) {
            return a.getName().compareTo(b.getName());
        }
    }
}



