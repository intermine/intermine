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
 * @author Florian Reisinger
 *
 */
public class FlyNode extends FlyHashGraphElement 
    {
    
    private static final long serialVersionUID = 9999902903901L;

    /**
     * Constructs a new FlyNode
     * @param label name or identifier of this node
     */
    public FlyNode(String label) {
        super(label);
    }

    /**
     * Compares the specified node with this node for equality.
     * @param n node to compare with
     * @return true if both nodes contain the same information
     * @see FlyHashGraphElement#isEqual(FlyHashGraphElement)
     */
    public boolean isEqual(FlyNode n) {
        return ((FlyHashGraphElement) this).isEqual((FlyHashGraphElement) n);
    }

    /**
     * @see java.lang.Object#toString()
     * @return a String representing this FlyNode
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Node ");
        sb.append(super.toString());
        return sb.toString();
    }

}
