/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.query;

import org.apache.log4j.Logger;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.ReferenceDescriptor;
import org.flymine.metadata.CollectionDescriptor;

/**
 * Class that helps build queries or parts of queries for common situations.
 *
 * @author Andrew Varley
 */
public class QueryHelper
{
    protected static final Logger LOG = Logger.getLogger(QueryHelper.class);

    /**
     * Create a query that will retrieve a set of objects from the data store given
     * a set of example objects of the same class.
     * <p>
     * For example:
     * <pre>
     * Company (key = name, type)
     * Department (key = company, name)
     * Group (key = departments)
     *
     * Query for company is:
     *
     * select company
     * from company
     * where ((company.name = &lt;company1.name&gt; and company.type = &lt;company1.type&gt;)
     *        or (company.name = &lt;company2.name&gt; and company.type = &lt;company2.type&gt;))
     *
     * Query for department is:
     *
     * select department
     * from company, department
     * where department.company CONTAINS company
     * and ((department.name = &lt;department1.name&gt;
     *       and company = &lt;department1.company&gt;)
     *      or (department.name = &lt;department2.name&gt;
     *          and company = &lt;department2.company&gt;)
     *
     * Query for group is: TODO: THIS IS WRONG
     *
     * select group
     * from group, departments
     * where group.departments CONTAINS department
     * and (department = &lt;group1.department&gt;
     *      or department = &lt;group2.department&gt; ...)
     *
     * and combinations thereof, for keys containing a mixture of
     * attributes, references and collections
     * </pre>
     *
     * @param orig the set of Objects to query for
     * @param model the metadata used to build the query
     * @return a Query that will retrieve these objects from the data store
     */
    public static Query createQueryForExampleSet(Set orig, Model model) {

        Set collectionClasses = new HashSet();
        Map referenceQueryClasses = new HashMap();

        if (orig.size() < 1) {
            return null;
        }

        // Find out what class we are dealing with
        Class clazz = TypeUtil.getElementType(orig);
        QueryClass qc = new QueryClass(clazz);

        Query q = new Query();
        q.addToSelect(qc);
        q.addFrom(qc);

        // AND together the constraints combining QueryClasses in the FROM list
        ConstraintSet csCombining = new ConstraintSet(ConstraintSet.AND);

        // OR together the constraints for each object in Set
        ConstraintSet csOr = new ConstraintSet(ConstraintSet.OR);

        Iterator i = orig.iterator();
        while (i.hasNext()) {

            // AND together the constraints for each individual example in the Set
            ConstraintSet csThisObject = new ConstraintSet(ConstraintSet.AND);

            Object obj = i.next();
            // Check it is of the same class
            if (!obj.getClass().equals(clazz)) {
                throw new IllegalArgumentException("The objects in the given set "
                                                   + "are not of the same type");
            }

            // Get the primary keys for this object
            ClassDescriptor cld = model.getClassDescriptorByName(clazz.getName());
            Iterator keyIter = cld.getPkFieldDescriptors().iterator();

            while (keyIter.hasNext()) {
                FieldDescriptor field = (FieldDescriptor) keyIter.next();
                String fieldName = field.getName();
                try {
                    if (field instanceof CollectionDescriptor) {
                        throw new UnsupportedOperationException("Collections are not "
                                                                + "supported in primary keys");
                    } else if (field instanceof AttributeDescriptor) {
                        QueryField qf = new QueryField(qc, fieldName);
                        QueryValue value = new QueryValue(TypeUtil.getFieldValue(obj, fieldName));
                        Constraint c = new SimpleConstraint(qf, SimpleConstraint.EQUALS, value);
                        csThisObject.addConstraint(c);
                    } else if (field instanceof ReferenceDescriptor) {
                        // Get the class that this reference refers to
                        Class otherClass = TypeUtil.getField(clazz, fieldName).getType();

                        QueryClass otherQueryClass;
                        QueryReference qr = new QueryObjectReference(qc, fieldName);

                        Object otherObject = TypeUtil.getFieldValue(obj, fieldName);

                        // Add this to the from list of the query (if it is not already there)
                        if (!referenceQueryClasses.containsKey(fieldName)) {
                            otherQueryClass = new QueryClass(otherClass);
                            referenceQueryClasses.put(fieldName, otherQueryClass);
                            q.addFrom(otherQueryClass);
                            // And add a ClassConstraint for it
                            csCombining.addConstraint(new ContainsConstraint(qr,
                                    ContainsConstraint.CONTAINS, otherQueryClass));
                        } else {
                            otherQueryClass = (QueryClass) referenceQueryClasses.get(fieldName);
                        }

                        // Add the constraint for this object
                        Constraint c = new ClassConstraint(otherQueryClass,
                                                           ClassConstraint.EQUALS, otherObject);
                        csThisObject.addConstraint(c);
                    }
                } catch (NoSuchFieldException e) {
                    LOG.error("No such field " + fieldName + " in class " + qc.getType().getName());
                    return null;
                }  catch (IllegalAccessException e) {
                    LOG.error("No access field " + fieldName + " in object " + obj.toString());
                    return null;
                }
            }
            csOr.addConstraint(csThisObject);
        }
        csCombining.addConstraint(csOr);

        q.setConstraint(csCombining);

        return q;
    }

    /**
     * Create a query that will retrieve an object from the data store given
     * an example objects of the same class. The query will follow the primary
     * key dependencies for the related objects.
     * <p>
     * For example:
     * <pre>
     * Company (key = name, type)
     * Department (key = company, name)
     * Group (key = departments)
     *
     * Query for company is:
     *
     * select company
     * from company
     * where company.name = &lt;company1.name&gt;
     * and company.type = &lt;company1.type&gt;
     *
     * Query for department is:
     *
     * select department
     * from company, department
     * where department.company CONTAINS company
     * and department.name = &lt;department1.name&gt;
     * and company.name = &lt;department1.company.name&gt;
     * and company.type = &lt;department1.company.type&gt;
     *
     * Query for group is: TODO: not yet implemented
     *
     * select group
     * from group, department dept1, company company1, department dept2, ...
     * where group.departments CONTAINS dept1
     * and dept1.name = &lt;department1.name&gt;
     * and dept1.company CONTAINS company1
     * and company1.name = &lt;department1.company.name&gt;
     * and company.type = &lt;department1.company.type&gt;
     * and dept2.name = &lt;department1.name&gt;
     * and dept2.company CONTAINS company2
     * and company2.name = &lt;department2.company.name&gt;
     * and company.type = &lt;department2.company.type&gt;
     * ...
     *
     * and combinations thereof, for keys containing a mixture of
     * attributes, references and collections
     * </pre>
     *
     * @param obj the Object to query for
     * @param model the metadata used to build the query
     * @return a Query that will retrieve this object from the data store
     * @throws IllegalArgumentException if any primary key fields are not set in obj
     */

    public static Query createQueryForExampleObject(Object obj, Model model) {

        if (obj == null) {
            throw new NullPointerException("obj cannot be null");
        }

        Set collectionClasses = new HashSet();
        Map referenceQueryClasses = new HashMap();

        // Find out what class we are dealing with
        Class clazz = obj.getClass();
        QueryClass qc = new QueryClass(clazz);

        Query q = new Query();
        q.addToSelect(qc);
        q.addFrom(qc);

        // AND together the constraints combining QueryClasses in the FROM list
        ConstraintSet csCombining = new ConstraintSet(ConstraintSet.AND);
        q.setConstraint(csCombining);
        addKeysToQuery(q, qc, obj, model);
        return q;
    }


    /**
     * Add the primary keys of a particular object to a query
     *
     * @param q the Query to be added
     * @param qc the QueryClass that represents obj in the Query
     * @param obj the Object that is to have its primary keys added
     * @param model the metadata used to build the query
     * @throws IllegalArgumentException if any primary key fields are not set
     *
     */
    protected static void addKeysToQuery(Query q, QueryClass qc, Object obj, Model model) {
        ClassDescriptor cld = model.getClassDescriptorByName(obj.getClass().getName());
        Iterator keyIter = cld.getPkFieldDescriptors().iterator();
        while (keyIter.hasNext()) {
            addKeyToQuery(q, qc, obj, (FieldDescriptor) keyIter.next(), model);
        }
    }

    /**
     * Add the primary key field of a particular object to a query
     *
     * @param q the Query to be added
     * @param qc the QueryClass that represents obj in the Query
     * @param obj the Object that is to have one of its primary key fields added
     * @param field the field in qc to be constrained in the Query
     * @param model the metadata used to build the query
     * @throws IllegalArgumentException if any primary key fields are not set
     *
     */
    protected static void addKeyToQuery(Query q, QueryClass qc, Object obj,
                                        FieldDescriptor field, Model model) {
        try {
            if (field instanceof CollectionDescriptor) {
                throw new UnsupportedOperationException("Collections are not "
                                                        + "supported in primary keys");
            } else if (field instanceof AttributeDescriptor) {
                QueryField qf = new QueryField(qc, field.getName());
                QueryValue value = new QueryValue(TypeUtil.getFieldValue(obj, field.getName()));
                if (value == null) {
                    throw new IllegalArgumentException("All primary key fields must be set");
                }
                Constraint c = new SimpleConstraint(qf, SimpleConstraint.EQUALS, value);
                ((ConstraintSet) q.getConstraint()).addConstraint(c);
            
            } else if (field instanceof ReferenceDescriptor) {
            
                Object otherObject = TypeUtil.getFieldValue(obj, field.getName());            
                if (otherObject == null) {
                    throw new IllegalArgumentException("All primary key fields must be set");
                }
            
                QueryReference qr = new QueryObjectReference(qc, field.getName());
                Class otherClass = TypeUtil.getField(obj.getClass(), field.getName()).getType();
                QueryClass otherQueryClass = new QueryClass(otherClass);
                q.addFrom(otherQueryClass);
                // And add a ClassConstraint for it
                ((ConstraintSet) q.getConstraint())
                    .addConstraint(new ContainsConstraint(qr, ContainsConstraint.CONTAINS, 
                                                          otherQueryClass));
                
                // Add the keys of the other object
                addKeysToQuery(q, otherQueryClass, otherObject, model);
            }
        } catch (NoSuchFieldException e) {
            LOG.error("No such field " + field + " in class " + qc.getType().getName());
        } catch (IllegalAccessException e) {
            LOG.error("Cannot access field " + field + " in object " + obj.toString());
         }

    }
}
