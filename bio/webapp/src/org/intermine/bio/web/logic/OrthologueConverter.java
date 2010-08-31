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
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
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
        q.addConstraint("Gene.homologues.homologue.organism.shortName",
                Constraints.eq(organismName));

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));

        return q;
    }

    /**
     * runs the orthologue conversion pathquery and returns a comma-delimited list of identifiers
     * used on list analysis page for intermine linking, called via Ajax
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
        pathQuery.addConstraint(bagType, Constraints.in(bagName));
        pathQuery.addConstraint(geneIdentifier, Constraints.isNotNull());
        pathQuery.setView(geneIdentifier);
        pathQuery.syncLogicExpression("and");
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(pathQuery);

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
        List<InterMineObject> objectList = null;
        try {
            objectList = im.getObjectStore().getObjectsByIds(bagList);
        } catch (ObjectStoreException e) {
            e.printStackTrace();
            return null;
        }
        pathQuery.addConstraint(bagType, Constraints.in(objectList));
        pathQuery.setView("Gene.homologues.homologue.id");
        pathQuery.syncLogicExpression("and");
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
            String parameter) throws UnsupportedEncodingException {
        if (StringUtils.isEmpty(parameter)) {
            return null;
        }

        PathQuery q = new PathQuery(model);

        // add columns to the output
        q.setView("Gene.primaryIdentifier, "
                + "Gene.organism.shortName,"
                + "Gene.homologues.homologue.primaryIdentifier,"
                + "Gene.homologues.homologue.organism.shortName,"
                + "Gene.homologues.type,"
                + "Gene.homologues.dataSets.name");

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));

        // organism
        q.addConstraint("Gene.organism", Constraints.lookup(parameter));

        // if the XML is too long, the link generates "HTTP Error 414 - Request URI too long"
        if (externalids.length() < 4000) {
            q.addConstraint("Gene.homologues.homologue", Constraints.lookup(externalids));
        }

        q.syncLogicExpression("and");

        String query = q.toXml(PathQuery.USERPROFILE_VERSION);
        String encodedurl = URLEncoder.encode(query, "UTF-8");

        String[] values = new String[]
        {
                String.valueOf(convertedSize), parameter,
                String.valueOf(externalids.split(",").length), type, encodedurl
        };
        ActionMessage am = new ActionMessage("portal.orthologues", values);
        return am;
    }

    @Override
    public WebResults getConvertedObjects(Profile profile, List<Integer> fromList, String type,
            String parameters) throws ObjectStoreException, PathException {

        PathQuery q = new PathQuery(model);
        List<Path> view = PathQueryResultHelper.getDefaultView(type, model, webConfig,
                "Gene.homologues.homologue", false);
        view = getFixedView(view);
        q.setViewPaths(view);

        List<InterMineObject> objectList = im.getObjectStore().getObjectsByIds(fromList);

        // gene
        q.addConstraint("Gene", Constraints.in(objectList));

        // organism
        q.addConstraint("Gene.homologues.homologue.organism", Constraints.lookup(parameters));

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));

        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);

        return executor.execute(q);
    }

    /**
     *If view contains joined organism, this will make sure, that
     * organism is joined as a inner join. Else constraint on organism doesn't work.
     * @param pathQuery
     * @param joinPath
     * @throws PathException
     * */
    private List<Path> getFixedView(List<Path> view) throws PathException {
        String invalidPath = "Gene.homologues.homologue:organism";
        String validPath = "Gene.homologues.homologue.organism";
        List<Path> ret = new ArrayList<Path>();
        for (Path path : view) {
            if (path.toString().contains(invalidPath)) {
                String newPathString = path.toString().replace(invalidPath, validPath);
                path = new Path(path.getModel(), newPathString);
            }
            ret.add(path);
        }
        return ret;
    }
}
