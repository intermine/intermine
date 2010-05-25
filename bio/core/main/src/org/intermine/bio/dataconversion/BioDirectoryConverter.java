package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.DirectoryConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * A DirectoryConverter that automatically sets the dataSets collection of
 * objects as they are stored.
 *
 * @author Julie Sullivan
 */

public abstract class BioDirectoryConverter extends DirectoryConverter
{
    private final Map<String, Item> dataSets = new HashMap<String, Item>();
    private final Map<String, Item> dataSources = new HashMap<String, Item>();

    /**
     * Create a new BioDirectoryConverter.
     * @param writer the Writer used to output the resultant items
     * @param model the data model
     * @param dataSourceName the DataSource name
     * @param dataSetTitle the DataSet title
     */
    public BioDirectoryConverter (ItemWriter writer, Model model,
                             String dataSourceName, String dataSetTitle) {
        super(writer, model);

        Item dataSource = null;
        Item dataSet = null;
        if (StringUtils.isNotEmpty(dataSourceName) && StringUtils.isNotEmpty(dataSetTitle)) {
            dataSource = getDataSourceItem(dataSourceName);
            dataSet = getDataSetItem(dataSetTitle, dataSource);
        }
        BioStoreHook hook = new BioStoreHook(model, dataSet, dataSource);
        setStoreHook(hook);
    }

    /**
     * Return a DataSource item for the given title
     * @param name the DataSource name
     * @return the DataSource Item
     */
    public Item getDataSourceItem(String name) {
        if (name == null) {
            return null;
        }

        Item dataSource = dataSources.get(name);
        if (dataSource == null) {
            dataSource = createItem("DataSource");
            dataSource.setAttribute("name", name);
            try {
                store(dataSource);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSource with name: " + name, e);
            }
            dataSources.put(name, dataSource);
        }
        return dataSource;
    }

    /**
     * Return a DataSet item for the given name
     * @param title the DataSet title
     * @param dataSourceItem the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    public Item getDataSetItem(String title, Item dataSourceItem) {
        return getDataSetItem(title, null, null, dataSourceItem);
    }

    /**
     * Return a DataSet item with the given details.
     * @param title the DataSet title
     * @param url the new url field, or null if the url shouldn't be set
     * @param description the new description field, or null if the field shouldn't be set
     * @param dataSourceItem the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    public Item getDataSetItem(String title, String url, String description, Item dataSourceItem) {
        Item dataSet = dataSets.get(title);
        if (dataSet == null) {
            dataSet = createItem("DataSet");
            dataSet.setAttribute("name", title);
            dataSet.setReference("dataSource", dataSourceItem);
            if (url != null) {
                dataSet.setAttribute("url", url);
            }
            if (description != null) {
                dataSet.setAttribute("description", description);
            }
            try {
                store(dataSet);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            dataSets.put(title, dataSet);
        }
        return dataSet;
    }
}
