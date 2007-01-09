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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.model.userprofile.Tag;
import org.intermine.path.Path;
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
     */
    public static Map convertObjects(ServletContext servletContext, Class typeA, Class typeB,
            Collection objects) {
        return new HashMap();
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
                    if (select1.getEndType().isAssignableFrom(typeA)) {
                        // Correct typeA in SELECT list. Now check for editable constraint.
                        if ((tq.getEditableConstraints(select1.toStringNoConstraints()).size() == 1)
                                && (tq.getAllEditableConstraints().size() == 1)) {
                            // Editable constraint is okay.
                            retval.put(((Path) view.get(1)).getEndType(), tq);
                        }
                    }
                }
            }
        }
        return retval;
    }
}
