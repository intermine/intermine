package org.flymine.dataloader;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
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
     * @param dataSource name of data source being loaded
     * @param os the ObjectStore being written to
     * @return instance of a concrete IntegrationWriter according to property
     * @throws ObjectStoreException if anything goes wrong
     */

    public static IntegrationWriter getIntegrationWriter(String alias, String dataSource,
                                                         ObjectStore os)
        throws ObjectStoreException {
        if (alias == null) {
            throw new NullPointerException("Dataloader alias cannot be null");
        }
        if (alias.equals("")) {
            throw new IllegalArgumentException("Dataloader alias cannot be empty");
        }
        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        if (props.size() == 0) {
            throw new ObjectStoreException("No Dataloader properties were found for '"
                                           + alias + "'");
        }
        props = PropertiesUtil.stripStart(alias, props);
        String integrationWriterClassName = props.getProperty("integration");
        if (integrationWriterClassName == null) {
            throw new ObjectStoreException(alias + " does not have an integration class specified"
                                           + " (check properties file)");
        }
        String writerAlias = props.getProperty("osw");
        if (writerAlias == null) {
            throw new ObjectStoreException(alias + " does not have an osw alias specified"
                                           + " (check properties file)");
        }
        Properties oswProps = PropertiesUtil.getPropertiesStartingWith(writerAlias);
        String writerClassName = oswProps.getProperty(writerAlias + ".class");

        // first build ObjectStoreWriter

        ObjectStoreWriter writer = null;
        try {
            Class writerClass = Class.forName(writerClassName);
            Constructor c = writerClass.getConstructor(new Class[] {ObjectStore.class});
            writer = (ObjectStoreWriter) c.newInstance(new Object[] {os});
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified ObjectStoreWriter class '"
                                           + writerClassName + "' for " + alias
                                           + " (check properties file) " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find appropriate constructor for "
                                           + "ObjectStoreWriter: " + writerClassName
                                           + " (ObjectStore.class)"
                                           + " - check properties file, " + e.getMessage());
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate ObjectStoreWriter class: "
                                           + writerClassName + ", " + e.toString());
        }

        // now build IntegrationWriter using datasource name and ObjectStoreWriter

        IntegrationWriterAbstractImpl iw = null;
        try {
            Class integrationWriterClass = Class.forName(integrationWriterClassName);
            Constructor c = integrationWriterClass.getConstructor(
                                                                  new Class[] {
                                                                      String.class,
                                                                      ObjectStore.class, 
                                                                      ObjectStoreWriter.class});
            iw = (IntegrationWriterAbstractImpl) c.newInstance(new Object[]
                {dataSource, os, writer});
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified IntegrationWriter class '"
                                           + integrationWriterClassName
                                           + "' for " + alias + " (check properties file)");
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find appropriate constructor for "
                                           + "IntegrationWriter: " + integrationWriterClassName 
                                           + "(String, ObjectStore, ObjectStoreWriter)"
                                           + " - check properties file");
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate IntegrationWriter "
                                           + "class: " + integrationWriterClassName);
        }
        return iw;
    }
}
