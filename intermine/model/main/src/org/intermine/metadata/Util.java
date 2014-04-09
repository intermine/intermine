package org.intermine.metadata;
/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * Copies of utility functions found elsewhere needed by the the model code.
 *
 * @author Julie Sullivan
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
    protected static boolean equals(Object a, Object b) {
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
    protected static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
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

}

/**
 * Subclass of java.util.Properties that rejects duplicate definitions of a given property.
 *
 * @author Matthew Wakeling
 */
class NonOverrideableProperties extends Properties
{
    /**
     * Empty constructor.
     */
    public NonOverrideableProperties() {
        super();
    }

    /**
     * Constructor with defaults.
     *
     * @param p default properties
     */
    public NonOverrideableProperties(Properties p) {
        super(p);
    }

    /**
     * Override put, but do not allow existing values to be changed.
     *
     * {@inheritDoc}
     */
    @Override
    public Object put(Object key, Object value) {
        Object old = get(key);
        if ((old != null) && (!old.equals(value))) {
            throw new IllegalArgumentException("Cannot override non-overrideable property " + key
                    + " = " + old + " with new value " + value);
        }
        return super.put(key, value);
    }
}
