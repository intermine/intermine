package org.flymine.objectstore;

import java.lang.reflect.Method;
import java.util.Properties;

import org.flymine.util.PropertiesUtil;

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
        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        props = PropertiesUtil.stripStart(alias, props);
        String clsName = props.getProperty("class");
        String impl = props.getProperty("alias");
        Properties subProps = PropertiesUtil.stripStart(impl, props);
        Class cls = null;
        try {
            cls = Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot not find ObjectStore class '" + clsName
                                           + "' (check properties file)");
        }
        Method m = cls.getDeclaredMethod("getInstance", new Class[] {Properties.class});
        return (ObjectStore) m.invoke(null, new Object[] {subProps});
    }
}
