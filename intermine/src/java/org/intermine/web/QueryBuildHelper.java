package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.flymine.metadata.Model;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryHelper;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.util.TypeUtil;

/**
 * Helper methods for Query building.
 *
 * @author Kim Rutherford
 */

public class QueryBuildHelper
{
    /**
     * Return the field name (in an object) for a given field name in the form.
     *
     * @param fieldName the field name from a form.
     * @return the field name as it appears in the class.
     */
    public static String getFieldName(String fieldName) {
        return fieldName.substring(0, fieldName.lastIndexOf("_"));
    }

    /**
     * Give a class name a unique alias given the existing aliases (eg Company -> Company_3)
     * @param existingAliases the Collection of existing aliases
     * @param type the class name
     * @return a new alias
     */
    protected static String aliasClass(Collection existingAliases, String type) {
        String prefix = toAlias(type);
        int max = 0;
        for (Iterator i = existingAliases.iterator(); i.hasNext();) {
            String alias = (String) i.next();
            if (alias.substring(0, alias.lastIndexOf("_")).equals(prefix)) {
                int suffix = Integer.valueOf(alias.substring(alias.lastIndexOf("_") + 1))
                    .intValue();
                if (suffix >= max) {
                    max = suffix + 1;
                }
            }
        }
        return prefix + "_" + max;
    }

    /**
     * Convert a class name to a alias prefix
     * @param type the class name
     * @return a suitable prefix for an alias
     */
    protected static String toAlias(String type) {
        return TypeUtil.unqualifiedName(type);
    }

    /**
     * Add a new DisplayQueryClass of type className to the current query
     * @param queryClasses the exiting queryClasses
     * @param className the class name
     */
    protected static void addClass(Map queryClasses, String className) {
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(className);

        String alias = aliasClass(queryClasses.keySet(), className);
        queryClasses.put(alias, d);
    }
    
    /**
     * Create a Query from a Collection of DisplayQueryClasses
     * @param queryClasses the DisplayQueryClasses
     * @param model the relevant metadata
     * @param savedBags the savedBags on the session
     * @return the Query
     * @throws Exception if an error occurs in constructing the Query
     */
    protected static Query createQuery(Map queryClasses, Model model, Map savedBags)
        throws Exception {
        Query q = new Query();
        Map mapping = new HashMap();
        for (Iterator i = queryClasses.keySet().iterator(); i.hasNext();) {
            String alias = (String) i.next();
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(alias);
            QueryClass qc = new QueryClass(Class.forName(d.getType()));
            q.alias(qc, alias);
            q.addFrom(qc);
            q.addToSelect(qc);
            mapping.put(d, qc);
        }
        
        for (Iterator i = queryClasses.keySet().iterator(); i.hasNext();) {
            String alias = (String) i.next();
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(alias);
            QueryClass qc = (QueryClass) mapping.get(d);
            ClassDescriptor cld = model.getClassDescriptorByName(d.getType());
            for (Iterator j = d.getConstraintNames().iterator(); j.hasNext();) {
                String constraintName = (String) j.next();
                String fieldName = (String) d.getFieldName(constraintName);
                FieldDescriptor fd = (FieldDescriptor) cld.getFieldDescriptorByName(fieldName);
                if (fd instanceof AttributeDescriptor) {
                    QueryHelper.addConstraint(q, fieldName, qc,
                                              (ConstraintOp) d.getFieldOp(constraintName),
                                              new QueryValue(d.getFieldValue(constraintName)));
                }
            }
        }
        return q;
    }
}
