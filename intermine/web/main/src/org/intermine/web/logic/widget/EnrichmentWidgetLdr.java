package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;

import org.intermine.objectstore.query.Query;

/**
 * Prepares the data and queries for the enrichment widget controller to process.
 * @author Julie Sullivan
 */
public interface EnrichmentWidgetLdr
{
    /**
     * @param calcTotal whether or not to calculate the total number of annotated objects in the
     * sample 
     * @return the query representing the sample population (the list)
     */
    public Query getAnnotatedSampleQuery(boolean calcTotal);
    
    /**
     * @param calcTotal whether or not to calculate the total number of annotated objects in the
     * database 
     * @return the query representing the entire population (all the items in the database)
     */
    public Query getAnnotatedPopulationQuery(boolean calcTotal);

    /**
     * @param keys the keys to the records to be exported
     * @return the query representing the records to be exported
     */
    public Query getExportQuery(List<String> keys);
    
    /**
     * @return description of reference population, ie "Accounting dept"
     */
    public Collection<String> getPopulationDescr();
    
    /**    
     * @return if the widget should have an external link, where it should go to
     */
    public String getExternalLink();

    /**
     * this was used for tiffin.  obsolete?
     * @return the string to append to the end of external link
     */
    public String getAppendage();
    
    /**
     * returns the relevant query.  this method is used for all 4 queries:
     * 
     * k = total annotated with this term in bag
     * n = total annotated with any term in bag (used to be bag.count)
     * M = total annotated with this term in reference population
     * N = total annotated with any term in reference population
     * @param useBag whether or not to use the bag in the query
     * @param calcTotal whether or not to return the results or return the count
     * @param keys the keys of the records to be exported
     * @return query to return the correct result set for this widget
     */
    abstract Query getQuery(boolean calcTotal, boolean useBag, List<String> keys);
    
}
