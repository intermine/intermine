package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2008 FlyMine
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

import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;

/**
 * An element that can appear in the SELECT clause of a query, representing extra data to be
 * collected for the Results - namely a collection referenced in an object in the results. In order
 * to reference further into this collection, this class contains many of the features of Query.
 * That is, you can add QueryFields and QueryPathExpressions to the SELECT list. You can also add
 * QueryClasses to the FROM list and constraints to the WHERE clause. A default QueryClass
 * corresponding to the collection is available from the getDefaultClass() method.
 *
 * @author Matthew Wakeling
 */
public class QueryCollectionPathExpression implements QueryPathExpression
{
    private QueryClass qc;
    private QueryObjectPathExpression qope;
    private String collectionName;
    private Class type;
    private QueryClass defaultClass;
    private List<QuerySelectable> selectList = new ArrayList();
    private List<FromElement> additionalFromList = new ArrayList();
    private Constraint constraint = null;
    private boolean singleton = false;
    private Map<FromElement, String> aliases = new HashMap();

    /**
     * Constructs a QueryCollectionPathExpression representing a collection reference from the given
     * QueryClass to the given collection name.
     *
     * @param qc the QueryClass
     * @param collectionName the name of the relevant collection
     * @throws IllegalArgumentException if the field is not a collection
     */
    public QueryCollectionPathExpression(QueryClass qc, String collectionName) {
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        if (collectionName == null) {
            throw new NullPointerException("Collection name parameter is null");
        }
        type = TypeUtil.getFieldType(qc.getType(), collectionName);
        if (type == null) {
            throw new IllegalArgumentException("Field " + collectionName + " not found in "
                    + qc.getType());
        }
        if (!Collection.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Field " + qc.getType().getName() + "."
                    + collectionName + " is not a collection");
        }
        this.qc = qc;
        this.qope = null;
        this.collectionName = collectionName;
        defaultClass = new QueryClass(TypeUtil.getElementType(qc.getType(), collectionName));
    }

    /**
     * Constructs a QueryCollectionPathExpression representing a collection reference from the given
     * QueryObjectPathExpression to the given collection name.
     *
     * @param qope the QueryObjectPathExpression
     * @param collectionName the name of the relevant collection
     * @throws IllegalArgumentException if the field is not a collection
     */
    public QueryCollectionPathExpression(QueryObjectPathExpression qope, String collectionName) {
        if (qope == null) {
            throw new NullPointerException("QueryObjectPathExpression parameter is null");
        }
        if (collectionName == null) {
            throw new NullPointerException("Collection name parameter is null");
        }
        type = TypeUtil.getFieldType(qope.getType(), collectionName);
        if (type == null) {
            throw new IllegalArgumentException("Field " + collectionName + " not found in "
                    + qope.getType());
        }
        if (!Collection.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Field " + qope.getType().getName() + "."
                    + collectionName + " is not a collection");
        }
        this.qc = null;
        this.qope = qope;
        this.collectionName = collectionName;
        defaultClass = new QueryClass(TypeUtil.getElementType(qope.getType(), collectionName));
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
     * Returns the QueryObjectPathExpression of which the field is a member.
     *
     * @return the QueryObjectPathExpression
     */
    public QueryObjectPathExpression getQope() {
        return qope;
    }

    /**
     * Returns the name of the collection.
     *
     * @return collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * {@inheritDoc}
     */
    public Class getType() {
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
     * @param bag a Collection of objects to fetch data for
     * @return a Query
     */
    public Query getQuery(Collection<InterMineObject> bag) {
        Query q = new Query();
        QueryClassBag qcb = new QueryClassBag(qc == null ? qope.getType() : qc.getType(), bag);
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
                            collectionName), ConstraintOp.CONTAINS, defaultClass));
        } else {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(constraint);
            cs.addConstraint(new ContainsConstraint(new QueryCollectionReference(qcb,
                            collectionName), ConstraintOp.CONTAINS, defaultClass));
            q.setConstraint(cs);
        }
        q.setDistinct(false);
        return q;
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
}
