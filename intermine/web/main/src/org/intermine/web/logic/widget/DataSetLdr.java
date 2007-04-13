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

import java.util.Map;


/**
 * An Interface which defines a frame
 * in which data can be retrieved, organised and created
 * to then be passed to a JFreeChart for representation
 * @author Xavier Watkins
 *
 */
public interface DataSetLdr
{    
    /**
     * @return the map [String, GraphDataSet]
     */
    public Map getDataSets();
}
