package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

import org.intermine.model.InterMineObject;
import org.intermine.metadata.FieldDescriptor;

import org.flymine.model.genomic.Annotation;
import org.flymine.model.genomic.Location;

/**
 * Common operations for post processing.
 *
 * @author Richard Smith
 */
public class PostProcessUtil
{

    /**
     * Create a clone of given InterMineObject including the id
     * @param obj object to clone
     * @return the cloned object
     * @throws IllegalAccessException if problems with reflection
     */
    public static InterMineObject cloneInterMineObject(InterMineObject obj)
        throws IllegalAccessException {
        InterMineObject newObj = copyInterMineObject(obj);
        newObj.setId(obj.getId());
        return newObj;
    }


    /**
     * Create a copy of given InterMineObject with *no* id set
     * @param obj object to copy
     * @return the copied object
     * @throws IllegalAccessException if problems with reflection
     */
    public static InterMineObject copyInterMineObject(InterMineObject obj)
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
            if (!fieldName.equals("id")) {
                TypeUtil.setFieldValue(newObj, fieldName,
                                       TypeUtil.getFieldProxy(obj, fieldName));
            }
        }
        return newObj;
    }

    /**
     * Query ObjectStore for all objects that connect the given object and any subject classes.
     * Return an iterator ordered by objectCls.
     * e.g. To return Transcript and Location where Transcript.subjects CONTAINS Location pass
     * Transcript.class, Location.class, "subjects"
     * @param os an ObjectStore to query
     * @param mainCls object type of the first class
     * @param referredCls type of the second
     * @param colName name of collection in mainCls that contains objects of type referredCls
     * @return an iterator over the results
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Iterator findConnectedClasses(ObjectStore os, Class mainCls, Class referredCls,
                                                String colName) throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(mainCls);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);
        QueryClass qcRel = new QueryClass(referredCls);
        q.addFrom(qcRel);
        q.addToSelect(qcRel);
        q.addToOrderBy(qcObj);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryCollectionReference col1 = new QueryCollectionReference(qcObj, colName);
        ContainsConstraint cc1 = new ContainsConstraint(col1, ConstraintOp.CONTAINS, qcRel);
        cs.addConstraint(cc1);
        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask
                                                   .PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);
        return res.iterator();
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

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask
                                                   .PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);

        return res.iterator();
    }

    /**
     * Query ObjectStore for all SymmetricalRelation objects (or specified subclass)
     * for the given object (ie. Obj1 <- SymmetricalRelation -> Obj2  - obj1 and obj2 will be in the
     * bioEntities collection of the SymmetricalRelation).
     * Return an iterator over: obj1, SymmetricalRelation, obj2 order by obj1
     * @param os an ObjectStore to query
     * @param objectCls the type in the bioEntities collection of the SymmetricalRelation
     * @param relationCls type of SymmetricalRelation
     * @return an iterator over the results.  Each pair of objects will be returned twice: (obj1,
     * rel, obj2) and (obj2, rel, obj1).
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Iterator findSymmetricalRelation(ObjectStore os, Class objectCls,
                                                   Class relationCls) throws ObjectStoreException {
        // TODO check objectCls and subjectCls assignable to BioEntity
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj1 = new QueryClass(objectCls);
        q.addFrom(qcObj1);
        q.addToSelect(qcObj1);
        q.addToOrderBy(qcObj1);
        QueryClass qcRel = new QueryClass(relationCls);
        q.addFrom(qcRel);
        q.addToSelect(qcRel);
        QueryClass qcObj2 = new QueryClass(objectCls);
        q.addFrom(qcObj2);
        q.addToSelect(qcObj2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryCollectionReference ref1 = new QueryCollectionReference(qcRel, "bioEntities");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj1);
        cs.addConstraint(cc1);
        QueryCollectionReference ref2 = new QueryCollectionReference(qcRel, "bioEntities");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcObj2);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask
                                                   .PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);
        return res.iterator();
    }


    /**
     * Query ObjectStore for BioProperty subclasses related to BioEntities by an
     * Annotation object.  Select BioEntity and BioProperty.  Return an iterator
     * ordered by BioEntity
     * @param os an ObjectStore to query
     * @param entityCls type of BioEntity
     * @param propertyCls class of BioProperty to select
     * @return an iterator over the results
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Iterator findProperties(ObjectStore os, Class entityCls, Class propertyCls)
        throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcEntity = new QueryClass(entityCls);
        q.addFrom(qcEntity);
        q.addToSelect(qcEntity);
        QueryClass qcAnn = new QueryClass(Annotation.class);
        q.addFrom(qcAnn);
        QueryClass qcProperty = new QueryClass(propertyCls);
        q.addFrom(qcProperty);
        q.addToSelect(qcProperty);
        q.addToOrderBy(qcEntity);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryCollectionReference col1 = new QueryCollectionReference(qcEntity, "annotations");
        ContainsConstraint cc1 = new ContainsConstraint(col1, ConstraintOp.CONTAINS, qcAnn);
        cs.addConstraint(cc1);
        QueryObjectReference ref1 = new QueryObjectReference(qcAnn, "property");
        ContainsConstraint cc2 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcProperty);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask
                                                   .PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(500);
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
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(500);
        return res.iterator();
    }

    /**
     * Query ObjectStore for all Location object between given object (eg. Chromosome) and
     * subject (eg. Gene) classes.  Return an iterator over the results ordered by subject if
     * orderBySubject is true, otherwise order by object.
     * @param os the ObjectStore to find the Locations in
     * @param objectCls object type of the Location
     * @param subjectCls subject type of the Location
     * @param orderBySubject if true order the results using the subjectCls, otherwise order by
     * objectCls
     * @return a Results object: object.id, location, subject
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Results findLocationAndObjects(ObjectStore os, Class objectCls, Class subjectCls,
                                                 boolean orderBySubject)
        throws ObjectStoreException {
        // TODO check objectCls and subjectCls assignable to BioEntity

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(objectCls);
        QueryField qfObj = new QueryField(qcObj, "id");
        q.addFrom(qcObj);
        q.addToSelect(qfObj);
        if (!orderBySubject) {
            q.addToOrderBy(qfObj);
        }
        QueryClass qcSub = new QueryClass(subjectCls);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);
        if (orderBySubject) {
            q.addToOrderBy(qcSub);
        }
        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        q.setConstraint(cs);
        Set indexesToCreate = new HashSet();
        indexesToCreate.add(qfObj);
        indexesToCreate.add(qcLoc);
        indexesToCreate.add(qcSub);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
                                                   PostProcessOperationsTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());

        return res;
    }
    /**
     * Query ObjectStore for all Location object between given object (eg. Chromosome) and
     * subject classes (eg. Gene).  Return an iterator over the results ordered by object
     * @param os the ObjectStore to find the Locations in
     * @param objectCls object type of the Location
     * @param subjectCls subject type of the Location
     * @return a Results object: object.id, location
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Results findLocations(ObjectStore os, Class objectCls, Class subjectCls)
        throws ObjectStoreException {
        // TODO check objectCls and subjectCls assignable to BioEntity

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(objectCls);
        QueryField qfObj = new QueryField(qcObj, "id");
        q.addFrom(qcObj);
        q.addToSelect(qfObj);
        q.addToOrderBy(qfObj);

        QueryClass qcSub = new QueryClass(subjectCls);
        q.addFrom(qcSub);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        q.setConstraint(cs);
        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask
                                                   .PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());

        return res;
    }

    /**
     * Query ObjectStore for all Location object that conect the given BioEntity classes.
     * (eg. Contig->Supercontig->Chromosome)
     * @param os the ObjectStore to find the Locations in
     * @param firstClass the first BioEntity of the three (eg. Contig)
     * @param secondClass the second BioEntity (eg. Supercontig)
     * @param thirdClass the third BioEntity (eg. Chromosome)
     * @return a Results object with rows: firstObject, locationFirstToSecond, secondObject,
     * locationSecondToThird, thirdObject
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Results findLocationsToTransform(ObjectStore os, Class firstClass,
                                                   Class secondClass, Class thirdClass)
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
        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessOperationsTask
                                                   .PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());

        return res;
    }
}
