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


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.bio.util.BioConverterUtil;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.DataConverterStoreHook;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreException;
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
    private static final Map<String, String> SO_TERMS = new HashMap<String, String>();

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
    public void processItem(DataConverter dataConverter, Item item) {
        String soTermId = getSoTerm(dataConverter, item);
        setDataSets(model, item, dataSet.getIdentifier(), dataSource.getIdentifier(), soTermId);
    }

    /**
     * Do the work of processItem() by setting DataSet and DataSource references and collections
     * on the given Item.
     * @param model the data model
     * @param item the Item to process
     * @param dataSetId the item id of the DataSet to add
     * @param dataSourceId the item id of the DataSource to add
     * @param soTermId item id of the SO Term to add
     */
    public static void setDataSets(Model model, Item item, String dataSetId, String dataSourceId,
            String soTermId) {
        String className = item.getClassName();
        ClassDescriptor cd = model.getClassDescriptorByName(className);
        ReferenceDescriptor rd = cd.getReferenceDescriptorByName("source");
        String dataSourceClassName = "org.intermine.model.bio.DataSource";
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
        if (item.canHaveReference("sequenceOntologyTerm")
                && !item.hasReference("sequenceOntologyTerm")) {
            System.out.println("can have reference");
            if (!StringUtils.isEmpty(soTermId)) {
                System.out.println("setting reference");
                item.setReference("sequenceOntologyTerm", soTermId);
            }
        }
    }

    private static String getSoTerm(DataConverter dataConverter, Item item) {
        String soName = null;
        try {
            soName = BioConverterUtil.javaNameToSO(item.getClassName());
            if (soName == null) {
                return null;
            }
            String soRefId = SO_TERMS.get(soName);
            if (StringUtils.isEmpty(soRefId)) {
                Item soterm = dataConverter.createItem("SOTerm");
                soterm.setAttribute("name", soName);
                dataConverter.store(soterm);
                soRefId = soterm.getIdentifier();
                SO_TERMS.put(soName, soRefId);
            }
            return soRefId;
        } catch (IOException e) {
            return null;
        } catch (ObjectStoreException e) {
            return null;
        }
    }
}
