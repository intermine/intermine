package org.flymine.dataloader;

/**
 * Loads information from a data source into the FlyMine database.
 *
 * @author Richard Smith
 */

public interface DataLoader
{

    /**
     * Run the DataLoader.
     */
    public void process();

}
