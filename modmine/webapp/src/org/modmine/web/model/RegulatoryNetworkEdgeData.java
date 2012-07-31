package org.modmine.web.model;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.web.model.CytoscapeNetworkEdgeData;

/**
 * This class extends CytoscapeNetworkEdgeData with specific feature for regulatory network.
 *
 * @author Fengyuan Hu
 *
 */
public class RegulatoryNetworkEdgeData extends CytoscapeNetworkEdgeData
{
    /**
     * @param obj a CytoscapeNetworkEdgeData object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return (this.getSourceId() + "-" + this.getTargetId()).hashCode();
    }
}
