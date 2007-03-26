package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;

/**
 * Class that helps build queries or parts of queries for common situations.
 *
 * @author Andrew Varley
 */
public class QueryCreator
{
    /**
     * Create a query that will retrieve an object from the objectstore, given an ID.
     *
     * @param id the ID of the object to fetch
     * @param clazz a Class in the object
     * @return a Query
     */
    public static Query createQueryForId(Integer id, Class clazz) {
        Query q = new Query();
        QueryClass qc = new QueryClass(clazz);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "id"), ConstraintOp.EQUALS,
                                             new QueryValue(id)));
        return q;
    }   

    /**
     * Create a query that will retrieve all objects with the given IDs from the objectstore.
     *
     * @param ids a collection of ID to fetch
     * @param clazz a Class in the object
     * @return a Query
     */
    public static Query createQueryForIds(Collection ids, Class clazz) {
        Query q = new Query();

        q.setDistinct(false);
        QueryClass qc = new QueryClass(clazz);
        q.addFrom(qc);
        q.addToSelect(qc);
        
        QueryField qf = new QueryField(qc, "id");
        q.setConstraint(new BagConstraint(qf, ConstraintOp.IN, ids));
        
        return q;
    }   

    /**
     * Create a query that will list the options for a particular
     * field or class in a query, given the existing constraints
     * <p>
     * For example:
     * <pre>
     * Original query:
     * SELECT c, d
     * FROM Company AS c, Department AS d
     * WHERE c.departments CONTAINS d
     * AND c.name LIKE 'A%'
     *
     * We want to know the possible department names are that we might
     * want to constrain
     *
     * Returned query:
     * SELECT DISTINCT d.name
     * FROM Company AS c, Department AS d
     * WHERE c.departments CONTAINS d
     * AND c.name LIKE 'A%'
     * </pre>
     *
     * @param q the original query
     * @param qn the QueryNode that we want to know the values for
     * @return the Query that will return the requested values
     */
    public static Query createQueryForQueryNodeValues(Query q, QueryNode qn) {

        Query ret = QueryCloner.cloneQuery(q);

        // Clear the SELECT part
        ret.clearSelect();

        QueryNode qnNew;
        if (qn instanceof QueryClass) {
            // Add the equivalent QueryNode for the cloned query
            String origAlias = (String) q.getAliases().get(qn);
            qnNew = (QueryNode) ret.getReverseAliases().get(origAlias);
        } else if (qn instanceof QueryField) {
            QueryField qf = (QueryField) qn;
            String origAlias = (String) q.getAliases().get(qf.getFromElement());
            qnNew = new QueryField((QueryClass) ret.getReverseAliases().get(origAlias),
                                   qf.getFieldName());
        } else {
            throw new IllegalArgumentException("Method can only deal with QueryClass "
                                               + "and QueryField");
        }

        ret.addToSelect(qnNew);
        ret.setDistinct(true);

        ret.clearOrderBy();
        ret.addToOrderBy(qnNew);
        return ret;

    }

    /**
     * Generates a query that searches for all objects in the database that have the fieldnames
     * set to the values in the object.
     *
     * @param model a Model
     * @param obj the Object to take values from
     * @param fieldNames the field names to search by
     * @return a Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query createQueryForExampleObject(Model model, InterMineObject obj,
            Set fieldNames) throws ObjectStoreException {
        Query q = new Query();
        Class cls = obj.getClass();
        Map fields = model.getFieldDescriptorsForClass(cls);
        QueryClass qc = new QueryClass(cls);
        q.addFrom(qc);
        q.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        try {
            Iterator fieldIter = fieldNames.iterator();
            while (fieldIter.hasNext()) {
                String fieldName = (String) fieldIter.next();
                FieldDescriptor fd = (FieldDescriptor) fields.get(fieldName);
                if (fd instanceof AttributeDescriptor) {
                    QueryField qf = new QueryField(qc, fieldName);
                    cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                new QueryValue(TypeUtil.getFieldValue(obj, fieldName))));
                } else if (fd instanceof CollectionDescriptor) {
                    throw new IllegalArgumentException("Cannot include a collection in the example"
                            + " fields");
                } else if (fd instanceof ReferenceDescriptor) {
                    ReferenceDescriptor ref = (ReferenceDescriptor) fd;
                    QueryClass subQc = new QueryClass(ref.getReferencedClassDescriptor().getType());
                    q.addFrom(subQc);
                    QueryObjectReference qor = new QueryObjectReference(qc, fieldName);
                    cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS, subQc));
                    cs.addConstraint(new ClassConstraint(subQc, ConstraintOp.EQUALS, 
                                (InterMineObject) TypeUtil.getFieldValue(obj, fieldName)));
                } else {
                    throw new IllegalArgumentException("Illegal field name for example: "
                            + fieldName);
                }
            }
            q.setConstraint(cs);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
        return q;
    }
}
