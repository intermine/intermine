package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.intermine.model.InterMineObject;
import org.flymine.model.genomic.*;

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
     * @throws Exception if problems with reflection
     */
    public static InterMineObject cloneInterMineObject(InterMineObject obj) throws Exception {
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
            TypeUtil.setFieldValue(newObj, fieldName,
                                   TypeUtil.getFieldProxy(obj, fieldName));
        }
        return newObj;
    }

    /**
     * Query ObjectStore for all Relation objects (or specified subclass)
     * between given object and subject classes.  Return an iterator ordered
     * by objectCls.
     * @param os an ObjectStore to query
     * @param objectCls object type of the Relation
     * @param subjectCls subject type of the Relation
     * @param relationCls type of relation
     * @return an iterator over the results
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Iterator findRelations(ObjectStore os, Class objectCls, Class subjectCls,
                                         Class relationCls) throws ObjectStoreException {
        // TODO check objectCls and subjectCls assignable to BioEntity

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(objectCls);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);
        QueryClass qcSub = new QueryClass(subjectCls);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);
        QueryClass qcRel = new QueryClass(relationCls);
        q.addFrom(qcRel);
        q.addToSelect(qcRel);
        q.addToOrderBy(qcObj);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcRel, "object");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcRel, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(10000);
        return res.iterator();
    }

    /**
     * Query ObjectStore for Genes and their Exons.
     * @param os an ObjectStore to query
     * @return an iterator over the results - (Gene, Exon) pairs
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    public static Iterator findGeneExonRelations(ObjectStore os) throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);
        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
        q.addToOrderBy(qcGene);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcTranscript, "gene");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcGene);
        cs.addConstraint(cc1);
        QueryCollectionReference ref2 = new QueryCollectionReference(qcTranscript, "exons");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcExon);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(10000);
        return res.iterator();
    }

    /**
     * Return an iterator over all objects of the given class in the ObjectStore provided.
     * @param os an ObjectStore to query
     * @param cls the class to select instances of
     * @return an iterator over the results
     * @throws ObjectStoreException if problem running query
     */
    public static Iterator selectObjectsOfClass(ObjectStore os, Class cls)
        throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qc = new QueryClass(cls);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        res.setBatchSize(10000);
        return res.iterator();
    }
}
