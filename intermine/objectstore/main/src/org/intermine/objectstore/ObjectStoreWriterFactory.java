package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.writebatch.BatchWriter;
import org.intermine.util.PropertiesUtil;

import org.apache.log4j.Logger;

/**
 * Produce ObjectStoreWriters
 *
 * @author Mark Woodbridge
 */

public class ObjectStoreWriterFactory
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreWriterFactory.class);

    /**
     * Return an ObjectStoreWriter configured using properties file
     * @param alias identifier for properties defining integration/writer parameters
     * @return instance of a concrete ObjectStoreWriter according to property
     * @throws ObjectStoreException if anything goes wrong
     */

    public static ObjectStoreWriter getObjectStoreWriter(String alias)
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
        String osAlias = props.getProperty("os");
        if (osAlias == null) {
            throw new ObjectStoreException(alias + " does not have an os alias specified"
                                           + " (check properties file)");
        }

        ObjectStore os;
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
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
        } catch (InvocationTargetException e) {
            throw new ObjectStoreException("Failed to instantiate ObjectStoreWriter class: "
                                           + clsName + ", osAlias: " + osAlias + " - "
                                           + e.getCause().toString(), e.getCause());

        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate ObjectStoreWriter class: "
                                           + clsName + ", " + e.toString());
        }
        if ("org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl".equals(clsName)) {
            String batchWriterClass = props.getProperty("batchWriter");
            if (batchWriterClass != null) {
                try {
                    Class cls = Class.forName(batchWriterClass);
                    Constructor c = cls.getConstructor(new Class[] {});
                    BatchWriter batchWriter = (BatchWriter) c.newInstance(new Object[] {});
                    ((ObjectStoreWriterInterMineImpl) osw).setBatchWriter(batchWriter);
                } catch (Exception e) {
                    LOG.warn("Could not find requested BatchWriter " + batchWriterClass);
                }
            }
        }
        return osw;
    }
}
