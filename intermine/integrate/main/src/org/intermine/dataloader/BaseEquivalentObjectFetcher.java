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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IntToIntMap;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Class providing methods to look up equivalent objects by primary key in a production
 * objectstore.
 *
 * @author Matthew Wakeling
 */
public class BaseEquivalentObjectFetcher implements EquivalentObjectFetcher
{
    private static final Logger LOG = Logger.getLogger(BaseEquivalentObjectFetcher.class);

    protected Model model;
    protected IntToIntMap idMap;
    protected ObjectStore lookupOs;
    protected Map<Class, Long> summaryTimes = new HashMap<Class, Long>();
    protected Map<Class, Integer> summaryCounts = new HashMap<Class, Integer>();
    protected Map<Class, Integer> summaryCallCounts = new HashMap<Class, Integer>();

    /**
     * Constructor for this EquivalentObjectFetcher.
     *
     * @param model a Model
     * @param idMap an IntToIntMap from source IDs to destination IDs
     * @param lookupOs an ObjectStore for the production database
     */
    public BaseEquivalentObjectFetcher(Model model, IntToIntMap idMap, ObjectStore lookupOs) {
        this.model = model;
        this.idMap = idMap;
        this.lookupOs = lookupOs;
    }

    /**
     * Returns the Model used.
     *
     * @return a Model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Returns the IdMap used.
     *
     * @return an IntToIntMap
     */
    public IntToIntMap getIdMap() {
        return idMap;
    }

    /**
     * Returns the objectstore that contains the equivalent objects.
     *
     * @return an ObjectStore
     */
    public ObjectStore getLookupOs() {
        return lookupOs;
    }

    /**
     * Close method - prints out summary data.
     *
     * @param source the Source used
     */
    public void close(Source source) {
        LOG.info("Base equivalent object query summary for source " + source + " :"
                + getSummary(source).toString());
    }

    /**
     * Returns a StringBuffer containing summary information.
     *
     * @param source the Source used
     * @return a StringBuffer
     */
    protected StringBuffer getSummary(Source source) {
        StringBuffer retval = new StringBuffer();
        TreeMap<String, Class> summaryNames = new TreeMap<String, Class>();
        for (Class c : summaryTimes.keySet()) {
            summaryNames.put(DynamicUtil.getFriendlyName(c), c);
        }
        for (String summaryName : summaryNames.keySet()) {
            Class summaryClass = summaryNames.get(summaryName);
            Long summaryTime = summaryTimes.get(summaryClass);
            Integer summaryCount = summaryCounts.get(summaryClass);
            Integer summaryCallCount = summaryCallCounts.get(summaryClass);
            retval.append("\nPerformed equivalence query for " + summaryName + " " + summaryCount
                     + "/" + summaryCallCount + " times.");
            if (summaryCount.longValue() > 0) {
                retval.append(" Average time "
                     + (summaryTime.longValue() / summaryCount.longValue()) + " ms");
            }
            // Work out if the class has no keys
            boolean noKeys = true;
            Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(summaryClass);
            for (ClassDescriptor cld : classDescriptors) {
                if (source == null) {
                    if (!PrimaryKeyUtil.getPrimaryKeys(cld).isEmpty()) {
                        noKeys = false;
                    }
                } else {
                    if (!DataLoaderHelper.getPrimaryKeys(cld, source).isEmpty()) {
                        noKeys = false;
                    }
                }
            }
            if (noKeys) {
                retval.append(" No primary keys for this class");
            }
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
    throws ObjectStoreException {
        Class summaryName = obj.getClass();
        Long soFar = summaryTimes.get(summaryName);
        Integer soFarCount = summaryCounts.get(summaryName);
        Integer soFarCallCount = summaryCallCounts.get(summaryName);
        if (soFar == null) {
            soFar = new Long(0L);
            soFarCount = new Integer(0);
            soFarCallCount = new Integer(0);
            summaryTimes.put(summaryName, soFar);
            summaryCounts.put(summaryName, soFarCount);
            summaryCallCounts.put(summaryName, soFarCallCount);
        }
        Query q = null;
        try {
            q = createPKQuery(obj, source, false);
        } catch (MetaDataException e) {
            throw new ObjectStoreException(e);
        }
        if (q != null) {
            SingletonResults result = lookupOs.executeSingleton(q);
            result.setNoOptimise();
            result.setNoExplain();
            long before = System.currentTimeMillis();
            try {
                result.get(0);
            } catch (Exception e) {
                // Ignore - operation will be repeated later
            }
            long time = System.currentTimeMillis() - before;
            summaryTimes.put(summaryName, new Long(time + soFar.longValue()));
            summaryCounts.put(summaryName, new Integer(soFarCount.intValue() + 1));
            summaryCallCounts.put(summaryName, new Integer(soFarCallCount.intValue() + 1));
            return result;
        } else {
            summaryCallCounts.put(summaryName, new Integer(soFarCallCount.intValue() + 1));
            return Collections.EMPTY_SET;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Query createPKQuery(InterMineObject obj, Source source, boolean queryNulls)
    throws MetaDataException {
        int subCount = 0;
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcIMO = new QueryClass(InterMineObject.class);
        q.addFrom(qcIMO);
        q.addToSelect(qcIMO);
        ConstraintSet where = new ConstraintSet(ConstraintOp.OR);
        Query subQ = null;

        Set classDescriptors = model.getClassDescriptorsForClass(obj.getClass());
        boolean valid = classDescriptors.isEmpty();
        boolean invalid = false;

        Iterator cldIter = classDescriptors.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            Set classQueries =
                createPKQueriesForClass(obj, source, queryNulls, cld);

            if (classQueries != null) {
                Iterator classQueriesIter = classQueries.iterator();

                while (classQueriesIter.hasNext()) {
                    subQ = (Query) classQueriesIter.next();
                    valid = true;
                    where.addConstraint(new SubqueryConstraint(qcIMO, ConstraintOp.IN, subQ));
                    subCount++;
                }
            } else {
                invalid = true;
            }
        }
        if (!invalid) {
            valid = true;
        }
        q.setConstraint(where);
        switch (subCount) {
            case 1:
                return subQ;
            case 0:
                if (!valid) {
                    // Whether to throw an exception here or just log a message is a business
                    // decision.  We have an object with no usable primary keys so it can't be
                    // integrated with anything (although we have defined that the class should
                    // be integrated.  For the moment just log an error.                    
                    LOG.warn("No valid primary key found for object: " + obj);  
                    //throw new IllegalArgumentException("No valid primary key found for object: "
                    //        + obj);
                }
                return null;
            default:
                return q;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Set createPKQueriesForClass(InterMineObject obj, Source source, boolean queryNulls,
            ClassDescriptor cld) throws MetaDataException {
        Set primaryKeys;
        if (source == null) {
            primaryKeys = new HashSet(PrimaryKeyUtil.getPrimaryKeys(cld).values());
        } else {
            primaryKeys = DataLoaderHelper.getPrimaryKeys(cld, source);
        }

        LOG.debug("primary keys for class " + cld.getName() + " = " + primaryKeys);

        Set returnSet = new LinkedHashSet();
        int pkCount = primaryKeys.size();

        Iterator pkSetIter = primaryKeys.iterator();
        while (pkSetIter.hasNext()) {
            PrimaryKey pk = (PrimaryKey) pkSetIter.next();
            try {
                createPKQueryForPK(obj, queryNulls, cld, pk, source, returnSet);
            } catch (IllegalArgumentException e) {
                pkCount--;
            }
        }

        if (primaryKeys.isEmpty() || (pkCount > 0)) {
            return returnSet;
        } else {
            return null;
        }
    }

    /**
     * Adds a Query to handle a primary key to a Set.
     *
     * @param obj an InterMineObject
     * @param queryNulls true to make null a valid value, false to make null ignore the key
     * @param cld the ClassDescriptor for the key
     * @param pk the PrimaryKey
     * @param source the Source of the object
     * @param returnSet the Set to which the Query will be added
     * @throws MetaDataException if something goes wrong
     */
    public void createPKQueryForPK(InterMineObject obj, boolean queryNulls, ClassDescriptor cld,
            PrimaryKey pk, Source source, Set returnSet) throws MetaDataException {
        if (!queryNulls && !DataLoaderHelper.objectPrimaryKeyNotNull(model, obj, cld, pk,
                    source, idMap)) {
            LOG.warn("Null values found for key (" + pk + ") for object: " + obj);
            throw new IllegalArgumentException("Null values");
        }

        Query query = new Query();
        query.setDistinct(false);
        QueryClass qc = new QueryClass(cld.getType());
        query.addFrom(qc);
        query.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        Iterator pkIter = pk.getFieldNames().iterator();
        while (pkIter.hasNext()) {
            String fieldName = (String) pkIter.next();
            FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
            if (fd instanceof AttributeDescriptor) {
                Object value;
                try {
                    value = TypeUtil.getFieldValue(obj, fieldName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("failed to get field value for field name: "
                                               + fieldName + " in " + obj, e);
                }
                if (value == null) {
                    cs.addConstraint(new SimpleConstraint(new QueryField(qc,
                                                                         fieldName),
                                                          ConstraintOp.IS_NULL));
                } else {
                    cs.addConstraint(new SimpleConstraint(new QueryField(qc,
                                                                         fieldName),
                                                          ConstraintOp.EQUALS,
                                                          new QueryValue(value)));
                }
            } else if (fd instanceof CollectionDescriptor) {
                throw new MetaDataException("Primary key " + pk.getName() + " for class "
                        + cld.getName() + " cannot contain collection " + fd.getName()
                        + ": collections cannot be part of a primary key. Please edit"
                        + model.getName() + "_keyDefs.properties");
            } else if (fd instanceof ReferenceDescriptor) {
                InterMineObject refObj;
                try {
                    refObj = (InterMineObject) TypeUtil.getFieldProxy(obj, fieldName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("failed to get field proxy for field name: "
                                               + fieldName + " in " + obj, e);
                }
                if (refObj == null) {
                    QueryObjectReference queryObjectReference =
                        new QueryObjectReference(qc, fieldName);
                    cs.addConstraint(new ContainsConstraint(queryObjectReference,
                                                            ConstraintOp.IS_NULL));
                } else {
                    Integer destId = null;
                    if (refObj.getId() != null) {
                        destId = idMap.get(refObj.getId());
                    }
                    if (destId == null) {
                        if (refObj instanceof ProxyReference) {
                            InterMineObject originalRefObj = refObj;
                            refObj = ((ProxyReference) refObj).getObject();

                            if (refObj == null) {
                                throw new RuntimeException("cannot get object of ProxyReference "
                                                           + originalRefObj + " while processing "
                                                           + obj);
                            }
                        }
                        Query refSubQuery =
                            createPKQuery(refObj, source, queryNulls);

                        if (refSubQuery != null) {
                            ClassDescriptor referencedClassDescriptor =
                                ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
                            QueryClass qc2 = new QueryClass(referencedClassDescriptor.getType());
                            query.addFrom(qc2);
                            QueryObjectReference fieldQOF = new QueryObjectReference(qc, fieldName);
                            cs.addConstraint(new ContainsConstraint(fieldQOF,
                                                                    ConstraintOp.CONTAINS, qc2));
                            cs.addConstraint(new SubqueryConstraint(qc2, ConstraintOp.IN,
                                                                    refSubQuery));
                        } else {
                            throw new IllegalArgumentException("Reference was hinted out");
                        }
                    } else {
                        InterMineObject destObj = (InterMineObject)
                            DynamicUtil.createObject(Collections.singleton(InterMineObject.class));
                        destObj.setId(destId);
                        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc,
                                                                                         fieldName),
                                                                ConstraintOp.CONTAINS, destObj));
                    }
                }
            }
        }
 
        query.setConstraint(cs);
        returnSet.add(query);
    }
}
