package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;

/**
 * @author "Xavier Watkins"
 *
 */
public class OrthologueConverter extends BagConverter
{
    private Model model;
    private static final Logger LOG = Logger.getLogger(OrthologueConverter.class);
    /**
     * @param im intermine api
     * @param webConfig the webconfig
     */
    public OrthologueConverter(InterMineAPI im, WebConfig webConfig) {
        super(im, webConfig);
        model = im.getModel();
    }

    private PathQuery constructPathQuery(String organismName) {
        PathQuery q = new PathQuery(model);

        if (StringUtils.isNotEmpty(organismName)) {
            q.addConstraint(Constraints.eq("Gene.homologues.homologue.organism.shortName",
                organismName));
        }

        // homologue.type = "orthologue"
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));
        return q;
    }

    /**
     * runs the orthologue conversion pathquery and returns list of intermine IDs
     * used in the portal
     * @param profile the user's profile
     * @param bagType not used, always gene
     * @param bagList list of intermine object IDs
     * @param organismName name of homologue's organism
     * @return list of intermine IDs
     */
    public List<Integer> getConvertedObjectIds(Profile profile, String bagType,
            List<Integer> bagList, String organismName) {
        PathQuery pathQuery = constructPathQuery(organismName);
        pathQuery.addConstraint(Constraints.inIds("Gene", bagList));
        pathQuery.addView("Gene.homologues.homologue.id");
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(pathQuery);
        List<Integer> ids = new ArrayList<Integer>();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            ids.add((Integer) row.get(0).getField());
        }
        return ids;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getCounts(ObjectStore os, Model model, InterMineBag bag) {

        QueryClass qcGene = new QueryClass(Gene.class);
        QueryClass qcHomologue = null;
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryClass qcHomologueGene = new QueryClass(Gene.class);

        try {
            qcHomologue = new QueryClass(Class.forName(model.getPackageName() + ".Homologue"));
        } catch (ClassNotFoundException e) {
            LOG.error("Error counting orthologues on list analysis page", e);
            return null;
        }

        QueryField qfId = new QueryField(qcGene, "id");
        QueryField qfOrganism = new QueryField(qcOrganism, "shortName");

        QueryFunction objectCount = new QueryFunction();

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // genes in list
        cs.addConstraint(new BagConstraint(qfId, ConstraintOp.IN, bag.getContentsAsIds()));

        // gene.homologues
        QueryCollectionReference qr = new QueryCollectionReference(qcGene, "homologues");
        cs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcHomologue));

        // gene.homologues.homologue
        QueryObjectReference qor1 = new QueryObjectReference(qcHomologue, "homologue");
        cs.addConstraint(new ContainsConstraint(qor1, ConstraintOp.CONTAINS, qcHomologueGene));

        // gene.homologue.homologue.organism.shortName
        QueryObjectReference qor2 = new QueryObjectReference(qcHomologueGene, "organism");
        cs.addConstraint(new ContainsConstraint(qor2, ConstraintOp.CONTAINS, qcOrganism));

        Query q = new Query();

        q.addFrom(qcGene);
        q.addFrom(qcOrganism);
        q.addFrom(qcHomologue);
        q.addFrom(qcHomologueGene);

        q.setConstraint(cs);

        q.addToSelect(qfOrganism);
        q.addToSelect(objectCount);
        q.addToGroupBy(qfOrganism);
        q.addToOrderBy(qfOrganism);

        LOG.error("querying for orthologues " + q.toString());

        Map<String, String> results = new LinkedHashMap<String, String>();
        Results r = os.execute(q);
        Iterator iter = r.iterator();
        while (iter.hasNext()) {
            ResultsRow rr =  (ResultsRow) iter.next();
            String name = String.valueOf(rr.get(0));
            String count = String.valueOf(rr.get(1));
            results.put(name, count);
            LOG.error(name + " - # orthologues : " + count);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public ActionMessage getActionMessage(String externalids, int convertedSize, String type,
            String parameter) throws UnsupportedEncodingException {
        if (StringUtils.isEmpty(parameter)) {
            return null;
        }

        PathQuery q = new PathQuery(model);

        // add columns to the output
        q.addViewSpaceSeparated("Gene.primaryIdentifier "
                + "Gene.organism.shortName "
                + "Gene.homologues.homologue.primaryIdentifier "
                + "Gene.homologues.homologue.organism.shortName "
                + "Gene.homologues.type "
                + "Gene.homologues.dataSets.name");

        // homologue.type = "orthologue"
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));

        // organism
        q.addConstraint(Constraints.lookup("Gene.organism", parameter, ""));

        // if the XML is too long, the link generates "HTTP Error 414 - Request URI too long"
        if (externalids.length() < 4000) {
            q.addConstraint(Constraints.lookup("Gene.homologues.homologue", externalids, ""));
        }

        String query = q.toXml(PathQuery.USERPROFILE_VERSION);
        String encodedurl = URLEncoder.encode(query, "UTF-8");

        String[] values = new String[] {
            String.valueOf(convertedSize),
            parameter,
            String.valueOf(externalids.split(",").length),
            type,
            encodedurl };
        ActionMessage am = new ActionMessage("portal.orthologues", values);
        return am;
    }
}
