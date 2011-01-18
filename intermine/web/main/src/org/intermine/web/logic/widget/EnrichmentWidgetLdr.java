package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.query.Query;

/**
 * Prepares the data and queries for the enrichment widget controller to process.
 * @author Julie Sullivan
 */
public class EnrichmentWidgetLdr
{
    protected String externalLink, append;
    protected InterMineBag bag;

    // TODO this should be moved to bio
    protected Collection<String> organisms = new ArrayList<String>();
    protected Collection<String> organismsLower = new ArrayList<String>();

    /**
     * @return if the widget should have an external link, where it should go to
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * this was used for tiffin.  obsolete?
     * @return the string to append to the end of external link
     */
    public String getAppendage() {
        return append;
    }

    /**
     * @return description of reference population, ie "Accounting dept"
     */
    public Collection<String> getPopulationDescr() {
        // TODO this needs to be moved to bio
        return organisms;
    }

    /**
     * returns the relevant query.  this method is used for 6 widget queries.
     *
     * export query:
     *
     *      select identifier and term where key = what the user selected on the widget
     *
     * analysed query:
     *
     *      select object.id where object is used in query
     *
     *  the results of this query are used as a NOT_IN constraint in a pathquery.  the pathquery
     *  is run when the user clicks on the 'not analysed' number on the widget.
     *
     * population query:
     *
     *     M = total annotated with this term in reference population
     *
     * annotated population query:
     *
     *     N = total annotated with any term in reference population
     *
     * sample query:
     *
     *     k = total annotated with this term in bag
     *
     * annotated sample query:
     *
     *     n = total annotated with any term in bag (used to be bag.count)
     *
     * @param keys the keys of the records to be exported
     * @param action which query to be built.
     * @return query to return the correct result set for this widget
     */
    public Query getQuery(String action, List<String> keys)  {
        return null;
    }

    /**
     * @param calcTotal whether or not to calculate the total number of annotated objects in the
     * sample
     * @return the query representing the sample population (the list)
     */
    public Query getSampleQuery(boolean calcTotal) {
        String action = calcTotal ? "sampleTotal" : "sample";
        return getQuery(action, null);
    }

    /**
     * @param calcTotal whether or not to calculate the total number of annotated objects in the
     * database
     * @return the query representing the entire population (all the items in the database)
     */
    public Query getPopulationQuery(boolean calcTotal) {
        String action = calcTotal ? "populationTotal" : "population";
        return getQuery(action, null);
    }

    /**
     * @param keys the keys to the records to be exported
     * @return the query representing the records to be exported
     */
    public Query getExportQuery(List<String> keys) {
        return getQuery("export", keys);
    }
}
