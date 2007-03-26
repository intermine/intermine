package org.intermine.bio.networkview;

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
import java.util.Iterator;
import java.util.List;

import org.intermine.bio.networkview.network.FlyEdge;
import org.intermine.bio.networkview.network.FlyHashGraphElement;
import org.intermine.bio.networkview.network.FlyNetwork;
import org.intermine.bio.networkview.network.FlyNode;
import org.intermine.bio.networkview.network.FlyValueWrapper;

import org.apache.log4j.Logger;

import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;

/**
 * @author Florian Reisinger
 *
 */
public abstract class FlyNetworkIntegrator
{
    private static final Logger LOG = Logger.getLogger(FlyNetworkIntegrator.class);

    /**
     * Integrates all elements of the flymine network into cytoscape.
     * (Needs to run within the running Cytoscape instance -> uses static methods)
     * @param nw network to integrate into cytoscape
     */
    public static void integrateNetwork(FlyNetwork nw) {
        Collection nodes = nw.getNodes();
        Collection edges = nw.getEdges();
        integrateNodes(nodes);
        integrateEdges(edges);
    }

    /**
     * This will integrate all flymine nodes from the Collection into cytoscape
     * @param flyNodes Collection of flymine nodes that are to be integrated
     */
    protected static void integrateNodes(Collection flyNodes) {
        // all node attributes are stored in one list, one has to specify
        // the id of the node and the name of the attribute to retrieve its value
        CyAttributes cyNodeAtts = Cytoscape.getNodeAttributes();

        // add all nodes in the collection and there attributes
        for (Iterator iter = flyNodes.iterator(); iter.hasNext();) {
            FlyNode n = (FlyNode) iter.next();
            // adding node to cytoscape using cytoscape's getCyNode method with flag true
            // -> will try to retrieve a node with that name or add a new one if non is found
            Cytoscape.getCyNode(n.getLabel(), true);
            // to the cytoscape attributes add all attributes of this node
            addAttributes(cyNodeAtts, n);
        }
    }

    /**
     * This will integrate all flymine edges from the Collection into cytoscape
     * @param flyEdges Collection of flymine edges that are to be integrated
     */
    protected static void integrateEdges(Collection flyEdges) {
        CyAttributes cyEdgeAtts = Cytoscape.getEdgeAttributes();

        // add all edges in the collection
        for (Iterator iter = flyEdges.iterator(); iter.hasNext();) {
            FlyEdge e = (FlyEdge) iter.next();
            // adding edge to cytoscape using cytoscape's getCyEdge method
            CyEdge etmp = Cytoscape.getCyEdge(
                    e.getSource().getLabel(),                               // source_alias
                    e.getLabel(),                                           // edge name
                    e.getTarget().getLabel(),                               // target_alias
                    (String) e.getAttributeValue(Semantics.INTERACTION));   // interaction_type
            // need to set identifier/label manually because cytoscape always uses default values
            etmp.setIdentifier(e.getLabel());
            // adding attributes of this edge
            addAttributes(cyEdgeAtts, e);
        }
    }

    /**
     * This will insert all ge's attributes into cytoscape's CyAttributes
     * @param cyAtts cytoscape CyAttributes to insert the attributes into
     * @param ge FlyHashGraphElement to get the attributes from
     */
    /*
    protected static void addAttributesOld(CyAttributes cyAtts, FlyHashGraphElement ge) {
        for (Iterator it = ge.getAttributeNames().iterator(); it.hasNext();) {
            String name = (String) it.next();
            LOG.debug("processing attribute: " + name + " of element: " + ge.getLabel());
            Object o = ge.getAttributeValue(name);
            int flag = ge.getAttributeFlag(name);

            if (o instanceof Boolean) { // the object is boolean! 
                LOG.debug("object is of type Boolean! Flag is of value: " + flag);
                // now we have to check the flag to see what have to do with it
                if (flag == FlyValueWrapper.NOT_OVERWRITE
                    && cyAtts.getBooleanAttribute(ge.getLabel(), name) != null) {
                    // do nothing! there is already a value for that attribute
                    // and we do not want to overwrite it!
                } else { 
                    // there is no value for this attribute or the flag is set to OVERWRITE or
                    // the flag is set to ADD/COUNT -> no use adding boolean values -> overwrite
                    cyAtts.setAttribute(ge.getLabel(), name, (Boolean) o);
                }
            } else
            if (o instanceof Integer) { // the object is a Integer!
                LOG.debug("object is of type Integer! Flag is of value: " + flag);
                // now we have to check the flag to see what have to do with it
                if (flag == FlyValueWrapper.NOT_OVERWRITE
                    && cyAtts.getIntegerAttribute(ge.getLabel(), name) != null) {
                    LOG.debug("not updating (NOT_OVERWRITE) Integer value of attribute " + name);
                    // do nothing! there is already a value for that attribute
                    // and we do not want to overwrite it!
                } else if (flag == FlyValueWrapper.ADD) {
                    LOG.debug("updating (ADD) Integer value of attribute " + name);
                    int value = ((Integer) o).intValue();
                    Integer i = cyAtts.getIntegerAttribute(ge.getLabel(), name);
                    // if there is already a value present in cytoscape for that attribute
                    // than add that value to the new value
                    if (i != null) {
                        LOG.debug("adding value: '" + i.intValue() 
                                + "' and value: '" + value + "'");
                        value += i.intValue();
                    }
                    // else just add the new value
                    cyAtts.setAttribute(ge.getLabel(), name, new Integer(value));
                } else {
                    LOG.debug("updating (OVERWRITE) Integer value of attribute " + name);
                    // there is no value-attribute pair or
                    // the flag is set to overwrite
                    cyAtts.setAttribute(ge.getLabel(), name, (Integer) o);
                }
            } else 
            if (o instanceof Double) { // the object is a Double!
                LOG.debug("object is of type Double! Flag is of value: " + flag);
                // now we have to check the flag to see what have to do with it
                if (flag == FlyValueWrapper.NOT_OVERWRITE
                    && cyAtts.getDoubleAttribute(ge.getLabel(), name) != null) {
                    // do nothing! there is already a value for that attribute
                    // and we do not want to overwrite it!
                } else if (flag == FlyValueWrapper.ADD) {
                    double value = ((Double) o).doubleValue();
                    Double d = cyAtts.getDoubleAttribute(ge.getLabel(), name);
                    // if there is already a value present in cytoscape for that attribute
                    // than add that value to the new value
                    if (d != null) {
                        value += d.doubleValue();
                    }
                    // else just add the new value
                    cyAtts.setAttribute(ge.getLabel(), name, new Double(value));
                } else {
                    // there is no value-attribute pair or
                    // the flag is set to overwrite
                    LOG.debug("updating (OVERWRITE) Double value of attribute " + name);
                    cyAtts.setAttribute(ge.getLabel(), name, (Double) o);
                }
            } else 
            if (o instanceof String) {
                LOG.debug("object is of type String! Flag is of value: " + flag);
                // now we have to check the flag to see what have to do with it
                if (flag == FlyValueWrapper.NOT_OVERWRITE
                        && cyAtts.getStringAttribute(ge.getLabel(), name) != null) {
                    LOG.debug("not updating (NOT_OVERWRITE) String value of attribute " + name);
                    // do nothing! there is already a value for that attribute
                    // and we do not want to overwrite it!
                } else if (flag == FlyValueWrapper.ADD) {
                    LOG.debug("updating (ADD) String value of attribute " + name);
                    String value = ((String) o);
                    if (name.endsWith("_LIST")) {   // naming convention
                        // dealing with a List
                        // check if attribute already exists
                        if (cyAtts.hasAttribute(ge.getLabel(), name)) {
                            // check if it is a List as expected
                            if (cyAtts.getType(name) == CyAttributes.TYPE_SIMPLE_LIST) {
                                // TODO: also check list entries! have to be of type String!
                                // there is a attribute with the specified name and it is a list
                                // -> add new value
                                List attList = cyAtts.getAttributeList(ge.getLabel(), name);
                                if (!attList.contains(value)) {
                                    attList.add(value);
                                    cyAtts.setAttributeList(ge.getLabel(), name, attList);
                                }
                            } else {
                                // there is a attribute with the specified name, but 
                                // it is not a list
                                LOG.error("expected type " + CyAttributes.TYPE_SIMPLE_LIST 
                                        + " for attribute '" + name 
                                        +  "', but found type: " + cyAtts.getType(name));
                            }
                        } else {
                            // there is no attribute with that name -> create a new List
                            ArrayList list = new ArrayList();
                            list.add(value);
                            cyAtts.setAttributeList(ge.getLabel(), name, list);
                        }
                    } else { // should not be a list
                        // check if attribute already exists
                        if (cyAtts.hasAttribute(ge.getLabel(), name)) {
                            // there is already a attribute with that name, but not in a list!
                            // so, since we want to ADD, we will create a new attribute with the 
                            // correct syntax and add both values to it
                            if (cyAtts.getType(name) == CyAttributes.TYPE_STRING) {
                                String s = cyAtts.getStringAttribute(ge.getLabel(), name);
                                ArrayList list = new ArrayList();
                                list.add(s);
                                list.add(value);
                                cyAtts.setAttributeList(ge.getLabel(), name + "_LIST", list);
                            } else {
                                LOG.error("expected type " + CyAttributes.TYPE_STRING 
                                        + " for attribute '" + name 
                                        +  "', but found type: " + cyAtts.getType(name));
                            }
                        } else {
                            // no attribute with that name found -> create one
                            cyAtts.setAttribute(ge.getLabel(), name, (String) o);
                        }
                    }
                } else if (flag == FlyValueWrapper.COUNT) {
                    LOG.debug("updating (COUNT) String value of attribute " + name);
                    String value = ((String) o);
                    if (name.endsWith("_LIST")) {   // naming convention
                        // dealing with a List
                        // check if attribute already exists
                        if (cyAtts.hasAttribute(ge.getLabel(), name)) {
                            // check if it is a List as expected
                            if (cyAtts.getType(name) == CyAttributes.TYPE_SIMPLE_LIST) {
                                // TODO: also check list entries! have to be of type String!
                                // there is a attribute with the specified name and it is a list
                                // -> add new value
                                List attList = cyAtts.getAttributeList(ge.getLabel(), name);
                                if (!attList.contains(value)) {
                                    attList.add(value);
                                    // update the list 
                                    cyAtts.setAttributeList(ge.getLabel(), name, attList);
                                    // update the corresponding counter
                                    cyAtts.setAttribute(ge.getLabel(), 
                                            name.replaceAll("_LIST", "_COUNT"), 
                                            new Integer(attList.size()));
                                }
                            } else {
                                // there is a attribute with the specified name, but 
                                // it is not a list
                                LOG.error("expected type " + CyAttributes.TYPE_SIMPLE_LIST 
                                        + " for attribute '" + name 
                                        +  "', but found type: " + cyAtts.getType(name));
                            }
                        } else {
                            // there is no attribute with that name -> create a new List
                            ArrayList list = new ArrayList();
                            list.add(value);
                            // update the list 
                            cyAtts.setAttributeList(ge.getLabel(), name, list);
                            // update the corresponding counter
                            cyAtts.setAttribute(ge.getLabel(), 
                                    name.replaceAll("_LIST", "_COUNT"), 
                                    new Integer(list.size()));
                        }
                    } else { // should not be a list
                        // check if attribute already exists
                        if (cyAtts.hasAttribute(ge.getLabel(), name)) {
                            // there is already a attribute with that name, but NOT in a list!
                            // -> since we want to COUNT, we have to create a new list attribute 
                            // with the correct syntax, add both values to it and add a new 
                            // counter attribute 
                            if (cyAtts.getType(name) == CyAttributes.TYPE_STRING) {
                                String s = cyAtts.getStringAttribute(ge.getLabel(), name);
                                ArrayList list = new ArrayList();
                                list.add(s);
                                list.add(value);
                                cyAtts.setAttributeList(ge.getLabel(), name + "_LIST", list);
                                cyAtts.setAttribute(ge.getLabel(), 
                                        name.replaceAll("_LIST", "_COUNT"), 
                                        new Integer(list.size()));
                            } else {
                                LOG.error("expected type " + CyAttributes.TYPE_STRING 
                                        + " for attribute '" + name 
                                        +  "', but found type: " + cyAtts.getType(name));
                            }
                        } else {
                            // no attribute with that name found -> create one
                            cyAtts.setAttribute(ge.getLabel(), name, (String) o);
                        }
                    }
                } else {
                    LOG.debug("updating (OVERWRITE) String value of attribute " + name);
                    // there is no value-attribute pair or
                    // the flag is set to overwrite
                    cyAtts.setAttribute(ge.getLabel(), name, (String) o);
                }
            } else {
                LOG.error("Discovered illegal value type: " + o.getClass().toString() 
                        + " for attribute: " + name);
                LOG.error("allowed types are: Boolean, Integer, Double and String!");
            }
        }
    }
    */
    
    /**
     * This will insert all ge's attributes into cytoscape's CyAttributes
     * @param cyAtts cytoscape CyAttributes to insert the attributes into
     * @param ge FlyHashGraphElement to get the attributes from
     */
    protected static void addAttributes(CyAttributes cyAtts, FlyHashGraphElement ge) {
        for (Iterator it = ge.getAttributeNames().iterator(); it.hasNext();) {
            String name = (String) it.next();
            String element = ge.getLabel();
            LOG.debug("processing attribute: " + name + " of element: " + element);

            Object o = ge.getAttributeValue(name);
            int flag = ge.getAttributeFlag(name);

            LOG.debug("flag: " + flag);
            if (!cyAtts.hasAttribute(element, name)) {
                LOG.debug("attribute " + name + " does not exist.");
                // TODO: set lists!! handle COUNT flag
                if (o instanceof String) {
                    // type String -> create list if flag is set to COUNT or ADD
                    if (flag == FlyValueWrapper.COUNT) {
                        ArrayList list = new ArrayList();
                        list.add((String) o);
                        cyAtts.setAttributeList(element, name, list);
                        String counterName = "";
                        if (name.endsWith("_LIST")) {
                            counterName = name.replaceAll("_LIST", "_COUNT");
                        } else {
                            counterName = name + "_COUNT";
                        }
                        // TODO: check attribute exists and has propper type -> error else
                        cyAtts.setAttribute(element, counterName, new Integer(list.size()));
                    } else if (flag == FlyValueWrapper.ADD) {
                        ArrayList list = new ArrayList();
                        list.add(o);
                        cyAtts.setAttributeList(element, name, list);
                    } else { 
                        setAttribute(cyAtts, element, name, o);
                    }
                } else { // not String
                    setAttribute(cyAtts, element, name, o);
                }
            } else { // attribute already exists
                LOG.debug("attribute " + name + " exists.");
                if (flag == FlyValueWrapper.NOT_OVERWRITE) {
                    // do nothing! we do not want to overwrite existing values
                } else if (flag == FlyValueWrapper.OVERWRITE) {
                    // set value even if we have to overwrite an existing value
                    setAttribute(cyAtts, element, name, o);
                } else if (flag == FlyValueWrapper.ADD) {
                    // simple value adding: only makes sense for Integer or Double values
                    if (o instanceof Integer) {
                        int flyValue = ((Integer) o).intValue();
                        byte b = cyAtts.getType(name);  // type check
                        if (b == CyAttributes.TYPE_INTEGER) {
                            Integer cyValue = cyAtts.getIntegerAttribute(element, name);
                            flyValue += cyValue.intValue();
                            cyAtts.setAttribute(element, name, new Integer(flyValue));
                        } else {    // type check failed
                            LOG.error("Wrong value type! Found: " + b 
                                    + "expected: " + CyAttributes.TYPE_INTEGER + " (Integer).");
                        }
                    } else if (o instanceof Double) {
                        double flyValue = ((Double) o).doubleValue();
                        byte b = cyAtts.getType(name);  // type check
                        if (b == CyAttributes.TYPE_FLOATING) {
                            Double cyValue = cyAtts.getDoubleAttribute(element, name);
                            flyValue += cyValue.doubleValue();
                            cyAtts.setAttribute(element, name, new Double(flyValue));
                        } else {    // type check failed
                            LOG.error("Wrong value type! Found: " + b 
                                    + "expected: " + CyAttributes.TYPE_FLOATING + " (Double).");
                        }
                    } else if (o instanceof String) {
                        String flyValue = (String) o;
                        if (cyAtts.getType(name) == CyAttributes.TYPE_SIMPLE_LIST) {
                            // (potential) list of Strings -> just add another value
                            List list = cyAtts.getAttributeList(element, name);
                            if (list.iterator().next() instanceof String) {
                                // since all values in a list have to be of the same type
                                // we just have to check the first one
                                list.add(flyValue);
                            } else {
                                LOG.error("Error trying to ADD String value to attribute: " 
                                        + name + ". This attribute is not a List of StringS!");
                            }
                        } else if (cyAtts.getType(name) == CyAttributes.TYPE_STRING) {
                            // no List -> in order to keep the old value AND add the new value,
                            // create a new attribute that can hold multiple StringS
                            ArrayList list = new ArrayList();
                            list.add(flyValue);
                            // check if there already exists a attriute "name + _LIST" and 
                            // if it's of type TYPE_SIMPLE_LIST
                            if (cyAtts.getType(name + "_LIST") == CyAttributes.TYPE_SIMPLE_LIST) {
                                // (potential) list of Strings -> just add another value
                                List tmpList = cyAtts.getAttributeList(element, name);
                                if (tmpList.iterator().next() instanceof String) {
                                    // since all values in a list have to be of the same type
                                    // we just have to check the first one
                                    tmpList.add(flyValue);
                                } else {
                                    LOG.error("Error trying to ADD String value to attribute: " 
                                            + name + ". This attribute is not a List of StringS!");
                                }
                            } else {
                                // no attriute "name + _LIST" -> create a new one
                                cyAtts.setAttributeList(element, name + "_LIST", list);
                            }
                        } else {
                            //error
                            LOG.error("Error while trying to ADD String value to attribute: " 
                                    + name + ". Unexpected value type found!");
                        }
                    } else {
                        // other types are not supported yet!
                        LOG.error("Discovered unsupported value type " + o.getClass().toString() 
                                + " (attribute: " + name + ") for flag: " + flag);
                    }
                } else if (flag == FlyValueWrapper.COUNT) {
                    // only makes sense for String (list) values
                    if (o instanceof String) {
                        String flyValue = (String) o;
                        if (cyAtts.getType(name) == CyAttributes.TYPE_SIMPLE_LIST) {
                            // check list values -> Strings
                            List list = cyAtts.getAttributeList(element, name);
                            if (list.iterator().next() instanceof String) {
                                // since all values in a list have to be of the same type
                                // we just have to check the first one
                                if (list.contains(flyValue)) { // prevent duplicate entries
                                    LOG.warn("List attached to attribute '" + name 
                                            + "' of element '" + element
                                            + "' already contains value '"  + flyValue 
                                            + "'. Skipping value!");
                                } else {
                                    list.add(flyValue);
                                }
                                cyAtts.setAttributeList(element, name, list);
                                String counterName = "";
                                if (name.endsWith("_LIST")) {
                                    counterName = name.replaceAll("_LIST", "_COUNT");
                                } else {
                                    counterName = name + "_COUNT";
                                }
                                // TODO: check attribute exists and has propper type -> error else
                                cyAtts.setAttribute(element, counterName, new Integer(list.size()));
                            } else {
                                LOG.error("Error trying to add (COUNT) String value to attribute: " 
                                        + name + ". This attribute is not a List of StringS!");
                            }
                        } else if (cyAtts.getType(name) == CyAttributes.TYPE_STRING) {
                            // TODO: handle simple StringS
                            LOG.warn("operation currently not supported!");
                        } else {
                            // supports only lists of StringS -> else error
                            LOG.error("Error trying to add (COUNT) String value to attribute: " 
                                    + name + ". Invalid value type found!");
                        }
                    } else { // instanceof String
                        // COUNT flag supports only String values
                        LOG.error("Value for attribute '" + name + "' is not of type String! " 
                                + "Other types are not supported, ignoring value!");
                    }
                } else { // unknown flag type or no flag at all
                    LOG.error("Discovered unhanded flag type: " + flag);
                }
            }
        }
    }
    
    /**
     * method to set a name value pair for a specified element.
     * currently handels just the 4 basic Datatyps: Boolean, Integer, Double, String
     * @param cyAtts the cytoscape attributes to add the new values
     * @param element the element of the graph that has these attributes
     * @param name attribute name 
     * @param value attribute value
     */
    private static void setAttribute(CyAttributes cyAtts, String element, 
            String name, Object value) {
        // determine type of the value to set, check type of an potentially existing value
        // (if there exists no attribute with that name the getType method returns TYPE_UNDEFINED)
        // call the appropriate setAttribute method accordinng to the type of the value
        if (value instanceof Boolean 
                && (cyAtts.getType(name) == CyAttributes.TYPE_BOOLEAN 
                        || cyAtts.getType(name) == CyAttributes.TYPE_UNDEFINED)) {
            cyAtts.setAttribute(element, name, (Boolean) value);
        } else if (value instanceof Integer 
                && (cyAtts.getType(name) == CyAttributes.TYPE_INTEGER 
                        || cyAtts.getType(name) == CyAttributes.TYPE_UNDEFINED)) {
            cyAtts.setAttribute(element, name, (Integer) value);
        } else if (value instanceof Double 
                && (cyAtts.getType(name) == CyAttributes.TYPE_FLOATING
                        || cyAtts.getType(name) == CyAttributes.TYPE_UNDEFINED)) {
            cyAtts.setAttribute(element, name, (Double) value);
        } else if (value instanceof String 
                && (cyAtts.getType(name) == CyAttributes.TYPE_STRING
                        || cyAtts.getType(name) == CyAttributes.TYPE_UNDEFINED)) {
            cyAtts.setAttribute(element, name, (String) value);
        } else {  // set list as well ?
            LOG.error("Error setting value " + value + " for attribute: " + name 
                    + ". Possible value type missmatch or unhandled type.");
        }
    }

    /**
     * Convert a Collection of flymine FlyNodeS into a Collection of cytoscape CyNodeS
     * currently only works if the nodes are already in cytoscape????
     * @param flyNodes Collection of FlyNodeS to convert into CyNodeS
     * @return a Collection of corresponding CyNodeS
     */
    public static Collection convertNodesFly2Cy(Collection flyNodes) {
        // TODO: check that collection contains only flyNodes
        Collection cyNodes = new ArrayList();
        for (Iterator iter = flyNodes.iterator(); iter.hasNext();) {
            FlyNode fn = (FlyNode) iter.next();
            // call getCyNode with create flag = true! otherwise CyNodeS will NOT be created
            // existing CyNodeS however are not created
            CyNode cyn = Cytoscape.getCyNode(fn.getLabel(), true);
            cyNodes.add(cyn);
        }
        return cyNodes;
    }

    /**
     * Convert a Collection of flymine FlyEdgeS into a Collection of cytoscape CyEdgeS
     * currently only works if the edges are already in cytoscape????
     * @param flyEdges Collection of FlyEdgeS to convert into CyEdgeS
     * @return a Collection of corresponding CyEdgeS
     */
    public static Collection convertEdgesFly2Cy(Collection flyEdges) {
        Collection cyEdges = new ArrayList();
        for (Iterator iter = flyEdges.iterator(); iter.hasNext();) {
            FlyEdge fe = (FlyEdge) iter.next();

            CyEdge cye = Cytoscape.getCyEdge(fe.getSource().getLabel(), fe.getLabel(), fe
                    .getTarget().getLabel(), (String) fe
                    .getAttributeValue(Semantics.INTERACTION));

            // TODO: check if Edge really exists
            cyEdges.add(cye);
        }
        return cyEdges;

    }

    /**
     * Convert a Collection of flymine FlyNodeS into a int array of cytoscape CyNode ideces
     * currently only works if the nodes are already in cytoscape????
     * @param flyNodes Collection of FlyNodeS to convert into CyNode indeces
     * @return a int[] representing cytoscape CyNode ideces
     */
    public static int[] iConvertNodesFly2Cy(Collection flyNodes) {
        int[] cyNodes = new int[flyNodes.size()];
        int count = 0;
        for (Iterator iter = flyNodes.iterator(); iter.hasNext();) {
            FlyNode fn = (FlyNode) iter.next();
            CyNode cyn = Cytoscape.getCyNode(fn.getLabel());
            // TODO: check if Node really exists
            // what if not?? -> error or create new or ...
            cyNodes[count] = cyn.getRootGraphIndex();
            count++;
        }
        return cyNodes;
    }

    /**
     * Convert a Collection of flymine FlyEdgeS into a int array of cytoscape CyEdge indeces
     * currently only works if the edges are already in cytoscape????
     * @param flyEdges Collection of FlyEdgeS to convert into CyEdge indeces
     * @return a in[] representing cytoscape CyEdge ideces
     */
    public static int[] iConvertEdgesFly2Cy(Collection flyEdges) {
        int[] cyEdges = new int[flyEdges.size()];
        int count = 0;
        for (Iterator iter = flyEdges.iterator(); iter.hasNext();) {
            FlyEdge fe = (FlyEdge) iter.next();
            CyEdge cye = Cytoscape.getCyEdge(fe.getSource().getLabel(), fe.getLabel(), fe
                    .getTarget().getLabel(), (String) fe
                    .getAttributeValue(Semantics.INTERACTION));
            // TODO: check if Edge really exists
            cyEdges[count] = cye.getRootGraphIndex();
            count++;
        }
        return cyEdges;

    }

}
