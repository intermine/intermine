package org.flymine.objectstore;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.flymine.util.PropertiesUtil;

/**
 * Produce ObjectStoreWriters
 *
 * @author Mark Woodbridge
 */

public class ObjectStoreWriterFactory
{
    /**
     * Return an ObjectStoreWriter configured using properties file
     * @param alias identifier for properties defining integration/writer parameters
     * @param os the ObjectStore used by the ObjectStoreWriter
     * @return instance of a concrete IntegrationWriter according to property
     * @throws ObjectStoreException if anything goes wrong
     */

    public static ObjectStoreWriter getObjectStoreWriter(String alias, ObjectStore os)
        throws ObjectStoreException {
        if (alias == null) {
            throw new NullPointerException("ObjectStoreWriter alias cannot be null");
        }
        if (alias.equals("")) {
            throw new IllegalArgumentException("ObjectStoreWriter alias cannot be empty");
        }
        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        if (props.size() == 0) {
            throw new ObjectStoreException("No ObjectStoreWriter properties were found for '"
                                           + alias + "'");
        }
        props = PropertiesUtil.stripStart(alias, props);
        String clsName = props.getProperty("class");
        if (clsName == null) {
            throw new ObjectStoreException(alias + " does not have an ObjectStoreWriter class "
                                           + "specified (check properties file)");
        }

        ObjectStoreWriter osw = null;
        try {
            Class cls = Class.forName(clsName);
            Constructor c = cls.getConstructor(new Class[] {ObjectStore.class});
            osw = (ObjectStoreWriter) c.newInstance(new Object[] {os});
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified ObjectStoreWriter class '"
                                           + clsName + "' for " + alias
                                           + " (check properties file) " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find appropriate constructor for "
                                           + "ObjectStoreWriter: " + clsName
                                           + " (ObjectStore.class)"
                                           + " - check properties file, " + e.getMessage());
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate ObjectStoreWriter class: "
                                           + clsName + ", " + e.toString());
        }
        return osw;
    }
}
