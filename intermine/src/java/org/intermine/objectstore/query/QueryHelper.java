package org.flymine.objectstore.query;

import org.apache.log4j.Logger;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.flymine.util.ModelUtil;
import org.flymine.util.TypeUtil;

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
     * @return a Query that will retrieve these objects from the data store
     */

    public static Query createQueryForExampleSet(Set orig) {

        Set collectionClasses = new HashSet();
        Map referenceQueryClasses = new HashMap();

        if (orig.size() < 1) {
            return null;
        }

        // Find out what class we are dealing with
        Class clazz = TypeUtil.getElementType(orig);
        Set keys = ModelUtil.getKey(clazz);
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
            Iterator keysIter = keys.iterator();

            while (keysIter.hasNext()) {
                String key = (String) keysIter.next();
                int type = ModelUtil.getFieldType(clazz, key);

                try {

                    if (type == ModelUtil.ATTRIBUTE) {
                        QueryField qf = new QueryField(qc, key);
                        QueryValue value = new QueryValue(TypeUtil.getFieldValue(obj, key));
                        Constraint c = new SimpleConstraint(qf, SimpleConstraint.EQUALS, value);
                        csThisObject.addConstraint(c);
                    } else if (type == ModelUtil.REFERENCE) {
                        // Get the class that this reference refers to
                        Class otherClass = TypeUtil.getField(clazz, key).getType();

                        QueryClass otherQueryClass;
                        QueryReference qr = new QueryObjectReference(qc, key);

                        Object otherObject = TypeUtil.getFieldValue(obj, key);

                        // Add this to the from list of the query (if it is not already there)
                        if (!referenceQueryClasses.containsKey(key)) {
                            otherQueryClass = new QueryClass(otherClass);
                            referenceQueryClasses.put(key, otherQueryClass);
                            q.addFrom(otherQueryClass);
                            // And add a ClassConstraint for it
                            csCombining.addConstraint(new ContainsConstraint(qr,
                                    ContainsConstraint.CONTAINS, otherQueryClass));
                        } else {
                            otherQueryClass = (QueryClass) referenceQueryClasses.get(key);
                        }

                        // Add the constraint for this object
                        Constraint c = new ClassConstraint(otherQueryClass,
                                                           ClassConstraint.EQUALS, otherObject);
                        csThisObject.addConstraint(c);
                    } else if (type == ModelUtil.COLLECTION) {
                        throw new UnsupportedOperationException("Collections are not "
                                                                + "supported in primary keys");
                    }
                } catch (NoSuchFieldException e) {
                    LOG.error("No such field " + key + " in class " + qc.getType().getName());
                    return null;
                } catch (IllegalAccessException e) {
                    LOG.error("Cannot access field " + key + " in object " + obj.toString());
                    return null;
                }
            }

            csOr.addConstraint(csThisObject);
        }
        csCombining.addConstraint(csOr);

        q.setConstraint(csCombining);

        return q;
    }
}
