package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * Class representing an OBO type.
 *
 * @author Xavier Watkins
 *
 */
public class OboTypeDefinition
{
    private String id;
    private String name;
    boolean isTransitive;
    private List<String> transitiveOver;

    /**
     * The constructor
     *
     * @param id the identifier
     * @param name the name
     * @param isTransitive the transitivity
     */
    public OboTypeDefinition(String id, String name, boolean isTransitive) {
        super();
        this.id = id;
        this.isTransitive = isTransitive;
        this.name = name;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the isTransitive
     */
    public boolean isTransitive() {
        return isTransitive;
    }

    /**
     * @param isTransitive the isTransitive to set
     */
    public void setTransitive(boolean isTransitive) {
        this.isTransitive = isTransitive;
    }

    /**
     * @return the transitiveOver
     */
    public List<String> getTransitiveOver() {
        return transitiveOver;
    }

    /**
     * @param transitiveOver the transitiveOver to set
     */
    public void setTransitiveOver(List<String> transitiveOver) {
        this.transitiveOver = transitiveOver;
    }
}
