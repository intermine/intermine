/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore;

import java.lang.reflect.Method;
import java.util.Properties;

import org.flymine.util.PropertiesUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.MetaDataException;

/**
 * Produce ObjectStores
 *
 * @author Mark Woodbridge
 */
public class ObjectStoreFactory
{    
    /**
     * Return an ObjectStore configured using properties
     * @param alias the relevant prefix for the properties
     * @return a new ObjectStore
     * @throws Exception if an error occurs in instantiating the ObjectStore
     */
    public static ObjectStore getObjectStore(String alias) throws Exception {
        if (alias == null) {
            throw new NullPointerException("ObjectStore alias cannot be null");
        }
        if (alias.equals("")) {
            throw new IllegalArgumentException("ObjectStore alias cannot be empty");
        }
        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        if (0 == props.size()) {
            throw new ObjectStoreException("No ObjectStore properties were found for alias '"
                                           + alias + "'");
        }
        props = PropertiesUtil.stripStart(alias, props);
        String clsName = props.getProperty("class");
        if (clsName == null) {
            throw new ObjectStoreException(alias + " does not have an ObjectStore class specified"
                                           + " (check properties file)");
        }
        String modelName = props.getProperty("model");
        if (modelName == null) {
            throw new ObjectStoreException(alias + " does not have an model specified"
                                           + " (check properties file)");
        }
        Model model;
        try {
            model = Model.getInstanceByName(modelName);
            if (model == null) {
                throw new MetaDataException("Model is null despite load");
            }
        } catch (MetaDataException e) {
            throw new ObjectStoreException(e);
        }
        String impl = props.getProperty("alias");
        if (impl == null) {
            throw new ObjectStoreException(alias + " does not have an alias specified"
                                           + " (check properties file)");
        }
        Properties subProps = PropertiesUtil.stripStart(impl, props);
        Class cls = null;
        try {
            cls = Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified ObjectStore class '" + clsName
                                           + "' for " + alias + " (check properties file)");
        }
        Method m = cls.getDeclaredMethod("getInstance", 
                                         new Class[] {Properties.class, Model.class});
        return (ObjectStore) m.invoke(null, new Object[] {subProps, model});
    }
}
