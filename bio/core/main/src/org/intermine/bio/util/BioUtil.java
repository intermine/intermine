package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryExpression;
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

    private static final OrganismRepository OR = OrganismRepository.getOrganismRepository();

    private BioUtil() {
        // don't
    }

    /**
     * For a bag of objects, returns a list of organisms
     * @param os ObjectStore
     * @param lowercase if true, the organism names will be returned in lowercase
     * @param bag InterMineBag
     * @return collection of organism names
     */
    public static Collection<String> getOrganisms(ObjectStore os, InterMineBag bag,
            boolean lowercase) {
        return getOrganisms(os, bag, lowercase, "name");
    }

    /**
     * For a bag of objects, returns a list of organisms.
     * @param os ObjectStore
     * @param lowercase if true, the organism names will be returned in lowercase
     * @param bag InterMineBag
     * @param organismFieldName eg. name, shortName or taxonId
     * @return collection of organism names
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Collection<String> getOrganisms(ObjectStore os, InterMineBag bag,
            boolean lowercase, String organismFieldName) {

        Query q = new Query();
        Model model = os.getModel();
        QueryClass qcObject = null;
        try {
            String className = model.getPackageName() + "." + bag.getType();
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
        BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
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
     * Return a list of chromosomes for specified organism
     * @param os ObjectStore
     * @param organisms Organism names.  Assumes they are lowercase.
     * @param lowercase if true returns lowercase chromosome names.  the precomputed tables indexes
     * are all lowercase, so the chromosome names need to be lowercase when used in queries
     * @return collection of chromosome names
     */
    public static Collection<String> getChromosomes(ObjectStore os, Collection<String> organisms,
            boolean lowercase) {
        Model model = os.getModel();

        final String dmel = "drosophila melanogaster";
        ArrayList<String> chromosomes = new ArrayList<String>();

        if (organisms.contains("homo sapiens")) {
            chromosomes.add("1");
            chromosomes.add("2");
            chromosomes.add("3");
            chromosomes.add("4");
            chromosomes.add("5");
            chromosomes.add("6");
            chromosomes.add("7");
            chromosomes.add("8");
            chromosomes.add("9");
            chromosomes.add("10");
            chromosomes.add("11");
            chromosomes.add("12");
            chromosomes.add("13");
            chromosomes.add("14");
            chromosomes.add("15");
            chromosomes.add("16");
            chromosomes.add("17");
            chromosomes.add("18");
            chromosomes.add("19");
            chromosomes.add("20");
            chromosomes.add("21");
            chromosomes.add("22");
            if (lowercase) {
                chromosomes.add("x");
                chromosomes.add("y");
            } else {
                chromosomes.add("X");
                chromosomes.add("Y");
            }
            if (organisms.size() == 1) {
                return chromosomes;
            }
            organisms.remove("homo sapiens");
        }

        // TODO this may well go away once chromosomes sorted out in #1186
        if (organisms.contains(dmel)) {
            if (lowercase) {
                chromosomes.add("2l");
                chromosomes.add("2r");
                chromosomes.add("3l");
                chromosomes.add("3r");
                chromosomes.add("4");
                chromosomes.add("u");
                chromosomes.add("x");
            } else {
                chromosomes.add("2L");
                chromosomes.add("2R");
                chromosomes.add("3L");
                chromosomes.add("3R");
                chromosomes.add("4");
                chromosomes.add("U");
                chromosomes.add("X");
            }
            if (organisms.size() == 1) {
                return chromosomes;
            }
            organisms.remove(dmel);
        }

        Query q = new Query();

        QueryClass qcChromosome
            = new QueryClass(model.getClassDescriptorByName("Chromosome").getType());
        QueryClass qcOrganism
            = new QueryClass(model.getClassDescriptorByName("Organism").getType());
        QueryField qfChromosome = new QueryField(qcChromosome, "primaryIdentifier");
        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        q.addFrom(qcChromosome);
        q.addFrom(qcOrganism);

        q.addToSelect(qfChromosome);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference qr = new QueryObjectReference(qcChromosome, "organism");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc);

        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, qfOrganismName);
        BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, organisms);
        cs.addConstraint(bc);

        q.setConstraint(cs);

        q.addToOrderBy(qfChromosome);

        Results r = os.execute(q);
        Iterator<?> it = r.iterator();

        while (it.hasNext()) {
            ResultsRow<?> rr =  (ResultsRow<?>) it.next();
            String chromosome = (String) rr.get(0);
            if (lowercase) {
                chromosome.toLowerCase();
            }
            chromosomes.add(chromosome);
        }
        return chromosomes;
    }

    /**
     * Get the extra attributes needed for the DataSetLoader
     * @param os the objectstore
     * @param bag the bag
     * @return a collection of strings to pass to the datasetloader
     */
    public static Collection<String> getExtraAttributes(ObjectStore os, InterMineBag bag) {
        return getOrganisms(os, bag, false);
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
