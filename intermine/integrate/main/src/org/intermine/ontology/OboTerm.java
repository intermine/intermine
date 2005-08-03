package org.intermine.ontology;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Extension of DagTerm that adds a namespace attribute that is specific to the OBO
 * format.
 * 
 * @author Thomas Riley
 */
public class OboTerm extends DagTerm
{
    private String namespace;
    
    /**
     * Construct with an id and name.
     * @param id the id of this DAG term, may not be changed after construction
     * @param name a name for this DAG term
     */
    public OboTerm(String id, String name) {
        super(id, name);
    }
    
    /**
     * Get the namespace attribute.
     * @return term namespace
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Set the namespace attribute.
     * @param namespace the term namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
