package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

/**
 * @author Jakub Kulaviak
 **/
public class PubMedReference
{

    private Map<Integer, List<Integer>> references;

    private Integer organism;

    /**
     * Constructor.
     * @param organism id of organism of which references this object carries
     * @param references references between id of gene and ids of publications in PubMed
     */
    public PubMedReference(Integer organism, Map<Integer, List<Integer>> references) {
        this.organism = organism;
        this.references = references;
    }

    /**
     * @return references
     * {@link #PubMedReference(Integer, Map)}
     */
    public Map<Integer, List<Integer>> getReferences() {
        return references;
    }

    /**
     * @param references references
     * {@link #PubMedReference(Integer, Map)}
     */
    public void setReferences(Map<Integer, List<Integer>> references) {
        this.references = references;
    }

    /**
     * @return organism
     * {@link #PubMedReference(Integer, Map)}
     */
    public Integer getOrganism() {
        return organism;
    }

    /**
     *
     * @param organism organism
     * {@link #PubMedReference(Integer, Map)}
     */
    public void setOrganism(Integer organism) {
        this.organism = organism;
    }
}
