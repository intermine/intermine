package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.List;
import java.util.Map;

import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * An element that can appear in the SELECT clause of a query, representing extra data to be
 * collected for the Results - namely a collection or reference referenced in an object in the
 * results. The column in the Results associated with this object will be of type Collection. In
 * order to reference further into this field, this class contains many of the features of Query.
 * That is, you can add QueryFields and QueryPathExpressions to the SELECT list. You can also add
 * QueryClasses to the FROM list and constraints to the WHERE clause. A default QueryClass
 * corresponding to the field is available from the getDefaultClass() method.
 *
 * @author Matthew Wakeling
 */
public class QueryCollectionPathExpression implements QueryPathExpressionWithSelect, Queryable
{
    private QueryClass qc;
    private String fieldName;
    private Class<?> type;
    private Class<? extends FastPathObject> subclass = null;
    private QueryClass defaultClass;
    private List<QuerySelectable> selectList = new ArrayList<QuerySelectable>();
    private List<FromElement> additionalFromList = new ArrayList<FromElement>();
    private Constraint constraint = null;
    private boolean singleton = false;
    private boolean isCollection;
    private Map<FromElement, String> aliases = new HashMap<FromElement, String>();

    /**
     * Constructs a QueryCollectionPathExpression representing a reference from the given
     * QueryClass to the given field name.
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant collection or reference
     * @throws IllegalArgumentException if the field is not a collection or reference
     */
    public QueryCollectionPathExpression(QueryClass qc, String fieldName) {
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        if (fieldName == null) {
            throw new NullPointerException("Collection name parameter is null");
        }
        type = TypeUtil.getFieldType(qc.getType(), fieldName);
        if (type == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qc.getType());
        }
        if (Collection.class.isAssignableFrom(type)) {
            isCollection = true;
            defaultClass = new QueryClass(TypeUtil.getElementType(qc.getType(), fieldName));
        } else if (InterMineObject.class.isAssignableFrom(type)) {
            isCollection = false;
            defaultClass = new QueryClass(type);
        } else {
            throw new IllegalArgumentException("Field " + qc.getType().getName() + "."
                    + fieldName + " is not a collection or reference");
        }
        this.qc = qc;
        this.fieldName = fieldName;
    }

    /**
     * Constructs a QueryCollectionPathExpression representing a reference from the given
     * QueryClass to the given field name, constrained to be a particular subclass.
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant collection or reference
     * @param subclasses a Class that is a subclass of the field class
     * @throws IllegalArgumentException if the field is not a collection or reference
     */
    public QueryCollectionPathExpression(QueryClass qc, String fieldName, Class<?>... subclasses) {
        subclass = DynamicUtil.composeDescriptiveClass(subclasses);
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        if (fieldName == null) {
            throw new NullPointerException("Collection name parameter is null");
        }
        if (subclass == null) {
            throw new NullPointerException("Subclass parameter is null");
        }
        type = TypeUtil.getFieldType(qc.getType(), fieldName);
        if (type == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qc.getType());
        }
        Class<? extends FastPathObject> referenceType;
        if (Collection.class.isAssignableFrom(type)) {
            isCollection = true;
            referenceType = TypeUtil.getElementType(qc.getType(), fieldName);
            if (!referenceType.isAssignableFrom(subclass)) {
                throw new IllegalArgumentException("subclass parameter " + subclass.getName()
                        + " is not a subclass of collection element type "
                        + TypeUtil.getElementType(qc.getType(), fieldName).getName());
            }
            defaultClass = new QueryClass(subclass);
        } else if (InterMineObject.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked") Class<? extends FastPathObject> tmpType =
                (Class) type;
            referenceType = tmpType;
            if (!type.isAssignableFrom(subclass)) {
                throw new IllegalArgumentException("subclass parameter " + subclass.getName()
                        + " is not a subclass of reference type " + type.getName());
            }
            isCollection = false;
            defaultClass = new QueryClass(subclass);
        } else {
            throw new IllegalArgumentException("Field " + qc.getType().getName() + "."
                    + fieldName + " is not a collection or reference");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        if (subclass.equals(referenceType)) {
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
        return type;
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
        if (singleton && (selectList.size() >= 1)) {
            throw new IllegalArgumentException("Cannot have a singleton collection with more than"
                    + " one element on the SELECT list");
        }
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
     * Adds an element to the FROM list.
     *
     * @param node a QueryNode
     */
    public void addFrom(FromElement node) {
        additionalFromList.add(node);
    }

    /**
     * Adds an element to the FROM list, including an alias.
     *
     * @param node a FromElement
     * @param alias the alias
     */
    public void addFrom(FromElement node, String alias) {
        additionalFromList.add(node);
        aliases.put(node, alias);
    }

    /**
     * Returns the additional FROM list.
     *
     * @return a List
     */
    public List<FromElement> getFrom() {
        return Collections.unmodifiableList(additionalFromList);
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
     * @param bag a Collection of objects to fetch data for, or null to not constrain. If the
     * reference is a collection, then this bag should contain the objects that have the collection.
     * If the reference is to an object, then this bag should contain the objects pointed to by
     * the reference
     * @return a Query
     */
    public Query getQuery(Collection<? extends InterMineObject> bag) {
        if (isCollection) {
            Query q = new Query();
            // We know that any QueryClass that has a collection must be of a type that extends
            // InterMineObject, as you need an id to have a collection.
            @SuppressWarnings("unchecked") Class<? extends InterMineObject> tmpType =
                (Class) qc.getType();
            QueryClassBag qcb = new QueryClassBag(tmpType, bag);
            q.addFrom(qcb, "bag");
            q.addFrom(defaultClass, "default");
            for (FromElement node : additionalFromList) {
                if (aliases.containsKey(node)) {
                    q.addFrom(node, aliases.get(node));
                } else {
                    q.addFrom(node);
                }
            }
            q.addToSelect(new QueryField(qcb), "bagId");
            if (selectList.isEmpty()) {
                q.addToSelect(defaultClass);
            } else {
                for (QuerySelectable selectable : selectList) {
                    q.addToSelect(selectable);
                }
            }
            if (constraint == null) {
                q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qcb,
                                fieldName), ConstraintOp.CONTAINS, defaultClass));
            } else {
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                cs.addConstraint(constraint);
                cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb,
                                fieldName), ConstraintOp.CONTAINS, defaultClass));
                q.setConstraint(cs);
            }
            q.setDistinct(false);
            return q;
        } else {
            Query q = new Query();
            q.addFrom(defaultClass, "default");
            q.addToSelect(new QueryField(defaultClass, "id"));
            for (FromElement node : additionalFromList) {
                if (aliases.containsKey(node)) {
                    q.addFrom(node, aliases.get(node));
                } else {
                    q.addFrom(node);
                }
            }
            if (selectList.isEmpty()) {
                q.addToSelect(defaultClass);
            } else {
                for (QuerySelectable selectable : selectList) {
                    q.addToSelect(selectable);
                }
            }
            if (bag != null) {
                if (constraint == null) {
                    q.setConstraint(new BagConstraint(defaultClass, ConstraintOp.IN, bag));
                } else {
                    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                    cs.addConstraint(constraint);
                    cs.addConstraint(new BagConstraint(defaultClass, ConstraintOp.IN, bag));
                    q.setConstraint(cs);
                }
            } else if (constraint != null) {
                q.setConstraint(constraint);
            }
            q.setDistinct(false);
            return q;
        }
    }

    /**
     * Returns true if the SELECT list is empty or if singleton results are requested.
     *
     * @return a boolean
     */
    public boolean isSingleton() {
        return singleton || selectList.isEmpty();
    }

    /**
     * Sets whether the collection should be a singleton collection, or whether it should be a
     * collection of ResultRows.
     *
     * @param singleton true if the collection should be singletons
     */
    public void setSingleton(boolean singleton) {
        if (singleton && (selectList.size() > 1)) {
            throw new IllegalArgumentException("Cannot have a singleton collection with more than"
                    + " one element on the SELECT list");
        }
        this.singleton = singleton;
    }

    /**
     * Returns true if the reference is a collection. This class can be used for non-collection
     * references, as it adds the feature of having a FROM list (and with that, allowing a variable
     * number of rows).
     *
     * @return a boolean
     */
    public boolean isCollection() {
        return isCollection;
    }
}
