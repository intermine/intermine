package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.results.TableHelper;

/**
 * @author "Xavier Watkins"
 *
 */
public class OrthologueConverter implements BagConverter
{
    
    /**
     * The Constructor
     */
    public OrthologueConverter() {
        super();
    }

    /* (non-Javadoc)
     * @see org.intermine.web.logic.bag.BagConverter#getConvertedObjects(
     * javax.servlet.http.HttpSession, java.lang.String, java.util.List, java.lang.String)
     */
    public List<ResultsRow> getConvertedObjects (HttpSession session, String organism, 
                                      List<Integer> fromList, String type) 
                                      throws ClassNotFoundException, 
                                      ObjectStoreException {
        ServletContext servletContext = session.getServletContext();
        Model model = ((ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE)).getModel();
        Class typeClass = TypeUtil.instantiate(model.getPackageName() + "." + type);
        Map<String, QueryNode> pathToQueryNode = new HashMap<String, QueryNode>();
        Map<String, BagQueryResult> pathToBagQueryResult = new HashMap<String, BagQueryResult>();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);

        PathQuery pathQuery = new PathQuery(model);
        
        List<Path> view = new ArrayList<Path>();
        view.add(MainHelper.makePath(model, pathQuery, "Gene.orthologues.orthologue.id"));
        pathQuery.setView(view);
        String label = null, id = null, code = pathQuery.getUnusedConstraintCode();
        List objectList = os.getObjectsByIds(fromList);
        List newList = new ArrayList();
        for (Object object : objectList) {
            ResultsRow resRow = (ResultsRow) object;
            newList.add((InterMineObject) resRow.get(0));
        }
        Constraint c = new Constraint(ConstraintOp.IN, newList, 
                                        false, label, code, id, null);
        pathQuery.addNode(type).getConstraints().add(c);

        code = pathQuery.getUnusedConstraintCode();
        Constraint c2 = new Constraint(ConstraintOp.MATCHES, organism, 
                                        false, label, code, id, null);
        pathQuery.addNode("Gene.orthologues.orthologue.organism.shortName")
                                .getConstraints().add(c2);

        pathQuery.setConstraintLogic("A and B and C");
        pathQuery.syncLogicExpression("and");

        Query q = MainHelper.makeQuery(pathQuery, allBags, pathToQueryNode,
            servletContext, pathToBagQueryResult, false,
            (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
            (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
            (BagQueryConfig) servletContext.getAttribute(Constants
                .BAG_QUERY_CONFIG));
        Results results = TableHelper.makeResults(os, q);
        return results;
    }
}
