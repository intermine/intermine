package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.DataConverterStoreHook;
import org.intermine.xml.full.Item;

/**
 * An implementation of DataConverterStoreHook that adds DataSet and DataSource references and
 * collections to Items as they are stored.
 * @author Kim Rutherford
 */
public class DataSetStoreHook implements DataConverterStoreHook
{

    private final Item dataSet;
    private final Item dataSource;

    /**
     * @param dataSet the DataSet to add to items
     * @param dataSource the DataSource to add to the items
     */
    public DataSetStoreHook(Item dataSet, Item dataSource) {
        this.dataSet = dataSet;
        this.dataSource = dataSource;
    }

    /**
     * @see DataSetStoreHook#setDataSets(Item, Item, Item)
     * {@inheritDoc}
     */
    public void processItem(DataConverter dataConverter, Item item) {
        setDataSets(item, dataSet.getIdentifier(), dataSource.getIdentifier());
    }

    /**
     * Do the work of processItem() by setting DataSet and DataSource references and collections
     * on the given Item.
     * @param item the Item to process
     * @param dataSetId the item id of the DataSet to add
     * @param dataSourceId the item id of the DataSource to add
     */
    public static void setDataSets(Item item, String dataSetId, String dataSourceId) {
        if (item.canHaveReference("dataSource") && !item.hasReference("dataSource")) {
            item.setReference("dataSource", dataSourceId);
        }
        if (item.canHaveCollection("evidence")) {
            item.addToCollection("evidence", dataSetId);
        }
        if (item.canHaveCollection("dataSets")) {
            item.addToCollection("dataSets", dataSetId);
        }
    }
}
