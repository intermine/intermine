package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;

/**
 * A DataLoader with helper methods for creating and storing objects using an IntegrationWriter.
 *
 * @author Kim Rutherford
 */

public class DirectDataLoader extends DataLoader
{
    private static final Logger LOG = Logger.getLogger(DirectDataLoader.class);
    private int idCounter = 0;
    private int storeCount = 0;
    private long startTime;
    private long stepTime;
    private String sourceName;
    private String sourceType;
    private List<FastPathObject> buffer = new ArrayList<FastPathObject>();
    Map<Class<?>, Set<String>> keyClassRefs = null;

    private static final int LOG_FREQUENCY = 100000;
    private static final int COMMIT_FREQUENCY = 500000;
    private static final int BATCH_SIZE = 1000;

    /**
     * Create a new DirectDataLoader using the given IntegrationWriter and source name.
     * @param iw an IntegrationWriter
     * @param sourceName the source name
     * @param sourceType the source type
     */
    public DirectDataLoader(IntegrationWriter iw, String sourceName, String sourceType) {
        super(iw);
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.startTime = System.currentTimeMillis();
        this.stepTime = startTime;
    }

    /**
     * Store an object using the IntegrationWriter, buffering writes so that integration queries
     * and database writes can be run in batches.
     * @param o the InterMineObject
     * @throws ObjectStoreException if there is a problem in the IntegrationWriter
     */
    public void store(FastPathObject o) throws ObjectStoreException {

        buffer.add(o);

        if (buffer.size() == BATCH_SIZE) {
            storeBatch();
        }
    }

    /**
     *
     * @throws ObjectStoreException
     */
    private void storeBatch() throws ObjectStoreException {
        Source source = getIntegrationWriter().getMainSource(sourceName, sourceType);
        Source skelSource = getIntegrationWriter().getSkeletonSource(sourceName, sourceType);

        // first get equivalent objects for buffered objects
        if (getIntegrationWriter() instanceof IntegrationWriterDataTrackingImpl) {
            checkForProxiesInPrimaryKeys(source);

            HintingFetcher eof =
                    ((IntegrationWriterDataTrackingImpl) getIntegrationWriter()).getEof();
            if (eof instanceof BatchingFetcher) {
                // run all primary key queries at once for objects in this batch
                ((BatchingFetcher) eof).getEquivalentsForObjects(buffer);
            } else {
                LOG.warn("Not a batching fetcher, was: " + eof.getClass());
            }
        }

        // now store, the equivalent objects should be in cache
        for (FastPathObject o : buffer) {
            getIntegrationWriter().store(o, source, skelSource);
            storeCount++;
            if (storeCount % LOG_FREQUENCY == 0) {
                long now = System.currentTimeMillis();
                LOG.info("Dataloaded " + storeCount + " objects - running at "
                        + ((60000L * LOG_FREQUENCY) / (now - stepTime)) + " (avg "
                        + ((60000L * storeCount) / (now - startTime))
                        + ") objects per minute -- now on "
                        + Util.getFriendlyName(o.getClass()));
                stepTime = now;
            }
            if (storeCount % COMMIT_FREQUENCY == 0) {
                LOG.info("Committing transaction after storing " + storeCount + " objects.");
                getIntegrationWriter().batchCommitTransaction();
            }
        }
        // now clear ready for next objects
        buffer.clear();
    }

    /**
     * Fetch a map of class fields that are references and appear in primary keys.
     * @param source the source currently being loaded
     * @return a map from class name to a set of field names that are references
     */
    private Map<Class<?>, Set<String>> getReferencesInPrimaryKeys(Source source) {
        if (keyClassRefs == null) {
            keyClassRefs = new HashMap<Class<?>, Set<String>>();
            Set<PrimaryKey> keysForSource =  DataLoaderHelper.getSourcePrimaryKeys(source,
                    getIntegrationWriter().getModel());
            for (PrimaryKey pk :keysForSource) {
                for (String fieldName: pk.getFieldNames()) {
                    ClassDescriptor cld = pk.getClassDescriptor();
                    FieldDescriptor fld = cld.getFieldDescriptorByName(fieldName);
                    if (fld instanceof ReferenceDescriptor) {
                        LOG.info("Key has a reference: " + pk.getName() + " "
                                + cld.getName() + " " + fld.getName());
                        Set<String> refFieldNames = keyClassRefs.get(cld.getType());
                        if (refFieldNames == null) {
                            refFieldNames = new HashSet<String>();
                            keyClassRefs.put(cld.getType(), refFieldNames);
                        }
                        refFieldNames.add(fld.getName());
                    }
                }
            }
            LOG.info("Found " + keyClassRefs.size() + " keys that contain references: "
                    + keyClassRefs);
        }

        return keyClassRefs;
    }

    /**
     * Objects stored by the direct data loader can use ProxyReferences for referenced objects to
     * avoid keeping full InterMineObjects in memory in the parser. However, references CANNOT be
     * ProxyReferences if the reference is part of a primary key - e.g. if there is a primary key
     * on Gene.organism the organism reference in Gene objects may not be a ProxyReference.
     *
     * This method checks for keys that include references and verifies all objects of those classes
     * being loaded.
     * @param source the source being loaded
     * @throws IllegalArgumentException if a ProxyReference is found within a primary key.
     */
    private void checkForProxiesInPrimaryKeys(Source source) {
        // fetch any keys for this source that include references
        Map<Class<?>, Set<String>> refsInKeys = getReferencesInPrimaryKeys(source);

        // if there are no primary keys for this class that contain references we can do nothing
        if (!refsInKeys.isEmpty()) {
            // check all objects in the current buffer
            for (FastPathObject fpo : buffer) {
                if (fpo instanceof InterMineObject) {
                    InterMineObject imo = (InterMineObject) fpo;
                    for (Class<?> keyCls : refsInKeys.keySet()) {
                        if (DynamicUtil.isAssignableFrom(keyCls, DynamicUtil.getSimpleClass(imo))) {
                            for (String fieldName : refsInKeys.get(keyCls)) {
                                // check and throw an exception if a ProxyReference in a key field
                                checkForNullProxyReference(imo, fieldName);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * For the given object and reference field name check and throw an exception if the value in
     * the field is a ProxyReference instead of a full object.
     * @param imo the object to check
     * @param fieldName a reference field name to check
     */
    private void checkForNullProxyReference(InterMineObject imo, String fieldName) {
        try {
            InterMineObject refObj = (InterMineObject) imo.getFieldProxy(fieldName);
            if (refObj instanceof ProxyReference) {
                throw new IllegalArgumentException("Found ProxyReference in a key field reference"
                        + " for " + DynamicUtil.getSimpleClassName(imo) + "." + fieldName + "."
                        + " With the DirectDataLoader any reference that is part of a primary"
                        + " key must be set as an InterMineObject not a ProxyReference.");
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close the DirectDataLoader, must be called to make sure final batch of data is stored.
     * @throws ObjectStoreException if problems storing final data.
     */
    public void close() throws ObjectStoreException {
        // make sure we store any remaining objects
        storeBatch();
        long now = System.currentTimeMillis();
        LOG.info("Finished dataloading " + storeCount + " objects at " + ((60000L * storeCount)
                / (now - startTime)) + " objects per minute (" + (now - startTime)
            + " ms total) for source " + sourceName);
    }
    /**
     * Create a new object of the given class name and give it a unique ID.
     * @param className the class name
     * @return the new InterMineObject
     * @throws ClassNotFoundException if the given class doesn't exist
     */
    @SuppressWarnings("unchecked")
    public InterMineObject createObject(String className) throws ClassNotFoundException {
        return createObject((Class<? extends InterMineObject>) Class.forName(className));
    }

    /**
     * Create a new object of the given class and give it a unique ID.
     * @param c the class
     * @param <C> the type of the class
     * @return the new InterMineObject
     */
    public <C extends InterMineObject> C createObject(Class<C> c) {
        C o = DynamicUtil.simpleCreateObject(c);
        o.setId(new Integer(idCounter));
        idCounter++;
        return o;
    }

    /**
     * Create a 'simple object' which doesn't inherit from InterMineObject and doesn't have an id.
     * @param c the class of object to create
     * @param <C> the type of the class
     * @return an empty simple object of the given class
     */
    public <C extends FastPathObject> C createSimpleObject(Class<C> c) {
        C o = DynamicUtil.simpleCreateObject(c);
        return o;
    }

}
