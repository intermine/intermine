package org.intermine.pathquery;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.path.Path;
import org.intermine.path.PathError;
import org.intermine.util.CollectionUtil;
import org.intermine.util.StringUtil;

/**
 * Class to represent a path-based query.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class PathQuery
{
    private static final Logger LOG = Logger.getLogger(PathQuery.class);
    private Model model;
    protected LinkedHashMap<String, PathNode> nodes = new LinkedHashMap<String, PathNode>();
    private List<Path> view = new ArrayList<Path>();
    private List<OrderBy> sortOrder = new ArrayList<OrderBy>();
    private ResultsInfo info;
    private List<Throwable> problems = new ArrayList<Throwable>();
    protected LogicExpression constraintLogic = null;
    private Map<Path, String> pathDescriptions = new HashMap<Path, String>();
    private static final String MSG = "Invalid path - path cannot be a null or empty string";

    /**
     * Construct a new instance of PathQuery.
     * @param model the Model on which to base this query
     */
    public PathQuery(Model model) {
        this.model = model;
    }

    /**
     * Construct a new instance of PathQuery from an existing
     * instance.
     * @param query the existing query
     */
    public PathQuery(PathQuery query) {
        this.model = query.model;
        this.nodes = new LinkedHashMap<String, PathNode>(query.nodes);
        this.view = new ArrayList<Path>(query.view);
        this.sortOrder = new ArrayList<OrderBy>(query.sortOrder);
        this.info = query.info;
        this.problems = new ArrayList<Throwable>(query.problems);
        this.constraintLogic = query.constraintLogic;
        this.pathDescriptions = new HashMap<Path, String>(query.pathDescriptions);
    }


    /*****************************************************************************************/


    private void validateView(List<Path> viewList) {
        Iterator it = viewList.iterator();
        while (it.hasNext()) {
            Path path = (Path) it.next();
            if (path == null || path.equals("")) {
                it.remove();
            }
        }
    }

    private List<Path> makePaths(List<String> viewStrings) {
        List<Path> viewPaths = new ArrayList<Path>();
        Iterator it = viewStrings.iterator();
        while (it.hasNext()) {
            String pathString = (String) it.next();
            if (pathString == null || pathString.trim().equals("")) {
                logPathError(MSG);
                continue;
            }
            Path path = null;
            try {
                path = makePath(model, this, pathString.trim());
            } catch (PathError e) {
                logPathError(e);
            }
            if (path != null) {
                viewPaths.add(path);
            }
        }
        return viewPaths;
    }

    /**
     * Sets the select list of the query to the list of paths given.  Paths can be a single path
     * or a comma or space delimited list of paths.  To append a path to the list instead use
     * addView.
     * @param paths a list of paths to be the view list
     */
    public void setView(String paths) {
        if (paths == null || paths.equals("")) {
            logPathError(MSG);
            return;
        }
        String [] pathStrings = paths.split("[, ]+");
        setView(new ArrayList<String>(Arrays.asList(pathStrings)));
    }

    /**
     * Clears the view list and sets the value of view to the list of strings given
     * @param viewStrings a list of strings.
     */
    public void setView(List<String> viewStrings) {
        if (viewStrings.isEmpty()) {
            logPathError(MSG);
            return;
        }
        setViewPaths(makePaths(viewStrings));
    }

    /**
     * Sets the value of view
     * @param view a List of Paths
     */
    public void setViewPaths(List<Path> view) {
        validateView(view);
        this.view = view;
        validateOrderBy();  // QueryBuilder can't have empty order by clause
        // sets subclasses nodes
        for (Path path : view) {
            for (Map.Entry<String, String> entry
                            : path.getSubClassConstraintPaths().entrySet()) {
                String stringPath = entry.getKey();
                PathNode node = addNode(stringPath);
                node.setType(entry.getValue());
            }
        }
    }

    /**
     * Appends the paths to the end of the select list. Paths can be a single path
     * or a comma delimited list of paths.
     * @param paths a list of paths to be appended to the end of the view list
     */
    public void addView(String paths) {
        if (paths == null || paths.equals("")) {
            logPathError(MSG);
            return;
        }
        String [] pathStrings = paths.split(",");
        addView(new ArrayList<String>(Arrays.asList(pathStrings)));
    }

    /**
     * Appends the paths to the end of the select list, ignores any bad paths
     * @param viewStrings a list of paths to be appended to the end of the view list
     */
    public void addView(List<String> viewStrings) {
        addViewPaths(makePaths(viewStrings));
    }

    /**
     * Appends the paths to the end of the select list.
     * @param paths a list of paths to be appended to the end of the view list
     */
    public void addViewPaths(List<Path> paths) {
        validateView(paths);
        if (paths.isEmpty()) {
            logPathError(MSG);
            return;
        }
        try {
            view.addAll(paths);
        } catch (PathError e) {
            logPathError(e);
        }
    }

    /**
     * Add a path to the view
     * @param viewString the String version of the path to add - should not include any class
     * constraints (ie. use "Departement.employee.name" not "Departement.employee[Contractor].name")
     */
    @Deprecated public void addPathStringToView(String viewString) {
        try {
            view.add(PathQuery.makePath(model, this, viewString));
            validateOrderBy();
        } catch (PathError e) {
            logPathError(e);
        }
    }

    /**
     * Gets the value of view
     * @return a List of paths
     */
    public List<Path> getView() {
        return view;
    }

    /**
     * Return the view as a List of Strings.
     * @return the view as Strings
     */
    public List<String> getViewStrings() {
        List<String> retList = new ArrayList<String>();
        for (Path path: view) {
            retList.add(path.toStringNoConstraints());
        }
        return retList;
    }

    /**
     * Remove the Path with the given String representation from the view.  If the pathString
     * refers to a path that appears in a PathError in the problems collection, remove that problem.
     * @param pathString the path to remove
     */
    public void removeFromView(String pathString) {
        Iterator<Path> iter = view.iterator();
        while (iter.hasNext()) {
            Path viewPath = iter.next();
            if (viewPath.toStringNoConstraints().equals(pathString)
                            || viewPath.toString().equals(pathString)) {
                iter.remove();
            }
        }
        Iterator<Throwable> throwIter = problems.iterator();
        while (throwIter.hasNext()) {
            Throwable thr = throwIter.next();
            if (thr instanceof PathError) {
                PathError pe = (PathError) thr;
                if (pe.getPathString().equals(pathString)) {
                    throwIter.remove();
                }
            }
        }
        removeOrderBy(pathString);
    }

    private Path getFirstPathFromView() {
        if (!view.isEmpty()) {
            return view.get(0);
        }
        return null;
    }

    /**
     * Return true if and only if the view contains a Path that has pathString as its String
     * representation.
     * @param pathString the path to test
     * @return true if found
     */
    public boolean viewContains(String pathString) {
        for (Path viewPath: getView()) {
            if (viewPath.toStringNoConstraints().equals(pathString)
                            || viewPath.toString().equals(pathString)) {
                return true;
            }
        }
        return false;
    }


    /*****************************************************************************************/


    /**
     * Add a constraint to the query, allow code to be automatically assigned - eg A
     * @param constraint constraint to add to the query
     * @param path path to constrain, eg Employee.firstName
     * @return label of constraint
     */
    public String addConstraint(String path, Constraint constraint) {
        String code = getUnusedConstraintCode();
        constraint.code = code;
        return addConstraint(path, constraint, code);
    }

    /**
     * Add a constraint to the query and specify the code
     * @param constraint constraint to add to the query
     * @param code code for constraint
     * @param path path to constrain, eg Employee.firstName
     * @return label of constraint
     */
    public String addConstraint(String path, Constraint constraint, String code) {
        PathNode node = addNode(path);
        constraint.code = code;
        node.getConstraints().add(constraint);
        return code;
    }

    /**
     * Add a constraint to the query, assign a subclass
     * @param constraint constraint to add to the query
     * @param code code for constraint
     * @param path path to constrain, eg Employee.firstName
     * @param subclass type of node
     * @return label of constraint
     */
    public String addConstraint(String path, Constraint constraint, String code, String subclass) {
        PathNode node = addNode(path);
        constraint.code = code;
        node.setType(subclass);
        node.getConstraints().add(constraint);
        return code;
    }

    /**
     * Set the constraint logic expression. This expresses the AND and OR
     * relation between constraints.
     * @param constraintLogic the constraint logic expression
     */
    public void setConstraintLogic(String constraintLogic) {
        if (constraintLogic == null) {
            this.constraintLogic = null;
            return;
        }
        try {
            this.constraintLogic = new LogicExpression(constraintLogic);
        } catch (IllegalArgumentException err) {
            LOG.error("Failed to parse constraintLogic: " + constraintLogic, err);
        }
    }

    /**
     * Get the constraint logic expression as a string.  Will return null of there are < 2
     * constraints
     * @return the constraint logic expression as a string
     */
    public String getConstraintLogic() {
        if (constraintLogic == null) {
            return null;
        }
        return constraintLogic.toString();
    }

    /**
     * Get the LogicExpression. If there are one or zero constraints then
     * this method will return null.
     * @return the current LogicExpression or null
     */
    public LogicExpression getLogic() {
        return constraintLogic;
    }

    /**
     * Make sure that the logic expression is valid for the current query. Remove
     * any unknown constraint codes and add any constraints that aren't included
     * (using the default operator).
     * @param defaultOperator the default logical operator
     */
    public void syncLogicExpression(String defaultOperator) {
        if (getAllConstraints().size() <= 1) {
            setConstraintLogic(null);
        } else {
            Set<String> codes = getConstraintCodes();
            if (constraintLogic != null) {
                // limit to the actual variables
                constraintLogic.removeAllVariablesExcept(getConstraintCodes());
                // add anything that isn't there
                codes.removeAll(constraintLogic.getVariableNames());
            }
            addCodesToLogic(codes, defaultOperator);
        }
    }

    /**
     * Get all constraint codes.
     * @return all present constraint codes
     */
    private Set<String> getConstraintCodes() {
        Set<String> codes = new HashSet<String>();
        for (Iterator<Constraint> iter = getAllConstraints().iterator(); iter.hasNext(); ) {
            codes.add(iter.next().getCode());
        }
        return codes;
    }

    /**
     * Get a constraint code that hasn't been used yet.
     * @return a constraint code that hasn't been used yet
     */
    public String getUnusedConstraintCode() {
        char c = 'A';
        while (getConstraintByCode("" + c) != null) {
            c++;
        }
        return "" + c;
    }

    /**
     * Get a Constraint involved in this query by code. Returns null if no
     * constraint with the given code was found.
     * @param string the constraint code
     * @return the Constraint with matching code or null
     */
    public Constraint getConstraintByCode(String string) {
        Iterator<Constraint> iter = getAllConstraints().iterator();
        while (iter.hasNext()) {
            Constraint c = iter.next();
            if (string.equals(c.getCode())) {
                return c;
            }
        }
        return null;
    }

    /**
     * Add a set of codes to the logical expression using the given operator.
     * @param codes Set of codes (Strings)
     * @param operator operator to add with
     */
    protected void addCodesToLogic(Set<String> codes, String operator) {
        String logic = getConstraintLogic();
        if (logic == null) {
            logic = "";
        } else {
            logic = "(" + logic + ")";
        }
        for (Iterator<String> iter = codes.iterator(); iter.hasNext(); ) {
            if (!StringUtil.isEmpty(logic)) {
                logic += " " + operator + " ";
            }
            logic += iter.next();
        }
        setConstraintLogic(logic);
    }

    /**
     * Get all constraints.
     * @return all constraints
     */
    public List<Constraint> getAllConstraints() {
        List<Constraint> list = new ArrayList<Constraint>();
        for (Iterator<PathNode> iter = nodes.values().iterator(); iter.hasNext(); ) {
            PathNode node = iter.next();
            list.addAll(node.getConstraints());
        }
        return list;
    }


    /*****************************************************************************************/


    /**
     * If order by clause is empty, adds first path in the view.
     * The querybuilder currently (stupidly) assumes there is at least one path in the order by
     * list, so we need to check this.
     */
    public void validateOrderBy() {
        if (sortOrder.isEmpty()) {
            Path p = getFirstPathFromView();
            if (p != null) {
                sortOrder.add(new OrderBy(p));
            }
        }
    }

    /**
     * Sets the order by list to the list of paths given.  Paths can be a single path or a comma
     * delimited list of paths.  To append a path to the list instead use addOrderBy.
     * @param paths paths to create the order by list
     */
    public void setOrderBy(String paths) {
        if (paths == null || paths.equals("")) {
            logPathError(MSG);
            return;
        }
        setOrderBy(paths, Boolean.TRUE);
    }

    /**
     * Sets the order by list of the query to the list of paths given.  Paths can be a single path
     * or a comma delimited list of paths.  To append a path to the list instead use addOrderBy.
     * @param paths paths to create the order by list
     * @param sortAscending whether or not to sort all fields in ascending order
     */
    public void setOrderBy(String paths, Boolean sortAscending) {
        if (paths == null || paths.equals("")) {
            logPathError(MSG);
            return;
        }
        List<OrderBy> orderBy = new ArrayList<OrderBy>();
        try {
            for (String path : paths.split("[, ]+")) {
                orderBy.add(new OrderBy(makePath(model, this, path), sortAscending));
            }
        } catch (PathError e) {
            logPathError(e);
        }
        sortOrder = orderBy;
    }

    /**
     * Sets the order by list of the query to the list of paths given.  Paths can be a single path
     * or a comma delimited list of paths.  To append a path to the list instead use addOrderBy.
     *
     * @param paths paths to create the order by list
     */
    public void setOrderBy(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            logPathError(MSG);
            return;
        }
        setOrderBy(paths, Boolean.TRUE);
    }

    /**
     * Sets the order by list of the query to the list of paths given.  Paths can be a single path
     * or a comma delimited list of paths.  To append a path to the list instead use addOrderBy.
     * @param paths paths to create the order by list
     * @param sortAscending whether to sort all order by columns in ascending order
     */
    public void setOrderBy(List<String> paths, Boolean sortAscending) {
        if (paths == null || paths.isEmpty()) {
            logPathError(MSG);
            return;
        }
        List<OrderBy> orderBy = new ArrayList<OrderBy>();
        for (String path : paths) {
            if (path != null && !path.equals("")) {
                try {
                    orderBy.add(new OrderBy(makePath(model, this, path), sortAscending));
                } catch (PathError e) {
                    logPathError(e);
                }
            } else {
                logPathError(MSG);
            }
        }
        if (!orderBy.isEmpty()) {
            sortOrder = orderBy;
        }
    }

    /**
     * Sets the order by list of the query to the list of paths given.  Paths can be a single path
     * or a comma delimited list of paths.  To append a path to the list instead use addOrderBy.
     * assumes paths are valid
     * @param paths paths to create the order by list
     */
    public void setOrderByList(List<OrderBy> paths) {
        if (paths.isEmpty()) {
            logPathError(MSG);
            return;
        }
        List<OrderBy> orderByList = new ArrayList();
        for (OrderBy orderBy : paths) {
            if (orderBy != null && (orderBy.getField() != null
                            && !orderBy.getDirection().equals(""))) {
                orderByList.add(orderBy);
            } else {
                logPathError(MSG);
            }
        }
        if (!orderByList.isEmpty()) {
            sortOrder = orderByList;
        }
    }

    /**
     * Appends the paths to the end of the order by list.  Paths can be a single path
     * or a comma delimited list of paths.
     * @param paths a list of paths to be appended to the end of the order by list
     */
    public void addOrderBy(String paths) {
        if (paths.equals("")) {
            logPathError(MSG);
            return;
        }
        addOrderBy(paths, Boolean.TRUE);
    }

    /**
     * Appends the paths to the end of the order by list.  Paths can be a single path
     * or a comma delimited list of paths.
     * @param paths a list of paths to be appended to the end of the order by list
     * @param sortAscending whether or not to sort these fields in ascending order
     */
    public void addOrderBy(String paths, Boolean sortAscending) {
        if (paths.equals("")) {
            logPathError(MSG);
            return;
        }
        List<OrderBy> orderBy = new ArrayList<OrderBy>();
        String direction = (sortAscending.booleanValue() ? "asc" : "desc");
        for (String path : paths.split("[, ]")) {
            if (path != null && !path.equals("")) {
                try {
                    orderBy.add(new OrderBy(makePath(model, this, path), direction));
                } catch (PathError e) {
                    logPathError(e);
                }
            } else {
                logPathError(MSG);
            }
        }
        sortOrder.addAll(orderBy);
    }

    /**
     * Appends the paths to the end of the order by list.
     * @param paths a list of paths to be appended to the end of the order by list
     */
    public void addOrderBy(List<String> paths) {
        if (paths.size() == 0) {
            logPathError(MSG);
            return;
        }
        addOrderBy(paths, Boolean.TRUE);
    }

    /**
     * Appends the paths to the end of the order by list.
     * @param paths a list of paths to be appended to the end of the order by list
     * @param sortAscending whether or not to sort these fields in ascending order
     */
    public void addOrderBy(List<String> paths, Boolean sortAscending) {
        if (paths.size() == 0) {
            logPathError(MSG);
            return;
        }
        List<OrderBy> orderBy = new ArrayList<OrderBy>();
        try {
            for (String path : paths) {
                if (path != null && !path.equals("")) {
                    orderBy.add(new OrderBy(makePath(model, this, path)));
                } else {
                    logPathError(MSG);
                }
            }
        } catch (PathError e) {
            logPathError(e);
        }
        if (!orderBy.isEmpty()) {
            sortOrder.addAll(orderBy);
        }
    }

    /**
     * Sets the sort order
     * @param sortOrder list of paths
     */
    @Deprecated public void setSortOrder(List<OrderBy> sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Add a path to the sort order
     * @param sortOrderString the String version of the path to add - should not include any class
     * constraints (ie. use "Departement.employee.name" not "Departement.employee[Contractor].name")
     * @param direction asc or desc
     */
    @Deprecated public void addPathStringToSortOrder(String sortOrderString, String direction) {
        try {
            sortOrder.clear(); // there can only be one sort column
            Path p = PathQuery.makePath(model, this, sortOrderString);
            OrderBy o = new OrderBy(p, direction);
            sortOrder.add(o);
        } catch (PathError e) {
            addProblem(e);
        }
    }

    /**
     * Gets the sort order
     * @return a List of paths
     */
    public List<OrderBy> getSortOrder() {
        return sortOrder;
    }

    /**
     * Return the sort order as a List of Strings.
     * @return the sort order as Strings
     */
    public List<String> getSortOrderStrings() {
        List<String> retList = new ArrayList<String>();
        for (OrderBy orderBy: sortOrder) {
            retList.add(orderBy.getField().toStringNoConstraints() + " " + orderBy.getDirection());
        }
        return retList;
    }

    /**
     * Change direction of the first field in the sort order.  used by javascripts on query
     * builder page, which currently only has one sort field
     * @param direction New sorting direction - asc or desc
     */
    public void changeDirection(String direction) {
        if (!direction.equals("desc") && !direction.equals("asc")) {
            logPathError("Invalid directional string, must be asc or desc");
        }
        if (sortOrder != null && !sortOrder.isEmpty()) {
            sortOrder.get(0).setDirection(direction);
        } else {
            if (view.size() > 0) {
                sortOrder = new ArrayList();
                sortOrder.add(new OrderBy(view.get(0), direction));
            } else {
                logPathError("View list is empty");
            }
        }
    }

    /**
     * Remove a path from the sort order.  If on the order by list, replace with first item
     * on select list.  Used by the querybuilder only, as the querybuilder only ever has one
     * path in the order by clause.
     * @param viewString The string being removed from the view list
     */
    private void removeOrderBy(String viewString) {
        Iterator<OrderBy> iter = sortOrder.iterator();
        while (iter.hasNext()) {
            OrderBy sort = iter.next();
            if (sort.getField().toStringNoConstraints().equals(viewString)) {
                iter.remove();          // remove this path from the order by clause
                validateOrderBy();      // QB requires that the order by clause not be empty
                return;
            }
        }
    }

    /**
     * Removes everything from the order by list and adds the first path in the view list
     */
    public void resetOrderBy() {
        sortOrder = new ArrayList();
        validateOrderBy();
    }


    /*****************************************************************************/


    /**
     * Gets the value of nodes
     * @return the value of nodes
     */
    public Map<String, PathNode> getNodes() {
        return nodes;
    }

    /**
     * Get a PathNode by path.
     * @param path a path
     * @return the PathNode for path path
     */
    public PathNode getNode(String path) {
        return nodes.get(path);
    }

    /**
     * Add a node to the query using a path, adding parent nodes if necessary
     * @param path the path for the new Node
     * @return the PathNode that was added to the nodes Map
     */
    public PathNode addNode(String path) {
        PathNode node;

        if (nodes.get(path) != null) {
            return nodes.get(path);
        }

        // the new node will be inserted after this one or at the end if null
        String previousNodePath = null;
        int lastIndex = Math.max(path.lastIndexOf("."), path.lastIndexOf(":"));

        if (lastIndex == -1) {
            node = new PathNode(path);
            if (model.isGeneratedClassesAvailable()) {
                if (!model.isGeneratedClassAvailable(path)) {
                    logPathError(new ClassNotFoundException("Class " + path + " is not available."));
                }                 
            }
        } else {
            String prefix = path.substring(0, lastIndex);
            if (nodes.containsKey(prefix)) {
                Iterator<String> pathsIter = nodes.keySet().iterator();

                while (pathsIter.hasNext()) {
                    String pathFromMap = pathsIter.next();
                    if (pathFromMap.startsWith(prefix)) {
                        previousNodePath = pathFromMap;
                    }
                }

                PathNode parent = nodes.get(prefix);
                String fieldName = path.substring(lastIndex + 1);
                node = new PathNode(parent, fieldName, path.charAt(lastIndex) == ':');
                try {
                    node.setModel(model);
                } catch (Exception err) {
                    logPathError(err);
                }
            } else {
                addNode(prefix);
                return addNode(path);
            }
        }

        nodes = CollectionUtil.linkedHashMapAdd(nodes, previousNodePath, path, node);

        return node;
    }

    /**
     * Clone this PathQuery
     * @return a PathQuery
     */
    public PathQuery clone() {
        PathQuery query = new PathQuery(model);
        IdentityHashMap<PathNode, PathNode> newNodes = new IdentityHashMap();
        for (Iterator<Entry<String, PathNode>> i = nodes.entrySet().iterator(); i.hasNext();) {
            Entry<String, PathNode> entry = i.next();
            query.getNodes().put(entry.getKey(), cloneNode(query, entry.getValue(), newNodes,
                                                           model));
        }
        query.getView().addAll(view);
        query.getSortOrder().addAll(sortOrder);
        if (problems != null) {
            query.problems = new ArrayList<Throwable>(problems);
        }
        query.pathDescriptions = new HashMap<Path, String>(pathDescriptions);
        query.setConstraintLogic(getConstraintLogic());
        query.info = info;
        return query;
    }

    /**
     * Clone a PathNode.
     *
     * @param query PathQuery containing cloned PathNode
     * @param node a PathNode
     * @param newNodes a Map from old PathNodes to new PathNodes, to link up parents properly
     * @param model the Model
     * @return a copy of the PathNode
     */
    protected static PathNode cloneNode(PathQuery query, PathNode node,
                                        IdentityHashMap<PathNode, PathNode> newNodes, Model model) {
        if (newNodes.containsKey(node)) {
            return newNodes.get(node);
        }
        PathNode newNode;
        PathNode parent = (PathNode) node.getParent();
        if (parent == null) {
            newNode = new PathNode(node.getType());
        } else {
            parent = cloneNode(query, parent, newNodes, model);
            newNode = new PathNode(parent, node.getFieldName(), node.isOuterJoin());
            try {
                newNode.setModel(model);
            } catch (IllegalArgumentException err) {
                query.addProblem(err);
            } catch (ClassNotFoundException e) {
                query.addProblem(e);
            }
            newNode.setType(node.getType());
        }
        for (Iterator i = node.getConstraints().iterator(); i.hasNext();) {
            Constraint constraint = (Constraint) i.next();
            newNode.getConstraints().add(new Constraint(constraint.getOp(),
                                                        constraint.getValue(),
                                                        constraint.isEditable(),
                                                        constraint.getDescription(),
                                                        constraint.getCode(),
                                                        constraint.getIdentifier(),
                                                        constraint.getExtraValue()));
        }
        newNodes.put(node, newNode);
        return newNode;
    }

    /**
     * Gets the value of model
     * @return the value of model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Return the map from Path objects (from the view) to their descriptions.
     * @return the path descriptions map
     */
    public Map<Path, String> getPathDescriptions() {
        return pathDescriptions;
    }

    /**
     * Returns the  path description for given path. Path description is computed according to the
     * known paths and corresponding path descriptions.
     * @param pathNoConstraints path without constraints
     * @return computed description or original path
     */
    public String getPathDescription(String pathNoConstraints) {
        String path = pathNoConstraints;
        String longestPrefix = "";
        String longestPrefixAlias = "";
        // in pathDescription object are saved prefixes and corresponding aliases
        // (path descriptions). The longest known prefix is searched in path for and corresponding
        // alias replaces the prefix.
        for (Map.Entry<Path, String> entry: pathDescriptions.entrySet()) {
            // can be a bad path
            if (entry.getKey().toStringNoConstraints() != null) {
                String prefix = entry.getKey().toStringNoConstraints();
                if (path.startsWith(prefix) && prefix.length() > longestPrefix.length()) {
                    longestPrefix = prefix;
                    longestPrefixAlias = entry.getValue();
                }
            }
        }
        if (!longestPrefix.equals("")) {
            return path.replaceFirst(longestPrefix, longestPrefixAlias);
        }
        return path;
    }

    /**
     * Return the description for the given path from the view.
     * @return the description Map
     */
    public Map<String, String> getPathStringDescriptions() {
        Map<String, String> retMap = new HashMap<String, String>();
        for (Map.Entry<Path, String> entry: pathDescriptions.entrySet()) {
            retMap.put(entry.getKey().toString(), entry.getValue());
            retMap.put(entry.getKey().toStringNoConstraints(), entry.getValue());
        }
        return retMap;
    }

    /**
     * Add a description to a path in the view.  If the viewString isn't a valid view path, add an
     * exception to the problems list.
     * @param viewString the string form of a path in the view
     * @param description the description
     */
    public void addPathStringDescription(String viewString, String description) {
        try {
            Path path = makePath(model, this, viewString);
            pathDescriptions.put(path, description);
        } catch (PathError e) {
            logPathError(e);
        }
    }

    /**
     * Provide a list of the names of bags mentioned in the query
     * @return the list of bag names
     */
    public List<Object> getBagNames() {
        List<Object> bagNames = new ArrayList<Object>();
        for (Iterator<PathNode> i = nodes.values().iterator(); i.hasNext();) {
            PathNode node = i.next();
            for (Iterator j = node.getConstraints().iterator(); j.hasNext();) {
                Constraint c = (Constraint) j.next();
                if (BagConstraint.VALID_OPS.contains(c.getOp())) {
                    bagNames.add(c.getValue());
                }
            }
        }
        return bagNames;
    }

    /**
     * Given the string version of a path (eg. "Department.employees.seniority"), and a PathQuery,
     * create a Path object.  The PathQuery is needed to find the class constraints that affect the
     * path.
     *
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
            subClassConstraintMap.put(viewPathName.replace(':', '.'), pathNode.getType());
        }
        Path path = new Path(model, fullPathName, subClassConstraintMap);
        return path;
    }

    /**
     * Get info regarding this query
     * @return the info
     */
    public ResultsInfo getInfo() {
        return info;
    }

    /**
     * Set info about this query
     * @param info the info
     */
    public void setInfo(ResultsInfo info) {
        this.info = info;
    }

    /**
     * Get the exceptions generated while deserialising this path query.
     * @return exceptions relating to this path query
     */
    public Throwable[] getProblems() {
        return problems.toArray(new Throwable[0]);
    }

    /**
     * Sets problems.
     * @param problems problems
     */
    public void setProblems(List<Throwable> problems) {
        this.problems = (problems != null ?  problems : new ArrayList<Throwable>());
    }

    /**
     * Find out whether the path query is valid against the current model.
     * @return true if query is valid, false if not
     */
    public boolean isValid() {
        return (problems.size() == 0);
    }

    /**
     * Adds problem to path query.
     * @param err problem
     */
    public void addProblem(Throwable err) {
        problems.add(err);
    }

    /**
     * Serialise this query in XML format.
     * @param name query name to put in xml
     * @return PathQuery in XML format
     */
    public String toXml(String name) {
        return PathQueryBinding.marshal(this, name, model.getName());
    }

    /**
     * Serialise to XML with no name.
     * @return the XML
     */
    public String toXml() {
        return PathQueryBinding.marshal(this, "", model.getName());
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return (o instanceof PathQuery)
        && model.equals(((PathQuery) o).model)
        && nodes.equals(((PathQuery) o).nodes)
        && view.equals(((PathQuery) o).view)
        && sortOrder.equals(((PathQuery) o).sortOrder)
        && pathDescriptions.equals(((PathQuery) o).pathDescriptions);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 2 * model.hashCode()
        + 3 * nodes.hashCode()
        + 5 * view.hashCode()
        + 7 * sortOrder.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "{PathQuery: model=" + model.getName() + ", nodes=" + nodes + ", view=" + view
        + ", sortOrder=" + sortOrder + ", pathDescriptions=" + pathDescriptions + "}";
    }

    private void logPathError(String msg) {
        logPathError(new PathError(msg, null));
    }

    private void logPathError(Throwable e) {
        LOG.error("Path error", e);
        addProblem(e);
    }
}
