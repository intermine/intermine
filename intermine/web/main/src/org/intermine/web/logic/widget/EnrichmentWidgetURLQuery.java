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

import org.intermine.objectstore.query.Query;

/**
 * @author julie sullivan
 *
 */
public interface EnrichmentWidgetURLQuery
{    
    /**
     * @return the query 
     */
    public Query getQuery();
    
}
