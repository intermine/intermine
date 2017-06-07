package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;

/**
 * An element that can appear in the SELECT clause of a query, representing extra data to be
 * collected for the Results - namely a object referenced by some other object in the results. In
 * order to reference further into this reference, this class contains many of the features of
 * Query. That is, you can add QueryFields and QueryPathExpressions to the SELECT list. You can also
 * add QueryClasses to the FROM list and constraints to the WHERE clause. A default QueryClass
 * corresponding to the reference is available from the getDefaultClass method. Counter-intuitively,
 * this path expression may return multiple rows per original row, if extra things are added to the
 * FROM list for example. In this case, this object should be on the SELECT list of the original
 * Query. In the case where this object is guaranteed to return a maximum of one row per original
 * row, then PathExpressionField objects should be put in the SELECT list of the original query
 * instead. The definition is that if this object contains anything in the FROM element, then we
 * cannot guarantee that it will only have one row per original row.
 *
 * @author Matthew Wakeling
 */
public class QueryObjectPathExpression implements QueryPathExpressionWithSelect, Queryable
{
    private QueryClass qc;
    private String fieldName;
    private Class<? extends InterMineObject> type;
    private Class<? extends FastPathObject> subclass = null;
    private QueryClass defaultClass;
    private List<QuerySelectable> selectList = new ArrayList<QuerySelectable>();
    private Constraint constraint = null;

    /**
     * Constructs a QueryObjectPathExpression representing an object reference from the given
     * QueryClass to the given fieldname.
     *
     * @param qc the QueryClass of the starting class
     * @param fieldName the name of field in qc we want to perform an outer join on
     * @throws IllegalArgumentException if the field is not an object reference
     */
    public QueryObjectPathExpression(QueryClass qc, String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        Method field = TypeUtil.getGetter(qc.getType(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qc.getType());
        }
        if (Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        if (!InterMineObject.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is not an object reference"
                    + " type - was " + field.getReturnType() + " instead");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        @SuppressWarnings("unchecked") Class<? extends InterMineObject> tmpType =
            (Class) field.getReturnType();
        this.type = tmpType;
        defaultClass = new QueryClass(type);
    }

    /**
     * Constructs a QueryObjectPathExpression representing an object reference from the given
     * QueryClass to the given fieldname, constrained to be a particular subclass.
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant field
     * @param subclasses a Class that is a subclass of the field class
     * @throws IllegalArgumentException if the field is not an object reference
     */
    public QueryObjectPathExpression(QueryClass qc, String fieldName, Class<?>... subclasses) {
        subclass = DynamicUtil.composeDescriptiveClass(subclasses);
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        if (subclass == null) {
            throw new NullPointerException("Subclass parameter is null");
        }
        Method field = TypeUtil.getGetter(qc.getType(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qc.getType());
        }
        if (Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        if (!InterMineObject.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is not an object reference"
                    + " type - was " + field.getReturnType() + " instead");
        }
        if (!field.getReturnType().isAssignableFrom(subclass)) {
            throw new IllegalArgumentException("subclass parameter " + subclass.getName()
                    + " is not a subclass of reference type " + type.getName());
        }
        this.qc = qc;
        this.fieldName = fieldName;
        @SuppressWarnings("unchecked") Class<? extends InterMineObject> tmpType =
            (Class) field.getReturnType();
        this.type = tmpType;
        defaultClass = new QueryClass(subclass);
        if (subclass.equals(type)) {
            subclass = null;
        }
    }

    /**
     * Returns the QueryClass of which the field is a member.
     *
     * @return the QueryClass
     */
    public QueryClass getQueryClass() {
        return qc;
    }

    /**
     * Returns the name of the field.
     *
     * @return field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the subclass if it exists.
     *
     * @return the subclass
     */
    public Class<? extends FastPathObject> getSubclass() {
        return subclass;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getType() {
        if (selectList.isEmpty()) {
            return type;
        } else {
            return selectList.get(0).getType();
        }
    }

    /**
     * Returns the QueryClass that represents the collection in this object.
     *
     * @return a QueryClass
     */
    public QueryClass getDefaultClass() {
        return defaultClass;
    }

    /**
     * Adds an element to the SELECT list. If the SELECT list is left empty, then the collection
     * will use default behaviour.
     *
     * @param selectable a QuerySelectable
     */
    public void addToSelect(QuerySelectable selectable) {
        selectList.add(selectable);
    }

    /**
     * Returns the SELECT list.
     *
     * @return a List
     */
    public List<QuerySelectable> getSelect() {
        return Collections.unmodifiableList(selectList);
    }

    /**
     * Sets the additional constraint.
     *
     * @param c a Constraint
     */
    public void setConstraint(Constraint c) {
        constraint = c;
    }

    /**
     * Returns the additional constraint.
     *
     * @return a Constraint
     */
    public Constraint getConstraint() {
        return constraint;
    }

    /**
     * Returns the Query that will fetch the data represented by this object, given a Collection
     * of objects to fetch it for.
     *
     * @param bag a Collection of objects to fetch data for, or null to not constrain
     * @param isNoNotXml true if the database is in missingNotXml mode
     * @return a Query
     */
    public Query getQuery(Collection<Integer> bag, boolean isNoNotXml) {
        if (isNoNotXml && (constraint == null) && selectList.isEmpty() && (subclass == null)) {
            Query q = new Query();
            QueryClass newQc = new QueryClass(InterMineObject.class);
            q.addFrom(newQc);
            q.addToSelect(new QueryField(newQc, "id"));
            q.addToSelect(newQc);
            if (bag != null) {
                q.setConstraint(new BagConstraint(new QueryField(newQc, "id"), ConstraintOp.IN,
                        bag));
            }
            q.setDistinct(false);
            return q;
        } else {
            Query q = new Query();
            q.addFrom(defaultClass, "default");
            QueryField defaultId = new QueryField(defaultClass, "id");
            q.addToSelect(defaultId);
            if (selectList.isEmpty()) {
                q.addToSelect(defaultClass);
            } else {
                for (QuerySelectable selectable : selectList) {
                    q.addToSelect(selectable);
                }
            }
            if (!q.getSelect().contains(defaultClass)) {
                q.addToSelect(defaultClass);
            }
            if (bag != null) {
                if (constraint == null) {
                    q.setConstraint(new BagConstraint(defaultId, ConstraintOp.IN, bag));
                } else {
                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                    cs.addConstraint(constraint);
                    cs.addConstraint(new BagConstraint(defaultId, ConstraintOp.IN, bag));
                    q.setConstraint(cs);
                }
            }
            q.setDistinct(false);
            return q;
        }
    }
}
