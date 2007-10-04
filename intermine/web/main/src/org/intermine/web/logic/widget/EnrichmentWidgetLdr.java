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
 * An Interface which defines a frame
 * in which data can be retrieved, organised and created
 * to then be passed to a widget for representation
 * @author julie sullivan
 *
 */
public interface EnrichmentWidgetLdr
{    
    /**
     * @return the query representing the sample population (the bag)
     */
    public Query getSample();
    
    /**
     * @return the query representing the entire population (all the items in the database)
     */
    public Query getPopulation();

    /**
     * 
     * @param os
     * @param bag
     * @return description of reference population, ie "Accounting dept"
     */
    public Collection getReferencePopulation();
    
    /** 
     * @param os     
     * @return the query representing the sample population (the bag)
     */
    public int getTotal(ObjectStore os);
    
    /**
     * @return if the widget should have an external link, where it should go to
     */
    public String getExternalLink();
    
    /**
     * 
     * @return the string to append to the end of external link
     */
    public String getAppendage();
}
