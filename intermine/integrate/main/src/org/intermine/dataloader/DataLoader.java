package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.DataConverterStoreHook;

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
    private DataConverterStoreHook storeHook;

    /**
     * No-arg constructor for testing purposes
     */
    private DataLoader() {
        // empty
    }

    /**
     * Set a hook for this converter that will be called just before each Item is stored.
     * The processItem() method in DataConverterStoreHook will be passed the Item, which
     * it can modify.
     * @param dataConverterStoreHook the hook
     */
    public void setStoreHook(DataConverterStoreHook dataConverterStoreHook) {
        this.storeHook = dataConverterStoreHook;
    }

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
