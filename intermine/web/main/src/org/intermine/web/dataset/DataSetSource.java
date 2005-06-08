package org.intermine.web.dataset;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * 
 * @author Thomas Riley
 */
public class DataSetSource
{
    /** Name of source. */
    public String sourceName;

    /**
     * @return Returns the sourceName.
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * @param sourceName The sourceName to set.
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
}
