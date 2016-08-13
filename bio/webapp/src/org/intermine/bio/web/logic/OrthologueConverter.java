package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
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
     * @throws ObjectStoreException if can't store to database
     */
    public List<Integer> getConvertedObjectIds(Profile profile, String bagType,
            List<Integer> bagList, String organismName) throws ObjectStoreException {
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
     * @throws ObjectStoreException if the query cannot be run.
     */
    public Map<String, String> getCounts(Profile profile, InterMineBag bag)
        throws ObjectStoreException {
        PathQuery pathQuery = constructPathQuery(null);
        pathQuery.addConstraint(Constraints.inIds("Gene", bag.getContentsAsIds()));
        pathQuery.addView("Gene.homologues.homologue.organism.shortName");
        pathQuery.addView("Gene.homologues.homologue.id");
        pathQuery.addOrderBy("Gene.homologues.homologue.organism.shortName", OrderDirection.ASC);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(pathQuery);
        Map<String, String> results = new LinkedHashMap<String, String>();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String homologue = (String) row.get(0).getField();
            String count = results.get(homologue);
            if (count == null) {
                count = "0";
            }
            int plusOne = Integer.parseInt(count);
            results.put(homologue, String.valueOf(++plusOne));
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
