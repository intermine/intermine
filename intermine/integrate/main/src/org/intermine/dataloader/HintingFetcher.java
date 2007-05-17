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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Class providing EquivalentObjectFetcher functionality that makes use of hints to improve
 * performance.
 *
 * @author Matthew Wakeling
 */
public class HintingFetcher extends BaseEquivalentObjectFetcher
{
    private static final Logger LOG = Logger.getLogger(HintingFetcher.class);

    EquivalentObjectHints hints;
    int savedDatabaseEmpty = 0;
    protected Map<String, Long> savedTimes = new TreeMap<String, Long>();
    protected Map<String, Integer> savedCounts = new TreeMap<String, Integer>();

    /**
     * Constructor
     *
     * @param fetcher another EquivalentObjectFetcher
     */
    public HintingFetcher(BaseEquivalentObjectFetcher fetcher) {
        super(fetcher.getModel(), fetcher.getIdMap(), fetcher.getLookupOs());
        this.hints = new EquivalentObjectHints(lookupOs);
    }

    /**
     * {@inheritDoc}
     */
    public void close(Source source) {
        LOG.info("Hinting equivalent object query summary for source " + source + " :"
                + getSummary(source).toString());
    }

    /**
     * {@inheritDoc}
     */
    protected StringBuffer getSummary(Source source) {
        StringBuffer retval = super.getSummary(source);
        if (savedDatabaseEmpty > 0) {
            retval.append("\nSaved " + savedDatabaseEmpty + " queries on empty database");
        }
        for (String summaryName : savedTimes.keySet()) {
            long savedTime = savedTimes.get(summaryName).longValue();
            int savedCount = savedCounts.get(summaryName).intValue();
            if (savedCount == 0) {
                retval.append("\nNo queries saved for " + summaryName + ", hints took " + savedTime
                        + " ms to fetch");
            } else {
                retval.append("\nSaved " + savedCount + " queries for " + summaryName
                        + ", hints took " + savedTime + " ms to fetch");
            }
            Set queried = hints.getQueried(summaryName);
            Set values = hints.getValues(summaryName);
            if (queried != null) {
                retval.append(". Queried values " + queried + " in database values " + values);
            }
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
        throws ObjectStoreException {
        if (hints.databaseEmpty()) {
            savedDatabaseEmpty++;
            return Collections.EMPTY_SET;
        }
        boolean allPkClassesEmpty = true;
        Set classDescriptors = lookupOs.getModel().getClassDescriptorsForClass(obj.getClass());
        Iterator cldIter = classDescriptors.iterator();
        while (cldIter.hasNext() && allPkClassesEmpty) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            Set primaryKeys = DataLoaderHelper.getPrimaryKeys(cld, source);
            if (!primaryKeys.isEmpty()) {
                long time = System.currentTimeMillis();
                boolean classNotExists = hints.classNotExists(cld.getType());
                String className = DynamicUtil.getFriendlyName(cld.getType());
                if (!savedTimes.containsKey(className)) {
                    savedTimes.put(className, new Long(System.currentTimeMillis() - time));
                    savedCounts.put(className, new Integer(0));
                }
                if (!classNotExists) {
                    allPkClassesEmpty = false;
                } else {
                    savedCounts.put(className, new Integer(savedCounts.get(className).intValue()
                                + 1));
                }
            }
        }
        if (allPkClassesEmpty) {
            return Collections.EMPTY_SET;
        }
        return super.queryEquivalentObjects(obj, source);
    }

    /**
     * {@inheritDoc}
     */
    public Set createPKQueriesForClass(InterMineObject obj, Source source, boolean queryNulls,
            ClassDescriptor cld) throws MetaDataException {
        if (hints.classNotExists(cld.getType())) {
            return Collections.EMPTY_SET;
        }
        return super.createPKQueriesForClass(obj, source, queryNulls, cld);
    }

    /**
     * {@inheritDoc}
     */
    public void createPKQueryForPK(InterMineObject obj, boolean queryNulls, ClassDescriptor cld,
            PrimaryKey pk, Source source, Set returnSet) throws MetaDataException {
        for (String fieldName : pk.getFieldNames()) {
            FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
            if (fd instanceof AttributeDescriptor) {
                Object value;
                try {
                    value = TypeUtil.getFieldValue(obj, fieldName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to get field value for field name: "
                            + fieldName + " in " + obj, e);
                }
                long time = System.currentTimeMillis();
                boolean pkQueryFruitless = hints.pkQueryFruitless(cld.getType(), fieldName, value);
                String summaryName = DynamicUtil.getFriendlyName(cld.getType()) + "."
                    + fieldName;
                if (!savedTimes.containsKey(summaryName)) {
                    savedTimes.put(summaryName, new Long(System.currentTimeMillis() - time));
                    savedCounts.put(summaryName, new Integer(0));
                }
                if (pkQueryFruitless) {
                    savedCounts.put(summaryName, new Integer(savedCounts.get(summaryName).intValue()
                                + 1));
                    return;
                }
            } else if (fd instanceof CollectionDescriptor) {
                // Do nothing
            } else if (fd instanceof ReferenceDescriptor) {
                InterMineObject refObj;
                try {
                    refObj = (InterMineObject) TypeUtil.getFieldProxy(obj, fieldName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to get field proxy for field name: "
                            + fieldName + " in " + obj, e);
                }
                if (refObj != null) {
                    Integer destId = null;
                    if (refObj.getId() != null) {
                        destId = idMap.get(refObj.getId());
                    }
                    if ((destId != null) || (refObj.getId() == null)) {
                        long time = System.currentTimeMillis();
                        boolean pkQueryFruitless = hints.pkQueryFruitless(cld.getType(), fieldName,
                                destId);
                        String summaryName = DynamicUtil.getFriendlyName(cld.getType()) + "."
                            + fieldName;
                        if (!savedTimes.containsKey(summaryName)) {
                            savedTimes.put(summaryName, new Long(System.currentTimeMillis()
                                        - time));
                            savedCounts.put(summaryName, new Integer(0));
                        }
                        if (pkQueryFruitless) {
                            savedCounts.put(summaryName, new Integer(savedCounts.get(summaryName)
                                        .intValue() + 1));
                            return;
                        }
                    }
                }
            }
        }
        super.createPKQueryForPK(obj, queryNulls, cld, pk, source, returnSet);
    }
}
