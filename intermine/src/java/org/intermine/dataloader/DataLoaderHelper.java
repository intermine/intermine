package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.Model;
import org.flymine.model.datatracking.Source;
import org.flymine.util.TypeUtil;
import org.flymine.util.PropertiesUtil;

/**
 * Class providing utility methods to help with primary key and data source priority configuration
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class DataLoaderHelper
{
    protected static Map modelKeys = new HashMap();
    protected static Map sourceKeys = new HashMap();
 
    /**
     * Retrieve a map from key name to PrimaryKey object
     *
     * @param cld the ClassDescriptor to fetch primary keys for
     * @return the Map from key names to PrimaryKeys
     */
    protected static Map getPrimaryKeys(ClassDescriptor cld) {
        Map keyMap = new HashMap();
        for (Iterator i = cld.getSuperDescriptors().iterator(); i.hasNext();) {
            ClassDescriptor superCld = (ClassDescriptor) i.next();
            keyMap.putAll(getPrimaryKeys(superCld));
        }
        Properties keys = getKeyProperties(cld.getModel());
        String cldName = TypeUtil.unqualifiedName(cld.getName());
        Properties cldKeys = PropertiesUtil.getPropertiesStartingWith(cldName, keys);
        cldKeys = PropertiesUtil.stripStart(cldName, cldKeys);
        for (Iterator i = cldKeys.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String keyName = (String) entry.getKey();
            PrimaryKey key = new PrimaryKey((String) entry.getValue());
            keyMap.put(keyName, key);
        }
        return keyMap;
    }

    /**
     * Return a Set of PrimaryKeys relevant to a given Source for a ClassDescriptor
     *
     * @param cld the ClassDescriptor
     * @param source the Source
     * @return a Set of PrimaryKeys
     */
    public static Set getPrimaryKeys(ClassDescriptor cld, Source source) {
        Properties keys = getKeyProperties(source);
        Map map = getPrimaryKeys(cld);
        Set keySet = new HashSet();
        String cldName = TypeUtil.unqualifiedName(cld.getName());
        String keyList = (String) keys.get(cldName);
        for (StringTokenizer st = new StringTokenizer(keyList, ", "); st.hasMoreTokens();) {
            keySet.add(map.get(st.nextToken()));
        }
        return keySet;
    }
    
    /**
     * Return the Properties that enumerate the keys for this Source
     *
     * @param source the Source
     * @return the relevant Properties
     */
    protected static Properties getKeyProperties(Source source) {
        Properties keys = null;
        synchronized (sourceKeys) {
            keys = (Properties) sourceKeys.get(source);
            if (keys == null) {
                keys = loadProperties(source.getName() + "_keys.properties");
                sourceKeys.put(source, keys);
            }
        }
        return keys;
    } 
 
    /**
     * Return the Properties that specify the key fields for the classes in this Model
     *
     * @param model the Model
     * @return the relevant Properties
     */
    protected static Properties getKeyProperties(Model model) {
        Properties keys = null;
        synchronized (modelKeys) {
            keys = (Properties) modelKeys.get(model);
            if (keys == null) {
                keys = loadProperties(model.getName() + "_keyDefs.properties");
                modelKeys.put(model, keys);
            }
        }
        return keys;
    }

    /**
     * Load a specified properties file
     *
     * @param filename the filename of the properties file
     * @return the corresponding Properties object
     */
    protected static Properties loadProperties(String filename) {
        Properties keys = new Properties();
        try {
            InputStream is = DataLoaderHelper.class.getClassLoader()
                .getResourceAsStream(filename);
            if (is == null) {
                throw new IllegalArgumentException("Cannot find properties file " + filename);
            }
            keys.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return keys;
    }
}
