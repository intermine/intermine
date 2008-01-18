package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.objectstore.query.Query;

import org.intermine.objectstore.ObjectStore;


/**
 * Prepares the data and queries for the enrichment widget controller to process.   
 * @author Julie Sullivan
 */
public interface EnrichmentWidgetLdr
{
    /**
     * @return the query representing the sample population (the list)
     */
    public Query getSample();

    /**
     * @return the query representing the entire population (all the items in the database)
     */
    public Query getPopulation();

    /**
     * @return description of reference population, ie "Accounting dept"
     */
    public Collection<String> getReferencePopulation();

    /**
     * @param os the object store
     * @return the total number of objects in the database
     */
    public int getTotal(ObjectStore os);

    /**
     *  this was used for tiffin.  obsolete? 
     * @return if the widget should have an external link, where it should go to
     */
    public String getExternalLink();

    /**
     * this was used for tiffin.  obsolete?
     * @return the string to append to the end of external link
     */
    public String getAppendage();
    
}
