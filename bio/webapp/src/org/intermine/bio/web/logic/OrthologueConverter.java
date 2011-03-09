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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebResults;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;

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

        // organism
        q.addConstraint(Constraints.eq("Gene.homologues.homologue.organism.shortName",
                organismName));

        // homologue.type = "orthologue"
        q.addConstraint(Constraints.eq("Gene.homologues.type", "orthologue"));

        return q;
    }

    /**
     * runs the orthologue conversion pathquery and returns a comma-delimited list of identifiers
     * used on list analysis page for intermine linking, called via Ajax
     *
     * @param profile the user's profile
     * @param bagType the class of the list, has to be gene I think
     * @param bagName name of list
     * @param organismName name of homologue's organism
     * @return commadelimited list of identifiers, eg. eve,zen
     */
    public String getConvertedObjectFields(Profile profile, String bagType, String bagName,
            String organismName) {
        StringBuffer orthologues = new StringBuffer();
        String geneIdentifier = "Gene.homologues.homologue.primaryIdentifier";
        PathQuery pathQuery = constructPathQuery(organismName);
        pathQuery.addConstraint(Constraints.in(bagType, bagName));
        pathQuery.addConstraint(Constraints.isNotNull(geneIdentifier));
        pathQuery.addView(geneIdentifier);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = null;

        try {
            it = executor.execute(pathQuery);
        } catch (Exception e) {
            throw new RuntimeException("bad pathquery", e);
        }

        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String orthologue = row.get(0).getField().toString();
            if (orthologues.length() > 0) {
                orthologues.append(",");
            }
            orthologues.append(orthologue);
        }
        if (orthologues.length() == 0) {
            return null;
        }
        return orthologues.toString();
    }

    /**
     * runs the orthologue conversion pathquery and returns list of intermine IDs
     * used in the portal
     * @param profile the user's profile
     * @param bagType the class of the list, has to be gene I think
     * @param bagList list of intermine object IDs
     * @param organismName name of homologue's organism
     * @return list of intermine IDs
     */
    public List<Integer> getConvertedObjectIds(Profile profile, String bagType,
            List<Integer> bagList, String organismName) {
        PathQuery pathQuery = constructPathQuery(organismName);
        pathQuery.addConstraint(Constraints.inIds(bagType, bagList));
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
        q.addConstraint(Constraints.eq("Gene.homologues.type", "orthologue"));

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

    @Override
    public WebResults getConvertedObjects(Profile profile, List<Integer> fromList, String type,
            String parameters) throws ObjectStoreException {

        PathQuery q = new PathQuery(model);
        List<String> view = PathQueryResultHelper.getDefaultViewForClass(type, model, webConfig,
                "Gene.homologues.homologue");
        q.addViews(view);

        // gene
        q.addConstraint(Constraints.inIds("Gene", fromList));

        // organism
        q.addConstraint(Constraints.lookup("Gene.homologues.homologue.organism", parameters, ""));

        // homologue.type = "orthologue"
        q.addConstraint(Constraints.eq("Gene.homologues.type", "orthologue"));

        WebResultsExecutor executor = im.getWebResultsExecutor(profile);

        return executor.execute(q);
    }
}
