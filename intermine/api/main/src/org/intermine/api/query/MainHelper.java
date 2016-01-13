package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.template.TemplateManager;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryPathExpression;
import org.intermine.objectstore.query.QueryPathExpressionWithSelect;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Queryable;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.WidthBucketFunction;
import org.intermine.pathquery.LogicExpression;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintIds;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintMultitype;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.PropertiesUtil;

/**
 * Helper methods for main controller and main action
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @author Matthew Wakeling
 */
public final class MainHelper
{
    private MainHelper() {
    }

    private static final Logger LOG = Logger.getLogger(MainHelper.class);

    private static final LookupTokeniser LOOKUP_TOKENISER = LookupTokeniser.getLookupTokeniser();

    /**
     * Converts a PathQuery object into an ObjectStore Query object, and optionally populates a Map
     * from String path in the PathQuery to the object in the Query that represents it.
     *
     * @param pathQuery the PathQuery
     * @param savedBags the current saved bags map (a Map from bag name to InterMineBag)
     * @param pathToQueryNode optional parameter which will be populated with entries, mapping from
     * String path in the pathQuery to objects in the result Query
     * @param bagQueryRunner a BagQueryRunner to use to perform LOOKUPs
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * returned
     * @return an ObjectStore Query object
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery pathQuery, Map<String, InterMineBag> savedBags,
            Map<String, QuerySelectable> pathToQueryNode, BagQueryRunner bagQueryRunner,
            Map<String, BagQueryResult> returnBagQueryResults) throws ObjectStoreException {
        synchronized (pathQuery) {
            List<String> problems = pathQuery.verifyQuery();
            if (!problems.isEmpty()) {
                throw new ObjectStoreException("PathQuery is invalid: " + problems);
            }
            Query q = new Query();
            try {
                makeQuery(q, pathQuery.getRootClass(), pathQuery, savedBags, pathToQueryNode,
                        bagQueryRunner, returnBagQueryResults);
            } catch (PathException e) {
                throw new Error("PathQuery is invalid, but was valid earlier", e);
            }
            return q;
        }
    }

    /**
     * Converts a PathQuery object into an ObjectStore Query object, and optionally populates a Map
     * from String path in the PathQuery to the object in the Query that represents it. This is the
     * recursive private method that performs the algorithm.
     *
     * @param q a Query, QueryObjectPathExpression, or QueryCollectionPathExpression, depending on
     * the level of recursion reached so far
     * @param root the path representing the level of recursion - we will process this outer join
     * group
     * @param query the PathQuery
     * @param savedBags the current saved bags map (a Map from bag name to InterMineBag)
     * @param pathToQueryNode optional parameter which will be populated with entries, mapping from
     * String path in the pathQuery to objects in the result Query
     * @param bagQueryRunner a BagQueryRunner to use to perform LOOKUPs
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * returned
     * @throws ObjectStoreException if something goes wrong
     */
    private static void makeQuery(Queryable q, String root, PathQuery query,
            Map<String, InterMineBag> savedBags, Map<String, QuerySelectable> pathToQueryNode,
            BagQueryRunner bagQueryRunner,
            Map<String, BagQueryResult> returnBagQueryResults) throws ObjectStoreException {
        PathQuery pathQuery = query;
        Model model = pathQuery.getModel();

        // We need to call getQueryToExecute() first.  For template queries this gets a query that
        // excludes any optional constraints that have been switched off.  A normal PathQuery is
        // unchanged.
        pathQuery = pathQuery.getQueryToExecute();
        try {
            // This is the root constraint set that will be set in the query
            ConstraintSet andCs = new ConstraintSet(ConstraintOp.AND);

            // This is the Map that stores what we will put in pathToQueryNode. Because we can't
            // trust what is in there already, we use a separate variable and copy across afterwards
            Map<String, QuerySelectable> queryBits = new HashMap<String, QuerySelectable>();

            // If we have recursed, and are operating on a PathExpression, then we need to extract
            // the default class which was set up in the parent group and add it to the queryBits
            if (q instanceof QueryObjectPathExpression) {
                queryBits.put(root, ((QueryObjectPathExpression) q).getDefaultClass());
            } else if (q instanceof QueryCollectionPathExpression) {
                queryBits.put(root, ((QueryCollectionPathExpression) q).getDefaultClass());
            }

            // This is a Map from main path to outer join group of all classes in the query
            Map<String, String> outerJoinGroups = pathQuery.getOuterJoinGroups();
            // This is the subclass map from the query, for creating Path objects
            Map<String, String> subclasses = pathQuery.getSubclasses();

            // Get the logic expression for the relevant outer join group, and the list of
            // relevant constraint codes
            Set<String> relevantCodes = pathQuery.getConstraintGroups().get(root);
            LogicExpression logic = pathQuery.getConstraintLogicForGroup(root);

            logic = handleNullOuterJoins(root, pathQuery, model, relevantCodes,
                    logic);

            // This is the set of loop constraints that participate in the class collapsing
            // mechanism. All others must have a ClassConstraint generated for them.
            Set<PathConstraintLoop> participatingLoops = findParticipatingLoops(logic, pathQuery
                    .getConstraints());
            // This is the map of EQUALS loop constraints, from the path that should be omitted
            // from the Query to the path that represents both paths.
            Map<String, String> loops = makeLoopsMap(participatingLoops);

            // Get any paths in the query that are constrained to be NULL/NOT NULL references or
            // collections AND don't appear in other constraints or the query view. These will only
            // be accessed in an EXISTS subquery and shouldn't be add to the FROM.
            Map<String, String> pathConstraintNullOnly =
                    getPathConstraintNulls(model, pathQuery, true);

            // Set up queue system. We don't know what order we want to process these entries in,
            // so a queue allows us to put one we can't process yet to the back of the queue to
            // process later
            LinkedList<String> queue = new LinkedList<String>();
            for (String path : outerJoinGroups.keySet()) {
                queue.addLast(path);
            }

            Map<String, String> deferralReasons = new HashMap<String, String>();
            int queueDeferred = 0;
            // This is a Map of PathExpression objects that have been created. They will be added to
            // the SELECT list later on, when we can determine the correct order in the SELECT list.
            Map<String, QueryPathExpressionWithSelect> pathExpressions
                = new HashMap<String, QueryPathExpressionWithSelect>();
            while (!queue.isEmpty()) {
                if (queueDeferred > queue.size() + 2) {
                    throw new IllegalArgumentException("Cannot handle entries in queue: " + queue
                            + ", reasons: " + deferralReasons + ", root = " + root);
                }
                String stringPath = queue.removeFirst();
                deferralReasons.remove(stringPath);
                Path path = new Path(model, stringPath, subclasses);
                String outerJoinGroup = outerJoinGroups.get(stringPath);
                if (path.isRootPath()) {
                    // This is the root path. Just add the QueryClass, no further action.
                    if (root.equals(outerJoinGroup)) {
                        // This class is relevant to this outer join group
                        QueryClass qc = new QueryClass(path.getEndType());
                        ((Query) q).addFrom(qc);
                        queryBits.put(stringPath, qc);
                    }
                } else if (stringPath.equals(root)) {
                    // We are on the root of an outer join. No action required
                } else {
                    String parent = path.getPrefix().getNoConstraintsString();
                    QueryClass parentQc = (QueryClass) ((queryBits.get(parent)
                                instanceof QueryClass) ? queryBits.get(parent) : null);
                    if (parentQc == null) {
                        if (root.equals(outerJoinGroups.get(parent))) {
                            // We cannot process this path yet. It depends on a parent that hasn't
                            // been processed yet. Put it to the back of the queue.
                            deferralReasons.put(stringPath, "Could not process path " + stringPath
                                    + " because its parent has not yet been processed");
                            queue.addLast(stringPath);
                            queueDeferred++;
                            continue;
                        }
                    } else {
                        if (root.equals(outerJoinGroup)) {
                            // This class is relevant to this outer join group
                            QueryClass qc;
                            if (loops.containsKey(stringPath)) {
                                // This path is looped on another path
                                qc = (QueryClass) queryBits.get(loops.get(stringPath));
                                if (qc == null) {
                                    deferralReasons.put(stringPath, "Could not process path "
                                            + stringPath + " because it is looped onto a class ("
                                            + loops.get(stringPath) + ") that has not been "
                                            + "processed yet");
                                    queue.addLast(stringPath);
                                    queueDeferred++;
                                    continue;
                                }
                            } else {
                                qc = new QueryClass(path.getEndType());
                                if (!pathConstraintNullOnly.containsKey(path.toString())) {
                                    if (q instanceof Query) {
                                        ((Query) q).addFrom(qc);
                                    } else {
                                        ((QueryCollectionPathExpression) q).addFrom(qc);
                                    }
                                }
                            }

                            // unless there is ONLY a null constraint on this ref/col path we need
                            // to add a contains constraint to make the join
                            if (!pathConstraintNullOnly.containsKey(stringPath)) {
                                if (path.endIsReference()) {
                                    andCs.addConstraint(new ContainsConstraint(
                                                new QueryObjectReference(parentQc,
                                                    path.getLastElement()), ConstraintOp.CONTAINS,
                                                qc));
                                } else {
                                    andCs.addConstraint(new ContainsConstraint(
                                                new QueryCollectionReference(parentQc,
                                                    path.getLastElement()), ConstraintOp.CONTAINS,
                                                qc));
                                }
                            }
                            queryBits.put(stringPath, qc);
                        } else {
                            // This is a path from another outer join group. We only need to act if
                            // the parent path is from this outer join group - in that case, we
                            // make a PathExpression and recurse
                            if (root.equals(outerJoinGroups.get(parent))) {
                                // We need to act. However, first we need to know whether to use a
                                // collection or reference path expression
                                boolean isCollection = path.endIsCollection();
                                // Even if this is false, we may still need to upgrade to collection
                                // if there are multiple paths in the outer join group
                                if (!isCollection) {
                                    int groupSize = 0;
                                    for (Map.Entry<String, String> entry
                                            : outerJoinGroups.entrySet()) {
                                        if (outerJoinGroup.equals(entry.getValue())) {
                                            groupSize++;
                                        }
                                    }
                                    if (groupSize > 1) {
                                        isCollection = true;
                                    }
                                }
                                if (isCollection) {
                                    QueryCollectionPathExpression qn
                                        = new QueryCollectionPathExpression(parentQc,
                                                path.getLastElement(), path.getEndType());
                                    makeQuery(qn, stringPath, pathQuery, savedBags,
                                            pathToQueryNode, bagQueryRunner, returnBagQueryResults);
                                    queryBits.put(stringPath, qn);
                                    pathExpressions.put(stringPath, qn);
                                } else {
                                    QueryObjectPathExpression qn
                                        = new QueryObjectPathExpression(parentQc,
                                                path.getLastElement(), path.getEndType());
                                    makeQuery(qn, stringPath, pathQuery, savedBags, pathToQueryNode,
                                            bagQueryRunner, returnBagQueryResults);
                                    queryBits.put(stringPath, qn);
                                    pathExpressions.put(stringPath, qn);
                                }
                            }
                        }
                    }
                }
                deferralReasons.remove(stringPath);
                queueDeferred = 0;
            }

            Map<String, Constraint> codeToConstraint = putConstraintsInMap(q,
                    savedBags, bagQueryRunner, returnBagQueryResults,
                    pathQuery, model, queryBits, subclasses, relevantCodes,
                    participatingLoops);

            // Use the constraint logic to create a ConstraintSet structure with the constraints
            // inserted into it
            createConstraintStructure(logic, andCs, codeToConstraint);

            setConstraints(q, andCs);

            List<QuerySelectable> select = generateSelectList(root, pathQuery, model, queryBits,
                    outerJoinGroups, subclasses, pathExpressions);
            copySelectList(q, select);
            generateOrderBy(q, pathQuery, model, queryBits, subclasses);
            if (pathToQueryNode != null) {
                pathToQueryNode.putAll(queryBits);
            }
        } catch (PathException e) {
            throw new ObjectStoreException("PathException while converting PathQuery to ObjectStore"
                    + " Query", e);
        }
    }

    private static LogicExpression handleNullOuterJoins(String root,
            PathQuery pathQuery, Model model, Set<String> relevantCodes,
            LogicExpression logicExpression) {
        LogicExpression logic = logicExpression;
        // This is complicated - for NULL/NOT NULL constraints on refs/cols that span an outer
        // join boundary we need the constraint to be on the left side of the boundary, i.e.
        // in the main part of the query rather than subquery on the select. We may need to
        // move a constraint code from another outer join group.
        // e.g. Company.departments IS_NOT_NULL and Company.departments is an outer join
        Map<String, String> nullRefColConstraints = getPathConstraintNulls(model, pathQuery,
                false);
        for (String constraintPath : nullRefColConstraints.keySet()) {
            OuterJoinStatus ojs = pathQuery.getOuterJoinStatus(constraintPath);
            if (ojs == OuterJoinStatus.OUTER) {
                // which side of outer join are we on?
                if (root.split("\\.").length < constraintPath.split("\\.").length) {
                    // we're on the left side of the outer join so we want to add this
                    // constraint to the relevant codes now
                    String code = nullRefColConstraints.get(constraintPath);
                    if (!relevantCodes.contains(code)) {
                        relevantCodes.add(code);
                        logic = addToConstraintLogic(logic, code);
                    }
                } else {
                    // we've recursed into an outer join so we don't want to process this
                    // constraint now, remove it if it's in the relevant codes
                    String code = nullRefColConstraints.get(constraintPath);
                    if (relevantCodes.contains(code)) {
                        relevantCodes.remove(code);
                        logic = removeFromConstraintLogic(logic, code);
                    }
                }
            }
        }
        return logic;
    }

    private static void setConstraints(Queryable q, ConstraintSet andCs) {
        if (!andCs.getConstraints().isEmpty()) {
            Constraint c = andCs;
            while ((c instanceof ConstraintSet)
                    && (((ConstraintSet) c).getConstraints().size() == 1)) {
                c = ((ConstraintSet) c).getConstraints().iterator().next();
            }
            q.setConstraint(c);
        }
    }

    private static void copySelectList(Queryable q, List<QuerySelectable> select) {
        // Copy select list into query:
        QueryClass defaultClass = null;
        if (q instanceof QueryObjectPathExpression) {
            defaultClass = ((QueryObjectPathExpression) q).getDefaultClass();
        }
        if ((select.size() == 1) && select.get(0).equals(defaultClass)) {
            // Don't add anything to the SELECT list - default is fine
        } else {
            for (QuerySelectable qs : select) {
                if (qs instanceof QueryObjectPathExpression) {
                    QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                    if (qope.getSelect().size() > 1) {
                        for (int i = 0; i < qope.getSelect().size(); i++) {
                            q.addToSelect(new PathExpressionField(qope, i));
                        }
                    } else {
                        q.addToSelect(qope);
                    }
                } else {
                    q.addToSelect(qs);
                }
            }
        }
    }

    private static void generateOrderBy(Queryable q, PathQuery pathQuery,
            Model model, Map<String, QuerySelectable> queryBits,
            Map<String, String> subclasses) throws PathException {
        if (q instanceof Query) {
            Query qu = (Query) q;
            for (OrderElement order : pathQuery.getOrderBy()) {
                QueryField qf = (QueryField) queryBits.get(order.getOrderPath());
                if (qf == null) {
                    Path path = new Path(model, order.getOrderPath(), subclasses);
                    QueryClass qc = (QueryClass) queryBits.get(path.getPrefix()
                            .getNoConstraintsString());
                    qf = new QueryField(qc, path.getLastElement());
                    queryBits.put(order.getOrderPath(), qf);
                }
                if ((!qu.getOrderBy().contains(qf)) && (!qu.getOrderBy()
                        .contains(new OrderDescending(qf)))) {
                    if (order.getDirection().equals(OrderDirection.DESC)) {
                        qu.addToOrderBy(new OrderDescending(qf));
                    } else {
                        qu.addToOrderBy(qf);
                    }
                }
            }
            for (String view : pathQuery.getView()) {
                QueryField qf = (QueryField) queryBits.get(view);
                if (qf != null) {
                    // If qf IS null, that means it is in another outer join group, as we have
                    // populated queryBits earlier with all view objects
                    if ((!qu.getOrderBy().contains(qf)) && (!qu.getOrderBy()
                            .contains(new OrderDescending(qf)))) {
                        qu.addToOrderBy(qf);
                    }
                }
            }
        }
    }

    // Generate the SELECT list
    private static List<QuerySelectable> generateSelectList(String root,
            PathQuery pathQuery, Model model,
            Map<String, QuerySelectable> queryBits,
            Map<String, String> outerJoinGroups,
            Map<String, String> subclasses,
            Map<String, QueryPathExpressionWithSelect> pathExpressions)
        throws PathException {

        HashSet<String> pathExpressionsDone = new HashSet<String>();
        List<QuerySelectable> select = new ArrayList<QuerySelectable>();
        for (String view : pathQuery.getView()) {
            Path path = new Path(model, view, subclasses);
            String parentPath = path.getPrefix().getNoConstraintsString();
            String outerJoinGroup = outerJoinGroups.get(parentPath);
            if (root.equals(outerJoinGroup)) {
                QueryClass qc = (QueryClass) queryBits.get(parentPath);
                QueryField qf = new QueryField(qc, path.getLastElement());
                queryBits.put(view, qf);
                if (!select.contains(qc)) {
                    select.add(qc);
                }
            } else {
                while ((!path.isRootPath())
                        && (!root.equals(outerJoinGroups.get(path.getPrefix()
                                    .getNoConstraintsString())))) {
                    path = path.getPrefix();
                }
                if (!path.isRootPath()) {
                    // We have found a path in the view that is a path expression we want to
                    // use
                    view = path.getNoConstraintsString();
                    if (!pathExpressionsDone.contains(view)) {
                        QueryPathExpressionWithSelect pe = pathExpressions.get(view);
                        QueryClass qc = pe.getQueryClass();
                        if (!select.contains(qc)) {
                            select.add(qc);
                        }
                        if (!select.contains(pe)) {
                            select.add(pe);
                        }
                    }
                }
            }
        }
        return select;
    }

    private static Map<String, Constraint> putConstraintsInMap(Queryable q,
            Map<String, InterMineBag> savedBags, BagQueryRunner bagQueryRunner,
            Map<String, BagQueryResult> returnBagQueryResults,
            PathQuery pathQuery, Model model,
            Map<String, QuerySelectable> queryBits,
            Map<String, String> subclasses, Set<String> relevantCodes,
            Set<PathConstraintLoop> participatingLoops) throws PathException,
            BagNotFound, ObjectStoreException {
        // For each of the relevant codes, produce a Constraint object, and put it in a Map.
        // Constraints that do not have a code (namely loop NOT EQUALS) can be put straight into
        // the andCs.
        Map<String, Constraint> codeToConstraint = new HashMap<String, Constraint>();
        for (Map.Entry<PathConstraint, String> entry : pathQuery.getConstraints().entrySet()) {
            String code = entry.getValue();
            if (relevantCodes.contains(code)) {
                PathConstraint constraint = entry.getKey();
                String stringPath = constraint.getPath();
                Path path = new Path(model, stringPath, subclasses);
                QuerySelectable field = queryBits.get(constraint.getPath());
                if (field == null) {
                    // This must be a constraint on an attribute, as all the classes will
                    // already be in querybits
                    QueryClass qc = (QueryClass) queryBits.get(path.getPrefix()
                            .getNoConstraintsString());
                    field = new QueryField(qc, path.getLastElement());
                    queryBits.put(stringPath, field);
                }
                if (constraint instanceof PathConstraintAttribute) {
                    PathConstraintAttribute pca = (PathConstraintAttribute) constraint;
                    Class<?> fieldType = path.getEndType();
                    if (String.class.equals(fieldType)) {
                        codeToConstraint.put(code, makeQueryStringConstraint(
                                    (QueryField) field, pca));
                    } else if (Date.class.equals(fieldType)) {
                        codeToConstraint.put(code, makeQueryDateConstraint(
                                    (QueryField) field, pca));
                    } else {
                        // Use simple forms of operators when not dealing with strings.
                        ConstraintOp simpleOp = ConstraintOp.EXACT_MATCH == pca.getOp()
                                ? ConstraintOp.EQUALS
                                        : ConstraintOp.STRICT_NOT_EQUALS == pca.getOp()
                                            ? ConstraintOp.NOT_EQUALS : pca.getOp();
                        codeToConstraint.put(code, new SimpleConstraint((QueryField) field,
                                simpleOp, new QueryValue(TypeUtil.stringToObject(
                                        fieldType, pca.getValue()))));
                    }

                } else if (constraint instanceof PathConstraintNull) {
                    if (path.endIsAttribute()) {
                        codeToConstraint.put(code, new SimpleConstraint((QueryField) field,
                                    constraint.getOp()));
                    } else {
                        String parent = path.getPrefix().getNoConstraintsString();
                        QueryClass parentQc = (QueryClass) ((queryBits.get(parent)
                                    instanceof QueryClass) ? queryBits.get(parent) : null);
                        if (path.endIsReference()) {
                            QueryObjectReference qr = new QueryObjectReference(parentQc,
                                    path.getLastElement());
                            codeToConstraint.put(code, new ContainsConstraint(qr,
                                    constraint.getOp()));
                        } else { // collection
                            QueryCollectionReference qr = new QueryCollectionReference(parentQc,
                                    path.getLastElement());
                            codeToConstraint.put(code, new ContainsConstraint(qr,
                                    constraint.getOp()));
                        }
                    }
                } else if (constraint instanceof PathConstraintLoop) {
                    // We need to act if this is not a participating constraint - otherwise
                    // this has been taken care of above.
                    if (!participatingLoops.contains(constraint)) {
                        PathConstraintLoop pcl = (PathConstraintLoop) constraint;
                        if (pcl.getPath().length() > pcl.getLoopPath().length()) {
                            codeToConstraint.put(code, new ClassConstraint((QueryClass)
                                        queryBits.get(pcl.getLoopPath()), constraint.getOp(),
                                        (QueryClass) field));
                        } else {
                            codeToConstraint.put(code, new ClassConstraint((QueryClass) field,
                                        constraint.getOp(), (QueryClass) queryBits
                                        .get(((PathConstraintLoop) constraint).getLoopPath())));
                        }
                    }
                } else if (constraint instanceof PathConstraintSubclass) {
                    // No action needed.
                } else if (constraint instanceof PathConstraintBag) {
                    PathConstraintBag pcb = (PathConstraintBag) constraint;
                    InterMineBag bag = savedBags.get(pcb.getBag());
                    if (bag == null) {
                        throw new BagNotFound(pcb.getBag());
                    }
                    codeToConstraint.put(code, new BagConstraint((QueryNode) field, pcb.getOp(),
                                bag.getOsb()));
                } else if (constraint instanceof PathConstraintIds) {
                    codeToConstraint.put(code, new BagConstraint(new QueryField(
                                    (QueryClass) field, "id"), constraint.getOp(),
                                ((PathConstraintIds) constraint).getIds()));
                } else if (constraint instanceof PathConstraintRange) {
                    PathConstraintRange pcr = (PathConstraintRange) constraint;
                    codeToConstraint.put(code, makeRangeConstraint(q, (QueryNode) field, pcr));
                } else if (constraint instanceof PathConstraintMultitype) {
                    PathConstraintMultitype pcmt = (PathConstraintMultitype) constraint;
                    codeToConstraint.put(code, makeMultiTypeConstraint(pathQuery.getModel(),
                            (QueryNode) field, pcmt));
                } else if (constraint instanceof PathConstraintMultiValue) {
                    Class<?> fieldType = path.getEndType();
                    if (String.class.equals(fieldType)) {
                        codeToConstraint.put(code, new BagConstraint((QueryField) field,
                                constraint.getOp(), ((PathConstraintMultiValue) constraint)
                                .getValues()));
                    } else {
                        Collection<Object> objects = new ArrayList<Object>();
                        for (String s : ((PathConstraintMultiValue) constraint).getValues()) {
                            objects.add(TypeUtil.stringToObject(fieldType, s));
                        }
                        codeToConstraint.put(code, new BagConstraint((QueryField) field,
                                constraint.getOp(), objects));
                    }
                } else if (constraint instanceof PathConstraintLookup) {
                    QueryClass qc = (QueryClass) field;
                    PathConstraintLookup pcl = (PathConstraintLookup) constraint;
                    if (bagQueryRunner == null) {
                        throw new NullPointerException("Cannot convert this PathQuery to an "
                                + "ObjectStore Query without a BagQueryRunner");
                    }
                    String identifiers = pcl.getValue();
                    BagQueryResult bagQueryResult;
                    List<String> identifierList = LOOKUP_TOKENISER.tokenise(identifiers);
                    try {
                        bagQueryResult = bagQueryRunner.searchForBag(qc.getType()
                                .getSimpleName(), identifierList, pcl.getExtraValue(), true);
                    } catch (ClassNotFoundException e) {
                        throw new ObjectStoreException(e);
                    } catch (InterMineException e) {
                        throw new ObjectStoreException(e);
                    }
                    codeToConstraint.put(code, new BagConstraint(new QueryField(qc, "id"),
                                ConstraintOp.IN, bagQueryResult.getMatchAndIssueIds()));
                    if (returnBagQueryResults != null) {
                        returnBagQueryResults.put(stringPath, bagQueryResult);
                    }
                } else {
                    throw new ObjectStoreException("Unknown constraint type "
                            + constraint.getClass().getName());
                }
            }
        }
        return codeToConstraint;
    }

    /**
     * Construct a new multi-type constraint.
     * @param model The model to look for types within.
     * @param field The subject of the constraint.
     * @param pcmt The constraint itself.
     * @return A constraint.
     * @throws ObjectStoreException if the constraint names types that are not in the model.
     */
    protected static Constraint makeMultiTypeConstraint(
            Model model,
            QueryNode field,
            PathConstraintMultitype pcmt) throws ObjectStoreException {
        QueryField typeClass = new QueryField((QueryClass) field, "class");
        ConstraintOp op = (pcmt.getOp() == ConstraintOp.ISA)
                ? ConstraintOp.IN : ConstraintOp.NOT_IN;
        Set<Class<?>> classes = new TreeSet<Class<?>>(new ClassNameComparator());
        for (String name: pcmt.getValues()) {
            ClassDescriptor cd = model.getClassDescriptorByName(name);
            if (cd == null) { // PathQueries should take care of this, but you know.
                throw new ObjectStoreException(
                        String.format("%s is not a class in the %s model", name, model.getName()));
            }
            classes.add(cd.getType());
        }

        return new BagConstraint(typeClass, op, classes);
    }

    private static Map<String, String> makeLoopsMap(Collection<PathConstraintLoop> constraints) {
        // A PathConstraintLoop should participate in this mechanism if it is an EQUALS constraint,
        // and its code is not inside an OR in the constraint logic.

        // Let's look at this from an equivalence groups point of view. We need to cope with the
        // situation where a = a.b.c and a.d = a.b.c, putting all three into an equivalence group.
        // The group name should be the shortest path in the group, or the lowest compareTo() for
        // a tie-break.
        Map<String, String> membership = new HashMap<String, String>();
        Map<String, Set<String>> groups = new HashMap<String, Set<String>>();
        for (PathConstraintLoop loop : constraints) {
            if (ConstraintOp.EQUALS.equals(loop.getOp())) {
                String path1 = loop.getPath();
                String path2 = loop.getLoopPath();
                if (membership.containsKey(path1)) {
                    if (membership.containsKey(path2)) {
                        String existingGroup1 = membership.get(path1);
                        String existingGroup2 = membership.get(path2);
                        if (!existingGroup1.equals(existingGroup2)) {
                            Set<String> members1 = groups.remove(existingGroup1);
                            Set<String> members2 = groups.remove(existingGroup2);
                            members1.addAll(members2);
                            String shorter = shorterPath(existingGroup1, existingGroup2);
                            for (String toAdd : members1) {
                                membership.put(toAdd, shorter);
                            }
                            groups.put(shorter, members1);
                        }
                    } else {
                        String existingGroup = membership.get(path1);
                        Set<String> members = groups.remove(existingGroup);
                        members.add(path2);
                        String shorter = shorterPath(path2, existingGroup);
                        for (String toAdd : members) {
                            membership.put(toAdd, shorter);
                        }
                        groups.put(shorter, members);
                    }
                } else {
                    if (membership.containsKey(path2)) {
                        String existingGroup = membership.get(path2);
                        Set<String> members = groups.remove(existingGroup);
                        members.add(path1);
                        String shorter = shorterPath(path1, existingGroup);
                        for (String toAdd : members) {
                            membership.put(toAdd, shorter);
                        }
                        groups.put(shorter, members);
                    } else {
                        String shorter = shorterPath(path1, path2);
                        membership.put(path2, shorter);
                        membership.put(path1, shorter);
                        groups.put(shorter, new HashSet<String>(Arrays.asList(path1, path2)));
                    }
                }
            }
        }
        Map<String, String> retval = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : membership.entrySet()) {
            if (!entry.getKey().equals(entry.getValue())) {
                retval.put(entry.getKey(), entry.getValue());
            }
        }
        return retval;
    }

    // find any reference or collection paths in query that have NULL/NOT NULL constraints.
    // If nullOnly then return those that have a NULL/NOT NULL constraint that otherwise don't
    // appear in the view or other constraints
    private static Map<String, String> getPathConstraintNulls(Model model, PathQuery pq,
            boolean nullOnly) {
        Map<String, String> nullRefsAndCols = new HashMap<String, String>();
        for (Map.Entry<PathConstraint, String> entry : pq.getConstraints().entrySet()) {
            PathConstraint constraint = entry.getKey();
            String code = entry.getValue();
            if (constraint instanceof PathConstraintNull) {
                try {
                    Path constraintPath = new Path(model, constraint.getPath());
                    if (constraintPath.endIsReference() || constraintPath.endIsCollection()) {
                        boolean isNullOnly = true;
                        // look for any view elements starting with this path
                        for (String viewPath : pq.getView()) {
                            if (viewPath.startsWith(constraintPath.toString())) {
                                isNullOnly = false;
                            }
                        }

                        // look for any other constraints starting with this path
                        for (PathConstraint otherCon : pq.getConstraints().keySet()) {
                            if (otherCon != constraint
                                    && otherCon.getPath().startsWith(constraintPath.toString())) {
                                isNullOnly = false;
                            }
                        }

                        // constraint path wasn't found elsewhere so it's a null collection only
                        if (nullOnly && isNullOnly) {
                            nullRefsAndCols.put(constraintPath.toString(), code);
                        } else if (!nullOnly) {
                            nullRefsAndCols.put(constraintPath.toString(), code);
                        }
                    }
                } catch (PathException e) {
                    // this shouldn't happen because the query is already verified
                    LOG.warn("Error finding paths constrainted to null only:" + e);
                }
            }
        }
        return nullRefsAndCols;
    }

    private static String shorterPath(String path1, String path2) {
        if (path1.length() > path2.length()) {
            return path2;
        } else if (path2.length() > path1.length()) {
            return path1;
        } else if (path1.compareTo(path2) > 0) {
            return path2;
        } else if (path1.compareTo(path2) < 0) {
            return path1;
        } else {
            throw new IllegalArgumentException("Two paths are identical: " + path1);
        }
    }

    /**
     * Returns the Set of PathConstraintLoop objects that will participate in the QueryClass
     * collapsing mechanism.
     *
     * @param logic the constraint logic
     * @param constraints a Map from PathConstraint to code
     * @return a Set of PathConstraintLoop objects
     */
    protected static Set<PathConstraintLoop> findParticipatingLoops(LogicExpression logic,
            Map<PathConstraint, String> constraints) {
        if (logic != null) {
            LogicExpression.Node node = logic.getRootNode();
            Set<String> codes = new HashSet<String>();
            findAndCodes(codes, node);
            Set<PathConstraintLoop> retval = new HashSet<PathConstraintLoop>();
            for (Map.Entry<PathConstraint, String> entry : constraints.entrySet()) {
                if (codes.contains(entry.getValue())) {
                    if (entry.getKey() instanceof PathConstraintLoop) {
                        if (ConstraintOp.EQUALS.equals(entry.getKey().getOp())) {
                            retval.add((PathConstraintLoop) entry.getKey());
                        }
                    }
                }
            }
            return retval;
        }
        return Collections.emptySet();
    }

    /**
     * Finds all the codes in a constraint logic that are ANDed in the given constraint logic.
     *
     * @param codes codes are added to this
     * @param node a node to traverse
     */
    protected static void findAndCodes(Set<String> codes, LogicExpression.Node node) {
        if (node instanceof LogicExpression.Variable) {
            codes.add(((LogicExpression.Variable) node).getName());
        } else if (node instanceof LogicExpression.And) {
            for (LogicExpression.Node child : ((LogicExpression.And) node).getChildren()) {
                findAndCodes(codes, child);
            }
        }
    }

    /**
     * Make a SimpleConstraint for the given constraint.  The Constraint will be
     * case-insensitive.  If the constraint value contains a wildcard and the operation is "=" or
     * "&lt;&gt;" then the operation will be changed to "LIKE" or "NOT_LIKE" as appropriate.
     */
    private static SimpleConstraint makeQueryStringConstraint(QueryField qf,
            PathConstraintAttribute c) {
        QueryEvaluable qe;
        String value;
        ConstraintOp op = c.getOp();

        // Perform case insensitive matches, unless asked specifically not to.
        if (ConstraintOp.EXACT_MATCH.equals(op) || ConstraintOp.STRICT_NOT_EQUALS.equals(op)) {
            qe = qf;
            value = c.getValue();
            op = (ConstraintOp.EXACT_MATCH.equals(op))
                    ? ConstraintOp.EQUALS: ConstraintOp.NOT_EQUALS;
        } else {
            qe = new QueryExpression(QueryExpression.LOWER, qf);
            value = Util.wildcardUserToSql(c.getValue().toLowerCase());
        }

        // notes:
        //   - we always turn EQUALS into a MATCHES(LIKE) constraint and rely on Postgres
        //     to be sensible
        //   - lowerCaseValue is quoted in a way suitable for a LIKE constraint, but not for an
        //     normal equals.  for example 'Dpse\GA10108' needs to be 'Dpse\\GA10108' for equals
        //     but 'Dpse\\\\GA10108' (and hence "Dpse\\\\\\\\GA10108" as a Java string because
        //     backslash must be quoted with a backslash)
        if (ConstraintOp.EQUALS.equals(op)) {
            return new SimpleConstraint(qe, ConstraintOp.MATCHES, new QueryValue(value));
        } else if (ConstraintOp.NOT_EQUALS.equals(op)) {
            return new SimpleConstraint(qe, ConstraintOp.DOES_NOT_MATCH, new QueryValue(value));
        } else if (ConstraintOp.CONTAINS.equals(op)) {
            return new SimpleConstraint(qe, ConstraintOp.MATCHES,
                    new QueryValue("%" + value + "%"));
        } else {
            return new SimpleConstraint(qe, op, new QueryValue(value));
        }
    }


    /**
     * Make a SimpleConstraint for the given Date Constraint.  The time stored in the Date will be
     * ignored.  Example webapp constraints and the coresponding object store constraints:
     * <table>
     *     <thead>
     *       <tr>
     *         <th>Webapp Version</th>
     *         <th>ObjectStore Version</th>
     *      </tr>
     *  </thead>
     *  <tbody>
     *    <tr>
     *      <td>
     *          <code>&lt;= 2008-01-02</code>
     *      </td>
     *      <td>
     *          <code>&gt;= 2008-01-02 23:59:59</code>
     *         </td>
     *     </tr>
     *     <tr>
     *      <td>
     *          <code>&gt; 2008-01-02</code>
     *      </td>
     *      <td>
     *          <code>&lt; 2008-01-02 00:00:00</code>
     *         </td>
     *     </tr>
     *     <tr>
     *      <td>
     *          <code>&gt; 2008-01-02</code>
     *      </td>
     *      <td>
     *          <code>&gt; 2008-01-02 23:59:59</code>
     *         </td>
     *     </tr>
     *     <tr>
     *      <td>
     *          <code>&gt;= 2008-01-02</code>
     *      </td>
     *      <td>
     *          <code>&gt; 2008-01-02 00:00:00</code>
     *         </td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * @param qf the QueryNode in the new query
     * @param c the webapp constraint
     * @return a new object store constraint
     */
    protected static Constraint makeQueryDateConstraint(QueryField qf, PathConstraintAttribute c) {
        Date dateValue = (Date) TypeUtil.stringToObject(Date.class, c.getValue());

        Calendar startOfDay = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        startOfDay.setTime(dateValue);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        QueryValue startOfDayQV = new QueryValue(startOfDay.getTime());

        Calendar endOfDay = (Calendar) startOfDay.clone();
        endOfDay.add(Calendar.DATE, 1);
        QueryValue endOfDayQV = new QueryValue(endOfDay.getTime());

        if (ConstraintOp.EXACT_MATCH.equals(c.getOp()) || ConstraintOp.EQUALS.equals(c.getOp())) {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.GREATER_THAN_EQUALS,
                        startOfDayQV));
            cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.LESS_THAN, endOfDayQV));
            return cs;
        } else if (ConstraintOp.NOT_EQUALS.equals(c.getOp())) {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
            cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.LESS_THAN, startOfDayQV));
            cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.GREATER_THAN_EQUALS,
                        endOfDayQV));
            return cs;
        } else if (ConstraintOp.LESS_THAN_EQUALS.equals(c.getOp())) {
            return new SimpleConstraint(qf, ConstraintOp.LESS_THAN, endOfDayQV);
        } else if (ConstraintOp.LESS_THAN.equals(c.getOp())) {
            return new SimpleConstraint(qf, ConstraintOp.LESS_THAN, startOfDayQV);
        } else if (ConstraintOp.GREATER_THAN.equals(c.getOp())) {
            return new SimpleConstraint(qf, ConstraintOp.GREATER_THAN_EQUALS, endOfDayQV);
        } else if (ConstraintOp.GREATER_THAN_EQUALS.equals(c.getOp())) {
            return new SimpleConstraint(qf, ConstraintOp.GREATER_THAN_EQUALS, startOfDayQV);
        } else {
            throw new RuntimeException("Unknown ConstraintOp: " + c);
        }
    }

    /**
     * Given a LogicExpression, a Map from codes to Constraint objects, and a ConstraintSet to put
     * it all in, construct a tree of ConstraintSets that reflects the expression.
     *
     * @param logic the LogicExpression object
     * @param cs the ConstraintSet to put the constraints in
     * @param codeToConstraint a Map from constraint code to Constraint object
     */
    protected static void createConstraintStructure(LogicExpression logic, ConstraintSet cs,
            Map<String, Constraint> codeToConstraint) {
        if (logic != null) {
            LogicExpression.Node node = logic.getRootNode();
            createConstraintStructure(node, cs, codeToConstraint);
        }
    }

    /**
     * Given a LogicExpression.Node, a Map from codes to Constraint objects, and a ConstraintSet to
     * put it all in, construct a tree of ConstraintSets that reflects the expression.
     *
     * @param node the LogicExpression.Node object
     * @param cs the ConstraintSet to put the constraints in
     * @param codeToConstraint a Map from constraint code to Constraint object
     */
    protected static void createConstraintStructure(LogicExpression.Node node, ConstraintSet cs,
            Map<String, Constraint> codeToConstraint) {
        if (node instanceof LogicExpression.Variable) {
            Constraint con = codeToConstraint.get(((LogicExpression.Variable) node).getName());
            if (con != null) {
                // If it is null, then it is probably a Loop constraint that participated in
                // QueryClass collapsing.
                cs.addConstraint(con);
            }
        } else {
            LogicExpression.Operator op = (LogicExpression.Operator) node;
            ConstraintSet set = null;
            if (op instanceof LogicExpression.And) {
                if (ConstraintOp.AND.equals(cs.getOp())) {
                    set = cs;
                } else {
                    set = new ConstraintSet(ConstraintOp.AND);
                }
            } else {
                if (ConstraintOp.OR.equals(cs.getOp())) {
                    set = cs;
                } else {
                    set = new ConstraintSet(ConstraintOp.OR);
                }
            }
            for (LogicExpression.Node child : op.getChildren()) {
                createConstraintStructure(child, set, codeToConstraint);
            }
            if (set != cs) {
                cs.addConstraint(set);
            }
        }
    }

    /**
     * Add a constraint code to a logic expression, ANDed with any constraints already in the
     * expression, e.g. 'A OR B' + code C -> '(A OR B) AND C'. If the expression is null a new
     * expression is created.
     * @param logic an existing constraint logic
     * @param code the code to add
     * @return a new logic expression including the new code
     */
    protected static LogicExpression addToConstraintLogic(LogicExpression logic, String code) {
        LogicExpression newLogic = logic;
        if (logic == null) {
            newLogic = new LogicExpression(code);
        } else {
            newLogic = new LogicExpression("(" + logic.toString() + ") AND " + code);
        }
        return newLogic;
    }

    /**
     * Remove a constraint code from a logic expression, e.g. '(A OR B) AND C' -> 'B AND C'. If
     * there is only one code in the expression return null.
     * @param logic an existing constraint logic
     * @param code the code to remove
     * @return a new logic expression or null if the expression is now empty
     */
    protected static LogicExpression removeFromConstraintLogic(LogicExpression logic,
            String code) {
        if (logic != null) {
            try {
                logic.removeVariable(code);
            } catch (IllegalArgumentException e) {
                // an IllegalArgumentException is thrown if we try to remove the root node, this
                // would make an empty expression so we can just set it to null
                return null;
            }
        }
        return logic;
    }

    /**
     * Generate a query from a PathQuery, to summarise a particular column of results.
     *
     * @param pathQuery the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode Map, into which columns to display will be placed
     * @param summaryPath a String path of the column to summarise
     * @param os an ObjectStore to do LOOKUP queries in
     * @param classKeys class key config
     * @param bagQueryConfig a BagQueryConfig object
     * @param pm the ProfileManager to fetch the superuser profile from
     * @param occurancesOnly Force summary to take form of item summary if true.
     * @return the generated summary query
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public static Query makeSummaryQuery(
            PathQuery pathQuery,
            Map<String, InterMineBag> savedBags,
            Map<String, QuerySelectable> pathToQueryNode,
            String summaryPath,
            ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig,
            ProfileManager pm,
            boolean occurancesOnly) throws ObjectStoreException {
        TemplateManager templateManager = new TemplateManager(pm.getSuperuserProfile(),
                os.getModel());
        BagQueryRunner bagQueryRunner = new BagQueryRunner(os, classKeys, bagQueryConfig,
                templateManager);
        return MainHelper.makeSummaryQuery(pathQuery, summaryPath, savedBags, pathToQueryNode,
                bagQueryRunner, occurancesOnly);
    }

    /**
     * Generate a query from a PathQuery, to summarise a particular column of results.
     *
     * @param pathQuery the PathQuery
     * @param summaryPath a String path of the column to summarise
     * @param savedBags the current saved bags map
     * @param pathToQueryNode Map, into which columns to display will be placed
     * @param bagQueryRunner a BagQueryRunner to execute bag queries
     * @return the generated summary query
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public static Query makeSummaryQuery(
            PathQuery pathQuery,
            String summaryPath,
            Map<String, InterMineBag> savedBags,
            Map<String, QuerySelectable> pathToQueryNode,
            BagQueryRunner bagQueryRunner) throws ObjectStoreException {
        return makeSummaryQuery(pathQuery, summaryPath, savedBags, pathToQueryNode,
                bagQueryRunner, false);
    }

    /**
     * Generate a query from a PathQuery, to summarise a particular column of results.
     *
     * @param pathQuery the PathQuery
     * @param summaryPath a String path of the column to summarise
     * @param savedBags the current saved bags map
     * @param pathToQueryNode Map, into which columns to display will be placed
     * @param bagQueryRunner a BagQueryRunner to execute bag queries
     * @param occurancesOnly Force summary to take form of item summary if true.
     * @return the generated summary query
     * @throws ObjectStoreException if there is a problem creating the query
     */
    public static Query makeSummaryQuery(
            PathQuery pathQuery,
            String summaryPath,
            Map<String, InterMineBag> savedBags,
            Map<String, QuerySelectable> pathToQueryNode,
            BagQueryRunner bagQueryRunner,
            boolean occurancesOnly) throws ObjectStoreException {
        Map<String, QuerySelectable> origPathToQueryNode = new HashMap<String, QuerySelectable>();
        Query subQ = null;
        subQ = makeQuery(pathQuery, savedBags, origPathToQueryNode, bagQueryRunner, null);
        subQ.clearOrderBy();
        Map<String, QuerySelectable> newSelect = new LinkedHashMap<String, QuerySelectable>();
        Set<QuerySelectable> oldSelect = new HashSet<QuerySelectable>();
        for (QuerySelectable qs : subQ.getSelect()) {
            oldSelect.add(qs);
            if (qs instanceof QueryClass) {
                newSelect.put(subQ.getAliases().get(qs), qs);
            } else if (!(qs instanceof QueryPathExpression)) {
                newSelect.put(subQ.getAliases().get(qs), qs);
            }
        }
        subQ.clearSelect();
        for (Map.Entry<String, QuerySelectable> selectEntry : newSelect.entrySet()) {
            subQ.addToSelect(selectEntry.getValue(), selectEntry.getKey());
        }
        return recursiveMakeSummaryQuery(origPathToQueryNode, summaryPath, subQ, oldSelect,
                pathToQueryNode, occurancesOnly);
    }

    private static Query recursiveMakeSummaryQuery(
            Map<String, QuerySelectable>
            origPathToQueryNode,
            String summaryPath,
            Query subQ, Set<QuerySelectable> oldSelect,
            Map<String, QuerySelectable> pathToQueryNode,
            boolean occurancesOnly) {
        QueryField qf = (QueryField) origPathToQueryNode.get(summaryPath);
        try {
            if ((qf == null) || (!subQ.getFrom().contains(qf.getFromElement()))) {
                // This column may be an outer join
                String prefix = summaryPath.substring(0, summaryPath.lastIndexOf('.'));
                String fieldName = summaryPath.substring(summaryPath.lastIndexOf('.') + 1);
                QuerySelectable qs = origPathToQueryNode.get(prefix);
                if (qs == null) {
                    throw new NullPointerException("Error - path " + summaryPath + " is not in map "
                            + origPathToQueryNode);
                } else if (qs instanceof QueryObjectPathExpression) {
                    QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                    if ((!oldSelect.contains(qs))
                            && (!oldSelect.contains(new PathExpressionField(qope, 0)))) {
                        throw new IllegalArgumentException("QueryObjectPathExpression is too deeply"
                                + " nested");
                    }
                    // We need to add QueryClasses to the query for this outer join. This will make
                    // it an inner join, so the "no object" results will disappear.
                    QueryClass lastQc = qope.getDefaultClass();
                    qf = new QueryField(lastQc, fieldName);
                    subQ.addFrom(lastQc);
                    subQ.addToSelect(lastQc);
                    QueryClass rootQc = qope.getQueryClass();
                    QueryHelper.addAndConstraint(subQ, new ContainsConstraint(
                                new QueryObjectReference(rootQc, qope.getFieldName()),
                                ConstraintOp.CONTAINS, lastQc));
                    if (qope.getConstraint() != null) {
                        QueryHelper.addAndConstraint(subQ, qope.getConstraint());
                    }
                } else if (qs instanceof QueryCollectionPathExpression) {
                    QueryCollectionPathExpression qcpe = (QueryCollectionPathExpression) qs;
                    //if (qcpe.getSelect().isEmpty() && qcpe.getFrom().isEmpty()
                    //        && oldSelect.contains(qcpe)) {
                    if (oldSelect.contains(qcpe)) {
                        QueryClass firstQc = qcpe.getDefaultClass();
                        qf = new QueryField(firstQc, fieldName);
                        subQ.addFrom(firstQc);
                        subQ.addToSelect(firstQc);
                        QueryClass rootQc = qcpe.getQueryClass();
                        try {
                            QueryHelper.addAndConstraint(subQ, new ContainsConstraint(
                                        new QueryCollectionReference(rootQc, qcpe.getFieldName()),
                                        ConstraintOp.CONTAINS, firstQc));
                        } catch (IllegalArgumentException e) {
                            QueryHelper.addAndConstraint(subQ, new ContainsConstraint(
                                        new QueryObjectReference(rootQc, qcpe.getFieldName()),
                                        ConstraintOp.CONTAINS, firstQc));
                        }
                        for (FromElement extraQc : qcpe.getFrom()) {
                            if (extraQc instanceof QueryClass) {
                                subQ.addFrom(extraQc);
                                subQ.addToSelect((QueryClass) extraQc);
                            } else {
                                throw new IllegalArgumentException("FromElement is not a "
                                        + "QueryClass: " + extraQc);
                            }
                        }
                        if (qcpe.getConstraint() != null) {
                            QueryHelper.addAndConstraint(subQ, qcpe.getConstraint());
                        }
                    } else {
                        throw new IllegalArgumentException("QueryCollectionPathExpression is too"
                                + " complicated to summarise");
                    }
                } else {
                    throw new IllegalArgumentException("Error - path " + prefix + " resolves to"
                            + " unknown object " + qs);
                }
            }
        } catch (IllegalArgumentException e) {
            for (QuerySelectable qs : oldSelect) {
                try {
                    if ((qs instanceof PathExpressionField)
                            && (((PathExpressionField) qs).getFieldNumber() == 0)) {
                        QueryObjectPathExpression qope = ((PathExpressionField) qs).getQope();
                        Query tempSubQ = QueryCloner.cloneQuery(subQ);
                        QueryClass lastQc = qope.getDefaultClass();
                        tempSubQ.addFrom(lastQc);
                        tempSubQ.addToSelect(lastQc);
                        QueryClass rootQc = qope.getQueryClass();
                        QueryHelper.addAndConstraint(tempSubQ, new ContainsConstraint(
                                    new QueryObjectReference(rootQc, qope.getFieldName()),
                                    ConstraintOp.CONTAINS, lastQc));
                        if (qope.getConstraint() != null) {
                            QueryHelper.addAndConstraint(tempSubQ, qope.getConstraint());
                        }
                        return recursiveMakeSummaryQuery(origPathToQueryNode, summaryPath, tempSubQ,
                                new HashSet<QuerySelectable>(qope.getSelect()), pathToQueryNode,
                                occurancesOnly);
                    } else if (qs instanceof QueryCollectionPathExpression) {
                        QueryCollectionPathExpression qcpe = (QueryCollectionPathExpression) qs;
                        QueryClass firstQc = qcpe.getDefaultClass();
                        Query tempSubQ = QueryCloner.cloneQuery(subQ);
                        tempSubQ.addFrom(firstQc);
                        tempSubQ.addToSelect(firstQc);
                        QueryClass rootQc = qcpe.getQueryClass();
                        try {
                            QueryHelper.addAndConstraint(tempSubQ, new ContainsConstraint(
                                        new QueryCollectionReference(rootQc, qcpe.getFieldName()),
                                        ConstraintOp.CONTAINS, firstQc));
                        } catch (IllegalArgumentException e2) {
                            QueryHelper.addAndConstraint(tempSubQ, new ContainsConstraint(
                                        new QueryObjectReference(rootQc, qcpe.getFieldName()),
                                        ConstraintOp.CONTAINS, firstQc));
                        }
                        for (FromElement extraQc : qcpe.getFrom()) {
                            if (extraQc instanceof QueryClass) {
                                tempSubQ.addFrom(extraQc);
                                tempSubQ.addToSelect((QueryClass) extraQc);
                            } else {
                                throw new IllegalArgumentException("FromElement is not a "
                                        + "QueryClass: " + extraQc);
                            }
                        }
                        if (qcpe.getConstraint() != null) {
                            QueryHelper.addAndConstraint(tempSubQ, qcpe.getConstraint());
                        }
                        return recursiveMakeSummaryQuery(origPathToQueryNode, summaryPath, tempSubQ,
                                new HashSet<QuerySelectable>(qcpe.getSelect()), pathToQueryNode,
                                    occurancesOnly);
                    }
                } catch (IllegalArgumentException e2) {
                    // Ignore it - we are searching for a working branch of the query
                }
            }
            throw new IllegalArgumentException(
                    "Cannot find path (" + summaryPath + ") in query", e);
        }

        Query q = new Query();
        q.addFrom(subQ);
        subQ.addToSelect(qf);
        qf = new QueryField(subQ, qf);
        Class<?> summaryType = qf.getType();

        QueryField origQf = (QueryField) origPathToQueryNode.get(summaryPath);
        String fieldName = origQf.getFieldName();
        String className = Util.getFriendlyName(((QueryClass) origQf.getFromElement())
                .getType());

        if (!occurancesOnly && isNumeric(summaryType)
                && (!SummaryConfig.summariseAsOccurrences(className + "." + fieldName))) {
            return getHistogram(subQ, qf, pathToQueryNode);
        } else if ((summaryType == String.class) || (summaryType == Boolean.class)
                || (summaryType == Long.class) || (summaryType == Integer.class)
                || (summaryType == Short.class) || (summaryType == Byte.class)
                || (summaryType == Float.class) || (summaryType == Double.class)
                || (summaryType == BigDecimal.class)) {
            q.addToSelect(qf);
            q.addToGroupBy(qf);
            QueryNode count = new QueryFunction();
            q.addToSelect(count);
            pathToQueryNode.put(summaryPath, qf);
            pathToQueryNode.put("Occurrences", count);
            q.addToOrderBy(new OrderDescending(count));
        } else {
            // Probably Date
            throw new IllegalArgumentException("Cannot summarise this column");
        }
        return q;
    }

    private static boolean isNumeric(Class<?> summaryType) {
        return (summaryType == Long.class) || (summaryType == Integer.class)
                || (summaryType == Short.class) || (summaryType == Byte.class)
                || (summaryType == Float.class) || (summaryType == Double.class)
                || (summaryType == BigDecimal.class);
    }

    /**
     * Produce a histogram query for a numerical column.
     *
     * In addition to the bucket number and the count for each bucket, each row also includes
     * the general statistics previously supplied for backwards compatibility.
     *
     * BASIC IDEA:
     * <pre>
     * select bq.max, bq.min, sum(bq.c) as total, bq.bucket, from (
     *     select count(*) as c,
     *            q1.value as val,
     *            width_bucket(q1.value, q2.min, (q2.max * 1.01), 10) as bucket,
     *            q2.max as max,
     *            q2.min as min
     *     from (select v.value from values as v) as vals,
     *          (select max(v.value) as max, min(v.value) as min from values as v) as stats
     *     group by vals.value, stats.min, stats.max order by bucket, vals.value
     * ) as bq
     * group by bq.bucket, bq.max, bq.min
     * order by bq.bucket;
     * </pre>
     *
     * @param subq The source of the data.
     * @param qf The field that contains the numerical information we are interested in.
     * @param pathToQueryNode The map to update with names of columns.
     * @return A query that when run will return a result set where each row has a bin number
     *         where 1 <= binNumber <= configuredMaxNoOfBins and a number of items in the data
     *         set that belong in the given bin.
     */
    private static Query getHistogram(
            Query source,
            QueryField qf,
            Map<String, QuerySelectable> pathToQueryNode) {

        // Inner 1
        Query vq = new Query();
        vq.addFrom(source);
        vq.addToSelect(qf);
        vq.setDistinct(false);

        // Inner 2
        Query statsq = new Query();
        statsq.addFrom(source);
        QueryFunction min = new QueryFunction(qf, QueryFunction.MIN);
        QueryFunction max = new QueryFunction(qf, QueryFunction.MAX);
        QueryFunction avg = new QueryFunction(qf, QueryFunction.AVERAGE);
        QueryFunction stddev = new QueryFunction(qf, QueryFunction.STDDEV);
        QueryEvaluable bins = new QueryValue(SummaryConfig.getNumberOfBins());

        Class<?> summaryType = qf.getType();
        if (summaryType == Long.class || summaryType == Integer.class) {
            bins = new QueryExpression(
                bins, QueryExpression.LEAST,
                new QueryExpression(max, QueryExpression.SUBTRACT, min)
            );
        }

        statsq.addToSelect(min);
        statsq.addToSelect(max);
        statsq.addToSelect(avg);
        statsq.addToSelect(stddev);
        statsq.addToSelect(bins);

        // Inner 3
        Query bucketq = new Query();
        bucketq.setDistinct(false);
        QueryFunction count = new QueryFunction();
        QueryField val = new QueryField(vq, qf);
        QueryField maxval = new QueryField(statsq, max);
        QueryField minval = new QueryField(statsq, min);
        QueryField meanval = new QueryField(statsq, avg);
        QueryField devval = new QueryField(statsq, stddev);
        QueryExpression upperBound = new QueryExpression(
                new QueryCast(maxval, BigDecimal.class),
                QueryExpression.MULTIPLY,
                new QueryCast(new QueryValue(new Double(1.01)), BigDecimal.class));
        QueryField noOfBuckets = new QueryField(statsq, bins);

        QueryFunction bucket = new WidthBucketFunction(val, minval, upperBound, noOfBuckets);
        bucketq.addFrom(vq);
        bucketq.addFrom(statsq);
        bucketq.addToSelect(count);
        bucketq.addToSelect(val);
        bucketq.addToSelect(maxval);
        bucketq.addToSelect(minval);
        bucketq.addToSelect(meanval);
        bucketq.addToSelect(devval);
        bucketq.addToSelect(bucket);
        bucketq.addToSelect(noOfBuckets);

        bucketq.addToGroupBy(val);
        bucketq.addToGroupBy(maxval);
        bucketq.addToGroupBy(minval);
        bucketq.addToGroupBy(meanval);
        bucketq.addToGroupBy(devval);
        bucketq.addToGroupBy(noOfBuckets);
        bucketq.addToOrderBy(bucket);
        bucketq.addToOrderBy(val);

        // Outer
        Query q = new Query();
        QueryField bmax = new QueryField(bucketq, maxval);
        QueryField bmin = new QueryField(bucketq, minval);
        QueryField bmean = new QueryField(bucketq, meanval);
        QueryField bdev = new QueryField(bucketq, devval);
        QueryField bbucket = new QueryField(bucketq, bucket);
        QueryFunction bucketTotal = new QueryFunction(
                new QueryField(bucketq, count), QueryFunction.SUM);
        QueryField buckets = new QueryField(bucketq, noOfBuckets);
        q.addFrom(bucketq);

        q.addToSelect(bmin);
        q.addToSelect(bmax);
        q.addToSelect(bmean);
        q.addToSelect(bdev);
        q.addToSelect(buckets);
        q.addToSelect(bbucket);
        q.addToSelect(bucketTotal);

        q.addToGroupBy(bmin);
        q.addToGroupBy(bmax);
        q.addToGroupBy(bmean);
        q.addToGroupBy(bdev);
        q.addToGroupBy(bbucket);
        q.addToGroupBy(buckets);

        q.addToOrderBy(bbucket);

        pathToQueryNode.put("Minimum", bmin);
        pathToQueryNode.put("Maximum", bmax);
        pathToQueryNode.put("Average", bmean);
        pathToQueryNode.put("Standard Deviation", bdev);
        pathToQueryNode.put("Buckets", bucketTotal);
        pathToQueryNode.put("Bucket", bbucket);
        pathToQueryNode.put("Occurances", bucketTotal);

        return q;
    }

    /**
     * @param props properties to configure the range queries
     */
    public static void loadHelpers(Properties props) {
        RangeConfig.loadHelpers(props);
    }

    // Allow collections with stable orderings by class name.
    private static final class ClassNameComparator implements Comparator<Class<?>>
    {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    /**
     * @author Alex
     */
    protected static final class RangeConfig
    {
        private RangeConfig() {
            // Restricted constructor.
        }

        protected static Map<Class<?>, RangeHelper> rangeHelpers;

        static {
            init();
        }

        /**
         * reset
         */
        protected static void reset() {
            init();
        }

        private static void init() {
            rangeHelpers = new HashMap<Class<?>, RangeHelper>();
            // Default basic helpers.
//            rangeHelpers.put(int.class, new IntHelper());
//            rangeHelpers.put(Integer.class, new IntHelper());
//            rangeHelpers.put(String.class, new StringHelper());
            loadHelpers(PropertiesUtil.getProperties());
        }

        /**
         * @param allProps all properties
         */
        @SuppressWarnings("unchecked")
        protected static void loadHelpers(Properties allProps) {
            Properties props = PropertiesUtil.getPropertiesStartingWith("pathquery.range.",
                    allProps);
            for (String key: props.stringPropertyNames()) {
                String[] parts = key.split("\\.", 3);
                if (parts.length != 3) {
                    throw new IllegalStateException(
                        "Property names must be in the format "
                            + "pathquery.range.${FullyQualifiedClassName}, got '" + key + "'"
                    );
                }
                String targetTypeName = parts[2];
                Class<?> targetType;
                try {
                    targetType = Class.forName(targetTypeName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Cannot find class named in config: '" + key
                            + "'", e);
                }
                String helperName = props.getProperty(key);
                Class<RangeHelper> helperType;
                try {
                    helperType = (Class<RangeHelper>) Class.forName(helperName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Cannot find class named in congfig: '" + helperName
                            + "'");
                }
                RangeHelper helper;
                try {
                    helper = helperType.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Could not instantiate range helper for '" + key
                            + "'", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not instantiate range helper for '" + key
                            + "'", e);
                }
                rangeHelpers.put(targetType, helper);
                LOG.info("ADDED RANGE HELPER FOR " + targetType + " (" + helperType.getName()
                        + ")");
            }
        }

        /**
         * @param type class
         * @return true if there is helper for this class of object
         */
        public static boolean hasHelperForType(Class<?> type) {
            return rangeHelpers.containsKey(type);
        }

        /**
         * @param type type
         * @return helper for given type
         */
        public static RangeHelper getHelper(Class<?> type) {
            return rangeHelpers.get(type);
        }
    }

    /**
     * @return set of classes that are legal to use with range constraints
     */
    public static Set<Class<?>> getValidRangeTargets() {
        return RangeConfig.rangeHelpers.keySet();
    }

    /**
     * Controls access to configuration information on which fields should be summarised as a count
     * of occurrences.
     *
     * @author Matthew Wakeling
     */
    protected static final class SummaryConfig
    {
        private SummaryConfig() {
        }

        private static Set<String> config;

        static {
            config = new HashSet<String>();
            String stringConfig = PropertiesUtil.getProperties()
                .getProperty("querySummary.summariseAsOccurrences");
            if (stringConfig != null) {
                String[] stringConfigs = stringConfig.split(",");
                for (String configEntry : stringConfigs) {
                    configEntry = configEntry.trim();
                    if (configEntry.contains(" ")) {
                        throw new IllegalArgumentException("querySummary.summariseAsOccurrences "
                                + "property contains an entry with a space: \"" + configEntry
                                + "\". Entries should be comma-separated.");
                    }
                    config.add(configEntry);
                }
            }
        }

        /**
         * Returns whether the given field name is configured to be summarised as a count of
         * occurrences.
         *
         * @param fieldName a class name, a dot, and a field name
         * @return true if the field should be summarised as a count of occurrences, false for
         * a mean and standard deviation.
         */
        public static boolean summariseAsOccurrences(String fieldName) {
            return config.contains(fieldName);
        }

        /**
         * Returns the number of bins to split a histogram into.
         * @return The number of bins.
         */
        public static Integer getNumberOfBins() {
            return Integer.valueOf(
                    PropertiesUtil.getProperties().getProperty("querySummary.no-of-bins", "20"));
        }
    }

    /**
     * @param q field
     * @param node class
     * @param con contraint
     * @return range constraint
     */
    public static Constraint makeRangeConstraint(
            Queryable q,
            QueryNode node,
            PathConstraintRange con) {
        Class<?> type = node.getType();

        if (RangeConfig.hasHelperForType(type)) {
            RangeHelper helper = RangeConfig.getHelper(type);

            return helper.createConstraint(q, node, con);
        }
        throw new RuntimeException("No range constraints are possible for paths of type "
                + type.getName());
    }

}
