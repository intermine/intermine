package org.intermine.bio.chado;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

/**
 * Objects of this class represent one row of the chado cvterm table.
 * @author Kim Rutherford
 */
public class ChadoCVTerm
{
    private String name;
    private Set<ChadoCVTerm> directParents = new HashSet<ChadoCVTerm>();
    private Set<ChadoCVTerm> directChildren = new HashSet<ChadoCVTerm>();

    /**
     * Create a new cv term object
     * @param name the name (from the chado cvterm table)
     */
    public ChadoCVTerm(String name) {
        this.name = name;
    }

    /**
     * Return the name of this term.
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Set the name of this term.
     * @param name the name to set
     */
    public final void setName(String name) {
        this.name = name;
    }


    /**
     * Get the direct parents of this term.
     * @return the direct parents
     */
    public final Set<ChadoCVTerm> getDirectParents() {
        return directParents;
    }

    /**
     * Set the direct parents of this term.
     * @param directParents the parents to set
     */
    public final void setDirectParents(Set<ChadoCVTerm> directParents) {
        this.directParents = directParents;
    }

    /**
     * Get the direct children of this term.
     * @return the direct children
     */
    public final Set<ChadoCVTerm> getDirectChildren() {
        return directChildren;
    }

    /**
     * Set the direct children of this term.
     * @param directChildren the children to set
     */
    public final void setDirectChildren(Set<ChadoCVTerm> directChildren) {
        this.directChildren = directChildren;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChadoCVTerm) {
            return ((ChadoCVTerm) obj).name.equals(name);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
