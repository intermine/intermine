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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a static class that provides a method to clone a Query object.
 *
 * @author Matthew Wakeling
 */
public class QueryCloner
{
    /**
     * Clones a query object.
     *
     * @param query a Query to clone
     * @return a Query object not connected to the original, but identical
     */
    public static Query cloneQuery(Query query) {
        Query newQuery = new Query();
        try {
            Map aliases = query.getAliases();
            Map fromElementMap = new HashMap();
            Iterator fromIter = query.getFrom().iterator();
            while (fromIter.hasNext()) {
                FromElement origFrom = (FromElement) fromIter.next();
                FromElement newFrom = null;
                if (origFrom instanceof QueryClass) {
                    newFrom = origFrom;
                } else if (origFrom instanceof Query) {
                    newFrom = cloneQuery((Query) origFrom);
                } else if (origFrom instanceof QueryClassBag) {
                    Collection bag = ((QueryClassBag) origFrom).getBag();
                    Class type = ((QueryClassBag) origFrom).getType();
                    if (bag == null) {
                        newFrom = new QueryClassBag(type, ((QueryClassBag) origFrom).getOsb());
                    } else {
                        newFrom = new QueryClassBag(type,
                                (Collection) cloneThing(((QueryClassBag) origFrom).getBag(), null));
                    }
                } else {
                    throw new IllegalArgumentException("Unknown type of FromElement " + origFrom);
                }
                newQuery.addFrom(newFrom, (String) aliases.get(origFrom));
                fromElementMap.put(origFrom, newFrom);
            }
            Iterator selectIter = query.getSelect().iterator();
            while (selectIter.hasNext()) {
                QuerySelectable origSelect = (QuerySelectable) selectIter.next();
                QuerySelectable newSelect = (QuerySelectable) cloneThing(origSelect,
                        fromElementMap);
                newQuery.addToSelect(newSelect, (String) aliases.get(origSelect));
            }
            Iterator orderIter = query.getOrderBy().iterator();
            while (orderIter.hasNext()) {
                QueryOrderable origOrder = (QueryOrderable) orderIter.next();
                QueryOrderable newOrder = (QueryOrderable) cloneThing(origOrder, fromElementMap);
                newQuery.addToOrderBy(newOrder);
            }
            Iterator groupIter = query.getGroupBy().iterator();
            while (groupIter.hasNext()) {
                QueryNode origGroup = (QueryNode) groupIter.next();
                QueryNode newGroup = (QueryNode) cloneThing(origGroup, fromElementMap);
                newQuery.addToGroupBy(newGroup);
            }
            newQuery.setConstraint((Constraint) cloneThing(query.getConstraint(), fromElementMap));
            newQuery.setDistinct(query.isDistinct());
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("No such field: " + e.getMessage());
        }
        return newQuery;
    }

    private static Object cloneThing(Object orig, Map fromElementMap)
            throws NoSuchFieldException {
        if (orig == null) {
            return null;
        } else if (orig instanceof FromElement) {
            return fromElementMap.get(orig);
        } else if (orig instanceof QueryField) {
            QueryField origF = (QueryField) orig;
            return new QueryField((FromElement) fromElementMap.get(origF.getFromElement()),
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
                return new QueryFunction((QueryField) cloneThing(origF.getParam(), fromElementMap),
                        origF.getOperation());
            } else {
                return new QueryFunction((QueryExpression) cloneThing(origF.getParam(),
                            fromElementMap), origF.getOperation());
            }
        } else if (orig instanceof QueryExpression) {
            QueryExpression origE = (QueryExpression) orig;
            if ((origE.getOperation() == QueryExpression.SUBSTRING) && (origE.getArg3() != null)) {
                return new QueryExpression((QueryEvaluable)
                        cloneThing(origE.getArg1(), fromElementMap),
                        (QueryEvaluable) cloneThing(origE.getArg2(), fromElementMap),
                        (QueryEvaluable) cloneThing(origE.getArg3(), fromElementMap));
            } else if ((origE.getOperation() == QueryExpression.LOWER) || (origE.getOperation()
                    == QueryExpression.UPPER)) {
                return new QueryExpression(origE.getOperation(),
                        (QueryEvaluable) cloneThing(origE.getArg1(), fromElementMap));
            } else {
                return new QueryExpression((QueryEvaluable)
                        cloneThing(origE.getArg1(), fromElementMap),
                        origE.getOperation(),
                        (QueryEvaluable) cloneThing(origE.getArg2(), fromElementMap));
            }
        } else if (orig instanceof QueryCast) {
            return new QueryCast((QueryEvaluable) cloneThing(((QueryCast) orig).getValue(),
                        fromElementMap), ((QueryCast) orig).getType());
        } else if (orig instanceof QueryObjectPathExpression) {
            QueryObjectPathExpression origC = (QueryObjectPathExpression) orig;
            return new QueryObjectPathExpression((QueryClass) fromElementMap
                    .get(origC.getQueryClass()), origC.getFieldName());
        } else if (orig instanceof QueryFieldPathExpression) {
            QueryFieldPathExpression origC = (QueryFieldPathExpression) orig;
            return new QueryFieldPathExpression((QueryClass) fromElementMap
                    .get(origC.getQueryClass()), origC.getReferenceName(), origC.getFieldName(),
                    origC.getDefaultValue());
        } else if (orig instanceof SimpleConstraint) {
            SimpleConstraint origC = (SimpleConstraint) orig;
            if ((origC.getOp() == ConstraintOp.IS_NULL)
                    || (origC.getOp() == ConstraintOp.IS_NOT_NULL)) {
                return new SimpleConstraint((QueryEvaluable) cloneThing(origC.getArg1(),
                            fromElementMap), origC.getOp());
            } else {
                return new SimpleConstraint((QueryEvaluable) cloneThing(origC.getArg1(),
                            fromElementMap), origC.getOp(),
                        (QueryEvaluable) cloneThing(origC.getArg2(), fromElementMap));
            }
        } else if (orig instanceof ConstraintSet) {
            ConstraintSet origC = (ConstraintSet) orig;
            ConstraintSet newC = new ConstraintSet(origC.getOp());
            Iterator conIter = origC.getConstraints().iterator();
            while (conIter.hasNext()) {
                newC.addConstraint((Constraint) cloneThing(conIter.next(), fromElementMap));
            }
            return newC;
        } else if (orig instanceof ContainsConstraint) {
            ContainsConstraint origC = (ContainsConstraint) orig;
            if (origC.getOp().equals(ConstraintOp.IS_NULL) || origC.getOp().equals(
                        ConstraintOp.IS_NOT_NULL)) {
                return new ContainsConstraint((QueryObjectReference) cloneThing(
                            origC.getReference(), fromElementMap), origC.getOp());
            } else if (origC.getQueryClass() == null) {
                return new ContainsConstraint((QueryReference) cloneThing(origC.getReference(),
                            fromElementMap), origC.getOp(), origC.getObject());
            } else {
                return new ContainsConstraint((QueryReference) cloneThing(origC.getReference(),
                            fromElementMap), origC.getOp(),
                        (QueryClass) cloneThing(origC.getQueryClass(), fromElementMap));
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
                        (QueryEvaluable) cloneThing(origC.getQueryEvaluable(), fromElementMap),
                        origC.getOp(), cloneQuery(origC.getQuery()));
            }
        } else if (orig instanceof BagConstraint) {
            BagConstraint origC = (BagConstraint) orig;
            Collection bag = origC.getBag();
            if (bag instanceof Set) {
                bag = new HashSet(bag);
            } else if (bag instanceof List) {
                bag = new ArrayList(bag);
            }
            if (bag == null) {
                return new BagConstraint((QueryNode) cloneThing(origC.getQueryNode(),
                            fromElementMap), origC.getOp(), origC.getOsb());
            } else {
                return new BagConstraint((QueryNode) cloneThing(origC.getQueryNode(),
                            fromElementMap), origC.getOp(), bag);
            }
        } else if (orig instanceof SubqueryExistsConstraint) {
            SubqueryExistsConstraint origC = (SubqueryExistsConstraint) orig;
            return new SubqueryExistsConstraint(origC.getOp(), cloneQuery(origC.getQuery()));
        } else if (orig instanceof Set) {
            return new HashSet((Set) orig);
        } else if (orig instanceof List) {
            return new ArrayList((List) orig);
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
                        .getQueryOrderable(), fromElementMap));
        }
        throw new IllegalArgumentException("Unknown object type: " + orig);
    }
}
