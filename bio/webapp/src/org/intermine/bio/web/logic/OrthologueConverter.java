package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebResults;
import org.intermine.metadata.Model;
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


    // D. melanogaster, C. lupus familiaris
    private static final Pattern ORGANISM_SHORTNAME_MATCHER = Pattern.compile("([a-zA-Z]\\..+)");
    private Model model;


    /**
     * @param im intermine api
     * @param webConfig the webconfig
     */
    public OrthologueConverter(InterMineAPI im, WebConfig webConfig) {
        super(im, webConfig);
        model = im.getModel();

    }

    private PathQuery constructPathQuery(String bagType, String bagName, String organismName) {
        PathQuery q = new PathQuery(model);

        // organism
        q.addConstraint("Gene.homologues.homologue.organism.shortName",
                Constraints.eq(organismName));

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));

        q.addConstraint(bagType, Constraints.in(bagName));
        return q;
    }

    /**
     * runs the orthologue conversion pathquery and returns a comma-delimited list of identifiers
     * @param profile the user's profile
     * @param bagType the class of the list, has to be gene I think
     * @param bagName name of list
     * @param organismName name of homologue's organism
     * @return commadelimited list of identifiers, eg. eve,zen
     */
    public String getConvertedObjectFields(Profile profile, String bagType,
            String bagName, String organismName) {
        StringBuffer orthologues = null;
        PathQuery pathQuery = constructPathQuery(bagType, bagName, organismName);
        pathQuery.setView("Gene.homologues.homologue.primaryIdentifier");
        pathQuery.syncLogicExpression("and");
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(pathQuery);

        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String orthologue = row.get(0).getField().toString();
            if (orthologues != null) {
                orthologues.append(",");
            }
            orthologues.append(orthologue);
        }
        if (orthologues == null) {
            return null;
        }
        return orthologues.toString();
    }

    /**
     * runs the orthologue conversion pathquery and returns list of intermine IDs
     * @param profile the user's profile
     * @param bagType the class of the list, has to be gene I think
     * @param bagName name of list
     * @param organismName name of homologue's organism
     * @return list of intermine IDs
     */
    public List<Integer> getConvertedObjectIds(Profile profile, String bagType,
            String bagName, String organismName) {
        PathQuery pathQuery = constructPathQuery(bagType, bagName, organismName);
        pathQuery.setView(bagType + ".id");
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(pathQuery);
        List<Integer> ids = new ArrayList();
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
            String ... parameters)
                    throws UnsupportedEncodingException {

        String organism = null, dataset = null;

        for (String param : parameters) {
            if (StringUtils.isEmpty(param)) {
                continue;
            }
            if (ORGANISM_SHORTNAME_MATCHER.matcher(param).matches()) {
                organism = param;
            } else {
                dataset = param;
            }
        }

        PathQuery q = new PathQuery(model);

        // add columns to the output
        q.setView("Gene.primaryIdentifier, "
                + "Gene.organism.shortName,"
                + "Gene.homologues.homologue.primaryIdentifier,"
                + "Gene.homologues.homologue.organism.shortName,"
                + "Gene.homologues.type,"
                + "Gene.homologues.dataSets.title");

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));

        // organism
        q.addConstraint("Gene.organism", Constraints.lookup(organism));

        // if the XML is too long, the link generates "HTTP Error 414 - Request URI too long"
        if (externalids.length() < 4000) {
            q.addConstraint("Gene.homologues.homologue", Constraints.lookup(externalids));
        }

        if (!StringUtils.isEmpty(dataset)) {
            // homologue.dataSets = dataset
            q.addConstraint("Gene.homologues.dataSet.title", Constraints.eq(dataset));
        }

        q.syncLogicExpression("and");

        String query = q.toXml(PathQuery.USERPROFILE_VERSION);
        String encodedurl = URLEncoder.encode(query, "UTF-8");

        String[] values = new String[]
            {
                String.valueOf(convertedSize),
                organism,
                String.valueOf(externalids.split(",").length),
                type,
                encodedurl
            };
        ActionMessage am = new ActionMessage("portal.orthologues", values);
        return am;
    }

    @Override
    public WebResults getConvertedObjects(Profile profile, List<Integer> fromList, String type,
            String... parameters) {
        // TODO Auto-generated method stub
        return null;
    }
}
