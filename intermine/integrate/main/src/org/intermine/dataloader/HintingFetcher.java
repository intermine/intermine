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
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;

/**
 * Class providing EquivalentObjectFetcher functionality that makes use of hints to improve
 * performance.
 *
 * @author Matthew Wakeling
 */
public class HintingFetcher extends BaseEquivalentObjectFetcher
{
    EquivalentObjectHints hints;

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
    public Set queryEquivalentObjects(InterMineObject obj, Source source)
        throws ObjectStoreException {
        if (hints.databaseEmpty()) {
            return Collections.EMPTY_SET;
        }
        boolean allPkClassesEmpty = true;
        Set classDescriptors = lookupOs.getModel().getClassDescriptorsForClass(obj.getClass());
        Iterator cldIter = classDescriptors.iterator();
        while (cldIter.hasNext() && allPkClassesEmpty) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            Set primaryKeys = DataLoaderHelper.getPrimaryKeys(cld, source);
            if (!primaryKeys.isEmpty()) {
                if (!hints.classNotExists(cld.getType())) {
                    allPkClassesEmpty = false;
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
                if (hints.pkQueryFruitless(cld.getType(), fieldName, value)) {
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
                        if (hints.pkQueryFruitless(cld.getType(), fieldName, destId)) {
                            return;
                        }
                    }
                }
            }
        }
        super.createPKQueryForPK(obj, queryNulls, cld, pk, source, returnSet);
    }
}
