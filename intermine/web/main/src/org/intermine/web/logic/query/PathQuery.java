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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ResultsInfo;

import org.intermine.metadata.Model;
import org.intermine.path.Path;
import org.intermine.path.PathError;
import org.intermine.util.CollectionUtil;

import java.io.StringReader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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
    private List<Path> sortOrder = new ArrayList<Path>();
    private ResultsInfo info;
    ArrayList<Throwable> problems = new ArrayList<Throwable>();
    protected LogicExpression constraintLogic = null;
    private Map<Path, String> pathDescriptions = new HashMap<Path, String>();

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
        this.sortOrder = new ArrayList<Path>(query.sortOrder);
        this.info = query.info;
        this.problems = new ArrayList<Throwable>(query.problems);
        this.constraintLogic = query.constraintLogic;
        this.pathDescriptions = new HashMap<Path, String>(query.pathDescriptions);
    }

    /**
     * Get the constraint logic expression.
     * @return the constraint logic expression
     */
    public String getConstraintLogic() {
        if (constraintLogic == null) {
            return null;
        } else {
            return constraintLogic.toString();
        }
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
     * Gets the value of model
     * @return the value of model
     */
    public Model getModel() {
        return model;
    }

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
    
    /**
     * Sets the value of view
     * @param view a List of Path
     */
    public void setView(List<Path> view) {
        this.view = view;
    }

    /**
     * Gets the value of view
     * @return a List of paths
     */
    public List<Path> getView() {
        return view;
    }

    /**
     * Sets the sort order
     * @param sortOrder list of paths
     */
    public void setSortOrder(List<Path> sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the sort order
     * @return a List of paths
     */
    public List<Path> getSortOrder() {
        return sortOrder;
    }
    
    
    /**
     * Add a path to the view
     * @param viewString the String version of the path to add - should not include any class
     * constraints (ie. use "Departement.employee.name" not "Departement.employee[Contractor].name")
     */
    public void addPathStringToView(String viewString) {
        try {
            view.add(MainHelper.makePath(model, this, viewString));
        } catch (PathError e) {
            problems.add(e);
        }
    }

    /**
     * Remove the Path with the given String representation from the view.  If the pathString 
     * refers to a path that appears in a PathError in the problems collection, remove that problem.
     * @param pathString the path to remove
     */
    public void removePathStringFromView(String pathString) {
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
    }

    private Path getFirstPathFromView() {
        Path viewPath = null;
        if (!view.isEmpty()) {
            Iterator<Path> iter = view.iterator();
             viewPath = iter.next();
        } 
        return viewPath;          
    }
    
    private void addPathToSortOrder(Path sortOrderPath) {
        try {
            sortOrder.clear(); // there can only be one sort column
            if (sortOrderPath != null) {
                sortOrder.add(sortOrderPath);
            }                
        } catch (PathError e) {
            problems.add(e);
        }
    }
    
    /**
     * Add a path to the sort order
     * @param sortOrderString the String version of the path to add - should not include any class
     * constraints (ie. use "Departement.employee.name" not "Departement.employee[Contractor].name")
     */
    public void addPathStringToSortOrder(String sortOrderString) {
        try {
            sortOrder.clear(); // there can only be one sort column
            sortOrder.add(MainHelper.makePath(model, this, sortOrderString));
        } catch (PathError e) {
            problems.add(e);
        }
    }

    /**
     * Remove the Path with the given String representation from the sort order.
     * @param sortOrderString the path to remove
     */
    public void removePathStringFromSortOrder(String sortOrderString) {
        addPathToSortOrder(getFirstPathFromView());
        Iterator<Path> iter = sortOrder.iterator();
        while (iter.hasNext()) {
            Path sortOrderPath = iter.next();
            if (sortOrderPath.toStringNoConstraints().equals(sortOrderString)
                || sortOrderPath.toString().equals(sortOrderString)) {
                iter.remove();
                return;
            }
        }       
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
     * Add a node to the query using a path, adding parent nodes if necessary
     * @param path the path for the new Node
     * @return the PathNode that was added to the nodes Map
     */
    public PathNode addNode(String path) {
        PathNode node;

        // the new node will be inserted after this one or at the end if null
        String previousNodePath = null;

        if (path.indexOf(".") == -1) {
            node = new PathNode(path);
            // Check whether starting point exists
            try {
                MainHelper.getQualifiedTypeName(path, model);
            } catch (ClassNotFoundException err) {
                problems.add(err);
            }
        } else {
            String prefix = path.substring(0, path.lastIndexOf("."));
            if (nodes.containsKey(prefix)) {
                Iterator<String> pathsIter = nodes.keySet().iterator();

                while (pathsIter.hasNext()) {
                    String pathFromMap = pathsIter.next(); 
                    if (pathFromMap.startsWith(prefix)) {
                        previousNodePath = pathFromMap;
                    }
                }

                PathNode parent = nodes.get(prefix);
                String fieldName = path.substring(path.lastIndexOf(".") + 1);
                node = new PathNode(parent, fieldName);
                try {
                    node.setModel(model);
                } catch (Exception err) {
                    problems.add(err);
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
     * Get the exceptions generated while deserialising this path query query.
     * @return exceptions relating to this path query
     */
    public Throwable[] getProblems() {
        return problems.toArray(new Throwable[0]);
    }
    
    /**
     * Find out whether the path query is valid against the current model.
     * @return true if query is valid, false if not
     */
    public boolean isValid() {
        return (problems.size() == 0);
    }

    /**
     * Clone this PathQuery
     * @return a PathQuery
     */
    public Object clone() {
        PathQuery query = new PathQuery(model);
        for (Iterator<Entry<String, PathNode>> i = nodes.entrySet().iterator(); i.hasNext();) {
            Entry<String, PathNode> entry = i.next();
            query.getNodes().put(entry.getKey(), clone(query, entry.getValue()));
        }
        query.getView().addAll(view);
        query.getSortOrder().addAll(sortOrder);
        if (problems != null) {
            query.problems = new ArrayList<Throwable>(problems);
        }
        query.pathDescriptions = new HashMap<Path, String>(pathDescriptions);
        query.setConstraintLogic(getConstraintLogic());
        return query;
    }

    /**
     * Clone a PathNode
     * @param query PathQuery containing cloned PathNode
     * @param node a PathNode
     * @return a copy of the PathNode
     */
    protected PathNode clone(PathQuery query, PathNode node) {
        PathNode newNode;
        PathNode parent = nodes.get(node.getPrefix());
        if (parent == null) {
            newNode = new PathNode(node.getType());
        } else {
            newNode = new PathNode(parent, node.getFieldName());
            try {
                newNode.setModel(model);
            } catch (IllegalArgumentException err) {
                query.problems.add(err);
            }
            newNode.setType(node.getType());
        }
        for (Iterator i = node.getConstraints().iterator(); i.hasNext();) {
            Constraint constraint = (Constraint) i.next();
            newNode.getConstraints().add(new Constraint(constraint.getOp(), constraint.getValue(),
                    constraint.isEditable(), constraint.getDescription(), constraint.getCode(),
                    constraint.getIdentifier()));
        }
        return newNode;
    }

    /**
     * @see Object#equals(Object)
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
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 2 * model.hashCode()
            + 3 * nodes.hashCode()
            + 5 * view.hashCode()
            + 7 * sortOrder.hashCode();
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        return "{PathQuery: model=" + model.getName() + ", nodes=" + nodes + ", view=" + view 
            + ", sortOrder=" + sortOrder + ", pathDescriptions=" + pathDescriptions + "}";
    }

    /**
     * Check validity of receiver by trying to create an objectstore Query. If
     * conversion fails, the exception is recorded and isValid will return false.
     * @param savedBags Map from bag name to bag
     */
    protected void checkValidity(Map savedBags) {
        try {
            MainHelper.makeQuery(this, savedBags);
        } catch (Exception err) {
            problems.add(err);
        }
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
            if (!StringUtils.isEmpty(logic)) {
                logic += " " + operator + " ";
            }
            logic += iter.next();
        }
        setConstraintLogic(logic);
    }
    
    /**
     * Remove some constraint code from the logic expression.
     * @param code the code to remove
     */
    public void removeCodeFromLogic(String code) {
        constraintLogic.removeVariable(code);
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
     * Rematerialise single query from XML.
     * @param xml PathQuery XML
     * @return a PathQuery object
     * @param savedBags Map from bag name to bag
     * @param classKeys class key fields for the model
     */
    public static PathQuery fromXml(String xml, Map savedBags, Map classKeys) {
        Map queries = PathQueryBinding.unmarshal(new StringReader(xml), savedBags, classKeys);
        return (PathQuery) queries.values().iterator().next();
    }

    /**
     * Return the map from Path objects (from the view) to their descriptions.
     * @return the path descriptions map
     */
    public Map<Path, String> getPathDescriptions() {
        return pathDescriptions;
    }
    
    /**
     * Return the description for the given path from the view.
     * @param pathString the path as a string
     * @return the description
     */
    public String getPathDescription(String pathString) {
        for (Map.Entry<Path, String> entry: pathDescriptions.entrySet()) {
            if (entry.getKey().toStringNoConstraints().equals(pathString)
                || entry.getKey().toString().equals(pathString)) {
                return entry.getValue();
            }
        }
        return null;
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
            Path path = MainHelper.makePath(model, this, viewString);
            pathDescriptions.put(path, description);
        } catch (PathError e) {
            problems.add(e);
        }
    }
}
