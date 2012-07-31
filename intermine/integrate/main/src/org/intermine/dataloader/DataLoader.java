package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Loads information from a data source into the InterMine database.
 * This class defines a member variable referencing an IntegrationWriter, which all DataLoaders
 * require.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public abstract class DataLoader
{
    private IntegrationWriter iw;

    /**
     * Construct a DataLoader
     *
     * @param iw an IntegrationWriter to write to
     */
    public DataLoader(IntegrationWriter iw) {
        this.iw = iw;
    }

    /**
     * Return the IntegrationWriter that was passed to the constructor.
     * @return the IntegrationWriter
     */
    public IntegrationWriter getIntegrationWriter() {
        return iw;
    }
}
