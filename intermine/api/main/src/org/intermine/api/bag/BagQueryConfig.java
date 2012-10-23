package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for BagQuery objects.
 * @author Kim Rutherford
 */
public class BagQueryConfig
{
    private String connectField;
    private String extraConstraintClassName;
    private String constrainField;
    private final Map<String, List<BagQuery>> bagQueries;
    private final Map<String, List<BagQuery>> preDefaultBagQueries;
    private Map<String, Set<AdditionalConverter>> additionalConverters;
    private boolean matchOnFirst = true;

    /**
     * Create a new BagQueryConfig object.
     * @param bagQueries a Map from class name to bag query
     * @param preDefaultBagQueries a separate map of queries to run before the default
     * @param additionalConverters extra converters configured - see getAdditionalConverters
     */
    public BagQueryConfig(Map<String, List<BagQuery>> bagQueries,
                          Map<String, List<BagQuery>> preDefaultBagQueries,
                          Map<String, Set<AdditionalConverter>> additionalConverters) {
        this.bagQueries = bagQueries;
        this.preDefaultBagQueries = preDefaultBagQueries;
        this.additionalConverters = additionalConverters;
    }

    /**
     * Return the class name that was passed to the constructor.  This (and connectField and
     * constrainField) is used to configure the addition of an extra constraint to the bag queries.
     * (eg. constraining the Organism).
     *
     * @return the extra class name
     */
    public String getExtraConstraintClassName() {
        return extraConstraintClassName;
    }

    /**
     * Set the class name of extra constraint to use in BagQuery objects using this config object.
     *
     * @param extraConstraintClassName the class name
     */
    public void setExtraConstraintClassName(String extraConstraintClassName) {
        this.extraConstraintClassName = extraConstraintClassName;
    }

    /**
     * Return the connecting field.
     *
     * @return the connecting field
     */
    public String getConnectField() {
        return connectField;
    }

    /**
     * Set the connecting field for adding an extra constraint to bag queries.
     *
     * @param connectField the field name
     */
    public void setConnectField(String connectField) {
        this.connectField = connectField;
    }

    /**
     * Return the constrain field.
     *
     * @return the constrain field
     */
    public String getConstrainField() {
        return constrainField;
    }

    /**
     * Set the field to constrain when making an extra constraint.
     *
     * @param constrainField the constraint field
     */
    public void setConstrainField(String constrainField) {
        this.constrainField = constrainField;
    }

    /**
     * Return a list of BagQuerys to run for a given type
     * @param type the type to fetch queries for
     * @return the BagQuerys Map
     */
    public List<BagQuery> getBagQueries(String type) {
        List<BagQuery> bqs = bagQueries.get(type);
        return ((bqs == null) ? new ArrayList<BagQuery>() : bqs);
    }

    /**
     * Return a List of BagQuerys for the given type that should be run before the
     * default query
     * @param type the type to fetch queries for
     * @return the BagQuerys Map
     */
    public List<BagQuery> getPreDefaultBagQueries(String type) {
        List<BagQuery> bqs = preDefaultBagQueries.get(type);
        return ((bqs == null) ? new ArrayList<BagQuery>() : bqs);
    }

    /**
     * Return a Map from a converter Class name (such as
     * org.intermine.bio.web.logic.OrthologueConverter) to set of additional converters.
     *
     * Used on the list analysis page to convert to different type, eg. orthologues
     *
     * @param type get converters for this type or a subtype of it
     * @return the additionalConverters
     */
    public Set<AdditionalConverter> getAdditionalConverters(String type) {
        return additionalConverters.get(type);
    }

    /**
     * If flag is true, bagqueryrunner queries for identifier until match is found, returning
     * only that first matching record.  If flag is false, bagqueryrunner queries for identifier
     * using ALL queries, returning ALL matches.
     * @param matchOnFirst the flag to set
     */
    public void setMatchOnFirst(boolean matchOnFirst) {
        this.matchOnFirst = matchOnFirst;
    }

    /**
     * If flag is true, bagqueryrunner queries for identifier until match is found, returning
     * only that first matching record.  If flag is false, bagqueryrunner queries for identifier
     * using ALL queries, returning ALL matches.
     * @return the matchOnFirst
     */
    public boolean getMatchOnFirst() {
        return matchOnFirst;
    }
}
