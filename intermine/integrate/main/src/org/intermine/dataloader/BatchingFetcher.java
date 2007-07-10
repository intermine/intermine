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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.CollectionUtil;
import org.intermine.util.DynamicUtil;
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
    protected Source source;

    /**
     * Constructor
     *
     * @param fetcher another EquivalentObjectFetcher
     */
    public BatchingFetcher(BaseEquivalentObjectFetcher fetcher, Source source) {
        super(fetcher);
        this.source = source;
    }

    /**
     * Returns an ObjectStore layered on top of the given ObjectStore, which reports to this fetcher
     * which objects are being loaded.
     *
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
                + getSummary(source).toString());
    }

    /**
     * {@inheritDoc}
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
    throws ObjectStoreException {
        if (source == this.source) {
            Set retval = equivalents.get(obj);
            if (retval != null) {
                equivalents.remove(obj);
                return retval;
            }
        }
        LOG.warn("Queried equivalent objects for " + obj + " - possible performance problem");
        return super.queryEquivalentObjects(obj, source);
    }

    /**
     * Fetches the equivalent object information for a whole batch of objects.
     *
     * @param batch the objects
     */
    protected void getEquivalentsFor(List<ResultsRow> batch) {
        Set<InterMineObject> objects = new HashSet<InterMineObject>();
        for (ResultsRow row : batch) {
            for (Object object : row) {
                if (object instanceof InterMineObject) {
                    objects.add((InterMineObject) object);
                }
            }
        }
        // Now objects contains all the objects we need to fetch data for.
        Map<InterMineObject, Set<InterMineObject>> results = new HashMap<InterMineObject,
            Set<InterMineObject>>();
        for (InterMineObject object : objects) {
            results.put(object, new HashSet<InterMineObject>());
        }

        Map<Class, List<InterMineObject>> categorised = CollectionUtil.groupByClass(objects, false);
        Set<PrimaryKey> pksDone = new HashSet<PrimaryKey>();
        for (Class c : categorised.keySet()) {
            Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(c);
            for (ClassDescriptor cld : classDescriptors) {
                Set<PrimaryKey> keysForClass;
                if (source == null) {
                    keysForClass = new HashSet<PrimaryKey>(PrimaryKeyUtil.getPrimaryKeys(cld)
                            .values());
                } else {
                    keysForClass = DataLoaderHelper.getPrimaryKeys(cld, source);
                }
                for (PrimaryKey pk : keysForClass) {
                    if (!pksDone.contains(pk)) {
                        pksDone.add(pk);
                        List<InterMineObject> objectsForPk = new ArrayList<InterMineObject>();
                        for (Map.Entry<Class, List<InterMineObject>> category : categorised
                                .entrySet()) {
                            if (cld.getType().isAssignableFrom(category.getKey())) {
                                objectsForPk.addAll(category.getValue());
                            }
                        }
                        // So now we have a list of objects for this Primary Key.
                        Query q = new Query();
                        QueryClass qc = new QueryClass(cld.getType());
                        q.addFrom(qc);
                        q.addToSelect(qc);
                        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                        q.setConstraint(cs);
                        for (String fieldName : pk.getFieldNames()) {
                            QueryField qf = new QueryField(qc, fieldName);
                            q.addToSelect(qf);
                            Set values = new HashSet();
                            for (InterMineObject object : objectsForPk) {
                                try {
                                    values.add(TypeUtil.getFieldValue(object, fieldName));
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            cs.addConstraint(new BagConstraint(qf, ConstraintOp.IN, values));
                        }
                        // Now make a map from the primary key values to source objects
                        Map<List, InterMineObject> keysToSourceObjects = new HashMap<List,
                            InterMineObject>();
                        for (InterMineObject object : objectsForPk) {
                            List values = new ArrayList();
                            for (String fieldName : pk.getFieldNames()) {
                                try {
                                    values.add(TypeUtil.getFieldValue(object, fieldName));
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            keysToSourceObjects.put(values, object);
                        }
                        // Iterate through query, and add objects to results
                        Results res = lookupOs.execute(q);
                        res.setNoExplain();
                        res.setNoOptimise();
                        res.setBatchSize(2000);
                        for (ResultsRow row : ((List<ResultsRow>) res)) {
                            List values = new ArrayList();
                            for (int i = 1; i <= pk.getFieldNames().size(); i++) {
                                values.add(row.get(i));
                            }
                            results.get(keysToSourceObjects.get(values)).add((InterMineObject) row
                                    .get(0));
                        }
                    }
                }
            }
        }
        equivalents.putAll(results);
    }

    private class NoseyObjectStore extends ObjectStorePassthruImpl
    {
        public NoseyObjectStore(ObjectStore os) {
            super(os);
        }

        public List<ResultsRow> execute(Query q, int start, int limit, boolean optimise,
                boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
            List<ResultsRow> retval = os.execute(q, start, limit, optimise, explain, sequence);
            getEquivalentsFor(retval);
            return retval;
        }
    }
}

