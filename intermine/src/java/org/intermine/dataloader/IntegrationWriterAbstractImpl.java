package org.flymine.dataloader;

import java.util.Properties;
import java.lang.reflect.Constructor;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.util.PropertiesUtil;

/**
 * Abstract implementation of ObjectStoreIntegrationWriter.  To retain
 * O/R mapping independence concrete subclasses should delegate writing to
 * a mapping tool specific implementation of ObjectStoreWriter.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */

public abstract class IntegrationWriterAbstractImpl implements IntegrationWriter
{

    protected ObjectStoreWriter osw;
    protected String dataSource;

    /**
     *
     * @param dataSource name of data source being loaded
     * @param os the ObjectStore being written to
     * @param alias identifier for properties defining integration/writer parameters
     * @return instance of a concrete IntegrationWriter according to property
     * @throws ObjectStoreException if anything goes wrong
     */
    public static IntegrationWriterAbstractImpl getInstance(String dataSource,
                                                            ObjectStore os, String alias)
        throws ObjectStoreException {

        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        if (0 == props.size()) {
            throw new ObjectStoreException("No ObjectStore properties were found for '"
                                           + alias + "'");
        }
        props = PropertiesUtil.stripStart(alias, props);
        String integrationName = props.getProperty("integration");

        String osw = props.getProperty("osw");
        Properties oswProps = PropertiesUtil.getPropertiesStartingWith(osw);
        String writerName = oswProps.getProperty(osw + ".class");

        ObjectStoreWriter writer;
        Class writerCls = null;
        try {
            writerCls = Class.forName(writerName);
            Constructor c = writerCls.getConstructor(new Class[] {ObjectStore.class});
            writer = (ObjectStoreWriter) c.newInstance(new Object[] {os});
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified ObjectStore class '"
                                           + writerName + "' for " + alias
                                           + " (check properties file) " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find appropriate constructor for "
                                           + "ObjectStoreWriter: " + writerCls
                                           + "(ObjectStore.class)"
                                           + "- check properties file, " + e.getMessage());
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate ObjectStoreWriter class: "
                                           + writerName + ", " + e.toString());
        }

        // now build IntegrationWriter with datasource name and ObjectStoreWriter

        Class integrationCls = null;
        IntegrationWriterAbstractImpl iWriter = null;
        try {
            integrationCls = Class.forName(integrationName);
            Constructor c;
            c = integrationCls.getConstructor(new Class[] {String.class, ObjectStoreWriter.class});
            iWriter = (IntegrationWriterAbstractImpl) c.newInstance(new Object[]
                {dataSource, writer});
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified ObjectStore class '"
                                           + integrationName
                                           + "' for " + alias + " (check properties file)");
        } catch (NoSuchMethodException e) {
            throw new ObjectStoreException("Cannot find appropriate constructor for "
                                           + "IntegrationWriter: " + integrationCls
                                           + "(String.class, ObjectStoreWriter.class)"
                                           + "- check properties file");
        } catch (Exception e) {
            throw new ObjectStoreException("Failed to instantiate IntegrationWriterAbstractImpl "
                                           + "class: " + integrationCls);
        }

        return iWriter;
    }


    /**
     * Given a new object from a data source find whether corresponding object exists in
     * ObjectStore and if so which fields the current data source has permission to write to.
     *
     * @param obj new object from datasource
     * @return details of object in database and which fields can be overridden
     * @throws ObjectStoreException if anything goes wrong retrieving object
     */
    public abstract IntegrationDescriptor getByExample(Object obj) throws ObjectStoreException;

    /**
     * Store an object in this ObjectStore, abstract.
     *
     * @param o the object to store
     * @param skeleton is this a skeleton object?
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public abstract void store(Object o, boolean skeleton) throws ObjectStoreException;

    /**
     * Store an object in this ObjectStore, delegates to internal ObjectStoreWriter.
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(Object o) throws ObjectStoreException {
        osw.store(o);
    }


    /**
     * Search database for object matching the given example object (i.e. primary key search)
     *
     * @param o the example object
     * @return the retieved object
     * @throws ObjectStoreException if an error occurs retieving the object
     */
    public Object getObjectByExample(Object o) throws ObjectStoreException {
        return osw.getObjectByExample(o);
    }


    /**
     * Delete an object from this ObjectStore, delegate to internal ObjectStoreWriter.
     *
     * @param o the object to delete
     * @throws ObjectStoreException if an error occurs during deletion of the object
     */
    public void delete(Object o) throws ObjectStoreException {
        osw.delete(o);
    }

    /**
     * Check whether the ObjectStore is performing a transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @return true if in a transaction, false otherwise
     * @throws ObjectStoreException if an error occurs the check
     */
    public boolean isInTransaction() throws ObjectStoreException {
        return osw.isInTransaction();
    }

    /**
     * Request that the ObjectStore begins a transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is in progress, or is aborted
     */
    public void beginTransaction() throws ObjectStoreException {
        osw.beginTransaction();
    }

    /**
     * Request that the ObjectStore commits and closes the transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is not in progress, or is aborted
     */
    public void commitTransaction() throws ObjectStoreException {
        osw.commitTransaction();
    }

    /**
     * Request that the ObjectStore aborts and closes the transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is not in progress
     */
    public void abortTransaction() throws ObjectStoreException {
        osw.abortTransaction();
    }
}
