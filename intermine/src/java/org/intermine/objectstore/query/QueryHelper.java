package org.flymine.objectstore.query;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.flymine.util.ModelUtil;
import org.flymine.util.TypeUtil;

/**
 * Class that heps build queries or parts of queries for common situations.
 *
 * @author Andrew Varley
 */
public class QueryHelper
{

    protected static final Logger LOG = Logger.getLogger(QueryHelper.class);

    /**
     * Create a query that will retrieve a set of objects from the data store given
     * a set of example objects of the same class
     *
     * For example:
     * Company (key = name)
     * Department (key = company, name)
     * Group (key = departments)
     *
     * Query for department is:
     *
     * select department
     * from company, department
     * where company.department = department
     * and (department = <example1>
     *      or department = <example2> ...)
     *
     * Query for company is:
     *
     * select company
     * from company
     * where (company.name = <example1.name>
     *        or comapny.name = <example2.name> ...)
     *
     * Query for group is:
     *
     * select group
     * from group, departments
     * where group.departments CONTAINS department
     * and (department = <example1>
     *      or department = <example2> ...)
     *
     * and combinations thereof, for keys containing a mixture of
     * attributes, references and collections
     *
     * @param orig the set of Objects to query for
     * @return a Query that will retrieve these objects from the data store
     */

    public static Query createQueryForExampleSet(Set orig) {

        Set collectionClasses = new HashSet();

        if (orig.size() < 1) {
            return null;
        }

        // Find out what class we are dealing with
        Class clazz = TypeUtil.getElementType(orig);
        Collection keys = ModelUtil.getKey(clazz);
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

                        // Add this to the from list of the query (if it is not already there)
                        QueryClass otherQueryClass = new QueryClass(otherClass);
                        QueryReference qr = new QueryObjectReference(qc, key);
                        q.addFrom(new QueryClass(otherClass));
                        // And add a ClassConstraint for it
                        csCombining.addConstraint(new ContainsConstraint(qr,
                                                    ContainsConstraint.CONTAINS, otherQueryClass));
                        Constraint c = new ClassConstraint(otherQueryClass,
                                                           ClassConstraint.EQUALS, obj);
                        csThisObject.addConstraint(c);
                    } else if (type == ModelUtil.COLLECTION) {

                        // Get the class that this reference refers to
                        Class otherClass = TypeUtil.getElementType(
                                    (Collection) TypeUtil.getFieldValue(clazz, key));

                        // Add this to the from list of the query (if it is not already there)
                        QueryClass otherQueryClass = new QueryClass(otherClass);
                        QueryReference qr = new QueryCollectionReference(qc, key);
                        if (!collectionClasses.contains(key)) {
                            collectionClasses.add(key);
                            q.addFrom(new QueryClass(otherClass));
                            // And add a ClassConstraint for it
                            csCombining.addConstraint(new ContainsConstraint(qr,
                                    ContainsConstraint.CONTAINS, otherQueryClass));
                        }
                        Constraint c = new ClassConstraint(otherQueryClass,
                                                           ClassConstraint.EQUALS, obj);
                        csThisObject.addConstraint(c);
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
