package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Utility methods for the flymine package.
 * @author Julie Sullivan
 */
public final class BioUtil
{
    protected static final Logger LOG = Logger.getLogger(BioUtil.class);
    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();

    private BioUtil() {
        // don't
    }

    /**
     * For a bag of objects, returns a list of organisms
     * @param os ObjectStore
     * @param lowercase if true, the organism names will be returned in lowercase
     * @param bagContents IDs of objects in bag
     * @param type type of bag
     * @return collection of organism names
     */
    public static Collection<String> getOrganisms(ObjectStore os, String type,
            List<Integer> bagContents, boolean lowercase) {
        return getOrganisms(os, type, bagContents, lowercase, "name");
    }

    /**
     * For a bag of objects, returns a list of organisms.
     * @param os ObjectStore
     * @param lowercase if true, the organism names will be returned in lowercase
     * @param bagContentsAsIds list of IDs in the bag
     * @param organismFieldName eg. name, shortName or taxonId
     * @param type type of bag
     * @return collection of organism names
     */
    public static Collection<String> getOrganisms(ObjectStore os, String type,
            List<Integer> bagContentsAsIds,
            boolean lowercase, String organismFieldName) {

        Query q = new Query();
        Model model = os.getModel();
        QueryClass qcObject = null;
        try {
            String className = model.getPackageName() + "." + type;
            qcObject  = new QueryClass(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return null;
        }
        QueryClass qcOrganism
            = new QueryClass(model.getClassDescriptorByName("Organism").getType());

        QueryField qfOrganismName = new QueryField(qcOrganism, "name");

        QueryField qfGeneId = new QueryField(qcObject, "id");

        q.addFrom(qcObject);
        q.addFrom(qcOrganism);

        if ("name".equals(organismFieldName)) {
            q.addToSelect(qfOrganismName);
            q.addToOrderBy(qfOrganismName);
        } else if ("taxonId".equals(organismFieldName) || "shortName".equals(organismFieldName)) {
            // will either be taxonId or shortname
            QueryField qfOrganism = new QueryField(qcOrganism, organismFieldName);
            q.addToSelect(qfOrganism);
            q.addToOrderBy(qfOrganism);
        } else {
            throw new RuntimeException(organismFieldName + " is not a valid field for Organism");
        }

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bagContentsAsIds);
        cs.addConstraint(bc);

        QueryObjectReference qr = new QueryObjectReference(qcObject, "organism");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc);

        q.setConstraint(cs);

        Results r = os.execute(q);
        Iterator<ResultsRow> it = ((Iterator) r.iterator());
        Collection<String> orgs = new ArrayList();

        while (it.hasNext()) {
            ResultsRow rr = it.next();
            Object org =  rr.get(0);
            if (org != null) {
                if (lowercase) {
                    orgs.add(org.toString().toLowerCase());
                } else {
                    orgs.add(org.toString());
                }
            }
        }
        return orgs;
    }

    /**
     * Looks in the organism repo for the taxon ID provided.  If the taxon ID is not there, it looks
     * for strains that use that ID.  Will return NULL if there is no strain and no taxon ID in
     * the organism data.
     *
     * @param taxonId original taxon ID
     * @return taxonId for organism, not the strain
     */
    public static Integer replaceStrain(Integer taxonId) {
        OrganismData od = OR.getOrganismDataByTaxon(taxonId);
        if (od == null) {
            return taxonId;
        }
        return new Integer(od.getTaxonId());
    }

}
