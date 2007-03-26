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


/**
 * Class representing a edge between two nodes
 * @author Florian Reisinger
 */
public class FlyEdge extends FlyHashGraphElement
{

    private static final long serialVersionUID = 9999902901901L;
    private FlyNode source;
    private FlyNode target;

    /**
     * Constructs a FlyEdge linking two FlyNodeS. 
     * Will use the default interaction type FlyNetwork.DEFAULT_INTERACTION_TYPE
     * and will construct a default label
     * @param source source node
     * @param target target node
     */
    public FlyEdge(FlyNode source, FlyNode target) {
        super(source.getLabel() + " (" + FlyNetwork.DEFAULT_INTERACTION_TYPE + ") "
                + target.getLabel());
        this.source = source;
        this.target = target;
        // the attribute "interaction" is used by cytoscape's visual mapper
        // to determine the visual apperance of the edge
        this.setAttribute(FlyNetwork.INTERACTION, FlyNetwork.DEFAULT_INTERACTION_TYPE);

    }

    /**
     * Constructs a FlyEdge linking two FlyNodeS. 
     * Will construct a default label
     * @param source source node
     * @param target target node
     * @param interactionType type of the interaction
     */
    public FlyEdge(FlyNode source, FlyNode target, String interactionType) {
        super("tmp");
        this.label = source.getLabel() + " (" + interactionType + ") "
                + target.getLabel();
        this.source = source;
        this.target = target;
        this.setAttribute(FlyNetwork.INTERACTION, interactionType);
    }

    /**
     * Constructs a FlyEdge linking two FlyNodeS. 
     * If interactionType is null the default is used
     * @param source source node
     * @param target target node
     * @param interactionType type of interaction
     * @param label name or identifier of the edge
     */
    public FlyEdge(FlyNode source, FlyNode target, String interactionType, String label) {
        super(label);
        if (interactionType == null) {
            interactionType = FlyNetwork.DEFAULT_INTERACTION_TYPE;
        }
        this.source = source;
        this.target = target;
        this.setAttribute(FlyNetwork.INTERACTION, interactionType);
    }

    /**
     * @return the source node
     */
    public FlyNode getSource() {
        return source;
    }

    /**
     * @return the target node
     */
    public FlyNode getTarget() {
        return target;
    }

    /**
     * Compares the specified edge with this edge for equality.
     * This will check if both edges have the same target and source 
     * nodes and will call the inherited equals method from 
     * FlyHashGraphElement to compare the labels and attributes.
     * @param e edge to compare with
     * @return true if both edges contain the same information
     * @see FlyHashGraphElement#isEqual(FlyHashGraphElement)
     */
    public boolean isEqual(FlyEdge e) {
        if (!this.source.isEqual(e.source)) {
            return false;
        }
        if (!this.target.isEqual(e.target)) {
            return false;
        }
        return ((FlyHashGraphElement) this).isEqual((FlyHashGraphElement) e);
    }

    /**
     * @see java.lang.Object#toString()
     * @return a String representing this FlyEdge
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Edge: " + this.source.label + " -> " + this.target.label + "\n");
        sb.append("Edge ");
        sb.append(super.toString());
        sb.append("source node: ");
        sb.append(this.source.toString());
        sb.append("target node: ");
        sb.append(this.target.toString());
        return sb.toString();
    }
}
