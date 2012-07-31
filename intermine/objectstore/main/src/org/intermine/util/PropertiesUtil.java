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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Convenience class for working with global properties
 *
 * @author Andrew Varley
 */
public final class PropertiesUtil
{
    private static final Logger LOG = Logger.getLogger(PropertiesUtil.class);

    private PropertiesUtil() {
        // nothing to do
    }

    private static Properties globalProperties = new Properties();

    static {
        // Read Properties from the following files, if present on the classpath:
        // default.intermine.properties: Common runtime Properties
        // intermine.properties: User runtime properties
        try {
            InputStream is = PropertiesUtil.class.getClassLoader()
                .getResourceAsStream("default.intermine.properties");

            if (is != null) {
                globalProperties.load(is);
            }

            is = PropertiesUtil.class.getClassLoader().getResourceAsStream("intermine.properties");

            if (is == null) {
                throw new RuntimeException("intermine.properties is not in the classpath");
            }
            globalProperties.load(is);
            is.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * Returns all InterMine properties
     *
     * @return the global properties for InterMine
     */
    public static Properties getProperties() {
        return globalProperties;
    }

    /**
     * Returns all Properties in props that begin with str
     *
     * @param str the String that the returned properties should start with
     * @param props the Properties to search through
     * @return a Properties object containing the subset of props
     */
    public static Properties getPropertiesStartingWith(String str, Properties props) {
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
     * Returns all global Properties that begin with str
     *
     * @param str the String that the returned properties should start with
     * @return a Properties object containing the subset of the global properties
     */
    public static Properties getPropertiesStartingWith(String str) {
        return getPropertiesStartingWith(str, globalProperties);
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
    public static Properties stripStart(String prefix, Properties props) {
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
     * Serialize properties to a string suitable for a subsequent load()
     * @param props the properties
     * @return the string
     * @throws IOException if an error occurs
     */
    public static String serialize(Properties props) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);
        return baos.toString();
    }

    /**
     * Load a specified properties file
     * @param filename the filename of the properties file
     * @return the corresponding Properties object
     */
    public static Properties loadProperties(String filename) {
        Properties props = new NonOverrideableProperties();
        try {
            ClassLoader loader = PropertiesUtil.class.getClassLoader();
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
}
