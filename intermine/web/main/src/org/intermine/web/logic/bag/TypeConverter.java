package org.intermine.web.logic.bag;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.InterMineException;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Static helper routines to convert bags between different types.
 *
 * @author Matthew Wakeling
 */
public class TypeConverter
{
    /**
     * Converts a List of objects from one type to another type using a TemplateQuery,
     * returns a map from an original object to the converted object(s).
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bag an InterMineBag or Collection of objects of type typeA
     * @return a Map from original object to a List of converted objects, or null if conversion is
     * possible (because no suitable template is available)
     * @throws InterMineException if an error occurs
     */
    public static Map<InterMineObject, List<InterMineObject>>
    getConvertMap(ServletContext servletContext, Class typeA, Class typeB, Object bag)
    throws InterMineException {
        PathQuery pq = getConversionMapQuery(servletContext, typeA, typeB, bag);
        if (pq == null) {
            return null;
        }

        Query q;
        try {
            q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP, null, servletContext, null, false,
                    (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                    (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                    (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG));
        } catch (ObjectStoreException e) {
            throw new InterMineException(e);
        }
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Results r;
        Map<InterMineObject, List<InterMineObject>> retval =
            new HashMap<InterMineObject, List<InterMineObject>>();
        r = os.execute(q);
        Iterator iter = r.iterator();
        while (iter.hasNext()) {
            List row = (List) iter.next();
            InterMineObject orig = (InterMineObject) row.get(0);
            InterMineObject derived = (InterMineObject) row.get(1);
            List<InterMineObject> ders = retval.get(orig);
            if (ders == null) {
                ders = new ArrayList<InterMineObject>();
                retval.put(orig, ders);
            }
            ders.add(derived);
        }
        return retval;
    }

    /**
     * Return a TemplateQuery that will convert objects from one type to another. The TemplateQuery
     * must be tagged with "converter", and must have an editable constraint that will take an
     * object of type A, and have two columns as the output, of type A and type B. The template
     * converts from type A to type B.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @return a TemplateQuery, or null if one cannot be found
     */
    public static TemplateQuery getConversionTemplate(ServletContext servletContext, Class typeA,
                                                      Class typeB) {
        return getConversionTemplates(servletContext, typeA).get(typeB);
    }

    /**
     * Get conversion query for the types provided, edited so that the first
     * type is constrainted to be in the bag.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bag an InterMineBag or Collection of objects of type typeA
     * @return a PathQuery that finds a conversion mapping for the given bag
     */
    public static PathQuery getConversionMapQuery(ServletContext servletContext,
                                                Class typeA, Class typeB, Object bag) {
        TemplateQuery tq = getConversionTemplate(servletContext, typeA, typeB);
        if (tq == null) {
            return null;
        }
        tq = (TemplateQuery) tq.clone();
        // We can be reckless here because all this is checked by getConversionTemplate
        PathNode node = tq.getEditableNodes().iterator().next();
        Constraint c = (Constraint) tq.getEditableConstraints(node).iterator().next();
        // This is a MAJOR hack - we assume that the constraint is on an ATTRIBUTE of the node we
        // want to constrain.
        PathNode parent = tq.getNodes().get(node.getParent().getPathString());
        tq.getNodes().remove(node.getPathString());
        Constraint newC = new Constraint(ConstraintOp.IN, bag, false, "", c.getCode(), null, null);
        parent.getConstraints().add(newC);
        return tq.getPathQuery();
    }


    /**
     * Converts a List of objects from one type to another type using a TemplateQuery,
     * returns the converted objects.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bag an InterMineBag or Collection of objects of type typeA
     * @return a SingletonResults containting the converted objects
     * @throws InterMineException if an error occurs
     */
    public static SingletonResults getConvertedObjects(ServletContext servletContext,
                                              Class typeA, Class typeB, Object bag)
    throws InterMineException {
        PathQuery pq = getConvertedObjectsQuery(servletContext, typeA, typeB, bag);
        if (pq == null) {
            return null;
        }
        if (pq.getView().size() > 1) {
            throw new InterMineException("ConvertedObjects query has more than one element on"
                    + " the view list: " + pq);
        }
        Query q;
        try {
            q = MainHelper.makeQuery(pq, Collections.EMPTY_MAP, null, servletContext, null, false,
                    (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                    (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                    (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG));
        } catch (ObjectStoreException e) {
            throw new InterMineException(e);
        }

        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        return os.executeSingleton(q);
    }


    /**
     * Get conversion query from the types provided, edited so that the first
     * type is constrainted to be in the bag and the first type is removed form the
     * view list so that the query only returns the converted type.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bag an InterMineBag or Collection of objects of type typeA
     * @return a PathQuery that finds converted objects for the given bag
     */
    public static PathQuery getConvertedObjectsQuery (ServletContext servletContext,
                                                Class typeA, Class typeB, Object bag) {
        PathQuery pq = getConversionMapQuery(servletContext, typeA, typeB, bag);
        if (pq == null) {
            return null;
        }
        // remove typeA from the output
        List<Path> view = pq.getView();
        for (Path viewElement : view) {
            if (viewElement.getStartClassDescriptor().getType().equals(typeA)) {
                view.remove(viewElement);
            }
        }
        return pq;
    }

    /**
     * Return a Map from typeB to a TemplateQuery that will convert from typeA to typeB.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @return a Map from Class to TemplateQuery
     */
    public static Map<Class, TemplateQuery> getConversionTemplates(ServletContext servletContext,
                                                                  Class typeA) {
        String sup = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        Profile p = pm.getProfile(sup);

        List tags = pm.getTags(TagNames.IM_CONVERTER, null, TagTypes.TEMPLATE, sup);
        Map<Class, TemplateQuery> retval = new HashMap<Class, TemplateQuery>();
        Iterator iter = tags.iterator();
        while (iter.hasNext()) {
            Tag tag = (Tag) iter.next();
            String oid = tag.getObjectIdentifier();
            TemplateQuery tq = p.getSavedTemplates().get(oid);
            if (tq != null) {
                // Find conversion types
                List<Path> view = tq.getView();
                if (view.size() == 2) {
                    // Correct number of SELECT list items
                    Path select1 = view.get(0);
                    Class tqTypeA = select1.getLastClassDescriptor().getType();
                    if (tqTypeA.isAssignableFrom(typeA)) {
                        // Correct typeA in SELECT list. Now check for editable constraint.
                        if ((tq.getEditableConstraints(select1.toStringNoConstraints()).size() == 1)
                                && (tq.getAllEditableConstraints().size() == 1)) {
                            // Editable constraint is okay.
                            Class typeB = view.get(1).getLastClassDescriptor().getType();
                            TemplateQuery prevTq = retval.get(typeB);
                            if (prevTq != null) {
                                Class prevTypeA = prevTq.getView().get(0)
                                    .getLastClassDescriptor().getType();
                                if (prevTypeA.isAssignableFrom(tqTypeA)) {
                                    // This tq is more specific
                                    retval.put(typeB, tq);
                                }
                            } else {
                                retval.put(typeB, tq);
                            }
                        }
                    }
                }
            }
        }
        return retval;
    }
}
