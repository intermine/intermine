package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.ClassKeyHelper;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Synonym;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * This post-process sets the isPrimary field on all Synonyms that have a value field that matches
 * the corresponding BioEntity.
 * @author Kim Rutherford
 */

public class SynonymUpdater
{
    protected ObjectStoreWriter osw;
    private Model model;
 
    /**
     * Make a new SynonymUpdater object
     * @param osw an ObjectStore to write to
     */
    public SynonymUpdater (ObjectStoreWriter osw) {
        setObjectStoreWriter(osw);
    }

    /**
     * Create a new SynonymUpdater.  setObjectStoreWriter() must be called before use.
     */
    public SynonymUpdater() {
        // empty
    }
    
    /**
     * Set the isPrimary flag of the Synonym object where the value of the Synonym matches one of
     * the class key fields of the subject BioEntity.
     * @throws Exception if there is a problem while processing
     */
    public void update() throws Exception {
        // TODO Auto-generated method stub

        InputStream is = getClassKeysInputStream();
        
        Properties classKeyProperties = new Properties();
        classKeyProperties.load(is);
        
        Map classKeyMap = ClassKeyHelper.readKeys(model, classKeyProperties);
        
        Query q = new Query();

        q.setDistinct(false);

        QueryClass bioentityQC = new QueryClass(BioEntity.class);
        q.addToSelect(bioentityQC);
        q.addFrom(bioentityQC);
        q.addToOrderBy(bioentityQC);

        QueryClass synonymQC = new QueryClass(Synonym.class);
        q.addFrom(synonymQC);
        q.addToSelect(synonymQC);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference synonymsRef =
                new QueryCollectionReference(bioentityQC, "synonyms");
        ContainsConstraint synonymConstraint =
                new ContainsConstraint(synonymsRef, ConstraintOp.CONTAINS, synonymQC);
        cs.addConstraint(synonymConstraint);

        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(5000);

        Iterator resIter = res.iterator();
        
        osw.beginTransaction();
        
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            BioEntity bioEntity= (BioEntity) rr.get(0);
            Synonym synonym = (Synonym) rr.get(1);
            String synonymValue = synonym.getValue();
            Set classes = DynamicUtil.decomposeClass(bioEntity.getClass());
            // clone so we don't change the ObjectStore cache
            Synonym synonymCopy = (Synonym) PostProcessUtil.cloneInterMineObject(synonym);
            boolean isPrimary = false;
            Iterator classesIter = classes.iterator();
            CLASSES:
                while (classesIter.hasNext()) {
                String className = ((Class) classesIter.next()).getName();
                Collection keyFields = ClassKeyHelper.getKeyFields(classKeyMap, className);
                Iterator keyFieldIter = keyFields.iterator();
                while (keyFieldIter.hasNext()) {
                    Set parts = (Set) keyFieldIter.next();
                    String fieldName = ((FieldDescriptor) parts.iterator().next()).getName();
                    Object fieldValue = TypeUtil.getFieldValue(bioEntity, fieldName);
                    if (fieldValue.equals(synonymValue)) {
                        isPrimary = true;
                        break CLASSES;
                    }
                }
            }
            if (isPrimary) {
                synonymCopy.setIsPrimary(Boolean.TRUE);
            } else {
                synonymCopy.setIsPrimary(Boolean.FALSE);
            }
            osw.store(synonymCopy);
        }
        
        osw.commitTransaction();
    }

    /**
     * Return the InputStream containing the list of class keys.
     * @return the file name
     * @throws Exception if the file can't be found
     */
    protected InputStream getClassKeysInputStream() throws Exception {
        return new FileInputStream("../../bio/core/props/resources/class_keys.properties");

    }

    /**
     * Set the ObjectStoreWriter to use when processing.
     * @param osw the ObjectStoreWriter
     */
    public void setObjectStoreWriter(ObjectStoreWriter osw) {
        this.osw = osw;
        this.model = osw.getModel();
    }
}
