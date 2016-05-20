package org.intermine.webservice.server.query.result;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.profile.InterMineBag;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathLengthComparator;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OrderDirection;
import org.intermine.webservice.server.core.Producer;

/**
 * @author Alexis Kalderimis
 *
 */
public class PathQueryBuilderForJSONObj extends PathQueryBuilder
{

    /**
     * Constructor.
     */
    protected PathQueryBuilderForJSONObj() {
        // empty constructor for testing
    }

    /**
     * Constructor
     * @param xml The XML to build into a query.
     * @param schemaUrl The schema to validate against.
     * @param savedBags A map of bags this query may contain.
     */
    public PathQueryBuilderForJSONObj(String xml, String schemaUrl,
            Producer<Map<String, InterMineBag>> savedBags) {
        super(xml, schemaUrl, savedBags);
    }

    /**
     * For the purposes of exporting into JSON objects the view must be:
     * <ul>
     *     <li>Ordered by length, such that Company.departments.name precedes
     *      Company.departments.employees.name</li>
     *  <li>Be constituted so that every class has an attribute on it, root class
     *      included, and every reference along the way. So a path such as
     *      Departments.employees.address.address is illegal unless it is preceded
     *      by Departments.id and Departments.employees.id (id is the default if
     *      none is supplied).</li>
     *  </ul>
     *  The purpose of this method is to perform the necessary transformations.
     *  @return a PathQuery with an appropriately mangled view.
     */
    @Override
    public PathQuery getQuery() {
        PathQuery beforeChanges = super.getQuery();
        return processQuery(beforeChanges);
    }

    /**
     * Transform a query from a standard one into one that conforms to the requirements
     * of JSON objects.
     * @param beforeChanges The query to transform.
     * @return A transformed query.
     */
    public static PathQuery processQuery(PathQuery beforeChanges) {
        PathQuery afterChanges = beforeChanges.clone();
        afterChanges.clearView();
        afterChanges.clearOrderBy();
        List<String> newViews = getAlteredViews(beforeChanges);
        afterChanges.addOrderBy(new OrderElement(newViews.get(0), OrderDirection.ASC));
        afterChanges.addViews(newViews);

        return afterChanges;
    }

    /**
     * Get the views for the transformed query.
     * @param pq The original query.
     * @return Its new views.
     */
    public static List<String> getAlteredViews(PathQuery pq) {
        List<String> originalViews = pq.getView();
        List<Path> viewPaths = new ArrayList<Path>();
        for (String v : originalViews) {
            try {
                viewPaths.add(pq.makePath(v));
            } catch (PathException e) {
                throw new RuntimeException("Problem making path " + v, e);
            }
        }
        Collections.sort(viewPaths, PathLengthComparator.getInstance());
        List<String> newViews = new ArrayList<String>();
        Set<Path> classesWithAttributes = new HashSet<Path>();

        String idPath = viewPaths.get(0).getStartClassDescriptor().getUnqualifiedName() + ".id";

        for (Path p : viewPaths) {
            if (!p.endIsAttribute()) {
                throw new RuntimeException("The view can only contain attribute paths - Got: '"
                        + p.toStringNoConstraints() + "'");
            }
            newViews.addAll(getNewViewStrings(classesWithAttributes, p));

        }
        int idPos = newViews.indexOf(idPath);
        if (idPos != 0) {
            if (idPos > 0) {
                newViews.remove(idPos);
            }
            newViews.add(0, idPath);
        }
        return newViews;
    }

    private static List<String> getNewViewStrings(Set<Path> classesWithAttributes, Path p) {
        // The prefix automatically has an attribute, since its child is in the view
        classesWithAttributes.add(p.getPrefix());
        List<Path> composingPaths = p.decomposePath();
        List<String> newParts = new ArrayList<String>();
        for (Path cp : composingPaths) {
            if (!cp.endIsAttribute() && !classesWithAttributes.contains(cp)) {
                newParts.add(getNewAttributeNode(classesWithAttributes, cp));
            }
        }
        newParts.add(p.toStringNoConstraints());
        return newParts;
    }

    /**
     * Get a new attribute node, adding the class it belongs to the to set of classes
     * with attributes.
     * @param classesWithAttributes The accumulator.
     * @param p The path to add an id attribute to.
     * @return The new path.
     */
    static String getNewAttributeNode(Set<Path> classesWithAttributes, Path p) {
        String retVal;
        try {
            retVal = p.append("id").toStringNoConstraints();
            classesWithAttributes.add(p);
        } catch (PathException e) {
            // This should be frankly impossible
            throw new RuntimeException("Couldn't extend "
                    + p.toStringNoConstraints() + " with 'id'", e);
        }
        return retVal;
    }

}
