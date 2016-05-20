package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.MainHelper;
import org.intermine.api.template.ApiTemplate;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

/**
 * Static helper routines to convert bags between different types.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public final class TypeConverter
{
    private static final Logger LOG = Logger.getLogger(TypeConverter.class);

    private TypeConverter() {
        // nothing to do
    }

    /**
     * Converts a List of objects from one type to another type using a TemplateQuery,
     * returns a map from an original object to the converted object(s).
     *
     * @param conversionTemplates a list of templates to be used for conversion
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bagOrIds an InterMineBag or Collection of Integer object ids
     * @param os the ObjectStore to execute queries in
     * @return a Map from original object to a List of converted objects, or null if conversion is
     * possible (because no suitable template is available)
     * @throws InterMineException if an error occurs
     */
    public static Map<InterMineObject, List<InterMineObject>>
    getConvertedObjectMap(List<ApiTemplate> conversionTemplates,
            Class<? extends InterMineObject> typeA,
            Class<? extends InterMineObject> typeB,
            Object bagOrIds, ObjectStore os) throws InterMineException {
        PathQuery pq = getConversionMapQuery(conversionTemplates, typeA, typeB, bagOrIds);

        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();
        if (bagOrIds instanceof InterMineBag) {
            InterMineBag bag = (InterMineBag) bagOrIds;
            savedBags.put(bag.getName(), bag);
        }
        if (pq == null) {
            return null;
        }

        Query q;
        try {
            // we can call without a BagQueryRunner as a valid conversion query can't contain
            // LOOKUP constraints.
            q = MainHelper.makeQuery(pq, savedBags, null, null, null);
        } catch (ObjectStoreException e) {
            throw new InterMineException(e);
        }
        Results r;
        Map<InterMineObject, List<InterMineObject>> retval =
            new HashMap<InterMineObject, List<InterMineObject>>();
        r = os.execute(q);
        Iterator<?> iter = r.iterator();
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
     * Get conversion query for the types provided, edited so that the first
     * type is constrained to be in the bag.
     *
     * If there is no suitable conversion template, returns <code>null</code>. Throws
     * a run-time exception if there is such a template, but it is not valid.
     *
     * @param conversionTemplates a list of templates to be used for conversion
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bagOrIds an InterMineBag or Collection of Integer object ids
     * @return a PathQuery that finds a conversion mapping for the given bag
     */
    public static PathQuery getConversionMapQuery(List<ApiTemplate> conversionTemplates,
                                                Class<? extends InterMineObject> typeA,
                                                Class<? extends InterMineObject> typeB,
                                                Object bagOrIds) {
        ApiTemplate tq = getConversionTemplates(conversionTemplates, typeA).get(typeB);
        if (tq == null) {
            return null;
        }
        tq = tq.clone();
        // We can be reckless here because all this is checked by getConversionTemplate
        String node = tq.getEditablePaths().iterator().next();
        PathConstraint c = tq.getEditableConstraints(node).iterator().next();
        // This is a MAJOR hack - we assume that the constraint is on an ATTRIBUTE of the node we
        // want to constrain.
        try {
            String parent = tq.makePath(node).getPrefix().getNoConstraintsString();
            if (bagOrIds instanceof InterMineBag) {
                InterMineBag bag = (InterMineBag) bagOrIds;
                tq.replaceConstraint(c, Constraints.in(parent, bag.getName()));
            } else if (bagOrIds instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Integer> ids = (Collection<Integer>) bagOrIds;
                tq.replaceConstraint(c, Constraints.inIds(parent, ids));
            }
            return tq;
        } catch (PathException e) {
            throw new RuntimeException("Template is invalid", e);
        }
    }


    /**
     * Get conversion query for the types provided, edited so that the first
     * type is constrained to be in the bag and the first type is removed from the
     * view list so that the query only returns the converted type.
     *
     * @param conversionTemplates a list of templates to be used for conversion
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param bagOrIds an InterMineBag or Collection of Integer object identifiers
     * @return a PathQuery that finds converted objects for the given bag
     */
    public static PathQuery getConversionQuery(List<ApiTemplate> conversionTemplates,
            Class<? extends InterMineObject> typeA,
            Class<? extends InterMineObject> typeB,
            Object bagOrIds) {
        PathQuery pq = getConversionMapQuery(conversionTemplates, typeA, typeB, bagOrIds);
        if (pq == null) {
            return null;
        }
        // remove typeA from the output and sort order
        try {
            List<String> view = pq.getView();
            for (String viewElement : view) {
                Path path = pq.makePath(viewElement);
                if (path.getLastClassDescriptor().getType().equals(typeA)) {
                    pq.removeView(viewElement);
                    for (OrderElement orderBy : pq.getOrderBy()) {
                        if (orderBy.getOrderPath().equals(viewElement)) {
                            pq.removeOrderBy(viewElement);
                        }
                    }
                }
            }
            return pq;
        } catch (PathException e) {
            throw new RuntimeException("Conversion query was invalid: ", e);
        }
    }

    /**
     * Return a Map from typeB to a TemplateQuery that will convert from typeA to typeB.
     * TODO: return a map of Class<InterMineObject> -> ApiTemplates
     *
     * @param conversionTemplates a list of templates to be used for conversion
     * @param typeA the type to convert from
     * @return a Map from Class to TemplateQuery
     */
    public static Map<Class, ApiTemplate> getConversionTemplates(
            List<ApiTemplate> conversionTemplates, Class typeA) {
        Map<Class, ApiTemplate> retval = new HashMap();
        for (ApiTemplate tq : conversionTemplates) {

            try {
                // Find conversion types
                List<String> view = tq.getView();
                if (view.size() == 2) {
                    // Correct number of SELECT list items
                    Path select1 = tq.makePath(view.get(0));
                    Class tqTypeA = select1.getLastClassDescriptor().getType();
                    if (tqTypeA.isAssignableFrom(typeA)) {
                        // Correct typeA in SELECT list. Now check for editable constraint.
                        if ((tq.getEditableConstraints(select1.toStringNoConstraints()).size() == 1)
                                && (tq.getEditableConstraints().size() == 1)) {
                            // Editable constraint is okay.
                            Class typeB = tq.makePath(view.get(1)).getLastClassDescriptor()
                                    .getType();
                            TemplateQuery prevTq = retval.get(typeB);
                            if (prevTq != null) {
                                Class prevTypeA = prevTq.makePath(prevTq.getView().get(0))
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
            } catch (PathException e) {
                e.fillInStackTrace();
                LOG.error("Invalid conversion template: " + e);
            }

        }
        return retval;
    }
}
