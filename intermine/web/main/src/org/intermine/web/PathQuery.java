package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ResultsInfo;

import org.intermine.metadata.Model;
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
    
    protected Model model;
    protected LinkedHashMap nodes = new LinkedHashMap();
    protected List view = new ArrayList();
    protected ResultsInfo info;
    protected ArrayList problems = new ArrayList();
    protected LogicExpression constraintLogic = null;
    protected Map alternativeViews = new TreeMap();

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
        this.nodes = query.nodes;
        this.view = query.view;
        this.info = query.info;
        this.problems = query.problems;
        this.constraintLogic = query.constraintLogic;
        this.alternativeViews = query.alternativeViews;
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
            Set codes = getConstraintCodes();            
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
    private Set getConstraintCodes() {
        Set codes = new HashSet();
        for (Iterator iter = getAllConstraints().iterator(); iter.hasNext(); ) {
            codes.add(((Constraint) iter.next()).getCode());
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
    public Map getNodes() {
        return nodes;
    }
    
    /**
     * Get a PathNode by path.
     * @param path a path
     * @return the PathNode for path path
     */
    public PathNode getNode(String path) {
        return (PathNode) nodes.get(path);
    }
    
    /**
     * Get all constraints.
     * @return all constraints
     */
    public List getAllConstraints() {
        ArrayList list = new ArrayList();
        for (Iterator iter = nodes.values().iterator(); iter.hasNext(); ) {
            PathNode node = (PathNode) iter.next();
            list.addAll(node.getConstraints());
        }
        return list;
    }
    
    /**
     * Sets the value of view
     * @param view a List of String paths
     */
    public void setView(List view) {
        this.view = view;
    }

    /**
     * Gets the value of view
     * @return a List of String paths
     */
    public List getView() {
        return view;
    }
    
    /**
     * Returns the view as a List of Path objects.
     * @return the value of view as Paths
     */
    public List getViewAsPaths() {
        List returnList = new ArrayList();
        Iterator iter = getView().iterator();
        while (iter.hasNext()) {
            returnList.add(MainHelper.makePath(model, this, (String) iter.next()));
        }
        return returnList;
    }
    
    /**
     * Get alternative select list by name.
     * @param name view name
     * @return List of Strings
     */
    public List getAlternativeView(String name) {
        return (List) alternativeViews.get(name);
    }
    
    /**
     * Get alternative select lists as an unmodifiable Map from name to List.
     * @return alternative select lists
     */
    public Map getAlternativeViews() {
        return Collections.unmodifiableMap(alternativeViews);
    }
    
    /**
     * Add an alternative select list.
     * @param name view name
     * @param alternateView the select list
     */
    public void addAlternativeView(String name, List alternateView) {
        alternativeViews.put(name, alternateView);
    }
    
    /**
     * Remove alternative select list by name
     * @param name view name
     */
    public void removeAlternativeView(String name) {
        alternativeViews.remove(name);
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
    public List getBagNames() {
        List bagNames = new ArrayList();
        for (Iterator i = nodes.values().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
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
                Iterator pathsIter = nodes.keySet().iterator();

                while (pathsIter.hasNext()) {
                    String pathFromMap = (String) pathsIter.next(); 
                    if (pathFromMap.startsWith(prefix)) {
                        previousNodePath = pathFromMap;
                    }
                }

                PathNode parent = (PathNode) nodes.get(prefix);
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
    public Exception[] getProblems() {
        return (Exception[]) problems.toArray(new Exception[0]);
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
        for (Iterator i = nodes.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            query.getNodes().put(entry.getKey(), clone(query, (PathNode) entry.getValue()));
        }
        query.getView().addAll(view);
        if (problems != null) {
            query.problems = new ArrayList(problems);
        }
        for (Iterator i = getAlternativeViews().entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            query.addAlternativeView((String) entry.getKey(), (List) entry.getValue());
        }
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
        PathNode parent = (PathNode) nodes.get(node.getPrefix());
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
            && alternativeViews.equals(((PathQuery) o).getAlternativeViews());
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 2 * model.hashCode()
            + 3 * nodes.hashCode()
            + 5 * view.hashCode();
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        return "{PathQuery: " + model + ", " + nodes + ", " + view + "}";
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
        Iterator iter = getAllConstraints().iterator();
        while (iter.hasNext()) {
            Constraint c = (Constraint) iter.next();
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
    protected void addCodesToLogic(Set codes, String operator) {
        String logic = getConstraintLogic();
        if (logic == null) {
            logic = "";
        } else {
            logic = "(" + logic + ")";
        }
        for (Iterator iter = codes.iterator(); iter.hasNext(); ) {
            if (!StringUtils.isEmpty(logic)) {
                logic += " " + operator + " ";
            }
            logic += (String) iter.next();
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
     */
    public static PathQuery fromXml(String xml, Map savedBags) {
        Map queries = PathQueryBinding.unmarshal(new StringReader(xml), savedBags);
        return (PathQuery) queries.values().iterator().next();
    }
}
