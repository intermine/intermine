package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.intermine.InterMineException;
import org.intermine.api.bag.InterMineBag;
import org.intermine.api.bag.TypeConverter;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Utility methods used when converting lists (bags) between types.
 * @author Richard Smith
 *
 */
public class BagConversionHelper 
{
    
    /**
     * Converts a List of objects from one type to another type using a TemplateQuery,
     * returns the converted objects.
     *
     * @param session of the user running the query
     * @param conversionTemplates a list of templates to be used for conversion
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param imBag an InterMineBag or Collection of objects of type typeA
     * @return a WebResults object containing the converted objects
     * @throws InterMineException if an error occurs
     * @throws ObjectStoreException if an error occurs
     */
    public static WebResults getConvertedObjects(HttpSession session,
                                                 List<TemplateQuery> conversionTemplates,
                                                 Class typeA, Class typeB, InterMineBag imBag)
    throws InterMineException, ObjectStoreException {
        ServletContext servletContext = session.getServletContext();
        
        PathQuery pq = TypeConverter.getConversionQuery(conversionTemplates, typeA, typeB, imBag);
        if (pq == null) {
            return null;
        }
        Path configuredPath = pq.getView().get(0);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        pq.setViewPaths(PathQueryResultHelper
                        .getDefaultView(TypeUtil.unqualifiedName(typeB.getName()), model,
                                        webConfig, configuredPath.getPrefix()
                                                        .toStringNoConstraints(), false));
        String label = null, id = null, code = pq.getUnusedConstraintCode();
        Constraint c = new Constraint(ConstraintOp.IN, imBag.getName(), false,
            label, code, id, null);
        pq.addNode(imBag.getType()).getConstraints().add(c);
        pq.syncLogicExpression("and");
        pq.setConstraintLogic("A and B");

        WebResultsExecutor executor = SessionMethods.getWebResultsExecutor(session);
        return executor.execute(pq);
    }
}
