package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.PathExpressionField;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
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
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Queryable;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.path.Path;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.LogicExpression;
import org.intermine.pathquery.MetadataNode;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.Util;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PagedTable;

/**
 * Helper methods for main controller and main action
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @author Matthew Wakeling
 */
public class MainHelper
{
    private static final Logger LOG = Logger.getLogger(MainHelper.class);

    /**
     * Given a path, render a set of metadata Nodes to the relevant depth
     * @param path of form Gene.organism.name
     * @param model the model used to resolve class names
     * @param isSuperUser true if the user is the superuser
     * @return an ordered Set of nodes
     */
    public static Collection<MetadataNode> makeNodes(String path, Model model,
                                                     boolean isSuperUser) {
        String className, subPath;
        if (path.indexOf(".") == -1) {
            className = path;
            subPath = "";
        } else {
            className = path.substring(0, path.indexOf("."));
            subPath = path.substring(path.indexOf(".") + 1);
        }
        Map<String, MetadataNode> nodes = new LinkedHashMap<String, MetadataNode>();
        nodes.put(className, new MetadataNode(className));
        try {
            makeNodes(getClassDescriptor(className, model), subPath, className, nodes, isSuperUser);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("class not found in the model", e);
        }
        return nodes.values();
    }

    /**
     * Recursive method used to add nodes to a set representing a path from a given ClassDescriptor
     * @param cld the root ClassDescriptor
     * @param path current path prefix (eg Gene)
     * @param currentPath current path suffix (eg organism.name)
     * @param nodes the current Node set
     * @param isSuperUser true if the user is the superuser
     * @throws ClassNotFoundException if a class name isn't in the model
     */
    protected static void makeNodes(ClassDescriptor cld, String path, String currentPath,
                                    Map<String, MetadataNode> nodes, boolean isSuperUser)
        throws ClassNotFoundException {
        List<FieldDescriptor> sortedNodes = new ArrayList<FieldDescriptor>();

        // compare FieldDescriptors by name
        Comparator<FieldDescriptor> comparator = new Comparator<FieldDescriptor>() {
            public int compare(FieldDescriptor o1, FieldDescriptor o2) {
                String fieldName1 = o1.getName().toLowerCase();
                String fieldName2 = o2.getName().toLowerCase();
                return fieldName1.compareTo(fieldName2);
            }
        };

        Set<FieldDescriptor> attributeNodes = new TreeSet<FieldDescriptor>(comparator);
        Set<FieldDescriptor> referenceAndCollectionNodes = new TreeSet<FieldDescriptor>(comparator);
        for (Iterator<FieldDescriptor> i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            FieldDescriptor fd = i.next();
            if (!fd.isReference() && !fd.isCollection()) {
                attributeNodes.add(fd);
            } else {
                referenceAndCollectionNodes.add(fd);
            }
        }

        sortedNodes.addAll(attributeNodes);
        sortedNodes.addAll(referenceAndCollectionNodes);

        for (Iterator<FieldDescriptor> i = sortedNodes.iterator(); i.hasNext();) {
            FieldDescriptor fd = i.next();
            String fieldName = fd.getName();

            if (fieldName.equals("id") && !isSuperUser) {
                continue;
            }

            String head, tail;
            if (path.indexOf(".") != -1) {
                head = path.substring(0, path.indexOf("."));
                tail = path.substring(path.indexOf(".") + 1);
            } else {
                head = path;
                tail = "";
            }

            String button;
            if (fieldName.equals(head)) {
                button = "-";
            } else if (fd.isReference() || fd.isCollection()) {
                button = "+";
            } else {
                button = " ";
            }

            MetadataNode parent = nodes.get(currentPath);
            MetadataNode node = new MetadataNode(parent, fieldName, button);
            node.setModel(cld.getModel());

            nodes.put(node.getPathString(), node);
            if (fieldName.equals(head)) {
                ClassDescriptor refCld = ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
                makeNodes(refCld, tail, currentPath + "." + head, nodes, isSuperUser);
            }
        }
    }

    /**
     * Make an InterMine query from a path query
     * @param query the PathQuery
     * @param savedBags the current saved bags map
     * @param servletContext the current servlet context
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * returned
     * @param pathToQueryNode optional parameter in which path to QueryNode map can be returned
     * @return an InterMine Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery query, Map savedBags,
            Map<String, QuerySelectable> pathToQueryNode, ServletContext servletContext,
            Map returnBagQueryResults) throws ObjectStoreException {
        return makeQuery(query, savedBags, pathToQueryNode, servletContext, returnBagQueryResults,
                         false,
                (ObjectStore) (servletContext == null ? null
                    : servletContext.getAttribute(Constants.OBJECTSTORE)),
                (Map) (servletContext == null ? null
                    : servletContext.getAttribute(Constants.CLASS_KEYS)),
                (BagQueryConfig) (servletContext == null ? null
                    : servletContext.getAttribute(Constants.BAG_QUERY_CONFIG)));
    }

    /**
     * Make an InterMine query from a path query
     * @param query the PathQuery
     * @param savedBags the current saved bags map
     * @param servletContext the current servlet context
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * returned
     * @return an InterMine Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery query, Map savedBags, ServletContext servletContext,
            Map returnBagQueryResults) throws ObjectStoreException {
        return makeQuery(query, savedBags, null, servletContext, returnBagQueryResults, false,
                (ObjectStore) (servletContext == null ? null
                    : servletContext.getAttribute(Constants.OBJECTSTORE)),
                (Map) (servletContext == null ? null
                    : servletContext.getAttribute(Constants.CLASS_KEYS)),
                (BagQueryConfig) (servletContext == null ? null
                    : servletContext.getAttribute(Constants.BAG_QUERY_CONFIG)));
    }
    /**
     * Make an InterMine query from a path query
     * @param pathQueryOrig the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode optional parameter in which path to QueryNode map can be returned
     * @param servletContext the current servlet context
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * @param checkOnly we're only checking the validity of the query, optimised to take less time
     * returned
     * @param os the ObjectStore that this will be run on
     * @param classKeys the class keys
     * @param bagQueryConfig the BagQueryConfig
     * @return an InterMine Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery pathQueryOrig, Map savedBags,
            Map<String, QuerySelectable> pathToQueryNode, ServletContext servletContext,
            Map returnBagQueryResults, boolean checkOnly, ObjectStore os,
            Map classKeys, BagQueryConfig bagQueryConfig) throws ObjectStoreException {
        BagQueryRunner bagQueryRunner = null;
        if (os != null) {
            bagQueryRunner = new BagQueryRunner(os, classKeys,
                    bagQueryConfig, servletContext);
        }
        return makeQuery(pathQueryOrig, savedBags, pathToQueryNode, bagQueryRunner,
                returnBagQueryResults, checkOnly);
    }

    /**
     * Validates path query. Any error message is set to path query.
     * @param pathQuery path query
     * @param savedBags saved bags
     */
    public static void checkPathQuery(PathQuery pathQuery, Map<String, InterMineBag> savedBags) {
        try {
            makeQuery(pathQuery, savedBags, null, null, null, true);
        } catch (Exception e) {
            pathQuery.addProblem(e);
        }
    }

    /**
     * Validates path queries. Any error message is set to path query.
     * @param queries path queries
     * @param savedBags saved bags
     */
    public static void checkPathQueries(Map<String, PathQuery> queries,
            Map<String, InterMineBag> savedBags) {
        for (PathQuery pathQuery : queries.values()) {
            checkPathQuery(pathQuery, savedBags);
        }
    }


    /**
     * Other version of makeQuery.
     * @param pathQueryOrig the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode optional parameter in which path to QueryNode map can be returned
     * @param returnBagQueryResults optional parameter in which any BagQueryResult objects can be
     * @param checkOnly we're only checking the validity of the query, optimised to take less time
     * returned
     * @param bagQueryRunner bag query runner
     * @return an InterMine Query
     * @throws ObjectStoreException if something goes wrong
     */
    public static Query makeQuery(PathQuery pathQueryOrig, Map savedBags,
            Map<String, QuerySelectable> pathToQueryNode, BagQueryRunner bagQueryRunner,
            Map returnBagQueryResults, boolean checkOnly) throws ObjectStoreException {
        PathQuery pathQuery = pathQueryOrig.clone();

        //first merge the query and the view
        Map qNodes = pathQuery.getNodes();
        for (Path p : pathQuery.getView()) {
            String path = p.toStringNoConstraints();
            if (!qNodes.containsKey(path)) {
                pathQuery.addNode(path);
            }
        }

        //create the real query
        Query q = new Query();

        recursiveMakeQuery(q, pathQuery, null, savedBags, pathToQueryNode, bagQueryRunner,
                returnBagQueryResults, checkOnly);
        return q;
    }

    private static void recursiveMakeQuery(Queryable q, PathQuery pathQuery, PathNode root,
            Map savedBags, Map<String, QuerySelectable> pathToQueryNode,
            BagQueryRunner bagQueryRunner, Map returnBagQueryResults, boolean checkOnly)
    throws ObjectStoreException {
        Model model = pathQuery.getModel();
        Map<String, ConstraintSet> codeToCS = new HashMap<String, ConstraintSet>();
        ConstraintSet rootcs = null;
        ConstraintSet andcs = new ConstraintSet(ConstraintOp.AND);

        if (pathQuery.getAllConstraints().size() == 1) {
            Constraint c = pathQuery.getAllConstraints().get(0);
            codeToCS.put(c.getCode(), andcs);
        } else if (pathQuery.getAllConstraints().size() > 1) {
            rootcs = makeConstraintSets(pathQuery.getLogic(), codeToCS, andcs);
        }
        q.setConstraint(andcs);
        if ((rootcs != null) && (rootcs != andcs)) {
            andcs.addConstraint(rootcs);
        }
        // Work out which bits of the query are not outer joins - we construct the query with only
        // those nodes to begin with.

        Set<PathNode> nonOuterNodes = findNonOuterNodes(pathQuery.getNodes(), root);

        Map<String, String> loops = makeLoopsMap(pathQuery, codeToCS, andcs, true, nonOuterNodes,
                root);

        Map<String, QuerySelectable> queryBits = new HashMap();
        if (q instanceof QueryObjectPathExpression) {
            queryBits.put(root.getPathString(), ((QueryObjectPathExpression) q).getDefaultClass());
        } else if (q instanceof QueryCollectionPathExpression) {
            queryBits.put(root.getPathString(), ((QueryCollectionPathExpression) q)
                    .getDefaultClass());
        }
        LinkedList<PathNode> queue = new LinkedList();

        //build the FROM and WHERE clauses
        for (Iterator i = pathQuery.getNodes().values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            queue.addLast(node);
        }

        Map<PathNode, String> deferralReasons = new HashMap<PathNode, String>();
        int queueDeferred = 0;
        while (!queue.isEmpty()) {
            if (queueDeferred > queue.size() + 10) {
                throw new IllegalArgumentException("Cannot handle entries in queue: " + queue
                        + ", reasons: " + deferralReasons);
            }
            PathNode node = queue.removeFirst();
            String path = node.getPathString();
            if (nonOuterNodes.contains(node)) {
                QueryReference qr = null;
                String finalPath = loops.get(path);

                if (finalPath == null) {
                    if (path.indexOf(".") == -1) {
                        QueryClass qc;
                        try {
                            qc = new QueryClass(TypeUtil.getClass(node.getType(), model));
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("class not found in the model: "
                                                               + node.getType(), e);
                        }
                        ((Query) q).addFrom(qc);
                        queryBits.put(path, qc);
                    } else {
                        String fieldName = node.getFieldName();
                        QueryClass parentQc = (QueryClass) queryBits.get(node.getPrefix());
                        if (parentQc == null) {
                            // We cannot process this QueryField yet. It depends on a parent
                            // QueryClass that we have not yet processed. Put it to the back of the
                            // queue.
                            deferralReasons.put(node, "Could not process QueryField " + node
                                    + " because its parent has not been processed");
                            queue.addLast(node);
                            queueDeferred++;
                            continue;
                        }

                        if (node.isAttribute()) {
                            QueryField qf = new QueryField(parentQc, fieldName);
                            queryBits.put(path, qf);
                        } else {
                            if (node.isReference()) {
                                qr = new QueryObjectReference(parentQc, fieldName);
                            } else {
                                qr = new QueryCollectionReference(parentQc, fieldName);
                            }
                            QueryClass qc;
                            try {
                                qc = new QueryClass(TypeUtil.getClass(node.getType(), model));
                            } catch (ClassNotFoundException e) {
                                throw new IllegalArgumentException("class not found in the model: "
                                                                   + node.getType(), e);
                            }
                            andcs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS,
                                        qc));
                            if (q instanceof Query) {
                                ((Query) q).addFrom(qc);
                            } else {
                                ((QueryCollectionPathExpression) q).addFrom(qc);
                            }
                            queryBits.put(path, qc);
                        }
                    }
                    finalPath = path;
                } else {
                    if (queryBits.get(finalPath) == null) {
                            //|| queryBits.get(node.getPrefix()) == null) {
                        // We cannot process this node yet. It is looped onto another node that has
                        // not been processed yet or the parent of this node hasn't yet been
                        // processed. Put it to the back of the queue.
                        deferralReasons.put(node, "Could not process node " + node + " because it"
                                + " is looped onto " + finalPath
                                + " which has not been processed yet");
                        queue.addLast(node);
                        queueDeferred++;
                        continue;
                    }
                    // TODO: Why? if (finalPath.indexOf(".") != -1) {
                        String fieldName = node.getFieldName();
                        QueryClass parentQc = (QueryClass) queryBits.get(node.getPrefix());
                        if (!node.isAttribute()) {
                            if (node.isReference()) {
                                qr = new QueryObjectReference(parentQc, fieldName);
                            } else {
                                qr = new QueryCollectionReference(parentQc, fieldName);
                            }
                            QueryClass qc = (QueryClass) queryBits.get(finalPath);
                            andcs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS,
                                        qc));
                        }
                    //}
                    queryBits.put(path, queryBits.get(finalPath));
                }

                QueryNode qn = (QueryNode) queryBits.get(finalPath);
                for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                    Constraint c = (Constraint) j.next();
                    String code = c.getCode();
                    ConstraintSet cs = codeToCS.get(code);
                    if (BagConstraint.VALID_OPS.contains(c.getOp())) {
                        QueryField qf = new QueryField((QueryClass) qn, "id");
                        if (c.getValue() instanceof InterMineBag) {
                            cs.addConstraint(new BagConstraint(qf, c.getOp(),
                                        ((InterMineBag) c.getValue()).getOsb()));
                        } else if (c.getValue() instanceof Collection) {
                            Collection idBag = new LinkedHashSet();
                            for (InterMineObject imo : ((Iterable<InterMineObject>) c.getValue())) {
                                idBag.add(imo.getId());
                            }
                            cs.addConstraint(new BagConstraint(qf, c.getOp(), idBag));
                        } else {
                            InterMineBag bag = (InterMineBag) savedBags.get(c.getValue());
                            if (bag == null) {
                                throw new RuntimeException("a bag (" + c.getValue()
                                        + ") used by this query no longer exists");
                            }
                            cs.addConstraint(new BagConstraint(qf, c.getOp(), bag.getOsb()));
                        }
                    } else if (node.isAttribute()) { //assume, for now, that it's a SimpleConstraint
                        cs.addConstraint(makeAttributeConstraint(qn, c));
                    } else if (node.isReference() && (c.getOp() == ConstraintOp.IS_NOT_NULL
                                || c.getOp() == ConstraintOp.IS_NULL)) {
                        cs.addConstraint(new ContainsConstraint((QueryObjectReference) qr,
                                    c.getOp()));
                    } else if (c.getOp() == ConstraintOp.LOOKUP) {
                        QueryClass qc = (QueryClass) qn;
                        if (checkOnly) {
                            try {
                                Class.forName(qc.getType().getName());
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                            continue;
                        }
                        String identifiers = (String) c.getValue();
                        BagQueryResult bagQueryResult;
                        List identifierList = new ArrayList();
                        StringTokenizer st = new StringTokenizer(identifiers, "\n\t,");
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            identifierList.add(token.trim());
                        }
                        try {
                            //LOG.info("Running bag query, with extra value " + c.getExtraValue());
                            bagQueryResult = bagQueryRunner.searchForBag(node.getType(),
                                identifierList, (String) c.getExtraValue(), true);
                        } catch (ClassNotFoundException e) {
                            throw new ObjectStoreException(e);
                        } catch (InterMineException e) {
                            throw new ObjectStoreException(e);
                        }
                        if (qc == null) {
                            LOG.error("qc is null. queryBits = " + queryBits + ", finalPath = "
                                    + finalPath + ", pathQuery: " + pathQuery);
                        }
                        if (bagQueryResult == null) {
                            LOG.error("bagQueryResult is null. queryBits = " + queryBits
                                    + ", finalPath = " + finalPath + ", pathQuery: "
                                    + pathQuery);
                        }
                        if (cs == null) {
                            LOG.error("cs is null. codeToCS = " + codeToCS + ", code = " + code
                                    + ", pathQuery: " + pathQuery);
                        }
                        cs.addConstraint(new BagConstraint(new QueryField(qc, "id"),
                                    ConstraintOp.IN, bagQueryResult.getMatchAndIssueIds()));
                        // TODO: The code that
                        // does a lookup for ifs should be moved out of this method. See #1284.
                        if (returnBagQueryResults != null) {
                            returnBagQueryResults.put(node.getPathString(), bagQueryResult);
                        }
                    }
                }
                deferralReasons.remove(node);
                queueDeferred = 0;
            }
        }

        // Now process loop constraints. The constraint parameter refers backwards and
        // forwards in the query so we can't process these in the above loop.
        makeQueryProcessLoopsHelper(pathQuery, codeToCS, loops, queryBits);

        if (andcs.getConstraints().isEmpty()) {
            q.setConstraint(null);
        } else if (andcs.getConstraints().size() == 1) {
            q.setConstraint((org.intermine.objectstore.query.Constraint)
                    (andcs.getConstraints().iterator().next()));
        }

        // build the SELECT list
        HashSet<PathNode> done = new HashSet<PathNode>();
        List<QuerySelectable> added = new ArrayList<QuerySelectable>();
        for (Path p : pathQuery.getView()) {
            PathNode pn = pathQuery.getNodes().get(p.toStringNoConstraints());
            if (nonOuterNodes.contains(pn)) {
                QueryNode qn = null;
                if (pn.isAttribute()) {
                    QueryClass qc = ((QueryClass) queryBits.get(pn.getPrefix()));
                    QueryField qf = new QueryField(qc, pn.getFieldName());
                    queryBits.put(pn.getPathString(), qf);
                    qn = qc;
                } else {
                    qn = (QueryNode) queryBits.get(pn.getPathString());
                }
                if (!added.contains(qn)) {
                    added.add(qn);
                }
            } else {
                while (pn != null && (!nonOuterNodes.contains(pn.getParent()))
                        && (!Util.equals(root, pn.getParent()))) {
                    pn = (PathNode) pn.getParent();
                }
                if (pn != null && (!done.contains(pn))) {
                    done.add(pn);
                    if (pn.isReference()) {
                        QueryClass qc = (QueryClass) queryBits.get(pn.getParent().getPathString());
                        if (qc == null) {
                            throw new NullPointerException("Failed to get path "
                                    + pn.getParent().getPathString() + " from " + queryBits);
                        }
                        Class subclass;
                        try {
                            subclass = TypeUtil.getClass(pn.getType(), model);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("Class not found in the model: "
                                    + pn.getType(), e);
                        }
                        try {
                            QueryObjectPathExpression qn = new QueryObjectPathExpression(qc,
                                    pn.getFieldName(), subclass);
                            recursiveMakeQuery(qn, pathQuery, pn, savedBags, pathToQueryNode,
                                    bagQueryRunner, returnBagQueryResults, checkOnly);
                            if (!added.contains(qc)) {
                                added.add(qc);
                            }
                            if (!added.contains(qn)) {
                                queryBits.put(pn.getPathString(), qn);
                                added.add(qn);
                            }
                        } catch (ClassCastException e) {
                            QueryCollectionPathExpression qn = new QueryCollectionPathExpression(qc,
                                    pn.getFieldName(), subclass);
                            recursiveMakeQuery(qn, pathQuery, pn, savedBags, pathToQueryNode,
                                    bagQueryRunner, returnBagQueryResults, checkOnly);
                            if (!added.contains(qc)) {
                                added.add(qc);
                            }
                            if (!added.contains(qn)) {
                                queryBits.put(pn.getPathString(), qn);
                                added.add(qn);
                            }
                        }
                    } else if (pn.isCollection()) {
                        QueryClass qc = (QueryClass) queryBits.get(pn.getParent().getPathString());
                        if (qc == null) {
                            throw new NullPointerException("Failed to get path "
                                    + pn.getParent().getPathString() + " from " + queryBits);
                        }
                        Class subclass;
                        try {
                            subclass = TypeUtil.getClass(pn.getType(), model);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalArgumentException("class not found in the model: "
                                    + pn.getType(), e);
                        }
                        QueryCollectionPathExpression qn = new QueryCollectionPathExpression(qc,
                                pn.getFieldName(), subclass);
                        recursiveMakeQuery(qn, pathQuery, pn, savedBags, pathToQueryNode,
                                bagQueryRunner, returnBagQueryResults, checkOnly);
                        if (!added.contains(qc)) {
                            added.add(qc);
                        }
                        if (!added.contains(qn)) {
                            queryBits.put(pn.getPathString(), qn);
                            added.add(qn);
                        }
                    }
                }
            }
        }
        QueryClass defaultClass = null;
        if (q instanceof QueryObjectPathExpression) {
            defaultClass = ((QueryObjectPathExpression) q).getDefaultClass();
        } else if (q instanceof QueryCollectionPathExpression) {
            defaultClass = ((QueryCollectionPathExpression) q).getDefaultClass();
        }
        if ((added.size() == 1) && added.get(0).equals(defaultClass)) {
            // Don't add anything to SELECT list - default is fine
        } else {
            for (QuerySelectable qs : added) {
                if (qs instanceof QueryObjectPathExpression) {
                    QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                    if (qope.getSelect().size() > 1) {
                        for (int i = 1; i <= qope.getSelect().size(); i++) {
                            q.addToSelect(new PathExpressionField(qope, i - 1));
                        }
                    } else {
                        q.addToSelect(qope);
                    }
                } else {
                    q.addToSelect(qs);
                }
            }
        }

        // build ORDER BY list
        if (q instanceof Query) {
            for (Path path : pathQuery.getSortOrder().keySet()) {
                PathNode pn = pathQuery.getNodes().get(path.toStringNoConstraints());
                if (nonOuterNodes.contains(pn)) {
                    QueryNode qn = (QueryNode) queryBits.get(pn.getPathString());

                    if (!((Query) q).getOrderBy().contains(qn)) {
                        ((Query) q).addToOrderBy(qn, pathQuery.getSortOrder().get(path));
                    }
                }
            }

            // put rest of select list in order by
            for (Path p : pathQuery.getView()) {
                String ps = p.toStringNoConstraints();
                PathNode pn = pathQuery.getNodes().get(ps);
                if (nonOuterNodes.contains(pn)) {
                    QueryNode selectNode = (QueryNode) queryBits.get(pn.getPathString());
                    if (!((Query) q).getOrderBy().contains(selectNode)) {
                        ((Query) q).addToOrderBy(selectNode);
                    }
                }
            }
        }

        // caller might want path to query node map (e.g. PrecomputeTask)
        if (pathToQueryNode != null) {
            pathToQueryNode.putAll(queryBits);
        }
    }

    private static org.intermine.objectstore.query.Constraint
        makeAttributeConstraint(QueryNode qn, Constraint c) {
        if (c.getOp() == ConstraintOp.IS_NOT_NULL
            || c.getOp() == ConstraintOp.IS_NULL) {
            return new SimpleConstraint((QueryEvaluable) qn, c.getOp());
        } else {
            if (qn.getType().equals(String.class)) {
                return makeQueryStringConstraint(qn, c);
            } else {
                if (qn.getType().equals(Date.class)) {
                    return makeQueryDateConstraint(qn, c);
                } else {
                    return new SimpleConstraint((QueryField) qn, c.getOp(),
                                                new QueryValue(c.getValue()));
                }
            }
        }
    }

    private static Set<PathNode> findNonOuterNodes(Map<String, PathNode> nodes, PathNode root) {
        Set<PathNode> retval = new LinkedHashSet();
        Set<PathNode> done = new LinkedHashSet();
        LinkedList<PathNode> queue = new LinkedList();
        for (PathNode node : nodes.values()) {
            queue.addLast(node);
        }
        Map<PathNode, String> deferralReasons = new HashMap<PathNode, String>();
        int queueDeferred = 0;
        while (!queue.isEmpty()) {
            if (queueDeferred > queue.size() + 10) {
                throw new IllegalArgumentException("Cannot handle entries in queue: " + queue
                        + ", reasons: " + deferralReasons + ", original node list: "
                        + nodes.values() + ", done = " + done);
            }
            PathNode node = queue.removeFirst();
            PathNode parent = (PathNode) node.getParent();
            if ((parent != null) && (!done.contains(parent))) {
                deferralReasons.put(node, "Parent \"" + parent + "\" not processed");
                queue.addLast(node);
                queueDeferred++;
            } else {
                if ((Util.equals(root, parent) || retval.contains(parent))
                            && (!node.isOuterJoin())) {
                    retval.add(node);
                }
                done.add(node);
                deferralReasons.remove(node);
                queueDeferred = 0;
            }
        }
        return retval;
    }

    /**
     * Make a SimpleConstraint for the given Constraint object.  The Constraint will be
     * case-insensitive.  If the Constraint value contains a wildcard and the operation is "=" or
     * "&lt;&gt;" then the operation will be changed to "LIKE" or "NOT_LIKE" as appropriate.
     */
    private static SimpleConstraint makeQueryStringConstraint(QueryNode qn, Constraint c) {
        QueryExpression qf = new QueryExpression(QueryExpression.LOWER, (QueryField) qn);
        String lowerCaseValue = ((String) c.getValue()).toLowerCase();

        // notes:
        //   - we always turn EQUALS into a MATCHES(LIKE) constraint and rely on Postgres
        //     to be sensible
        //   - lowerCaseValue is quoted in a way suitable for a LIKE constraint, but not for an
        //     normal equals.  for example 'Dpse\GA10108' needs to be 'Dpse\\GA10108' for equals
        //     but 'Dpse\\\\GA10108' (and hence "Dpse\\\\\\\\GA10108" as a Java string because
        //     backslash must be quoted with a backslash)
        if (c.getOp().equals(ConstraintOp.EQUALS)) {
            return new SimpleConstraint(qf, ConstraintOp.MATCHES, new QueryValue(lowerCaseValue));
        } else {
            if (c.getOp().equals(ConstraintOp.NOT_EQUALS)) {
                return new SimpleConstraint(qf, ConstraintOp.DOES_NOT_MATCH,
                                            new QueryValue(lowerCaseValue));
            } else {
                if (c.getOp().equals(ConstraintOp.CONTAINS)) {
                    return new SimpleConstraint(qf, ConstraintOp.MATCHES,
                                                new QueryValue("%" + lowerCaseValue + "%"));
                } else {
                    return new SimpleConstraint(qf, c.getOp(), new QueryValue(lowerCaseValue));
                }
            }
        }
    }


    /**
     * Make a SimpleConstraint for the given Date Constraint.  The time stored in the Date will be
     * ignored.  Example webapp constraints and the coresponding object store constraints:
     * "<= 2008-01-02"  -->  "<= 2008-01-02 23:59:59"
     * " < 2008-01-02"  -->  " < 2008-01-02 00:00:00"
     * " > 2008-01-02"  -->   "> 2008-01-02 23:59:59"
     * ">= 2008-01-02"  -->   "> 2008-01-02 00:00:00"
     * @param qn the QueryNode in the new query
     * @param c the webapp constraint
     * @return a new object store constraint
     */
    protected static org.intermine.objectstore.query.Constraint
        makeQueryDateConstraint(QueryNode qn, Constraint c) {
        Date dateValue = (Date) c.getValue();

        Calendar startOfDay = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        startOfDay.setTime(dateValue);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        QueryValue startOfDayQV = new QueryValue(startOfDay.getTime());

        Calendar endOfDay = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        endOfDay.setTime(dateValue);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        QueryValue endOfDayQV = new QueryValue(endOfDay.getTime());

        if (c.getOp().equals(ConstraintOp.EQUALS)
            || c.getOp().equals(ConstraintOp.NOT_EQUALS)) {
            SimpleConstraint startConstraint;
            SimpleConstraint endConstraint;
            ConstraintOp op;
            if (c.getOp().equals(ConstraintOp.EQUALS)) {
                startConstraint =
                    new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.GREATER_THAN_EQUALS,
                                         startOfDayQV);
                endConstraint =
                    new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN_EQUALS,
                                         endOfDayQV);
                op = ConstraintOp.AND;
            } else {
                // NOT_EQUALS
                startConstraint =
                    new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.LESS_THAN,
                                         startOfDayQV);
                endConstraint =
                    new SimpleConstraint((QueryEvaluable) qn, ConstraintOp.GREATER_THAN,
                                         endOfDayQV);
                op = ConstraintOp.OR;
            }
            ConstraintSet cs = new ConstraintSet(op);
            cs.addConstraint(startConstraint);
            cs.addConstraint(endConstraint);
            return cs;
        } else {
            if (c.getOp().equals(ConstraintOp.LESS_THAN_EQUALS)) {
                return new SimpleConstraint((QueryEvaluable) qn, c.getOp(), endOfDayQV);
            } else {
                if (c.getOp().equals(ConstraintOp.LESS_THAN)) {
                    return new SimpleConstraint((QueryEvaluable) qn, c.getOp(), startOfDayQV);
                } else {
                    if (c.getOp().equals(ConstraintOp.GREATER_THAN)) {
                        return new SimpleConstraint((QueryEvaluable) qn, c.getOp(), endOfDayQV);
                    } else {
                        if (c.getOp().equals(ConstraintOp.GREATER_THAN_EQUALS)) {
                            return new SimpleConstraint((QueryEvaluable) qn, c.getOp(), endOfDayQV);
                        } else {
                            throw new RuntimeException("Unknown ConstraintOp: " + c);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process loop constraints. The constraint parameter refers backwards and
     * forwards in the query so we can't process these in the main makeQuery loop
     */
    private static void makeQueryProcessLoopsHelper(PathQuery pathQuery,
            Map<String, ConstraintSet> codeToCS, Map<String, String> loops,
            Map<String, QuerySelectable> queryBits) {
        for (Iterator i = pathQuery.getNodes().values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            if (node.isReference() || node.isCollection()) {
                String path = node.getPathString();
                QueryNode qn = (QueryNode) queryBits.get(path);

                for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                    Constraint c = (Constraint) j.next();
                    ConstraintSet cs = codeToCS.get(c.getCode());
                    if ((c.getOp() == ConstraintOp.NOT_EQUALS)
                        || ((c.getOp() == ConstraintOp.EQUALS)
                            && (!loops.containsKey(path))
                            && (!loops.containsKey(c.getValue())))) {
                        QueryClass refQc = (QueryClass) queryBits.get(c.getValue());
                        if (refQc == null) {
                            throw new NullPointerException("Could not find QueryClass for "
                                    + c.getValue() + " in querybits: " + queryBits);
                        }
                        cs.addConstraint(new ClassConstraint((QueryClass) qn, c.getOp(), refQc));
                    }
                }
            }
        }
    }

    /*
     * Build a map to collapse nodes in loop queries
     */
    private static Map<String, String> makeLoopsMap(PathQuery pathQuery,
            Map<String, ConstraintSet> codeToCS, ConstraintSet andcs, boolean onlyEquals,
            Set<PathNode> nonOuterNodes, PathNode root) {
        if (root != null) {
            nonOuterNodes = new HashSet(nonOuterNodes);
            nonOuterNodes.add(root);
        }
        Map<String, String> loops = new HashMap<String, String>();
        for (PathNode node : pathQuery.getNodes().values()) {
            if (nonOuterNodes.contains(node)) {
                String path = node.getPathString();
                for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                    Constraint c = (Constraint) j.next();
                    if (c.getValue() instanceof InterMineBag) {
                        continue;
                    }
                    if ((node.isReference() || node.isCollection() || node.equals(root)
                                || (node.getParent() == null))
                            && ((c.getOp() == ConstraintOp.EQUALS)
                                || (c.getOp() == ConstraintOp.NOT_EQUALS))) {
                        String dest = (String) c.getValue();
                        boolean okay = false;
                        for (PathNode nonOuterNode : nonOuterNodes) {
                            if (nonOuterNode.getPathString().equals(dest)) {
                                okay = true;
                            }
                        }
                        if (!okay) {
                            throw new IllegalArgumentException("Error - loop constraint spans path"
                                    + " expression from " + path + " to " + dest);
                        }
                        if ((!onlyEquals) || ((c.getOp() == ConstraintOp.EQUALS)
                                    && (codeToCS.get(c.getCode()) == andcs))) {
                            String source;
                            if (dest.startsWith(path)) {
                                source = dest;
                                dest = path;
                            } else {
                                source = path;
                            }
                            String finalDest = loops.get(dest);
                            if (finalDest == null) {
                                finalDest = dest;
                            }
                            Map<String, String> newLoops = new HashMap<String, String>();
                            newLoops.put(source, finalDest);
                            for (Iterator<Entry<String, String>> k = loops.entrySet().iterator();
                                 k.hasNext();) {
                                Entry<String, String> entry = k.next();
                                String entryDest = entry.getValue();
                                if (entryDest.equals(source)) {
                                    entryDest = finalDest;
                                }
                                newLoops.put(entry.getKey(), entryDest);
                            }
                            loops = newLoops;
                        }
                    }
                }
            }
        }
        return loops;
    }

    /*
     ** currently unused
    private static Collection lowerCaseBag(Collection bag) {
        List retList = new ArrayList();
        Iterator iter = bag.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof String) {
                retList.add(((String) obj).toLowerCase());
            } else {
                retList.add(obj);
            }
        }
        return retList;
    }
     */

    /**
     * Given a LogicExpression, generate a tree of ConstraintSets that reflects the
     * expression and add entries to the codeToConstraintSet Map from map from
     * constraint code to ConstraintSet.
     *
     * @param logic the parsed logic expression
     * @param codeToConstraintSet output mapping from constraint code to ConstraintSet object
     * @param andcs an AND ConstraintSet that could be used as the root
     * @return root ConstraintSet
     */
    protected static ConstraintSet makeConstraintSets(LogicExpression logic,
                            Map<String, ConstraintSet> codeToConstraintSet, ConstraintSet andcs) {
        LogicExpression.Node node = logic.getRootNode();
        ConstraintSet root;
        if (node instanceof LogicExpression.And) {
            root = andcs;
            makeConstraintSets(node, root, codeToConstraintSet);
        } else if (node instanceof LogicExpression.Or) {
            root = new ConstraintSet(ConstraintOp.OR);
            makeConstraintSets(node, root, codeToConstraintSet);
        } else {
            throw new IllegalArgumentException("logic expression must contain a root operator");
        }

        return root;
    }

    /**
     * Given a Node in the expression logic and set of constraints, generate a tree of
     * ConstraintSets that reflects the expression and add entries to the codeToConstraintSet Map
     * from map from constraint code to ConstraintSet.
     * @param node a Node in the expression
     * @param set the constraints under this node
     * @param codeToConstraintSet output mapping from constraint code to ConstraintSet object
     */
    public static void makeConstraintSets(LogicExpression.Node node, ConstraintSet set,
                                          Map<String, ConstraintSet> codeToConstraintSet) {
        Iterator iter = node.getChildren().iterator();
        while (iter.hasNext()) {
            LogicExpression.Node child = (LogicExpression.Node) iter.next();
            if (child instanceof LogicExpression.And) {
                if (set.getOp() == ConstraintOp.AND) {
                    makeConstraintSets(child, set, codeToConstraintSet);
                } else {
                    ConstraintSet childSet = new ConstraintSet(ConstraintOp.AND);
                    set.addConstraint(childSet);
                    makeConstraintSets(child, childSet, codeToConstraintSet);
                }
            } else if (child instanceof LogicExpression.Or) {
                if (set.getOp() == ConstraintOp.OR) {
                    makeConstraintSets(child, set, codeToConstraintSet);
                } else {
                    ConstraintSet childSet = new ConstraintSet(ConstraintOp.OR);
                    set.addConstraint(childSet);
                    makeConstraintSets(child, childSet, codeToConstraintSet);
                }
            } else {
                // variable
                codeToConstraintSet.put(((LogicExpression.Variable) child).getName(), set);
            }
        }
    }

    /**
     * Get the metadata for a class by unqualified name
     * The name is looked up in the provided model
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the relevant ClassDescriptor
     * @throws ClassNotFoundException if the class name is not in the model
     */
    public static ClassDescriptor getClassDescriptor(String className, Model model)
        throws ClassNotFoundException {
        return model.getClassDescriptorByName(TypeUtil.getClass(className, model).getName());
    }

    /**
     * Take a Collection of ConstraintOps and builds a map from ConstraintOp.getIndex() to
     * ConstraintOp.toString() for each
     * @param ops a Collection of ConstraintOps
     * @return the Map from index to string
     */
    public static Map<Integer, String> mapOps(Collection ops) {
        Map<Integer, String> opString = new LinkedHashMap<Integer, String>();
        for (Iterator iter = ops.iterator(); iter.hasNext();) {
            ConstraintOp op = (ConstraintOp) iter.next();
            opString.put(op.getIndex(), op.toString());
        }
        return opString;
    }

    /**
     * Create constraint values for display. Returns a Map from Constraint to String
     * for each Constraint in the path query.
     *
     * @param pathquery  the PathQuery to look at
     * @return           Map from Constraint to displat value
     */
    public static Map<Constraint, String> makeConstraintDisplayMap(PathQuery pathquery) {
        Map<Constraint, String> map = new HashMap<Constraint, String>();
        Iterator iter = pathquery.getNodes().values().iterator();
        while (iter.hasNext()) {
            PathNode node = (PathNode) iter.next();
            Iterator citer = node.getConstraints().iterator();
            while (citer.hasNext()) {
                Constraint con = (Constraint) citer.next();
                map.put(con, con.getReallyDisplayValue());
            }
        }
        return map;
    }

    /**
     * Given a path, find out whether it represents an attribute or a reference/collection.
     *
     * @param path the path
     * @param pathQuery the path query
     * @return true if path ends with an attribute, false if not
     */
    public static boolean isPathAttribute(String path, PathQuery pathQuery) {
        String classname = getTypeForPath(path, pathQuery);
        return !(classname.startsWith(pathQuery.getModel().getPackageName())
                || classname.endsWith("InterMineObject"));
    }

    /**
     * Return the fully qualified type of the last node in the given path.
     * @param path the path
     * @param pathQuery the PathQuery that contains the given path
     * @return the fully qualified type name
     * @throws IllegalArgumentException if the path isn't valid for the PathQuery or if any
     * arguments are null
     */
    public static String getTypeForPath(String path, PathQuery pathQuery) {
        // find the longest path that has a type stored in the pathQuery, then use the model to find
        // the type of the last node

        if (path == null) {
            throw new IllegalArgumentException("path argument cannot be null");
        }

        if (pathQuery == null) {
            throw new IllegalArgumentException("pathQuery argument cannot be null");
        }

        Model model = pathQuery.getModel();

        PathNode testPathNode = pathQuery.getNodes().get(path);
        if (testPathNode != null) {
            try {
                return model.getQualifiedTypeName(testPathNode.getType());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("class \"" + testPathNode.getType()
                                                   + "\" not found");
            }
        }

        String[] bits = path.split("[.:]");

        List<String> bitsList = new ArrayList<String>(Arrays.asList(bits));

        String prefix = null;

        while (bitsList.size() > 0) {
            prefix = StringUtil.join(bitsList, ".");
            if (pathQuery.getNodes().get(prefix) != null) {
                break;
            }

            bitsList.remove(bitsList.size() - 1);
        }

        // the longest path prefix that has an entry in the PathQuery
        String longestPrefix = prefix;

        ClassDescriptor cld;

        if (bitsList.size() == 0) {
            try {
                cld = model.getClassDescriptorByName(model.getQualifiedTypeName(bits[0]));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("class \"" + bits[0] + "\" not found");
            }
        } else {
            PathNode pn = pathQuery.getNodes().get(longestPrefix);
            try {
                cld = getClassDescriptor(pn.getType(), model);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("class not found in the model: " + pn.getType(),
                                                   e);
            }
        }

        int startIndex = bitsList.size();

        if (startIndex < 1) {
            startIndex = 1;
        }

        for (int i = startIndex; i < bits.length; i++) {
            FieldDescriptor fd = cld.getFieldDescriptorByName(bits[i]);
            if (fd == null) {
                throw new IllegalArgumentException("could not find descriptor for: " + bits[i]
                                                   + " in " + cld.getName());
            }
            if (fd.isAttribute()) {
                return ((AttributeDescriptor) fd).getType();
            } else {
                cld = ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
            }
        }

        return cld.getName();
    }

    /**
     * Convert a path and prefix to a path.
     *
     * @param model the model this path conforms to
     * @param prefix the prefix (eg null or Department.company)
     * @param path the path (eg Company, Company.departments)
     * @return the new path
     */
    public static String toPathDefaultJoinStyle(Model model, String prefix, String path) {
        if (prefix != null) {
            if (path.indexOf(".") == -1) {
                path = prefix;
            } else {
                path = prefix + "." + path.substring(path.indexOf(".") + 1);
            }
        }
        return toPathDefaultJoinStyle(model, path);
    }

    /**
     * Given a path through the model set each join to outer/normal according to the defaults:
     *  - collections are outer joins
     *  - references are normal joins
     * e.g. Company.departments.name -> Company:departments.name
     * @param model the model this path conforms to
     * @param path the path to resolve
     * @return the new path
     */
    public static String toPathDefaultJoinStyle(Model model, String path) {
        
        // this will validate the path so we don't have to here
        Path dummyPath = new Path(model, path);
        
        String parts[] = path.split("[.:]");

        StringBuffer currentPath = new StringBuffer();
        currentPath.append(parts[0]);
        String clsName = model.getPackageName() + "." + parts[0];


        for (int i = 1; i < parts.length; i++) {
            String thisPart = parts[i];

            ClassDescriptor cld = model.getClassDescriptorByName(clsName);

            FieldDescriptor fld = cld.getFieldDescriptorByName(thisPart);
            if (fld.isCollection()) {
                currentPath.append(":");
            } else {
                currentPath.append(".");
            }
            
            currentPath.append(thisPart);
            // if an attribute this will be the end of the path, otherwise get the class of this
            // path element
            if (!fld.isAttribute()) {
                ReferenceDescriptor rfd = (ReferenceDescriptor) fld;
                clsName = rfd.getReferencedClassName();
            }
        }
        return currentPath.toString();
    }

    /**
     * Return the indexOf the last join in a path denoted by '.' or ':', return -1 if neither
     * join type exists in the path
     * @param path the path string to operate on
     * @return indexOf the last ':' or '.' in the path string or -1 if none present
     */
    public static int getLastJoinIndex(String path) {
        return (Math.max(path.indexOf("."), path.indexOf(":")));
    }
    
    /**
     * Return the indexOf the first join in a path denoted by '.' or ':', return -1 if neither
     * join type exists in the path
     * @param path the path string to operate on
     * @return indexOf the first ':' or '.' in the path string or -1 if none present
     */
    public static int getFirstJoinIndex(String path) {
        if (path.indexOf('.') < 0) {
            return path.indexOf(':');
        } else if (path.indexOf(":") < 0) {
            return path.indexOf('.');
        } else {
            return Math.min(path.indexOf('.'), path.indexOf(':'));
        }
    }
    
    
    /**
     * Generate a query from a PathQuery, to summarise a particular column of results.
     *
     * @param pathQuery the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode Map, into which columns to display will be placed
     * @param summaryPath a String path of the column to summarise
     * @param servletContext a ServletContext
     * @return an InterMine Query
     */
    public static Query makeSummaryQuery(PathQuery pathQuery, Map savedBags,
            Map<String, QuerySelectable> pathToQueryNode, String summaryPath,
            ServletContext servletContext) {
        return makeSummaryQuery(pathQuery, savedBags, pathToQueryNode, summaryPath,
                (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG),
                servletContext);
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
     * @param servletContext a ServletContext object
     * @return the generated summary query
     */
    public static Query makeSummaryQuery(PathQuery pathQuery, Map savedBags,
            Map<String, QuerySelectable> pathToQueryNode, String summaryPath, ObjectStore os,
            Map classKeys, BagQueryConfig bagQueryConfig, ServletContext servletContext) {
        Map<String, QuerySelectable> origPathToQueryNode = new HashMap();
        Query subQ = null;
        try {
            subQ = makeQuery(pathQuery, savedBags, origPathToQueryNode, servletContext, null,
                    false, os, classKeys, bagQueryConfig);
        } catch (ObjectStoreException e) {
            // Not possible if second-last argument is null
            throw new IllegalArgumentException("Should not ever happen", e);
        }
        subQ.clearOrderBy();
        Map<String, QuerySelectable> newSelect = new LinkedHashMap();
        Set<QuerySelectable> oldSelect = new HashSet();
        for (QuerySelectable qs : subQ.getSelect()) {
            oldSelect.add(qs);
            if (qs instanceof QueryClass) {
                newSelect.put(subQ.getAliases().get(qs), qs);
            } else if (!(qs instanceof QueryPathExpression)) {
                newSelect.put(subQ.getAliases().get(qs), qs);
            }
        }
        System.out.println("Original select: " + oldSelect);
        subQ.clearSelect();
        for (Map.Entry<String, QuerySelectable> selectEntry : newSelect.entrySet()) {
            subQ.addToSelect(selectEntry.getValue(), selectEntry.getKey());
        }
        QueryField qf = (QueryField) origPathToQueryNode.get(summaryPath);
        if ((qf == null) || (!subQ.getFrom().contains(qf.getFromElement()))) {
            // This column may be an outer join
            String prefix = summaryPath.substring(0, summaryPath.lastIndexOf('.'));
            String fieldName = summaryPath.substring(summaryPath.lastIndexOf('.') + 1);
            QuerySelectable qs = origPathToQueryNode.get(prefix);
            if (qs == null) {
                throw new NullPointerException("Error - path " + summaryPath + " is not in map "
                        + origPathToQueryNode);
            } else if (qs instanceof QueryObjectPathExpression) {
                if (!oldSelect.contains(qs)) {
                    throw new IllegalArgumentException("QueryObjectPathExpression is too deeply"
                           + " nested");
                }
                QueryObjectPathExpression qope = (QueryObjectPathExpression) qs;
                // We need to add QueryClasses to the query for this outer join. This will make it
                // an inner join, so the "no object" results will disappear.
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
                if (qcpe.getSelect().isEmpty() && qcpe.getFrom().isEmpty()
                        && oldSelect.contains(qcpe)) {
                    QueryClass firstQc = qcpe.getDefaultClass();
                    qf = new QueryField(firstQc, fieldName);
                    subQ.addFrom(firstQc);
                    QueryClass rootQc = qcpe.getQueryClass();
                    QueryHelper.addAndConstraint(subQ, new ContainsConstraint(
                                new QueryCollectionReference(rootQc, qcpe.getFieldName()),
                                ConstraintOp.CONTAINS, firstQc));
                    if (qcpe.getConstraint() != null) {
                        QueryHelper.addAndConstraint(subQ, qcpe.getConstraint());
                    }
                } else {
                    throw new IllegalArgumentException("QueryCollectionPathExpression is too"
                            + " complicated to summarise");
                }
            } else {
                throw new IllegalArgumentException("Error - path " + prefix + " resolves to unknown"
                        + " object " + qs);
            }
        }
        Query q = new Query();
        q.addFrom(subQ);
        subQ.addToSelect(qf);
        qf = new QueryField(subQ, qf);
        Class summaryType = qf.getType();
        if ((summaryType == Long.class) || (summaryType == Integer.class)
                || (summaryType == Short.class) || (summaryType == Byte.class)
                || (summaryType == Float.class) || (summaryType == Double.class)
                || (summaryType == BigDecimal.class)) {
            QueryNode min = new QueryFunction(qf, QueryFunction.MIN);
            QueryNode max = new QueryFunction(qf, QueryFunction.MAX);
            QueryNode avg = new QueryFunction(qf, QueryFunction.AVERAGE);
            QueryNode stddev = new QueryFunction(qf, QueryFunction.STDDEV);
            q.addToSelect(min);
            q.addToSelect(max);
            q.addToSelect(avg);
            q.addToSelect(stddev);
            pathToQueryNode.put("Minimum", min);
            pathToQueryNode.put("Maximum", max);
            pathToQueryNode.put("Average", avg);
            pathToQueryNode.put("Standard Deviation", stddev);
        } else if ((summaryType == String.class) || (summaryType == Boolean.class)) {
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

    /**
     * For a given PagedTable, return the corresponding PathQuery
     * TODO this only works for bags at the moment but need to be extended to work with anything
     * @param pagedTable the PagedTable
     * @param model the Model
     * @param bag the InterMineBag
     * @return a PathQuery
     */
    public static PathQuery webTableToPathQuery(PagedTable pagedTable, Model model,
                                                InterMineBag bag) {
        PathQuery pathQuery = new PathQuery(model);
        List columns = pagedTable.getColumns();
        List<Path> view = new ArrayList<Path>();
        for (Iterator iter = columns.iterator(); iter.hasNext();) {
            Column column = (Column) iter.next();
            view.add((Path) column.getPath());
        }
        pathQuery.setViewPaths(view);

        String bagType = bag.getType();
        ConstraintOp constraintOp = ConstraintOp.IN;
        String constraintValue = bag.getName();
        String label = null, id = null, code = pathQuery.getUnusedConstraintCode();
        Constraint c = new Constraint(constraintOp, constraintValue, false, label, code, id, null);
        pathQuery.addNode(bagType).getConstraints().add(c);
        return pathQuery;
    }
}
