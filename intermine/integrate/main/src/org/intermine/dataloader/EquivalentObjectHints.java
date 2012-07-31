package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SubqueryExistsConstraint;
import org.intermine.util.AlwaysSet;
import org.intermine.util.DynamicUtil;
import org.intermine.util.PseudoSet;

import org.apache.log4j.Logger;

/**
 * Object for holding hint data for the getEquivalentObjects method in IntegrationWriters.
 *
 * @author Matthew Wakeling
 */
public class EquivalentObjectHints
{
    private static final Logger LOG = Logger.getLogger(EquivalentObjectHints.class);
    private static final int SUMMARY_SIZE = 100;

    private boolean databaseEmptyChecked = false;
    private boolean databaseEmpty = false;
    private Map<Class<?>, Boolean> classStatus = new HashMap<Class<?>, Boolean>();
    private Map<ClassAndFieldName, Set<Object>> classAndFieldNameValues
        = new HashMap<ClassAndFieldName, Set<Object>>();
    private Map<ClassAndFieldName, Set<Object>> classAndFieldNameQueried
        = new HashMap<ClassAndFieldName, Set<Object>>();
    private Map<String, ClassAndFieldName> summaryToCafn = new HashMap<String, ClassAndFieldName>();

    private ObjectStore os;

    /**
     * Constructor.
     *
     * @param os an ObjectStore of a production database
     */
    public EquivalentObjectHints(ObjectStore os) {
        this.os = os;
    }

    /**
     * Returns true if the database was empty at the start of the run.
     *
     * @return a boolean
     */
    public boolean databaseEmpty() {
        if (databaseEmptyChecked) {
            return databaseEmpty;
        }
        // Okay, we haven't run the query yet.
        try {
            Query q = new Query();
            Query subQ = new Query();
            QueryClass qc = new QueryClass(InterMineObject.class);
            subQ.addFrom(qc);
            subQ.addToSelect(new QueryField(qc, "id"));
            q.addToSelect(new QueryValue(new Integer(1)));
            q.setConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, subQ));
            List<?> results = os.execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
            if (results.isEmpty()) {
                databaseEmpty = true;
            }
            databaseEmptyChecked = true;
        } catch (ObjectStoreException e) {
            LOG.warn("Error checking database", e);
            databaseEmptyChecked = true;
            databaseEmpty = false;
            return false;
        }
        return databaseEmpty;
    }

    /**
     * Returns true if there were no instances of the given class in the database.
     *
     * @param clazz the class, must be in the model
     * @return a boolean
     */
    public boolean classNotExists(Class<? extends FastPathObject> clazz) {
        if (databaseEmpty) {
            return true;
        }
        Boolean status = classStatus.get(clazz);
        if (status == null) {
            try {
                Query q = new Query();
                Query subQ = new Query();
                QueryClass qc = new QueryClass(clazz);
                subQ.addFrom(qc);
                subQ.addToSelect(new QueryField(qc, "id"));
                q.addToSelect(new QueryValue(new Integer(1)));
                q.setConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, subQ));
                List<?> results = os.execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
                if (results.isEmpty()) {
                    status = Boolean.TRUE;
                } else {
                    status = Boolean.FALSE;
                }
                classStatus.put(clazz, status);
            } catch (ObjectStoreException e) {
                LOG.warn("Error checking database for " + clazz, e);
                return false;
            }
        }
        return status.booleanValue();
    }

    /**
     * Returns true if there were no instances of the given class with the given field set to the
     * given value.
     *
     * @param clazz the class, must be in the model
     * @param fieldName the name of the field
     * @param value the value
     * @return a boolean
     */
    public boolean pkQueryFruitless(Class<? extends FastPathObject> clazz, String fieldName,
            Object value) {
        if (classNotExists(clazz)) {
            return true;
        }
        ClassAndFieldName cafn = new ClassAndFieldName(clazz, fieldName);
        String summaryName = DynamicUtil.getFriendlyName(clazz) + "." + fieldName;
        Set<Object> values = classAndFieldNameValues.get(cafn);
        if (values == null) {
            try {
                Query testQuery = new Query();
                Query q = new Query();
                QueryClass qc = new QueryClass(clazz);
                q.addFrom(qc);
                QueryEvaluable qs;
                try {
                    qs = new QueryField(qc, fieldName);
                } catch (IllegalArgumentException e) {
                    qs = new QueryForeignKey(qc, fieldName);
                }
                q.addToSelect(qs);
                q.setDistinct(false);
                testQuery.addFrom(q);
                testQuery.addToSelect(new QueryField(q, qs));
                testQuery.setDistinct(true);
                q.setLimit(SUMMARY_SIZE * 10);
                List<ResultsRow<Object>> results = os.execute(testQuery, 0, SUMMARY_SIZE, false,
                        false, ObjectStore.SEQUENCE_IGNORE);
                if (results.size() < SUMMARY_SIZE) {
                    q = QueryCloner.cloneQuery(q);
                    q.setLimit(Integer.MAX_VALUE);
                    q.setDistinct(true);
                    results = os.execute(q, 0, SUMMARY_SIZE, false, false,
                            ObjectStore.SEQUENCE_IGNORE);
                }
                if (results.size() >= SUMMARY_SIZE) {
                    if (Integer.class.equals(qs.getType())) {
                        q = new Query();
                        q.addFrom(qc);
                        q.addToSelect(new QueryFunction(qs, QueryFunction.MIN));
                        q.addToSelect(new QueryFunction(qs, QueryFunction.MAX));
                        q.setDistinct(false);
                        List<ResultsRow<Object>> results2 = os.execute(q, 0, 2, false, false,
                                ObjectStore.SEQUENCE_IGNORE);
                        values = new IntegerRangeSet(((Integer) results2.get(0).get(0)).intValue(),
                                ((Integer) results2.get(0).get(1)).intValue());
                    } else {
                        values = AlwaysSet.getInstance();
                    }
                } else {
                    values = new HashSet<Object>();
                    for (ResultsRow<Object> row : results) {
                        values.add(row.get(0));
                    }
                }
                classAndFieldNameValues.put(cafn, values);
                classAndFieldNameQueried.put(cafn, new HashSet<Object>());
                summaryToCafn.put(summaryName, cafn);
            } catch (ObjectStoreException e) {
                LOG.warn("Error checking database for " + clazz.getName() + "." + fieldName, e);
                return false;
            }
        }
        Set<Object> queried = classAndFieldNameQueried.get(cafn);
        if (queried instanceof HashSet<?>) {
            queried.add(value);
            if (queried.size() >= SUMMARY_SIZE) {
                if (value instanceof Integer) {
                    IntegerRangeSet newQueried = new IntegerRangeSet();
                    for (Object oldValue : queried) {
                        newQueried.add(oldValue);
                    }
                    classAndFieldNameQueried.put(cafn, newQueried);
                } else {
                    classAndFieldNameQueried.put(cafn, AlwaysSet.getInstance());
                }
            }
        } else if (queried instanceof IntegerRangeSet) {
            queried.add(value);
        }
        return !values.contains(value);
    }

    /**
     * Returns a Set of values that have been tested for a particular class and fieldname.
     *
     * @param summaryName a String
     * @return a Set of values, or an AlwaysSet if too many values were tested
     */
    public Set<Object> getQueried(String summaryName) {
        return classAndFieldNameQueried.get(summaryToCafn.get(summaryName));
    }

    /**
     * Returns a Set of values that were in the database for a particular class and fieldname.
     *
     * @param summaryName a String
     * @return a Set of values, or an AlwaysSet if too many values were tested
     */
    public Set<Object> getValues(String summaryName) {
        return classAndFieldNameValues.get(summaryToCafn.get(summaryName));
    }

    private static class ClassAndFieldName
    {
        private Class<? extends FastPathObject> clazz;
        private String fieldName;

        public ClassAndFieldName(Class<? extends FastPathObject> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode() + 3 * fieldName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ClassAndFieldName) {
                ClassAndFieldName c = (ClassAndFieldName) o;
                return clazz.equals(c.clazz) && fieldName.equals(c.fieldName);
            }
            return false;
        }
    }

    private static class IntegerRangeSet extends PseudoSet<Object>
    {
        private int low, high;

        public IntegerRangeSet() {
            this.low = Integer.MAX_VALUE;
            this.high = Integer.MIN_VALUE;
        }

        public IntegerRangeSet(int low, int high) {
            this.low = low;
            this.high = high;
        }

        public boolean contains(Object o) {
            int i = ((Integer) o).intValue();
            return (i >= low) && (i <= high);
        }

        @Override
        public boolean add(Object o) {
            int i = ((Integer) o).intValue();
            low = Math.min(low, i);
            high = Math.max(high, i);
            return false;
        }

        @Override
        public String toString() {
            return "IntegerRangeSet(" + low + " - " + high + ")";
        }
    }
}
