package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2008 FlyMine
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

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.WidgetUtil;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Organism;

/**
 * Utility methods for the flymine package.
 * @author Julie Sullivan
 */
public class BioUtil implements WidgetUtil
{
    /**
     * Constructor (required for widgets)
     */
    public BioUtil() {
        super();
    }

    /**
     * For a bag of objects, returns a list of organisms
     * @param os ObjectStore
     * @param lowercase if true, the organism names will be returned in lowercase
     * @param bag InterMineBag
     * @return collection of organism names
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getOrganisms(ObjectStore os, InterMineBag bag,
                                                  boolean lowercase) {

        Query q = new Query();
        Model model = os.getModel();
        QueryClass qcObject = null;
        try {
            qcObject  = new QueryClass(Class.forName(model.getPackageName() + "." + bag.getType()));
        } catch (ClassNotFoundException e) {
            return null;
        }
        QueryClass qcOrganism = new QueryClass(Organism.class);

        QueryField qfOrganismName = new QueryField(qcOrganism, "name");
        QueryField qfGeneId = new QueryField(qcObject, "id");

        q.addFrom(qcObject);
        q.addFrom(qcOrganism);

        q.addToSelect(qfOrganismName);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        BagConstraint bc = new BagConstraint(qfGeneId, ConstraintOp.IN, bag.getOsb());
        cs.addConstraint(bc);

        QueryObjectReference qr = new QueryObjectReference(qcObject, "organism");
        ContainsConstraint cc = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcOrganism);
        cs.addConstraint(cc);

        q.setConstraint(cs);

        q.addToOrderBy(qfOrganismName);

        Results r = os.execute(q);
        Iterator<ResultsRow> it = r.iterator();
        Collection<String> organismNames = new ArrayList<String>();

        while (it.hasNext()) {
            ResultsRow rr = it.next();
            String organismsName =  (String) rr.get(0);
            if (lowercase) {
                organismNames.add(organismsName.toLowerCase());
            } else {
                organismNames.add(organismsName);
            }
        }
        return organismNames;
    }


    /**
     * Return a list of chromosomes for specified organism
     * @param os ObjectStore
     * @param organisms Organism names.  Assumes they are lowercase.
     * @param lowercase if true returns lowercase chromosome names.  the precomputed tables indexes
     * are all lowercase, so the chromosome names need to be lowercase when used in queries
     * @return collection of chromosome names
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getChromosomes(ObjectStore os,
                                                    Collection<String> organisms,
                                                    boolean lowercase) {


        final String dmel = "drosophila melanogaster";
        ArrayList<String> chromosomes = new ArrayList<String>();

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

        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        QueryClass qcOrganism = new QueryClass(Organism.class);
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
        Iterator it = r.iterator();

        while (it.hasNext()) {
            ResultsRow rr =  (ResultsRow) it.next();
            String chromosome = (String) rr.get(0);
            if (lowercase) {
                chromosome.toLowerCase();
            }
            chromosomes.add(chromosome);
        }
        return chromosomes;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getExtraAttributes(ObjectStore os, InterMineBag bag) {
        return getOrganisms(os, bag, false);
    }
}
