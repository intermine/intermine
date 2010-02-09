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

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
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
public class OrthologueConverter implements BagConverter
{

    private static final Logger LOG = Logger.getLogger(OrthologueConverter.class);

    /**
     * The Constructor
     */
    public OrthologueConverter() {
        super();
    }

    /**
     * {@inheritDoc}
     * @throws PathException
     */
    public WebResults getConvertedObjects (HttpSession session, String organism,
                                      List<Integer> fromList, String type)
                                      throws ObjectStoreException, PathException {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        ObjectStore os = im.getObjectStore();
        WebConfig webConfig = SessionMethods.getWebConfig(session.getServletContext());

        PathQuery q = new PathQuery(model);
        List<Path> view = PathQueryResultHelper.getDefaultView(type, model, webConfig,
                        "Gene.homologues.homologue", false);
        view = getFixedView(view);
        q.setViewPaths(view);

        List<InterMineObject> objectList = os.getObjectsByIds(fromList);

        // gene
        q.addConstraint("Gene", Constraints.in(objectList));

        // organism
        q.addConstraint("Gene.homologues.homologue.organism", Constraints.lookup(organism));

        // homologue.type = "orthologue"
        q.addConstraint("Gene.homologues.type", Constraints.eq("orthologue"));


        q.setConstraintLogic("A and B and C");
        q.syncLogicExpression("and");
        LOG.info("PATH QUERY:" + q.toXml(PathQuery.USERPROFILE_VERSION));
        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);

        return executor.execute(q);
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
    public ActionMessage getActionMessage(Model model, String externalids, int convertedSize,
                                          String type, String organism)
                    throws UnsupportedEncodingException {
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

        q.setConstraintLogic("A and B and C");
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
