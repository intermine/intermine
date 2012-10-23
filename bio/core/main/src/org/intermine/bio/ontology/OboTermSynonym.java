package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2012 FlyMine
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
public class OboTermSynonym
{
    private String name, type;

    /**
     * Create a new instance of DagTermSynonym.
     * @param name the synonym name
     * @param type synonym type
     */
    public OboTermSynonym(String name, String type) {
        this.type = type;
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
     * Get the synonym type.
     * @return synonym type
     */
    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof OboTermSynonym
                && ((OboTermSynonym) o).type.equals(type)
                && ((OboTermSynonym) o).name.equals(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode() + 3 * type.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).append("type", type).toString();
    }
}
