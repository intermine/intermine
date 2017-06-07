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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;

/**
 * This is a static class that provides a method to clone a Query object.
 *
 * @author Matthew Wakeling
 */
public final class QueryCloner
{
    private QueryCloner() {
    }

    /**
     * Clones a query object.
     *
     * @param query a Query to clone
     * @return a Query object not connected to the original, but identical
     */
    public static Query cloneQuery(Query query) {
        Query newQuery = new Query();
        try {
            Map<Object, String> aliases = query.getAliases();
            Map<FromElement, FromElement> fromElementMap = new HashMap<FromElement, FromElement>();
            Map<QueryObjectPathExpression, QueryObjectPathExpression> qopeMap =
                new HashMap<QueryObjectPathExpression, QueryObjectPathExpression>();
            for (FromElement origFrom : query.getFrom()) {
                FromElement newFrom = null;
                if (origFrom instanceof QueryClass) {
                    newFrom = origFrom;
                } else if (origFrom instanceof Query) {
                    newFrom = cloneQuery((Query) origFrom);
                } else if (origFrom instanceof QueryClassBag) {
                    Collection<?> bag = ((QueryClassBag) origFrom).getBag();
                    Class<? extends InterMineObject> type = ((QueryClassBag) origFrom).getType();
                    if (bag == null) {
                        newFrom = new QueryClassBag(type, ((QueryClassBag) origFrom).getOsb());
                    } else {
                        newFrom = new QueryClassBag(type, (Collection<?>)
                                cloneThing(((QueryClassBag) origFrom).getBag(), null, null));
                    }
                } else {
                    throw new IllegalArgumentException("Unknown type of FromElement " + origFrom);
                }
                newQuery.addFrom(newFrom, aliases.get(origFrom));
                fromElementMap.put(origFrom, newFrom);
            }
            for (QuerySelectable origSelect : query.getSelect()) {
                QuerySelectable newSelect = (QuerySelectable) cloneThing(origSelect,
                        fromElementMap, qopeMap);
                newQuery.addToSelect(newSelect, aliases.get(origSelect));
            }
            for (QueryOrderable origOrder : query.getOrderBy()) {
                QueryOrderable newOrder = (QueryOrderable) cloneThing(origOrder, fromElementMap,
                        qopeMap);
                newQuery.addToOrderBy(newOrder);
            }
            for (QueryNode origGroup : query.getGroupBy()) {
                QueryNode newGroup = (QueryNode) cloneThing(origGroup, fromElementMap, qopeMap);
                newQuery.addToGroupBy(newGroup);
            }
            newQuery.setConstraint((Constraint) cloneThing(query.getConstraint(), fromElementMap,
                    qopeMap));
            newQuery.setDistinct(query.isDistinct());
            newQuery.setLimit(query.getLimit());
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("No such field: " + e.getMessage());
        }
        return newQuery;
    }

    private static Object cloneThing(Object orig, Map<FromElement, FromElement> fromElementMap,
            Map<QueryObjectPathExpression, QueryObjectPathExpression> qopeMap)
        throws NoSuchFieldException {
        if (orig == null) {
            return null;
        } else if (orig instanceof FromElement) {
            return fromElementMap.get(orig);
        } else if (orig instanceof QueryField) {
            QueryField origF = (QueryField) orig;
            return new QueryField(fromElementMap.get(origF.getFromElement()),
                    origF.getFieldName(), origF.getSecondFieldName(), origF.getType());
        } else if (orig instanceof QueryObjectReference) {
            QueryObjectReference origR = (QueryObjectReference) orig;
            return new QueryObjectReference((QueryClass) fromElementMap.get(origR.getQueryClass()),
                    origR.getFieldName());
        } else if (orig instanceof QueryForeignKey) {
            QueryForeignKey origK = (QueryForeignKey) orig;
            return new QueryForeignKey((QueryClass) fromElementMap.get(origK.getQueryClass()),
                    origK.getFieldName());
        } else if (orig instanceof QueryCollectionReference) {
            QueryCollectionReference origR = (QueryCollectionReference) orig;
            if (origR.getQueryClass() != null) {
                return new QueryCollectionReference((QueryClass)
                        fromElementMap.get(origR.getQueryClass()), origR.getFieldName());
            } else if (origR.getQcb() != null) {
                return new QueryCollectionReference((QueryClassBag)
                        fromElementMap.get(origR.getQcb()), origR.getFieldName());
            } else {
                return new QueryCollectionReference(origR.getQcObject(), origR.getFieldName());
            }
        } else if (orig instanceof QueryValue) {
            return new QueryValue(((QueryValue) orig).getValue());
        } else if (orig instanceof QueryFunction) {
            QueryFunction origF = (QueryFunction) orig;
            if (origF.getOperation() == QueryFunction.COUNT) {
                return orig;
            } else if (origF.getParam() instanceof QueryField) {
                return new QueryFunction((QueryField) cloneThing(origF.getParam(), fromElementMap,
                        qopeMap), origF.getOperation());
            } else {
                return new QueryFunction((QueryExpression) cloneThing(origF.getParam(),
                            fromElementMap, qopeMap), origF.getOperation());
            }
        } else if (orig instanceof QueryExpression) {
            QueryExpression origE = (QueryExpression) orig;
            if ((origE.getOperation() == QueryExpression.SUBSTRING) && (origE.getArg3() != null)) {
                return new QueryExpression((QueryEvaluable)
                        cloneThing(origE.getArg1(), fromElementMap, qopeMap),
                        (QueryEvaluable) cloneThing(origE.getArg2(), fromElementMap, qopeMap),
                        (QueryEvaluable) cloneThing(origE.getArg3(), fromElementMap, qopeMap));
            } else if ((origE.getOperation() == QueryExpression.LOWER) || (origE.getOperation()
                    == QueryExpression.UPPER)) {
                return new QueryExpression(origE.getOperation(),
                        (QueryEvaluable) cloneThing(origE.getArg1(), fromElementMap, qopeMap));
            } else {
                return new QueryExpression((QueryEvaluable)
                        cloneThing(origE.getArg1(), fromElementMap, qopeMap),
                        origE.getOperation(),
                        (QueryEvaluable) cloneThing(origE.getArg2(), fromElementMap, qopeMap));
            }
        } else if (orig instanceof QueryCast) {
            return new QueryCast((QueryEvaluable) cloneThing(((QueryCast) orig).getValue(),
                        fromElementMap, qopeMap), ((QueryCast) orig).getType());
        } else if (orig instanceof PathExpressionField) {
            PathExpressionField origP = (PathExpressionField) orig;
            QueryObjectPathExpression origQope = origP.getQope();
            QueryObjectPathExpression newQope = qopeMap.get(origQope);
            if (newQope == null) {
                newQope = (QueryObjectPathExpression) cloneThing(origQope, fromElementMap, qopeMap);
                qopeMap.put(origQope, newQope);
            }
            return new PathExpressionField(newQope, origP.getFieldNumber());
        } else if (orig instanceof QueryObjectPathExpression) {
            QueryObjectPathExpression origC = (QueryObjectPathExpression) orig;
            QueryObjectPathExpression retval = new QueryObjectPathExpression((QueryClass)
                    fromElementMap.get(origC.getQueryClass()), origC.getFieldName());
            Map<FromElement, FromElement> subFromElementMap =
                new HashMap<FromElement, FromElement>();
            subFromElementMap.put(origC.getDefaultClass(), retval.getDefaultClass());
            for (QuerySelectable selectable : origC.getSelect()) {
                retval.addToSelect((QuerySelectable) cloneThing(selectable, subFromElementMap,
                        qopeMap));
            }
            retval.setConstraint((Constraint) cloneThing(origC.getConstraint(), subFromElementMap,
                    qopeMap));
            return retval;
        } else if (orig instanceof QueryCollectionPathExpression) {
            QueryCollectionPathExpression origC = (QueryCollectionPathExpression) orig;
            QueryCollectionPathExpression retval;
            try {
                if (origC.getSubclass() == null) {
                    retval = new QueryCollectionPathExpression((QueryClass) fromElementMap
                            .get(origC.getQueryClass()), origC.getFieldName());
                } else {
                    retval = new QueryCollectionPathExpression((QueryClass) fromElementMap
                            .get(origC.getQueryClass()), origC.getFieldName(), origC.getSubclass());
                }
            } catch (NullPointerException e) {
                throw new NullPointerException("oldQc: " + origC.getQueryClass()
                        + ", fromElementMap: " + fromElementMap);
            }
            retval.setSingleton(origC.isSingleton());
            Map<FromElement, FromElement> subFromElementMap =
                new HashMap<FromElement, FromElement>();
            for (FromElement origFrom : origC.getFrom()) {
                FromElement newFrom = null;
                if (origFrom instanceof QueryClass) {
                    newFrom = origFrom;
                } else if (origFrom instanceof Query) {
                    newFrom = cloneQuery((Query) origFrom);
                } else if (origFrom instanceof QueryClassBag) {
                    Collection<?> bag = ((QueryClassBag) origFrom).getBag();
                    Class<? extends InterMineObject> type = ((QueryClassBag) origFrom).getType();
                    if (bag == null) {
                        newFrom = new QueryClassBag(type, ((QueryClassBag) origFrom).getOsb());
                    } else {
                        newFrom = new QueryClassBag(type,
                                (Collection<?>) cloneThing(((QueryClassBag) origFrom).getBag(),
                                        null, qopeMap));
                    }
                } else {
                    throw new IllegalArgumentException("Unknown type of FromElement " + origFrom);
                }
                retval.addFrom(newFrom);
                subFromElementMap.put(origFrom, newFrom);
            }
            subFromElementMap.put(origC.getDefaultClass(), retval.getDefaultClass());
            for (QuerySelectable selectable : origC.getSelect()) {
                retval.addToSelect((QuerySelectable) cloneThing(selectable, subFromElementMap,
                        qopeMap));
            }
            retval.setConstraint((Constraint) cloneThing(origC.getConstraint(), subFromElementMap,
                    qopeMap));
            return retval;
        } else if (orig instanceof Constraint) {
            return cloneConstraint((Constraint) orig, fromElementMap, qopeMap);
        } else if (orig instanceof OverlapRange) {
            OverlapRange or = (OverlapRange) orig;
            return new OverlapRange((QueryEvaluable) cloneThing(or.getStart(), fromElementMap,
                    qopeMap), (QueryEvaluable) cloneThing(or.getEnd(), fromElementMap, qopeMap),
                    (QueryObjectReference) cloneThing(or.getParent(), fromElementMap, qopeMap));
        } else if (orig instanceof Set<?>) {
            return new HashSet<Object>((Set<?>) orig);
        } else if (orig instanceof List<?>) {
            return new ArrayList<Object>((List<?>) orig);
        } else if (orig instanceof ObjectStoreBag) {
            // Immutable
            return orig;
        } else if (orig instanceof ObjectStoreBagCombination) {
            ObjectStoreBagCombination origO = (ObjectStoreBagCombination) orig;
            ObjectStoreBagCombination retval = new ObjectStoreBagCombination(origO.getOp());
            retval.getBags().addAll(origO.getBags());
            return retval;
        } else if (orig instanceof ObjectStoreBagsForObject) {
            ObjectStoreBagsForObject origO = (ObjectStoreBagsForObject) orig;
            if (origO.getBags() == null) {
                return new ObjectStoreBagsForObject(origO.getValue());
            } else {
                return new ObjectStoreBagsForObject(origO.getValue(),
                        origO.getBags());
            }
        } else if (orig instanceof OrderDescending) {
            return new OrderDescending((QueryOrderable) cloneThing(((OrderDescending) orig)
                        .getQueryOrderable(), fromElementMap, qopeMap));
        }
        throw new IllegalArgumentException("Unknown object type: " + orig);
    }

    private static Constraint cloneConstraint(Constraint orig,
            Map<FromElement, FromElement> fromElementMap,
            Map<QueryObjectPathExpression, QueryObjectPathExpression> qopeMap)
        throws NoSuchFieldException {

        if (orig instanceof SimpleConstraint) {
            SimpleConstraint origC = (SimpleConstraint) orig;
            if ((origC.getOp() == ConstraintOp.IS_NULL)
                    || (origC.getOp() == ConstraintOp.IS_NOT_NULL)) {
                return new SimpleConstraint((QueryEvaluable) cloneThing(origC.getArg1(),
                        fromElementMap, qopeMap), origC.getOp());
            } else {
                return new SimpleConstraint((QueryEvaluable) cloneThing(origC.getArg1(),
                        fromElementMap, qopeMap), origC.getOp(),
                        (QueryEvaluable) cloneThing(origC.getArg2(), fromElementMap, qopeMap));
            }
        } else if (orig instanceof ConstraintSet) {
            ConstraintSet origC = (ConstraintSet) orig;
            ConstraintSet newC = new ConstraintSet(origC.getOp());
            for (Constraint con : origC.getConstraints()) {
                newC.addConstraint((Constraint) cloneThing(con, fromElementMap, qopeMap));
            }
            return newC;
        } else if (orig instanceof ContainsConstraint) {
            ContainsConstraint origC = (ContainsConstraint) orig;
            if (origC.getOp().equals(ConstraintOp.IS_NULL) || origC.getOp().equals(
                    ConstraintOp.IS_NOT_NULL)) {
                return new ContainsConstraint((QueryReference) cloneThing(
                        origC.getReference(), fromElementMap, qopeMap), origC.getOp());
            } else if (origC.getQueryClass() == null) {
                return new ContainsConstraint((QueryReference) cloneThing(origC.getReference(),
                        fromElementMap, qopeMap), origC.getOp(), origC.getObject());
            } else {
                return new ContainsConstraint((QueryReference) cloneThing(origC.getReference(),
                        fromElementMap, qopeMap), origC.getOp(),
                        (QueryClass) cloneThing(origC.getQueryClass(), fromElementMap, qopeMap));
            }
        } else if (orig instanceof ClassConstraint) {
            ClassConstraint origC = (ClassConstraint) orig;
            if (origC.getArg2QueryClass() == null) {
                return new ClassConstraint((QueryClass) fromElementMap.get(origC.getArg1()),
                        origC.getOp(), origC.getArg2Object());
            } else {
                return new ClassConstraint((QueryClass) fromElementMap.get(origC.getArg1()),
                        origC.getOp(),
                        (QueryClass) fromElementMap.get(origC.getArg2QueryClass()));
            }
        } else if (orig instanceof SubqueryConstraint) {
            SubqueryConstraint origC = (SubqueryConstraint) orig;
            if (origC.getQueryEvaluable() == null) {
                return new SubqueryConstraint((QueryClass)
                        fromElementMap.get(origC.getQueryClass()), origC.getOp(),
                        cloneQuery(origC.getQuery()));
            } else {
                return new SubqueryConstraint(
                        (QueryEvaluable) cloneThing(origC.getQueryEvaluable(), fromElementMap,
                                qopeMap), origC.getOp(), cloneQuery(origC.getQuery()));
            }
        } else if (orig instanceof BagConstraint) {
            BagConstraint origC = (BagConstraint) orig;
            Collection<?> bag = origC.getBag();
            if (bag instanceof Set<?>) {
                bag = new HashSet<Object>(bag);
            } else if (bag instanceof List<?>) {
                bag = new ArrayList<Object>(bag);
            }
            if (bag == null) {
                return new BagConstraint((QueryNode) cloneThing(origC.getQueryNode(),
                        fromElementMap, qopeMap), origC.getOp(), origC.getOsb());
            } else {
                return new BagConstraint((QueryNode) cloneThing(origC.getQueryNode(),
                        fromElementMap, qopeMap), origC.getOp(), bag);
            }
        } else if (orig instanceof MultipleInBagConstraint) {
            MultipleInBagConstraint origC = (MultipleInBagConstraint) orig;
            Collection<?> bag = origC.getBag();
            if (bag instanceof Set<?>) {
                bag = new HashSet<Object>(bag);
            } else if (bag instanceof List<?>) {
                bag = new ArrayList<Object>(bag);
            }
            @SuppressWarnings("unchecked") List<QueryEvaluable> evaluables = (List) cloneThing(origC
                    .getEvaluables(), fromElementMap, qopeMap);
            return new MultipleInBagConstraint(bag, evaluables);
        } else if (orig instanceof SubqueryExistsConstraint) {
            SubqueryExistsConstraint origC = (SubqueryExistsConstraint) orig;
            return new SubqueryExistsConstraint(origC.getOp(), cloneQuery(origC.getQuery()));
        } else if (orig instanceof OverlapConstraint) {
            OverlapConstraint oc = (OverlapConstraint) orig;
            return new OverlapConstraint((OverlapRange) cloneThing(oc.getLeft(), fromElementMap,
                    qopeMap), oc.getOp(), (OverlapRange) cloneThing(oc.getRight(), fromElementMap,
                            qopeMap));
        } else {
            throw new IllegalArgumentException("Unknown constraint type "
                    + orig.getClass().getName());
        }
    }
}
