package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.util.Properties;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;

/**
 * Produce IntegrationWriters
 *
 * @author Mark Woodbridge
 */

public final class IntegrationWriterFactory
{
    private IntegrationWriterFactory() {
    }

    /**
     * Return an IntegrationWriter configured using properties file
     * @param alias identifier for properties defining integration/writer parameters
     * @return instance of a concrete IntegrationWriter according to property
     * @throws ObjectStoreException if anything goes wrong
     */

    public static IntegrationWriter getIntegrationWriter(String alias)
        throws ObjectStoreException {
        if (alias == null) {
            throw new NullPointerException("Integration alias cannot be null");
        }
        if ("".equals(alias)) {
            throw new IllegalArgumentException("Integration alias cannot be empty");
        }
        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        if (props.size() == 0) {
            throw new ObjectStoreException("No Integration properties were found for '"
                                           + alias + "'");
        }
        props = PropertiesUtil.stripStart(alias, props);
        String integrationWriterClassName = props.getProperty("class");
        if (integrationWriterClassName == null) {
            throw new ObjectStoreException(alias + " does not have an integration class specified"
                                           + " (check properties file)");
        }

        // now build IntegrationWriter using datasource name and ObjectStoreWriter

        props.setProperty("alias", alias);
        IntegrationWriter iw = null;
        try {
            Class<?> integrationWriterClass = Class.forName(integrationWriterClassName);
            Class<?>[] parameterTypes = new Class[] {String.class, Properties.class};
            Method m = integrationWriterClass.getMethod("getInstance", parameterTypes);
            iw = (IntegrationWriter) m.invoke(null, new Object[] {alias, props});
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified IntegrationWriter class '"
                                           + integrationWriterClassName
                                           + "' for " + alias + " (check properties file)", e);
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find getInstance method for "
                                           + "IntegrationWriter: " + integrationWriterClassName
                                           + " - check properties file", e);
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate IntegrationWriter "
                                           + "class: " + integrationWriterClassName, e);
        }
        return iw;
    }
}
