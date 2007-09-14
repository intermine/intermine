package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CollectionUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.Shutdownable;
import org.intermine.util.ShutdownHook;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Class providing EquivalentObjectFetcher functionality that batches fetches to improve
 * performance.
 *
 * @author Matthew Wakeling
 */
public class BatchingFetcher extends HintingFetcher
{
    private static final Logger LOG = Logger.getLogger(BatchingFetcher.class);
    protected Map<InterMineObject, Set<InterMineObject>> equivalents = Collections
        .synchronizedMap(new WeakHashMap<InterMineObject, Set<InterMineObject>>());
    protected DataTracker dataTracker;
    protected Source source;
    protected int batchQueried = 0;
    protected long timeSpentExecute = 0;
    protected long timeSpentPrefetchEquiv = 0;
    protected long timeSpentPrefetchTracker = 0;

    /**
     * Constructor
     *
     * @param fetcher another EquivalentObjectFetcher
     * @param dataTracker a DataTracker object to pass prefetch instructions to
     * @param source the data Source that is being loaded
     */
    public BatchingFetcher(BaseEquivalentObjectFetcher fetcher, DataTracker dataTracker,
            Source source) {
        super(fetcher);
        this.dataTracker = dataTracker;
        this.source = source;
    }

    /**
     * Returns an ObjectStore layered on top of the given ObjectStore, which reports to this fetcher
     * which objects are being loaded.
     *
     * @param os an ObjectStore
     * @return an ObjectStore
     */
    public ObjectStore getNoseyObjectStore(ObjectStore os) {
        return new NoseyObjectStore(os);
    }

    /**
     * {@inheritDoc}
     */
    public void close(Source source) {
        LOG.info("Batching equivalent object query summary for source " + source + " :"
                + getSummary(source).toString() + "\nQueried " + batchQueried
                + " objects by batch");
    }

    /**
     * {@inheritDoc}
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
    throws ObjectStoreException {
        if (source == this.source) {
            Set retval = equivalents.get(obj);
            if (retval != null) {
                //Set expected = super.queryEquivalentObjects(obj, source);
                //if (!retval.equals(expected)) {
                //    throw new RuntimeException("BatchingFetcher produced incorrect result."
                //            + " Expected " + expected + ", but got " + retval);
                //}
                return retval;
            } else {
                retval = super.queryEquivalentObjects(obj, source);
                //equivalents.put(obj, retval);
                return retval;
            }
        }
        return super.queryEquivalentObjects(obj, source);
    }

    /**
     * Fetches the equivalent object information for a whole batch of objects.
     *
     * @param batch the objects
     * @throws ObjectStoreException if something goes wrong
     */
    protected void getEquivalentsFor(List<ResultsRow> batch) throws ObjectStoreException {
        long time = System.currentTimeMillis();
        long time1 = time;
        boolean databaseEmpty = hints.databaseEmpty();
        if (savedDatabaseEmptyFetch == -1) {
            savedDatabaseEmptyFetch = System.currentTimeMillis() - time;
        }
        if (databaseEmpty) {
            savedDatabaseEmpty++;
            return;
        }
        // TODO: add all the objects that are referenced by these objects, and follow primary keys
        // We can make use of the ObjectStoreFastCollectionsForTranslatorImpl's ability to work this
        // all out for us.
        Set<InterMineObject> objects = new HashSet<InterMineObject>();
        for (ResultsRow row : batch) {
            for (Object object : row) {
                if (object instanceof InterMineObject) {
                    InterMineObject imo = (InterMineObject) object;
                    if (idMap.get(imo.getId()) == null) {
                        objects.add(imo);
                        for (String fieldName : TypeUtil.getFieldInfos(imo.getClass()).keySet()) {
                            Object fieldValue;
                            try {
                                fieldValue = TypeUtil.getFieldProxy(imo, fieldName);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                            if ((fieldValue instanceof InterMineObject)
                                    && (!(fieldValue instanceof ProxyReference))) {
                                objects.add((InterMineObject) fieldValue);
                            } else if (fieldValue instanceof Collection) {
                                for (Object collectionElement : ((Collection) fieldValue)) {
                                    if ((collectionElement instanceof InterMineObject)
                                            && (!(collectionElement instanceof ProxyReference))) {
                                        objects.add((InterMineObject) collectionElement);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        objects.removeAll(equivalents.keySet());
        // Now objects contains all the objects we need to fetch data for.
        Map<InterMineObject, Set<InterMineObject>> results = new HashMap<InterMineObject,
            Set<InterMineObject>>();
        for (InterMineObject object : objects) {
            results.put(object, new HashSet<InterMineObject>());
        }

        Map<PrimaryKey, ClassDescriptor> pksToDo = new HashMap();
        Map<ClassDescriptor, List<InterMineObject>> cldToObjectsForCld = new HashMap();
        Map<Class, List<InterMineObject>> categorised = CollectionUtil.groupByClass(objects, false);
        Set<ClassDescriptor> cldsDone = new HashSet<ClassDescriptor>();
        for (Class c : categorised.keySet()) {
            Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(c);
            for (ClassDescriptor cld : classDescriptors) {
                if (!cldsDone.contains(cld)) {
                    cldsDone.add(cld);
                    Set<PrimaryKey> keysForClass;
                    if (source == null) {
                        keysForClass = new HashSet<PrimaryKey>(PrimaryKeyUtil.getPrimaryKeys(cld)
                                .values());
                    } else {
                        keysForClass = DataLoaderHelper.getPrimaryKeys(cld, source);
                    }
                    if (!keysForClass.isEmpty()) {
                        time = System.currentTimeMillis();
                        boolean classNotExists = hints.classNotExists(cld.getType());
                        String className = DynamicUtil.getFriendlyName(cld.getType());
                        if (!savedTimes.containsKey(className)) {
                            savedTimes.put(className, new Long(System.currentTimeMillis() - time));
                        }
                        if (!classNotExists) {
                            //LOG.error("Inspecting class " + className);
                            List<InterMineObject> objectsForCld = new ArrayList<InterMineObject>();
                            for (Map.Entry<Class, List<InterMineObject>> category
                                    : categorised.entrySet()) {
                                if (cld.getType().isAssignableFrom(category.getKey())) {
                                    objectsForCld.addAll(category.getValue());
                                }
                            }
                            cldToObjectsForCld.put(cld, objectsForCld);
                            // So now we have a list of objects for this CLD.
                            for (PrimaryKey pk : keysForClass) {
                                //LOG.error("Adding pk " + cld.getName() + "." + pk.getName());
                                pksToDo.put(pk, cld);
                            }
                        } else {
                            //LOG.error("Empty class " + className);
                        }
                    }
                }
            }
        }
        Set<Integer> fetchedObjectIds = new HashSet();
        while (!pksToDo.isEmpty()) {
            int startPksToDoSize = pksToDo.size();
            Iterator<PrimaryKey> pkIter = pksToDo.keySet().iterator();
            while (pkIter.hasNext()) {
                PrimaryKey pk = pkIter.next();
                ClassDescriptor cld = pksToDo.get(pk);
                boolean canDoPkNow = true;
                Iterator<String> fieldNameIter = pk.getFieldNames().iterator();
                while (fieldNameIter.hasNext() && canDoPkNow) {
                    String fieldName = fieldNameIter.next();
                    FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
                    if (fd.isReference()) {
                        Iterator<ClassDescriptor> otherCldIter = pksToDo.values().iterator();
                        while (otherCldIter.hasNext() && canDoPkNow) {
                            ClassDescriptor otherCld = otherCldIter.next();
                            Class fieldClass = ((ReferenceDescriptor) fd)
                                .getReferencedClassDescriptor().getType();
                            if (otherCld.getType().isAssignableFrom(fieldClass)
                                    || fieldClass.isAssignableFrom(otherCld.getType())) {
                                canDoPkNow = false;
                            }
                        }
                    }
                }
                if (canDoPkNow) {
                    //LOG.error("Running pk " + cld.getName() + "." + pk.getName());
                    doPk(pk, cld, results, cldToObjectsForCld.get(cld),
                            fetchedObjectIds);
                    pkIter.remove();
                } else {
                    //LOG.error("Cannot do pk " + cld.getName() + "." + pk.getName() + " yet");
                }
            }
            if (pksToDo.size() == startPksToDoSize) {
                throw new RuntimeException("Error - cannot fetch any pks: " + pksToDo.keySet());
            }
        }
        batchQueried += results.size();
        equivalents.putAll(results);
        long time2 = System.currentTimeMillis();
        timeSpentPrefetchEquiv += time2 - time1;
        dataTracker.prefetchIds(fetchedObjectIds);
        time1 = System.currentTimeMillis();
        timeSpentPrefetchTracker += time1 - time2;
    }

    /**
     * Fetches equivalent objects for a particular primary key.
     *
     * @param pk the PrimaryKey
     * @param cld the ClassDescriptor of the PrimaryKey
     * @param results a Map to hold results that are to be added to the cache
     * @param objectsForCld a List of objects relevant to this PrimaryKey
     * @param fetchedObjectIds a Set to hold ids of objects that are fetched, to prefetch from the
     * data tracker later
     * @throws ObjectStoreException if something goes wrong
     */
    protected void doPk(PrimaryKey pk, ClassDescriptor cld, Map<InterMineObject,
            Set<InterMineObject>> results, List<InterMineObject> objectsForCld,
            Set<Integer> fetchedObjectIds) throws ObjectStoreException {
        Iterator<InterMineObject> objectsForCldIter = objectsForCld.iterator();
        while (objectsForCldIter.hasNext()) {
            int objCount = 0;
            int origObjCount = 0;
            Query q = new Query();
            QueryClass qc = new QueryClass(cld.getType());
            q.addFrom(qc);
            q.addToSelect(qc);
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            q.setConstraint(cs);
            Map<String, Set> fieldNameToValues = new HashMap<String, Set>();
            for (String fieldName : pk.getFieldNames()) {
                try {
                    QueryField qf = new QueryField(qc, fieldName);
                    q.addToSelect(qf);
                    Set values = new HashSet();
                    fieldNameToValues.put(fieldName, values);
                    cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, values));
                } catch (IllegalArgumentException e) {
                    QueryForeignKey qf = new QueryForeignKey(qc, fieldName);
                    q.addToSelect(qf);
                    Set values = new HashSet();
                    fieldNameToValues.put(fieldName, values);
                    cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, values));
                }
            }
            // Now make a map from the primary key values to source objects
            Map<List, InterMineObject> keysToSourceObjects = new HashMap<List, InterMineObject>();
            while (objectsForCldIter.hasNext() && (objCount < 500)) {
                InterMineObject object = objectsForCldIter.next();
                origObjCount++;
                try {
                    if (DataLoaderHelper.objectPrimaryKeyNotNull(model, object, cld, pk, source,
                                idMap)) {
                        List<Collection<Object>> values = new ArrayList();
                        boolean skipObject = false;
                        Map<String, Set> fieldsValues = new HashMap();
                        for (String fieldName : pk.getFieldNames()) {
                            try {
                                Object value = TypeUtil.getFieldValue(object, fieldName);
                                Set fieldValues;
                                if (value instanceof InterMineObject) {
                                    Integer id = idMap.get(((InterMineObject) value).getId());
                                    if (id == null) {
                                        Set<InterMineObject> eqs = results.get((InterMineObject)
                                                value);
                                        if (eqs == null) {
                                            eqs = queryEquivalentObjects((InterMineObject) value,
                                                    source);
                                        }
                                        fieldValues = new HashSet();
                                        for (InterMineObject obj : eqs) {
                                            fieldValues.add(obj.getId());
                                        }
                                    } else {
                                        fieldValues = Collections.singleton(id);
                                    }
                                } else {
                                    fieldValues = Collections.singleton(value);
                                }
                                values.add(fieldValues);
                                fieldsValues.put(fieldName, fieldValues);
                                for (Object fieldValue : fieldValues) {
                                    long time = System.currentTimeMillis();
                                    boolean pkQueryFruitless = hints.pkQueryFruitless(cld
                                            .getType(), fieldName, fieldValue);
                                    String summaryName = DynamicUtil.getFriendlyName(cld
                                            .getType()) + "." + fieldName;
                                    if (!savedTimes.containsKey(summaryName)) {
                                        savedTimes.put(summaryName, new Long(System
                                                    .currentTimeMillis() - time));
                                        savedCounts.put(summaryName, new Integer(0));
                                    }
                                    if (pkQueryFruitless) {
                                        skipObject = true;
                                    }
                                }
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (!skipObject) {
                            objCount++;
                            for (String fieldName : pk.getFieldNames()) {
                                fieldNameToValues.get(fieldName).addAll(fieldsValues
                                        .get(fieldName));
                            }
                            for (List valueSet : CollectionUtil.fanOutCombinations(values)) {
                                if (keysToSourceObjects.containsKey(valueSet)) {
                                    throw new ObjectStoreException("Duplicate objects found for pk "
                                            + cld.getName() + "." + pk.getName() + ": " + object);
                                }
                                keysToSourceObjects.put(valueSet, object);
                            }
                        }
                    }
                } catch (MetaDataException e) {
                    throw new ObjectStoreException(e);
                }
            }
            // Prune BagConstraints using the hints system.
            //boolean emptyQuery = false;
            //Iterator<String> fieldNameIter = pk.getFieldNames().iterator();
            //while (fieldNameIter.hasNext() && (!emptyQuery)) {
            //    String fieldName = fieldNameIter.next();
            //    Set values = fieldNameToValues.get(fieldName);
                //Iterator valueIter = values.iterator();
                //while (valueIter.hasNext()) {
                //    if (hints.pkQueryFruitless(cld.getType(), fieldName, valueIter.next())) {
                //        valueIter.remove();
                //    }
                //}
            //    if (values.isEmpty()) {
            //        emptyQuery = true;
            //    }
            //}
            if (objCount > 0) {
                // Iterate through query, and add objects to results
                long time = System.currentTimeMillis();
                int matches = 0;
                Results res = lookupOs.execute(q);
                res.setNoExplain();
                res.setNoOptimise();
                res.setBatchSize(2000);
                for (ResultsRow row : ((List<ResultsRow>) res)) {
                    List values = new ArrayList();
                    for (int i = 1; i <= pk.getFieldNames().size(); i++) {
                        values.add(row.get(i));
                    }
                    Set<InterMineObject> set = results.get(keysToSourceObjects.get(values));
                    if (set != null) {
                        set.add((InterMineObject) row.get(0));
                        matches++;
                    }
                    fetchedObjectIds.add(((InterMineObject) row.get(0)).getId());
                }
            }
        }
    }
 
    private class NoseyObjectStore extends ObjectStorePassthruImpl implements Shutdownable
    {
        public NoseyObjectStore(ObjectStore os) {
            super(os);
            ShutdownHook.registerObject(this);
        }

        /**
         * Called by the ShutdownHook on shutdown.
         */
        public void shutdown() {
            LOG.info("Time spent: Execute: " + timeSpentExecute + ", Prefetch equivalent objects: "
                    + timeSpentPrefetchEquiv + ", Prefetch tracker data: "
                    + timeSpentPrefetchTracker);
        }

        /**
         * {@inheritDoc}
         */
        public Results execute(Query q) {
            return new Results(q, this, getSequence(getComponentsForQuery(q)));
        }

        /**
         * {@inheritDoc}
         */
        public SingletonResults executeSingleton(Query q) {
            return new SingletonResults(q, this, getSequence(getComponentsForQuery(q)));
        }

        public List<ResultsRow> execute(Query q, int start, int limit, boolean optimise,
                boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
            long time = System.currentTimeMillis();
            List<ResultsRow> retval = os.execute(q, start, limit, optimise, explain, sequence);
            timeSpentExecute += System.currentTimeMillis() - time;
            getEquivalentsFor(retval);
            return retval;
        }
    }
}

