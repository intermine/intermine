package org.flymine.util;

import java.util.Properties;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;

/**
 * Convenience class for working with global properties
 *
 * @author Andrew Varley
 */
public class PropertiesUtil
{
    private PropertiesUtil() {
    }

    private static Properties globalProperties = new Properties();

    static {
        // Read Properties from the following files, if present on the classpath:
        // flymine.properties: Runtime Properties
        // flymine-build.properties: Buildtime properties
        try {
            InputStream is = PropertiesUtil.class.getClassLoader()
                .getResourceAsStream("flymine.properties");

            if (is != null) {
                globalProperties.load(is);
            }

        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * Returns all FlyMine properties
     *
     * @return the global properties for FlyMine
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
            throw new NullPointerException("str cannot be null");
        }
        if (props == null) {
            throw new NullPointerException("props cannot be null");
        }

        Properties subset = new Properties();
        Enumeration enum = props.keys();
        while (enum.hasMoreElements()) {
            String propertyName = (String) enum.nextElement();
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
     * Strips the give string off the keys of the given
     * Properties. For example, database.name=production =>
     * name=production.
     *
     * @param str the String to strip off
     * @param props the Properties object to change
     * @return a Properties object containing the same properties with
     * the initial string stripped off the keys
     */
    public static Properties stripStart(String str, Properties props) {
        if (str == null) {
            throw new NullPointerException("str cannot be null");
        }
        if (props == null) {
            throw new NullPointerException("props cannot be null");
        }
        Properties ret = new Properties();
        Enumeration enum = props.keys();
        while (enum.hasMoreElements()) {
            String propertyName = (String) enum.nextElement();
            if (propertyName.startsWith(str + ".")) {
                ret.put(propertyName.substring(str.length() + 1), props.get(propertyName));
            }
        }

        return ret;
    }


}
