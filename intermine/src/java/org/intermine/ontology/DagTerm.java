package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;

/**
 * Class to represent a DAG term
 * @author Mark Woodbridge
 */
public class DagTerm
{
    protected String id;
    protected String name;
    protected Set children = new HashSet();
    protected Set synonyms = new HashSet();
    /**
    * Constructor
    * @param id the id of the term
    * @param name the name of the term
    */
    public DagTerm(String id, String name) {
        this.id = id;
        this.name = name;
    }
    /**
     * Get the id of this term
     * @return the id
     */
    public String getId() { return id; }
    /**
     * Get the name of this term
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Get the synonyms of this term
     * @return the synonyms
     */
    public Set getSynonyms() { return synonyms; }
    /**
     * Get the children of this term
     * @return the children
     */
    public Set getChildren() { return children; }
}
