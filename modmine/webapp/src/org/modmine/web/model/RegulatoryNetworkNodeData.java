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

import java.util.Map;

import org.intermine.bio.web.model.CytoscapeNetworkNodeData;

/**
 * This class extends CytoscapeNetworkNodeData with specific feature for regulatory network.
 *
 * @author Fengyuan Hu
 *
 */
public class RegulatoryNetworkNodeData extends CytoscapeNetworkNodeData
{
    private String featueType; //e.g. miRNA/TF
    private Map<String, String> position;
}
