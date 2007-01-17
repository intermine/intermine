package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.path.Path;
import org.intermine.web.bag.BagElement;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.tagging.TagTypes;

/**
 * Static helper routines to convert bags between different types.
 *
 * @author Matthew Wakeling
 */
public class TypeConverter
{
    /**
     * Converts a List of objects from one type to another type using a TemplateQuery.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @param typeB the type to convert to
     * @param objects a Collection of objects of type typeA
     * @return a Map from original object to a List of converted objects
     * @throws ObjectStoreException if an error occurs
     */
    public static Map convertObjects(ServletContext servletContext, Class typeA, Class typeB,
            Collection objects) throws ObjectStoreException {
        TemplateQuery tq = getConversionTemplate(servletContext, typeA, typeB);
        if (tq == null) {
            throw new IllegalStateException("No template query available for conversion from "
                    + typeA + " to " + typeB);
        }
        tq = (TemplateQuery) tq.clone();
        // We can be reckless here because all this is checked by getConversionTemplate
        PathNode node = (PathNode) tq.getEditableNodes().iterator().next();
        Constraint c = (Constraint) tq.getEditableConstraints(node).iterator().next();
        // This is a MAJOR hack - we assume that the constraint is on an ATTRIBUTE of the node we
        // want to constraint. Just because our query builder has been crippled to only allow that.
        PathNode parent = (PathNode) tq.getNodes().get(node.getParent().getPath());
        tq.getNodes().remove(node.getPath());
        Collection bagElements = new ArrayList();
        Iterator objIter = objects.iterator();
        while (objIter.hasNext()) {
            InterMineObject object = (InterMineObject) objIter.next();
            bagElements.add(new BagElement(object.getId(), object.getClass().getName()));
        }
        Constraint newC = new Constraint(ConstraintOp.IN, new InterMineBag(null, null, null, null, 
                    null, bagElements), false, "", c.getCode(), null);
        parent.getConstraints().add(newC);

        Query q = MainHelper.makeQuery(tq, Collections.EMPTY_MAP, null);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Results r = os.execute(q);
        Map retval = new HashMap();
        Iterator iter = r.iterator();
        while (iter.hasNext()) {
            List row = (List) iter.next();
            InterMineObject orig = (InterMineObject) row.get(0);
            InterMineObject derived = (InterMineObject) row.get(1);
            List ders = (List) retval.get(orig);
            if (ders == null) {
                ders = new ArrayList();
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
        return (TemplateQuery) (getConversionTemplate(servletContext, typeA).get(typeB));
    }

    /**
     * Return a Map from typeB to a TemplateQuery that will convert from typeA to typeB.
     *
     * @param servletContext the ServletContext
     * @param typeA the type to convert from
     * @return a Map from Class to TemplateQuery
     */
    public static Map getConversionTemplate(ServletContext servletContext, Class typeA) {
        String sup = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        Profile p = pm.getProfile(sup);

        List tags = pm.getTags("converter", null, TagTypes.TEMPLATE, sup);
        Map retval = new HashMap();
        Iterator iter = tags.iterator();
        while (iter.hasNext()) {
            Tag tag = (Tag) iter.next();
            String oid = tag.getObjectIdentifier();
            TemplateQuery tq = (TemplateQuery) p.getSavedTemplates().get(oid);
            if (tq != null) {
                // Find conversion types
                List view = tq.getViewAsPaths();
                if (view.size() == 2) {
                    // Correct number of SELECT list items
                    Path select1 = (Path) view.get(0);
                    if (select1.getLastClassDescriptor().getType().isAssignableFrom(typeA)) {
                        // Correct typeA in SELECT list. Now check for editable constraint.
                        if ((tq.getEditableConstraints(select1.toStringNoConstraints()).size() == 1)
                                && (tq.getAllEditableConstraints().size() == 1)) {
                            // Editable constraint is okay.
                            retval.put(((Path) view.get(1)).getLastClassDescriptor().getType(), tq);
                        }
                    }
                }
            }
        }
        return retval;
    }
}
