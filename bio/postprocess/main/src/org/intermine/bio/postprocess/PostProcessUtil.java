package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.intermine.bio.util.Constants;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Common operations for post processing.
 *
 * @author Richard Smith
 */
public final class PostProcessUtil
{
    private PostProcessUtil() {
        //disable external instantiation
    }

    /**
     * Create a clone of given InterMineObject including the id.  This is designed for
     * altering and storing again (to avoid cache problems) so doesn't copy collections.
     *
     * @param obj object to clone
     * @param <O> The object type
     * @return the cloned object
     * @throws IllegalAccessException if problems with reflection
     */
    public static <O extends InterMineObject> O cloneInterMineObject(O obj)
        throws IllegalAccessException {
        return PostProcessUtil.cloneInterMineObject(obj, false);
    }


    /**
     * Create a copy of given InterMineObject with *no* id set and copies of collections
     *
     * @param obj object to copy
     * @param <O> The object type
     * @return the copied object
     * @throws IllegalAccessException if problems with reflection
     */
    public static <O extends InterMineObject> O copyInterMineObject(O obj)
        throws IllegalAccessException {
        O newObj = cloneInterMineObject(obj, true);
        newObj.setId(null);
        return newObj;
    }


    private static <O extends InterMineObject> O cloneInterMineObject(O obj,
            boolean copyCollections) throws IllegalAccessException {
        @SuppressWarnings("unchecked") Class<O> clazz = (Class<O>) obj.getClass();
        O newObj = DynamicUtil.createObject(clazz);

        for (String fieldName : TypeUtil.getFieldInfos(obj.getClass()).keySet()) {
            Object value = obj.getFieldProxy(fieldName);
            if (copyCollections && (value instanceof Collection<?>)) {
                newObj.setFieldValue(fieldName, new HashSet<Object>((Collection<?>) value));
            } else {
                newObj.setFieldValue(fieldName, value);
            }
        }
        return newObj;
    }


    /**
     * Return an iterator over the results of a query that connects two classes by a third using
     * arbitrary fields.
     * eg. To find Genes, Exon pairs where
     *   "gene.transcripts CONTAINS transcript AND transcript.exons CONTAINS exon"
     * pass Gene.class, "transcripts", Transcript.class, "exons", Exon.class
     * @param os an ObjectStore to query
     * @param sourceClass the first class in the query
     * @param sourceClassFieldName the field in the sourceClass which should contain the
     * connectingClass
     * @param connectingClass the class referred to by sourceClass.sourceFieldName
     * @param connectingClassFieldName the field in connectingClass which should contain
     * destinationClass
     * @param destinationClass the class referred to by
     * connectingClass.connectingClassFieldName
     * @param orderBySource if true query will be ordered by sourceClass
     * @return an iterator over the results - (Gene, Exon) pairs
     * @throws ObjectStoreException if problem reading ObjectStore
     * @throws IllegalAccessException if one of the field names doesn't exist in the corresponding
     * class.
     */
    public static Iterator<ResultsRow<InterMineObject>> findConnectingClasses(ObjectStore os,
            Class<? extends InterMineObject> sourceClass, String sourceClassFieldName,
            Class<? extends InterMineObject> connectingClass, String connectingClassFieldName,
            Class<? extends InterMineObject> destinationClass, boolean orderBySource)
        throws ObjectStoreException, IllegalAccessException {

        Query q = new Query();

        // we know that all rows will be distinct because there shouldn't be more than one relation
        // connecting the two objects
        q.setDistinct(false);
        QueryClass qcSource = new QueryClass(sourceClass);
        q.addFrom(qcSource);
        q.addToSelect(qcSource);
        if (orderBySource) {
            q.addToOrderBy(qcSource);
        }
        QueryClass qcConnecting = new QueryClass(connectingClass);
        q.addFrom(qcConnecting);
        QueryClass qcDest = new QueryClass(destinationClass);
        q.addFrom(qcDest);
        q.addToSelect(qcDest);
        if (!orderBySource) {
            q.addToOrderBy(qcDest);
        }
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryCollectionReference ref1 =
            new QueryCollectionReference(qcSource, sourceClassFieldName);
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcConnecting);
        cs.addConstraint(cc1);
        QueryReference ref2;

        Map<String, FieldDescriptor> descriptorMap = os.getModel()
            .getFieldDescriptorsForClass(connectingClass);
        FieldDescriptor fd = descriptorMap.get(connectingClassFieldName);

        if (fd == null) {
            throw new IllegalAccessException("cannot find field \"" + connectingClassFieldName
                                             + "\" in class " + connectingClass.getName());
        }

        if (fd.isReference()) {
            ref2 = new QueryObjectReference(qcConnecting, connectingClassFieldName);
        } else {
            ref2 = new QueryCollectionReference(qcConnecting, connectingClassFieldName);
        }
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcDest);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants
                                                   .PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 5000, true, true, true);

        @SuppressWarnings("unchecked") Iterator<ResultsRow<InterMineObject>> retval = (Iterator) res
            .iterator();
        return retval;
    }

}
