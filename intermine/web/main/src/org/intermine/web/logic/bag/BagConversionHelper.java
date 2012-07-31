package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.TypeConverter;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.ApiTemplate;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.util.TypeUtil;
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
            List<ApiTemplate> conversionTemplates, Class typeA, Class typeB,
            InterMineBag imBag) throws InterMineException, ObjectStoreException {
        ServletContext servletContext = session.getServletContext();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        PathQuery pq = TypeConverter.getConversionQuery(conversionTemplates, typeA, typeB, imBag);
        if (pq == null) {
            return null;
        }

        String convertFrom;
        try {
            Path convertFromPath = pq.makePath(pq.getView().get(0));
            convertFrom = convertFromPath.getPrefix().getNoConstraintsString();
        } catch (PathException e) {
            throw new RuntimeException("Invalid path in bag conversion query: "
                    + pq.getView().get(0), e);
        }
        WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
        Model model = im.getModel();
        String typeBStr = TypeUtil.unqualifiedName(typeB.getName());
        // bit hacky, remove any remainging ids on the view
        List<String> views = pq.getView();
        for (String viewPath : views) {
            if (viewPath.endsWith(".id")) {
                pq.removeView(viewPath);
            }
        }
        pq.addViews(PathQueryResultHelper.getDefaultViewForClass(typeBStr, model, webConfig,
                convertFrom));

        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        return executor.execute(pq);
    }
}
