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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.directwebremoting.WebContextFactory;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebResults;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author "Xavier Watkins"
 *
 */
public class OrthologueConverter extends BagConverter
{

    private static final Logger LOG = Logger.getLogger(OrthologueConverter.class);
    // D. melanogaster, C. lupus familiaris
    private static final Pattern ORGANISM_SHORTNAME_MATCHER = Pattern.compile("([a-zA-Z]\\..+)");
    private static InterMineAPI im;
    private static Model model;
    private static WebConfig webConfig;
    private static ObjectStore os;

    public OrthologueConverter() {
        super();
        ServletContext servletContext = WebContextFactory.get().getServletContext();
        HttpSession session = WebContextFactory.get().getSession();
        im = SessionMethods.getInterMineAPI(session);
        webConfig = SessionMethods.getWebConfig(servletContext);
        model = im.getModel();
        os = im.getObjectStore();
    }
    
    /**
     * {@inheritDoc}
     * @throws PathException
     */
    public WebResults getConvertedObjects (Profile profile, List<Integer> fromList, String type,
            String ... parameters) throws ObjectStoreException, PathException {
        String organism = null, dataset = null;

        for (String param : parameters) {
            if (StringUtils.isEmpty(param)) {
                continue;
            }
            Matcher m = ORGANISM_SHORTNAME_MATCHER.matcher(param);
            if (m.matches()) {
                organism = param;
            } else {
                dataset = param;
            }
        }
        List<InterMineObject> objectList = os.getObjectsByIds(fromList);
        PathQuery q = generateQuery(objectList, type, organism, dataset);

        LOG.info("PATH QUERY:" + q.toXml(PathQuery.USERPROFILE_VERSION));
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);

        return executor.execute(q);
    }

    private static PathQuery constructPathQuery(String organism, String dataset) {
        PathQuery q = new PathQuery(model);

        // organism
        q.addConstraint("Gene.homologues.homologue.organism", Constraints.lookup(organism));

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));

        if (StringUtils.isNotEmpty(dataset)) {
            // homologue.dataSets = dataset
            q.addConstraint("Gene.homologues.dataSets.title", Constraints.eq(dataset));
        }
        return q;
    }

    private PathQuery generateQuery(List<InterMineObject> objectList, String type, String organism,
            String dataset)
    throws PathException {
        PathQuery q = constructPathQuery(organism, dataset);

        List<Path> view = PathQueryResultHelper.getDefaultView(type, model, webConfig,
                        "Gene.homologues.homologue", false);
        view = getFixedView(view);
        q.setViewPaths(view);

        q.addConstraint(type, Constraints.in(objectList));

        q.syncLogicExpression("and");
        return q;
    }


    private static PathQuery generateQuery(String bagType, String bagName, String organismName) {
        PathQuery q = constructPathQuery(organismName, null);
        q.setView("Gene.homologues.homologue.primaryIdentifier");
        q.addConstraint(bagType, Constraints.in(bagName));
        q.syncLogicExpression("and");
        return q;
    }

    public String getFieldsFromConvertedObjects(Profile profile, String bagType,
            String bagName, String organismName) {
        String orthologues = "";
        PathQuery pathQuery = generateQuery(bagType, bagName, organismName);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        ExportResultsIterator it = executor.execute(pathQuery);
        
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String orthologue = row.get(0).getField().toString();
            if (StringUtils.isNotEmpty(orthologues)) {
                orthologues += ",";
            }
            orthologues += orthologue;
        }

        return orthologues;
    }

    /**
     * If view contains joined organism, this will make sure, that
     * organism is joined as a inner join. Else constraint on organism doesn't work.
     * @param pathQuery
     * @param joinPath
     * @throws PathException
     */
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
}
