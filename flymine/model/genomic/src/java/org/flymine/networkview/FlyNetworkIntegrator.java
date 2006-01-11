package org.flymine.networkview;

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
import java.util.Collection;
import java.util.Iterator;

import org.flymine.networkview.network.FlyEdge;
import org.flymine.networkview.network.FlyHashGraphElement;
import org.flymine.networkview.network.FlyNetwork;
import org.flymine.networkview.network.FlyNode;

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

    /**
     * Integrates all elements of the flymine network into cytoscape.
     * (Needs to run within Cytoscape)
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
     * @param nodes Collection of flymine nodes that are to be integrated
     */
    protected static void integrateNodes(Collection nodes) {
        CyAttributes cyAtts = Cytoscape.getNodeAttributes();

        // add all nodes in the collection
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            FlyNode n = (FlyNode) iter.next();
            // adding node to cytoscape using cytoscape's getCyNode method
            Cytoscape.getCyNode(n.getLabel(), true);
            // adding attributes of this node
            addAttributes(cyAtts, n);
        }
    }

    /**
     * This will integrate all flymine edges from the Collection into cytoscape
     * @param edges Collection of flymine edges that are to be integrated
     */
    protected static void integrateEdges(Collection edges) {
        CyAttributes cyEdgeAtts = Cytoscape.getEdgeAttributes();

        // add all edges in the collection
        for (Iterator iter = edges.iterator(); iter.hasNext();) {
            FlyEdge e = (FlyEdge) iter.next();
            // adding edge to cytoscape using cytoscape's getCyEdge method
            CyEdge etmp = Cytoscape.getCyEdge(e.getSource().getLabel(), e.getLabel(), e
                    .getTarget().getLabel(), (String) e
                    .getAttributeValue(Semantics.INTERACTION));
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
    protected static void addAttributes(CyAttributes cyAtts, FlyHashGraphElement ge) {
        for (Iterator it = ge.getAttributeNames().iterator(); it.hasNext();) {
            String name = (String) it.next();

            // TODO: better way to do this??
            Object o = ge.getAttributeValue(name);
            if (o instanceof Boolean) {
                cyAtts.setAttribute(ge.getLabel(), name, (Boolean) ge
                        .getAttributeValue(name));
            }
            if (o instanceof Integer) {
                cyAtts.setAttribute(ge.getLabel(), name, (Integer) ge
                        .getAttributeValue(name));
            }
            if (o instanceof Double) {
                cyAtts.setAttribute(ge.getLabel(), name, (Double) ge
                        .getAttributeValue(name));
            }
            if (o instanceof String) {
                cyAtts.setAttribute(ge.getLabel(), name, (String) ge
                        .getAttributeValue(name));
            }
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
