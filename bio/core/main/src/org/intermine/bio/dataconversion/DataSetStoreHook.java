package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.DataConverterStoreHook;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.XmlUtil;
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
    private final Model model;

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     * @param dataSet the DataSet to add to items
     * @param dataSource the DataSource to add to the items
     */
    public DataSetStoreHook(Model model, Item dataSet, Item dataSource) {
        this.model = model;
        this.dataSet = dataSet;
        this.dataSource = dataSource;
    }

    /**
     * @see DataSetStoreHook#setDataSets(Item, Item, Item)
     * {@inheritDoc}
     */
    public void processItem(@SuppressWarnings("unused") DataConverter dataConverter, Item item) {
        setDataSets(model, item, dataSet.getIdentifier(), dataSource.getIdentifier());
    }

    /**
     * Do the work of processItem() by setting DataSet and DataSource references and collections
     * on the given Item.
     * @param model the data model
     * @param item the Item to process
     * @param dataSetId the item id of the DataSet to add
     * @param dataSourceId the item id of the DataSource to add
     */
    public static void setDataSets(Model model, Item item, String dataSetId, String dataSourceId) {
        String className  = XmlUtil.getFragmentFromURI(item.getClassName());
        ClassDescriptor cd = model.getClassDescriptorByName(className);
        ReferenceDescriptor rd = cd.getReferenceDescriptorByName("source");
        String dataSourceClassName = "org.flymine.model.genomic.DataSource";
        if (rd != null && rd.getReferencedClassDescriptor().getName().equals(dataSourceClassName)
            && !item.hasReference("source")) {
            item.setReference("source", dataSourceId);
        }
        if (item.canHaveReference("dataSource") && !item.hasReference("dataSource")) {
            item.setReference("dataSource", dataSourceId);
        }
        if (item.canHaveReference("dataSet") && !item.hasReference("dataSet")) {
            item.setReference("dataSet", dataSetId);
        }
        if (item.canHaveCollection("dataSets")) {
            item.addToCollection("dataSets", dataSetId);
        }
    }
}
