package org.intermine.bio.ontology;

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
 * Object representing aa OboTerm synonym.
 * 
 * @author Thomas Riley
 */
public class OboTermSynonym extends DagTermSynonym
{
    private String type;
    
    /**
     * Create a new instance of DagTermSynonym.
     * @param name the synonym name
     * @param type synonym type
     */
    public OboTermSynonym(String name, String type) {
        super(name);
        this.type = type;
    }
    
    /**
     * Get the synonym type.
     * @return synonym type
     */
    public String getType() {
        return type;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return (o instanceof OboTermSynonym && super.equals(o)
                && ((OboTermSynonym) o).type.equals(type));
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return super.hashCode() + 3 * type.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).append("type", type).toString();
    }
}
