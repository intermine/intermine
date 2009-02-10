package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.flymine.model.genomic.Location;
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
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Common operations for post processing.
 *
 * @author Richard Smith
 */
public class PostProcessUtil
{

    /**
     * Create a clone of given InterMineObject including the id.  This is designed for
     * altering and storing again (to avoid cache problems) so doesn't copy collections.
     * @param obj object to clone
     * @return the cloned object
     * @throws IllegalAccessException if problems with reflection
     */
    public static InterMineObject cloneInterMineObject(InterMineObject obj)
        throws IllegalAccessException {
        return PostProcessUtil.cloneInterMineObject(obj, false);
    }


    /**
     * Create a copy of given InterMineObject with *no* id set and copies of collections
     * @param obj object to copy
     * @return the copied object
     * @throws IllegalAccessException if problems with reflection
     */
    public static InterMineObject copyInterMineObject(InterMineObject obj)
        throws IllegalAccessException {
        InterMineObject newObj = cloneInterMineObject(obj, true);
        newObj.setId(null);
        return newObj;
    }


    private static InterMineObject cloneInterMineObject(InterMineObject obj,
                                                        boolean copyCollections)
    throws IllegalAccessException {
        InterMineObject newObj = (InterMineObject)
        DynamicUtil.createObject(DynamicUtil.decomposeClass(obj.getClass()));
        Map fieldInfos = new HashMap();
        Iterator clsIter = DynamicUtil.decomposeClass(obj.getClass()).iterator();
        while (clsIter.hasNext()) {
            fieldInfos.putAll(TypeUtil.getFieldInfos((Class) clsIter.next()));
        }

        Iterator fieldIter = fieldInfos.keySet().iterator();
        while (fieldIter.hasNext()) {
            String fieldName = (String) fieldIter.next();
            Object value = TypeUtil.getFieldProxy(obj, fieldName);
            if (copyCollections && (value instanceof Collection)) {
                TypeUtil.setFieldValue(newObj, fieldName,
                    new HashSet((Collection) value));
            } else {
                TypeUtil.setFieldValue(newObj, fieldName, value);
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
    public static Iterator findConnectingClasses(ObjectStore os,
                                                 Class sourceClass, String sourceClassFieldName,
                                                 Class connectingClass,
                                                 String connectingClassFieldName,
                                                 Class destinationClass, boolean orderBySource)
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

        Map descriptorMap = os.getModel().getFieldDescriptorsForClass(connectingClass);
        FieldDescriptor fd = (FieldDescriptor) descriptorMap.get(connectingClassFieldName);

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
        Results res = os.execute(q, 500, true, true, true);

        return res.iterator();
    }


    /**
     * Return an iterator over all objects of the given class in the ObjectStore provided.
     * @param os an ObjectStore to query
     * @param cls the class to select instances of
     * @return an iterator over the results
     */
    public static Iterator selectObjectsOfClass(ObjectStore os, Class cls) {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(cls);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = os.executeSingleton(q, 500, true, true, true);
        return res.iterator();
    }



    /**
     * Query ObjectStore for all Location object that conect the given BioEntity classes.
     * (eg. Contig->Supercontig->Chromosome)
     * @param os the ObjectStore to find the Locations in
     * @param firstClass the first BioEntity of the three (eg. Contig)
     * @param secondClass the second BioEntity (eg. Supercontig)
     * @param thirdClass the third BioEntity (eg. Chromosome)
     * @param batchSize the batch size for the results object
     * @return a Results object with rows: firstObject, locationFirstToSecond, secondObject,
     * locationSecondToThird, thirdObject
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Results findLocationsToTransform(ObjectStore os, Class firstClass,
            Class secondClass, Class thirdClass, int batchSize)
        throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcFirst = new QueryClass(firstClass);
        q.addFrom(qcFirst);
        q.addToSelect(qcFirst);

        QueryClass qcFirstLoc = new QueryClass(Location.class);
        q.addFrom(qcFirstLoc);
        q.addToSelect(qcFirstLoc);

        QueryClass qcSecond = new QueryClass(secondClass);
        q.addFrom(qcSecond);
        q.addToSelect(qcSecond);

        QueryClass qcSecondLoc = new QueryClass(Location.class);
        q.addFrom(qcSecondLoc);
        q.addToSelect(qcSecondLoc);

        QueryClass qcThird = new QueryClass(thirdClass);
        q.addFrom(qcThird);
        q.addToSelect(qcThird);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference ref1 = new QueryObjectReference(qcFirstLoc, "subject");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcFirst);
        cs.addConstraint(cc1);

        QueryObjectReference ref2 = new QueryObjectReference(qcFirstLoc, "object");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSecond);
        cs.addConstraint(cc2);

        QueryObjectReference ref3 = new QueryObjectReference(qcSecondLoc, "subject");
        ContainsConstraint cc3 = new ContainsConstraint(ref3, ConstraintOp.CONTAINS, qcSecond);
        cs.addConstraint(cc3);

        QueryObjectReference ref4 = new QueryObjectReference(qcSecondLoc, "object");
        ContainsConstraint cc4 = new ContainsConstraint(ref4, ConstraintOp.CONTAINS, qcThird);
        cs.addConstraint(cc4);

        q.setConstraint(cs);
        ((ObjectStoreInterMineImpl) os).precompute(q, Constants
                                                   .PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, batchSize, true, true, true);

        return res;
    }
}
