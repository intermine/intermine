package org.intermine.bio.networkview.network;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import cytoscape.data.Semantics;

/**
 * flymine representation of a network 
 * used as container for network information used by cytoscape
 * @author Florian Reisinger
 */
public class FlyNetwork implements Serializable
{

    private static final long serialVersionUID = 9999902905901L;
    
    /**
     * name of the interaction type attribute -> "interaction"
     * needs to be the same as the one used by cytoscape
     */
    public static final String INTERACTION = Semantics.INTERACTION;

    /**
     * interaction type used for bait-prey protein-protein-interactions:
     */
    public static final String DEFAULT_INTERACTION_TYPE = "pp";

    /**
     * interaction type used for other protein-protein-interactions:
     */
    public static final String COMPLEX_INTERACTION_TYPE = "pc";

    private Hashtable nodes;

    private Hashtable edges;

    /**
     * Constructs a new empty FlyNetwork
     */
    public FlyNetwork() {
        nodes = new Hashtable();
        edges = new Hashtable();
    }

    /**
     * Will add a FlyNode object to the network, if not already existing.
     * Whether a FlyNode already exists is determined by its label, the
     * label has to be unique within one network.
     * @param n node to be added
     * @return true if adding was successful, false otherwise
     */
    public boolean addNode(FlyNode n) {
        // TODO: add type-check
        if (!containsNode(n)) {
            nodes.put(n.getLabel(), n);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates new node with specified label and will add it to the network, 
     * if not already existing.
     * @param label the label that will be asigned to the new node
     * @return true if adding was successful, false otherwise
     */
    public boolean addNode(String label) {
        FlyNode n = new FlyNode(label);
        return addNode(n);
    }

    /**
     * Will add a new edge to the network, if not already existing. Target and 
     * source node have to be present in the network. To create the edge a default
     * label of the form "source_label (interaction_type) target_label" and the 
     * default interaction type FlyNetwork.DEFAULT_INTERACTION_TYPE are used.
     * @param e edge to be added
     * @return true if edge was successfully added, false otherwise
     */
    public boolean addEdge(FlyEdge e) {
        if (this.containsNode(e.getSource()) && this.containsNode(e.getTarget())
                && !this.containsEdge(e)) {

            // add a copy of the specified edge:
            // create a new Edge with references to the Nodes 
            // already contained in the network instead of 
            // references to objects with the same context
            FlyEdge newEdge = new FlyEdge(this.getNode(e.getSource().getLabel()), this
                    .getNode(e.getTarget().getLabel()), null, e.getLabel());
            // copy all attriute information
            for (Iterator iter = e.getAttributeNames().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                Object value = e.getAttributeValue(name);
                int flag = e.getAttributeFlag(name);
                newEdge.setAttribute(name, value, flag);
            }
            edges.put(newEdge.getLabel(), newEdge);
            return true;
        } else {
            return false;
        }
    }

    /**
     * will add a new FlyEdge to the FlyNetwork, if not already existing.
     * This method will use a default label of the form 
     * "source_label (interaction_type) target_label"
     * and the default interaction type FlyNetwork.DEFAULT_INTERACTION_TYPE.
     * @param source source node of edge
     * @param target target node of edge
     * @return true if edge was successfully added, false otherwise
     */
    public boolean addEdge(FlyNode source, FlyNode target) {
        return this.addEdge(new FlyEdge(source, target));
    }

    /**
     * Will add a new FlyEdge to the FlyNetwork, if not already existing.
     * This method will use the default label.
     * @param source - source node of edge
     * @param target - target node of edge
     * @param interactionType - type of interaction
     * @return - true if edge was successfully added, false otherwise
     */
    public boolean addEdge(FlyNode source, FlyNode target, String interactionType) {
        return addEdge(new FlyEdge(source, target, interactionType));
    }

    /**
     * Will add a new FlyEdge to the FlyNetwork, if not already existing.
     * @param source source node of edge
     * @param target target node of edge
     * @param interactionType type of interaction, set null for default type
     * @param label label of the edge
     * @return true if edge was successfully added, false otherwise
     */
    public boolean addEdge(FlyNode source, FlyNode target, String interactionType,
            String label) {
        return addEdge(new FlyEdge(source, target, interactionType, label));
    }

    /**
     * Checks whether a node already exists in the network.
     * @param label label of the node to search for
     * @return true if a node with the specified label was found
     */
    public boolean containsNode(String label) {
        return nodes.containsKey(label);
    }

    /**
     * Checks whether a node already exists in the network
     * @param n node to search for
     * @return true if network contains node
     */
    public boolean containsNode(FlyNode n) {
        return this.containsNode(n.getLabel());
    }

    /**
     * Checks whether a edge already exists in the network
     * @param label label of the edge to search for
     * @return true if a edge with the specified label was found
     */
    public boolean containsEdge(String label) {
        return edges.containsKey(label);
    }

    /**
     * Checks whether a edge already exists in the network
     * @param e edge to search for
     * @return true if network contains edge
     */
    public boolean containsEdge(FlyEdge e) {
        return this.containsEdge(e.getLabel());
    }

    /**
     * @return a Collection of all nodes
     */
    public Collection getNodes() {
        return nodes.values();
    }

    /**
     * @return a Collection of all edges
     */
    public Collection getEdges() {
        return edges.values();
    }

    /**
     * @param label label of the edge to be returned
     * @return the edge with the specified label or null if non was found
     */
    public FlyEdge getEdge(String label) {
        return (FlyEdge) edges.get(label);
    }

    /**
     * @param label label of the node to be returned
     * @return  the node with the specified label or null if non was found
     */
    public FlyNode getNode(String label) {
        return (FlyNode) nodes.get(label);
    }

    /**
     * Compares two FlyNetworkS.
     * This will check if the networks contain the same elements.
     * Each element in one network need to have the same attributes 
     * as it's corresponding element in the other network.
     * This is not equal to the equals() method and does not change the hashcode() method
     * @param net the network to compare with
     * @return true if the networks contain the same information
     */
    public boolean isEqual(FlyNetwork net) {
        // TODO: optimise?
        Collection netNodes = net.getNodes();
        Collection netEdges = net.getEdges();

        // check whether the number of nodes is the same in both networks
        if (netNodes.size() != nodes.size()) {
            return false;
        }
        // check all nodes for equality
        for (Iterator iter = netNodes.iterator(); iter.hasNext();) {
            FlyNode n = (FlyNode) iter.next();
            if (!n.isEqual((FlyNode) nodes.get(n.getLabel()))) {
                return false;
            }
        }

        // check whether the number of edges is the same in both networks
        if (netEdges.size() != edges.size()) {
            return false;
        }
        // check all edges for equality
        for (Iterator iter = netEdges.iterator(); iter.hasNext();) {
            FlyEdge e = (FlyEdge) iter.next();
            if (!e.isEqual((FlyEdge) edges.get(e.getLabel()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return String containing the SIF representation of the network
     */
    public String toSIF() {
        StringBuffer sb = new StringBuffer();
        // temp. list of nodes to keep track of already processed nodes
        Collection tmpNodes = nodes.values();
        // iterate over all edges (interactions) and add a SIF line for each
        for (Iterator iter = edges.values().iterator(); iter.hasNext();) {
            FlyEdge edge = (FlyEdge) iter.next();
            sb.append(edge.getSource().getLabel() 
                    + "\t" + edge.getAttributeValue(FlyNetwork.INTERACTION) 
                    + "\t" + edge.getTarget().getLabel() + "\n");
            // remove the source and target nodes from temp. list 
            // because they already have been processed -> already in the SIF 
            if (tmpNodes.contains(edge.getSource())) {
                tmpNodes.remove(edge.getSource());
            }
            if (tmpNodes.contains(edge.getTarget())) {
                tmpNodes.remove(edge.getTarget());
            }
        }
        // process all nodes that were not part of a edge (interaction)
        // -> single nodes without any interaction
        if (tmpNodes.size() > 0) {
            for (Iterator iter = tmpNodes.iterator(); iter.hasNext();) {
                FlyNode node = (FlyNode) iter.next();
                sb.append(node.getLabel() + "\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * @see java.lang.Object#toString()
     * @return a String representing this FlyNetwork
     */
    public String toString() {
        return this.toString(false);
    }

    /**
     * @param longVersion flag whether to use a detailed version
     * @return a String representing showing all elements of the network
     */
    public String toString(boolean longVersion) {
        // TODO: display all information (all attributes)
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = nodes.values().iterator(); iter.hasNext();) {
            FlyNode node = (FlyNode) iter.next();
            if (longVersion) {
                sb.append(node);
            } else {
                sb.append("node: " + node.getLabel() + "\n");
            }
        }
        for (Iterator iter = edges.values().iterator(); iter.hasNext();) {
            FlyEdge edge = (FlyEdge) iter.next();
            if (longVersion) {
                sb.append(edge);
            } else {
                sb.append("edge: " + edge.getLabel() + "\n");
            }
        }
        return sb.toString();
    }

}
