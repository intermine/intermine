package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;


/**
 * Non static-ified copy of the PropertiesUtils class so we can load custom Enseml Stuff in...
 *
 * @author Peter McLaren
 */
public class EnsemblPropsUtil
{

    private Properties ensemblProps;

    /**
     * Constructor
     *
     * @param ensemblProps the contents of our ensembl_config file.
     * */
    protected EnsemblPropsUtil(Properties ensemblProps) {
        this.ensemblProps = ensemblProps;
    }

    /**
     * Returns all Properties in props that begin with str
     *
     * @param str   the String that the returned properties should start with
     * @param props the Properties to search through
     * @return a Properties object containing the subset of props
     */
    public Properties getPropertiesStartingWith(String str, Properties props) {
        if (str == null) {
            throw new NullPointerException("str cannot be null");
        }
        if (props == null) {
            throw new NullPointerException("props cannot be null");
        }

        Properties subset = new Properties();
        Enumeration propertyEnum = props.keys();
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
    public Properties getPropertiesStartingWith(String str) {
        return getPropertiesStartingWith(str, ensemblProps);
    }

    /**
     * Strips the give string off the keys of the given
     * Properties. For example, database.name=production =>
     * name=production.
     *
     * @param str   the String to strip off
     * @param props the Properties object to change
     * @return a Properties object containing the same properties with
     *         the initial string stripped off the keys
     */
    public Properties stripStart(String str, Properties props) {
        if (str == null) {
            throw new NullPointerException("str cannot be null");
        }
        if (props == null) {
            throw new NullPointerException("props cannot be null");
        }
        Properties ret = new Properties();
        Enumeration propertyEnum = props.keys();
        while (propertyEnum.hasMoreElements()) {
            String propertyName = (String) propertyEnum.nextElement();
            if (propertyName.startsWith(str + ".")) {
                ret.put(propertyName.substring(str.length() + 1), props.get(propertyName));
            }
        }

        return ret;
    }

    /**
     * Serialize properties to a string suitable for a subsequent load()
     *
     * @param props the properties
     * @return the string
     * @throws IOException if an error occurs
     */
    public String serialize(Properties props) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, null);
        return baos.toString();
    }

    /**
     * Load a specified properties file
     *
     * @param filename the filename of the properties file
     * @return the corresponding Properties object
     */
    public Properties loadProperties(String filename) {
        Properties props = new Properties();
        try {
            InputStream is = org.intermine.util.PropertiesUtil.class.getClassLoader()
                    .getResourceAsStream(filename);
            if (is == null) {
                return null;
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return props;
    }
}
