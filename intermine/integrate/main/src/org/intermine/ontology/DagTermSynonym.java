package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Object representing a DagTerm synonym.
 * 
 * @author Thomas Riley
 */
public class DagTermSynonym
{
    private String name;
    
    /**
     * Create a new instance of DagTermSynonym.
     * @param name the synoym name
     */
    public DagTermSynonym(String name) {
        this.name = name;
    }
    
    /**
     * Get the synonym name.
     * @return synonym name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @see Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return (o instanceof DagTermSynonym && ((DagTermSynonym) o).name.equals(name));
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return name.hashCode();
    }
    
    /**
     * @see Object#toString()
     */
    public String toString() {
        return new ToStringBuilder(this).append("name", name).toString();
    }
}
