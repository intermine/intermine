package org.intermine.web.bag;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * Configuration for BagQuery objects.
 * @author Kim Rutherford
 */
public class BagQueryConfig
{
    private String connectField;
    private String extraConstraintClassName;
    private String constrainField;
    private final Map bagQueries;

    /**
     * Create a new BagQueryConfig object.
     * @param bagQueries a Map from class name to bag query
     */
    public BagQueryConfig(Map bagQueries) {
        this.bagQueries = bagQueries;
    }

    /**
     * Return the class name that was passed to the constructor.  This (and connectField and
     * constrainField) is used to configure the addition of an extra constraint to the bag queries.
     * (eg. constraining the Organism)
     * @return the extra class name
     */
    public String getExtraConstraintClassName() {
        return extraConstraintClassName;
    }

    /**
     * Set the class name of extra constraint to use in BagQuery objects using this config object.
     * @see #getExtraConstraintClassName()
     * @param extraConstraintClassName the class name
     */
    public void setExtraConstrintClassName(String extraConstraintClassName) {
        this.extraConstraintClassName = extraConstraintClassName;
    }
    
    /**
     * Return the connecting field.
     * @see #getExtraConstraintClassName()
     * @return the connecting field
     */
    public String getConnectField() {
        return connectField;
    }

    /**
     * Set the connecting field for adding an extra constraint to bag queries.
     * @see #getExtraConstraintClassName()
     * @param connectField
     */
    public void setConnectField(String connectField) {
        this.connectField = connectField;
    }

    /**
     * Return the constrain field.
     * @see #getExtraConstraintClassName()
     * @return the constrain field
     */
    public String getConstrainField() {
        return constrainField;
    }

    /**
     * Set the field to constrain when making an extra constraint.
     * @see #getExtraConstraintClassName()
     * @param constrainField
     */
    public void setConstrainField(String constrainField) {
        this.constrainField = constrainField;
    }
    
    /**
     * Return a Map from type name to a List of BagQuerys to run for that type
     * @return the BagQuerys Map
     */
    public Map getBagQueries() {
        return bagQueries;
    }
}
