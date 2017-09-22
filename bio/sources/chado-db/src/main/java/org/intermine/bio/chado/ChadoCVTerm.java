package org.intermine.bio.chado;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

import java.lang.ref.WeakReference;

/**
 * Objects of this class represent one row of the chado cvterm table.
 * @author Kim Rutherford
 */
public class ChadoCVTerm
{
    private String name;
    private Set<ChadoCVTerm> directParents = new HashSet<ChadoCVTerm>();
    private Set<ChadoCVTerm> directChildren = new HashSet<ChadoCVTerm>();
    private WeakReference<Set<ChadoCVTerm>> allParentsRef = null;
    private WeakReference<Set<ChadoCVTerm>> allChildrenRef = null;

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
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Return a Set of all the parent ChadoCVTerms of this term.
     * @return all the parent terms
     */
    public Set<ChadoCVTerm> getAllParents() {
        Set<ChadoCVTerm> allParents = null;
        if (allParentsRef != null) {
            allParents = allParentsRef.get();
        }
        if (allParents == null) {
            allParents = new HashSet<ChadoCVTerm>();
            for (ChadoCVTerm parent: getDirectParents()) {
                allParents.addAll(parent.getAllParents());
            }
            allParents.addAll(getDirectParents());
            allParentsRef = new WeakReference<Set<ChadoCVTerm>>(allParents);
        }
        return allParents;
    }

    /**
     * Return a Set of all the child ChadoCVTerms of this term.
     * @return all the child terms
     */
    public Set<ChadoCVTerm> getAllChildren() {
        Set<ChadoCVTerm> allChildren = null;
        if (allChildrenRef != null) {
            allChildren = allChildrenRef.get();
        }
        if (allChildren == null) {
            allChildren = new HashSet<ChadoCVTerm>();
            for (ChadoCVTerm child: getDirectChildren()) {
                allChildren.addAll(child.getAllChildren());
            }
            allChildren.addAll(getDirectChildren());
            allChildrenRef = new WeakReference<Set<ChadoCVTerm>>(allChildren);
        }
        return allChildren;
    }

}
