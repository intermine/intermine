package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * Update GWAS objects with name and firstAuthor from referenced publication.
 *
 * @author Richard Smith
 */
public class EnsemblGwasDbPostprocess extends PostProcessor
{
    private static final Logger LOG = Logger.getLogger(EnsemblGwasDbPostprocess.class);
    protected ObjectStore os;

    /**
     * Create a new GWAS post processor from an ObjectStoreWriter
     * @param osw writer on production ObjectStore
     */
    public EnsemblGwasDbPostprocess(ObjectStoreWriter osw) {
        super(osw);
        this.os = osw.getObjectStore();
    }

    /**
     * Set names and firstAuthors on GWAS objects from the referenced publication.
     * @throws ObjectStoreException if anything goes wrong
     */
    public void postProcess()
        throws ObjectStoreException {

        long startTime = System.currentTimeMillis();

        Query q = new Query();

        q.setDistinct(false);

        QueryClass qcGwas =
            new QueryClass(os.getModel().getClassDescriptorByName("GWAS").getType());
        q.addFrom(qcGwas);
        q.addToSelect(qcGwas);
        q.addToOrderBy(qcGwas);

        QueryClass qcPub =
            new QueryClass(os.getModel().getClassDescriptorByName("Publication").getType());
        q.addFrom(qcPub);
        q.addToSelect(qcPub);

        QueryObjectReference pubRef = new QueryObjectReference(qcGwas, "publication");
        ContainsConstraint cc = new ContainsConstraint(pubRef, ConstraintOp.CONTAINS, qcPub);

        q.setConstraint(cc);

        Results res = os.execute(q, 5000, true, true, true);
        osw.beginTransaction();
        Iterator resIter = res.iterator();

        int count = 0;

        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            InterMineObject gwas = (InterMineObject) rr.get(0);
            InterMineObject pub = (InterMineObject) rr.get(1);
            
            InterMineObject tempGwas;
            try {
                tempGwas = PostProcessUtil.cloneInterMineObject(gwas);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            try {
                String pubTitle = (String) pub.getFieldValue("title");
                if (!StringUtils.isBlank(pubTitle)) {
                    tempGwas.setFieldValue("name", pubTitle);
                }
                
                String pubAuthor = (String) pub.getFieldValue("firstAuthor");
                if (!StringUtils.isBlank(pubAuthor)) {
                    tempGwas.setFieldValue("firstAuthor", pubAuthor);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            
            osw.store(tempGwas);
            count++;
        }

        LOG.info("Set " + count + " GWAS names and authors"
                + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();
    }
}
