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

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.PropertiesUtil;

/**
 * Produce IntegrationWriters
 *
 * @author Mark Woodbridge
 */

public class IntegrationWriterFactory
{
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
        if (alias.equals("")) {
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
        String writerAlias = props.getProperty("osw");
        if (writerAlias == null) {
            throw new ObjectStoreException(alias + " does not have an osw alias specified"
                                           + " (check properties file)");
        }

        ObjectStoreWriter writer = ObjectStoreWriterFactory.getObjectStoreWriter(writerAlias);

        // now build IntegrationWriter using datasource name and ObjectStoreWriter

        IntegrationWriterAbstractImpl iw = null;
        try {
            Class integrationWriterClass = Class.forName(integrationWriterClassName);
            Constructor c = integrationWriterClass.getConstructor(
                    new Class[] {ObjectStoreWriter.class });
            iw = (IntegrationWriterAbstractImpl) c.newInstance(new Object[] {writer });
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified IntegrationWriter class '"
                                           + integrationWriterClassName
                                           + "' for " + alias + " (check properties file)");
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find appropriate constructor for "
                                           + "IntegrationWriter: " + integrationWriterClassName 
                                           + "(ObjectStoreWriter)"
                                           + " - check properties file");
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate IntegrationWriter "
                                           + "class: " + integrationWriterClassName);
        }
        return iw;
    }
}
