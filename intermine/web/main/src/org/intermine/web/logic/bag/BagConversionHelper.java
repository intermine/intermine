package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.InterMineException;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.profile.TagManager;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Utility methods used when converting lists (bags) between types.
 * @author Richard Smith
 *
 */
public class BagConversionHelper 
{

    /**
     * Find template queries that are tagged for use as converters.
     * @param servletContext use to fetch ProfileManager and superuser account
     * @return a list of conversion templates
     */
    public static List<TemplateQuery> getConversionTemplates(ServletContext servletContext) {

        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        String sup = pm.getSuperuser();
        Profile p = pm.getProfile(sup);

        List<TemplateQuery> conversionTemplates = new ArrayList<TemplateQuery>();
        TagManager tagManager = SessionMethods.getTagManager(servletContext);
        List<Tag> tags = tagManager.getTags(TagNames.IM_CONVERTER, null, TagTypes.TEMPLATE, sup);

        for (Tag tag : tags) {
            String oid = tag.getObjectIdentifier();
            TemplateQuery tq = p.getSavedTemplates().get(oid);
            if (tq != null) {
                conversionTemplates.add(tq);
            }
        }
        return conversionTemplates;
    }
    
    /**
     * Converts a List of objects from one type to another type using a TemplateQuery,
     * returns the converted objects.
     *
     * @param profile the profile of the user running this conversion
     * @param servletContext the ServletContext
     * @param conversionTemplates a list of templates to be used for conversion
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param imBag an InterMineBag or Collection of objects of type typeA
     * @return a WebResults object containing the converted objects
     * @throws InterMineException if an error occurs
     * @throws ObjectStoreException if an error occurs
     */
    public static WebResults getConvertedObjects(Profile profile,
                                                 ServletContext servletContext,
                                                 List<TemplateQuery> conversionTemplates,
                                                 Class typeA, Class typeB, InterMineBag imBag)
    throws InterMineException, ObjectStoreException {
        PathQuery pq = TypeConverter.getConversionQuery(conversionTemplates, typeA, typeB, imBag);
        if (pq == null) {
            return null;
        }
        Path configuredPath = pq.getView().get(0);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = ((ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE)).getModel();
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
        WebResults webResults = PathQueryResultHelper.createPathQueryGetResults(pq, profile,
                        (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                        (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                        (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG),
                        servletContext);
        return webResults;
    }
}
