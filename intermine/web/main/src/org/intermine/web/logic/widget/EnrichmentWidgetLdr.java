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

import org.intermine.objectstore.query.Query;

/**
 * Prepares the data and queries for the enrichment widget controller to process.
 * @author Julie Sullivan
 */
public interface EnrichmentWidgetLdr
{
    /**
     * @return the query representing the sample population (the list)
     */
    public Query getAnnotatedSample();

    /**
     * @return the query representing the entire population (all the items in the database)
     */
    public Query getAnnotatedPopulation();

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
     * @return query to return the correct result set for this widget
     */
    public Query getQuery(boolean useBag);
    
}
