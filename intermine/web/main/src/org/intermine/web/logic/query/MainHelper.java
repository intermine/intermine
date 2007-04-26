package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.path.Path;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
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
    /**
     * Move an attribute from the session to the request, removing it from the session.
     * @param attributeName the attribute name
     * @param request the current request
     */
    public static void moveToRequest(String attributeName, HttpServletRequest request) {
        HttpSession session = request.getSession();
        request.setAttribute(attributeName, session.getAttribute(attributeName));
        session.removeAttribute(attributeName);
    }

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
        makeNodes(getClassDescriptor(className, model), subPath, className, nodes, isSuperUser);
        return nodes.values();
    }

    /**
     * Recursive method used to add nodes to a set representing a path from a given ClassDescriptor
     * @param cld the root ClassDescriptor
     * @param path current path prefix (eg Gene)
     * @param currentPath current path suffix (eg organism.name)
     * @param nodes the current Node set
     * @param isSuperUser true if the user is the superuser
     */
    protected static void makeNodes(ClassDescriptor cld, String path, String currentPath,
                                    Map<String, MetadataNode> nodes, boolean isSuperUser) {
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
        for (Iterator i = cld.getAllFieldDescriptors().iterator(); i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
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
     * @return an InterMine Query
     */
    public static Query makeQuery(PathQuery query, Map savedBags) {
        return makeQuery(query, savedBags, null);
    }

    /**
     * Make an InterMine query from a path query
     * @param pathQueryOrig the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode optional parameter in which path to QueryNode map can be returned
     * @return an InterMine Query
     */
    public static Query makeQuery(PathQuery pathQueryOrig, Map savedBags,
                                  Map<String, QueryNode> pathToQueryNode) {
        PathQuery pathQuery = (PathQuery) pathQueryOrig.clone();
        Map qNodes = pathQuery.getNodes();
        List<Path> view = pathQuery.getView();
        List<OrderBy> sortOrder = pathQuery.getSortOrder();
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

        //first merge the query and the view
        for (Iterator<Path> i = view.iterator(); i.hasNext();) {
            String path = i.next().toStringNoConstraints();
            if (!qNodes.containsKey(path)) {
                pathQuery.addNode(path);
            }
        }

        //create the real query
        Query q = new Query();
        q.setConstraint(andcs);
        if ((rootcs != null) && (rootcs != andcs)) {
            andcs.addConstraint(rootcs);
        }

        // Build a map to collapse nodes in loop queries

        Map<String, String> loops = new HashMap<String, String>();

        for (Iterator i = pathQuery.getNodes().values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            String path = node.getPathString();
            for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                Constraint c = (Constraint) j.next();
                if ((node.isReference() || node.isCollection())
                        && (c.getOp() == ConstraintOp.EQUALS)
                        && (codeToCS.get(c.getCode()) == andcs)) {
                    String dest = (String) c.getValue();
                    String finalDest = loops.get(dest);
                    if (finalDest == null) {
                        finalDest = dest;
                    }
                    Map<String, String> newLoops = new HashMap<String, String>();
                    newLoops.put(path, finalDest);
                    for (Iterator<Entry<String, String>> k = loops.entrySet().iterator();
                         k.hasNext();) {
                        Entry<String, String> entry = k.next();
                        String entryDest = entry.getValue();
                        if (entryDest.equals(path)) {
                            entryDest = finalDest;
                        }
                        newLoops.put(entry.getKey(), entryDest);
                    }
                    loops = newLoops;
                }
            }
        }

        Map<String, QueryNode> queryBits = new HashMap<String, QueryNode>();
        LinkedList<PathNode> queue = new LinkedList<PathNode>();

        //build the FROM and WHERE clauses
        for (Iterator i = pathQuery.getNodes().values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            queue.addLast(node);
        }

        while (!queue.isEmpty()) {
            PathNode node = queue.removeFirst();
            String path = node.getPathString();
            QueryReference qr = null;
            String finalPath = loops.get(path);

            if (finalPath == null) {           
                if (path.indexOf(".") == -1) {
                    QueryClass qc = new QueryClass(getClass(node.getType(), model));
                    q.addFrom(qc);
                    queryBits.put(path, qc);
                } else {
                    String fieldName = node.getFieldName();
                    QueryClass parentQc = (QueryClass) queryBits.get(node.getPrefix());
                    if (parentQc == null) {
                        // We cannot process this QueryField yet. It depends on a parent QueryClass
                        // that we have not yet processed. Put it to the back of the queue.
                        queue.addLast(node);
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
                        QueryClass qc = new QueryClass(getClass(node.getType(), model));
                        andcs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
                        q.addFrom(qc);
                        queryBits.put(path, qc);
                    }
                }
                finalPath = path;
            } else {
                if (queryBits.get(finalPath) == null) {
                    // We cannot process this node yet. It is looped onto another node that has not
                    // been processed yet. Put it to the back of the queue.
                    queue.addLast(node);
                    continue;
                }
                if (finalPath.indexOf(".") != -1) {
                    String fieldName = node.getFieldName();
                    QueryClass parentQc = (QueryClass) queryBits.get(node.getPrefix());
                    if (!node.isAttribute()) {
                        if (node.isReference()) {
                            qr = new QueryObjectReference(parentQc, fieldName);
                        } else {
                            qr = new QueryCollectionReference(parentQc, fieldName);
                        }
                        QueryClass qc = (QueryClass) queryBits.get(finalPath);
                        andcs.addConstraint(new ContainsConstraint(qr, ConstraintOp.CONTAINS, qc));
                    }
                }
                queryBits.put(path, queryBits.get(finalPath));
            }

            QueryNode qn = queryBits.get(finalPath);
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
                    if (c.getOp() == ConstraintOp.IS_NOT_NULL
                        || c.getOp() == ConstraintOp.IS_NULL) {
                        cs.addConstraint(new SimpleConstraint((QueryEvaluable) qn, c.getOp()));
                    } else {
                        if (qn.getType().equals(String.class)) {
                            // do a case-insensitive search
                            QueryExpression qf = new QueryExpression(QueryExpression.LOWER,
                                    (QueryField) qn);
                            String lowerCaseValue = ((String) c.getValue()).toLowerCase();
                            cs.addConstraint(new SimpleConstraint(qf, c.getOp(),
                                                                  new QueryValue(lowerCaseValue)));
                        } else {
                            cs.addConstraint(new SimpleConstraint((QueryField) qn, c.getOp(),
                                                                  new QueryValue(c.getValue())));
                        }
                    }
                } else if (node.isReference()) {
                    if (c.getOp() == ConstraintOp.IS_NOT_NULL
                        || c.getOp() == ConstraintOp.IS_NULL) {
                        cs.addConstraint(
                            new ContainsConstraint((QueryObjectReference) qr, c.getOp()));
                    }
                }
            }
        }

        // Now process loop != constraints. The constraint parameter refers backwards and 
        // forwards in the query so we can't process these in the above loop. 
        for (Iterator i = pathQuery.getNodes().values().iterator(); i.hasNext();) { 
            PathNode node = (PathNode) i.next(); 
            if (node.isReference() || node.isCollection()) {
                String path = node.getPathString(); 
                QueryNode qn = queryBits.get(path); 

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

        if (andcs.getConstraints().isEmpty()) {
            q.setConstraint(null);
        } else if (andcs.getConstraints().size() == 1) {
            q.setConstraint((org.intermine.objectstore.query.Constraint)
                    (andcs.getConstraints().iterator().next()));
        }
        
        // build the SELECT list
        for (Iterator<Path> i = view.iterator(); i.hasNext();) {
            PathNode pn = pathQuery.getNodes().get(i.next().toStringNoConstraints());
            QueryNode qn = null;
            if (pn.isAttribute()) {
                QueryClass qc = ((QueryClass) queryBits.get(pn.getPrefix()));
                QueryField qf = new QueryField(qc, pn.getFieldName());
                queryBits.put(pn.getPathString(), qf);
                qn = qc;
            } else {
                qn = queryBits.get(pn.getPathString());
            }
            if (!q.getSelect().contains(qn)) {
                q.addToSelect(qn);
            }
        }

        // build ORDER BY list
        for (Iterator<OrderBy> i = sortOrder.iterator(); i.hasNext();) {
            OrderBy o = i.next();
            PathNode pn = pathQuery.getNodes().get(o.getField().toStringNoConstraints());
            QueryNode qn = queryBits.get(pn.getPathString());
            
            if (!q.getOrderBy().contains(qn)) {           
                q.addToOrderBy(qn, o.getDirection());
            }
        }

        // put rest of select list in order by 
        for (Iterator<Path> i = view.iterator(); i.hasNext();) {
            String ps = i.next().toStringNoConstraints();
            PathNode pn = pathQuery.getNodes().get(ps);
            QueryNode selectNode = queryBits.get(pn.getPathString()); 
            if (!q.getOrderBy().contains(selectNode)) { 
                q.addToOrderBy(selectNode); 
            }
        }
            
        // caller might want path to query node map (e.g. PrecomputeTask)
        if (pathToQueryNode != null) {
            pathToQueryNode.putAll(queryBits);
        }

        return q;
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
     * Instantiate a class by unqualified name
     * The name should be "InterMineObject" or the name of class in the model provided
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the relevant Class
     */
    public static Class getClass(String className, Model model) {
        if ("InterMineObject".equals(className)) {
            className = "org.intermine.model.InterMineObject";
        } else {
            className = model.getPackageName() + "." + className;
        }
        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate a class by unqualified name
     * The name should be "Date" or that of a primitive container class such as "Integer"
     * @param className the name of the class
     * @return the relevant Class
     */
    public static Class getClass(String className) {
        Class cls = TypeUtil.instantiate(className);
        if (cls == null) {
            if ("Date".equals(className)) {
                cls = Date.class;
            } else {
                if ("BigDecimal".equals(className)) {
                    cls = BigDecimal.class;
                } else {
                    try {
                        cls = Class.forName("java.lang." + className);
                    } catch (Exception e) {
                        throw new RuntimeException("unknown class: " + className);
                    }
                }
            }
        }
        return cls;
    }

    /**
     * Get the metadata for a class by unqualified name
     * The name is looked up in the provided model
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the relevant ClassDescriptor
     */
    public static ClassDescriptor getClassDescriptor(String className, Model model) {
        return model.getClassDescriptorByName(getClass(className, model).getName());
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
                map.put(con, con.getDisplayValue());
            }
        }
        return map;
    }

    /**
     * Return the qualified name of the given unqualified class name.  The className must be in the
     * given model or in the java.lang package or one of java.util.Date or java.math.BigDecimal.
     * @param className the name of the class
     * @param model the Model used to resolve class names
     * @return the fully qualified name of the class
     * @throws ClassNotFoundException if the class can't be found
     */
    public static String getQualifiedTypeName(String className, Model model)
        throws ClassNotFoundException {

        if (className.indexOf(".") != -1) {
            throw new IllegalArgumentException("Expected an unqualified class name: " + className);
        }

        if (TypeUtil.instantiate(className) != null) {
            // a primative type
            return className;
        } else {
            if ("InterMineObject".equals(className)) {
                return "org.intermine.model.InterMineObject";
            } else {
                try {
                    return Class.forName(model.getPackageName() + "." + className).getName();
                } catch (ClassNotFoundException e) {
                    // fall through and try java.lang
                }
            }

            if ("Date".equals(className)) {
                return Date.class.getName();
            }

            if ("BigDecimal".equals(className)) {
                return BigDecimal.class.getName();
            }

            return Class.forName("java.lang." + className).getName();
        }
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
                return getQualifiedTypeName(testPathNode.getType(), model);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("class \"" + testPathNode.getType()
                                                   + "\" not found");
            }
        }

        String[] bits = path.split("[.]");

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
                cld = model.getClassDescriptorByName(getQualifiedTypeName(bits[0], model));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("class \"" + bits[0] + "\" not found");
            }
        } else {
            PathNode pn = pathQuery.getNodes().get(longestPrefix);
            cld = getClassDescriptor(pn.getType(), model);
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
     * Given the string version of a path (eg. "Department.employees.seniority"), and a PathQuery,
     * create a Path object.  The PathQuery is needed to find the class constraints that effect the
     * path.
     * @param model the Model to pass to the Path constructor
     * @param query the PathQuery
     * @param fullPathName the full path as a string
     * @return a new Path object
     */
    public static Path makePath(Model model, PathQuery query, String fullPathName) {
        Map<String, String> subClassConstraintMap = new HashMap<String, String>();
        
        Iterator viewPathNameIter = query.getNodes().keySet().iterator();
        while (viewPathNameIter.hasNext()) {
            String viewPathName = (String) viewPathNameIter.next();
            PathNode pathNode = query.getNode(viewPathName);
            subClassConstraintMap.put(viewPathName, pathNode.getType());
        }
        
        Path path = new Path(model, fullPathName, subClassConstraintMap);
        return path;
    }

    /**
     * Convert a path and prefix to a path
     * @param prefix the prefix (eg null or Department.company)
     * @param path the path (eg Company, Company.departments)
     * @return the new path
     */
    public static String toPath(String prefix, String path) {
        if (prefix != null) {
            if (path.indexOf(".") == -1) {
                path = prefix;
            } else {
                path = prefix + "." + path.substring(path.indexOf(".") + 1);
            }
        }
        return path;
    }

    /**
     * Generate a query from a PathQuery, to summarise a particular column of results.
     *
     * @param pathQuery the PathQuery
     * @param savedBags the current saved bags map
     * @param pathToQueryNode Map, into which columns to display will be placed
     * @param summaryPath a String path of the column to summarise
     * @return an InterMine Query
     */
    public static Query makeSummaryQuery(PathQuery pathQuery, Map savedBags, 
                                         Map<String, QueryNode> pathToQueryNode,
            String summaryPath) {
        Map<String, QueryNode> origPathToQueryNode = new HashMap<String, QueryNode>();
        Query q = makeQuery(pathQuery, savedBags, origPathToQueryNode);
        q.clearSelect();
        q.clearOrderBy();
        QueryField qf = (QueryField) origPathToQueryNode.get(summaryPath);
        if (qf == null) {
            throw new NullPointerException("Error - path " + summaryPath + " is not in map "
                    + origPathToQueryNode);
        }
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
        pathQuery.setView(view);
        pathQuery.addNode(bag.getType()).getConstraints().add(
                                                              new Constraint(ConstraintOp.IN, bag
                                                                  .getName()));
        return pathQuery;
    }
}
