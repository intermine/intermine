package org.intermine.bio.dataconversion;

/**
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

/**
 * A ChadoDBConverter that automatically handles setting of DataSet and DataSource references.
 * @author Kim Rutherford
 */
public class DataSetChadoDBConverter extends ChadoDBConverter
{
    private String dataSourceName;
    private String dataSetTitle;

    /**
     * Create a new DataSetChadoDBConverter.
     * @param database the Database to pass to the super class
     * @param tgtModel the Model to pass to the super class
     * @param writer the ItemWriter to pass to the super class
     */
    public DataSetChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process() throws Exception {
        if (dataSourceName == null) {
            throw new RuntimeException("dataSourceName not set in DataSetChadoDBConverter");
        }
        if (dataSetTitle == null) {
            throw new RuntimeException("dataSetTitle not set in DataSetChadoDBConverter");
        }

        setStoreHook(new DataSetStoreHook(getModel(), getDataSetItem(), getDataSourceItem()));
        super.process();
    }

    /**
     * Set the name of the DataSet Item to create for this converter.
     * @param title the title
     */
    public void setDataSetTitle(String title) {
        this.dataSetTitle = title;
    }

    /**
     * Set the name of the DataSource Item to create for this converter.
     * @param name the name
     */
    public void setDataSourceName(String name) {
        this.dataSourceName = name;
    }

    /**
     * Return the data source name set by setDataSourceName().
     * @return the data source name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Return the DataSet Item created from the dataSetTitle.
     * @return the DataSet Item
     */
    public Item getDataSetItem() {
        return getDataSetItem(dataSetTitle, getDataSourceItem());
    }

    /**
     * Return the DataSource Item created from the dataSourceName.
     * @return the DataSource Item
     */
    public Item getDataSourceItem() {
        return getDataSourceItem(dataSourceName);
    }
}
