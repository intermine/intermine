package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;

/**
 * This is a Java Bean to hold Cytoscape Web data.
 * Easy to be extended.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeNetworkData
{
    private String interactionString;
    private LinkedHashSet<String> dataSources;
    private LinkedHashSet<String> interactionShortNames;

    /**
     *
     * @return interactionString
     */
    public String getInteractionString() {
        return interactionString;
    }

    /**
     *
     * @param interactionString a record in format of "source\tinteractionType\ttarget"
     */
    public void setInteractionString(String interactionString) {
        this.interactionString = interactionString;
    }

    /**
     *
     * @return dataSources
     */
    public LinkedHashSet<String> getDataSources() {
        return dataSources;
    }

    /**
     *
     * @param dataSources FlyMine, etc...
     */
    public void setDataSources(LinkedHashSet<String> dataSources) {
        this.dataSources = dataSources;
    }

    /**
     *
     * @return interactionShortNames
     */
    public LinkedHashSet<String> getInteractionShortNames() {
        return interactionShortNames;
    }

    /**
     *
     * @param interactionShortNames shor name of an interaction
     */
    public void setInteractionShortNames(LinkedHashSet<String> interactionShortNames) {
        this.interactionShortNames = interactionShortNames;
    }

}
