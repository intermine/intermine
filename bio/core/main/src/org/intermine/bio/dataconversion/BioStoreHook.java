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
public class BioStoreHook implements DataConverterStoreHook
{
    private String dataSetRefId = null;
    private String dataSourceRefId = null;
    private final Model model;
    private static final Map<String, String> SO_TERMS = new HashMap<String, String>();
    private static String ontologyRefId = null;

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     * @param dataSet the DataSet to add to items
     * @param dataSource the DataSource to add to the items
     * @deprecated Use the other constructor instead
     */
    public BioStoreHook(Model model, Item dataSet, Item dataSource) {
        this.model = model;
        if (dataSet != null && dataSource != null) {
            this.dataSetRefId = dataSet.getIdentifier();
            this.dataSourceRefId = dataSource.getIdentifier();
        }
    }

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     * @param dataSet the DataSet to add to items
     * @param dataSource the DataSource to add to the items
     */
    public BioStoreHook(Model model, String dataSet, String dataSource) {
        this.model = model;
        this.dataSetRefId = dataSet;
        this.dataSourceRefId = dataSource;
    }

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     */
    public BioStoreHook(Model model) {
        this.model = model;
        this.dataSetRefId = null;
        this.dataSourceRefId = null;
    }

    /**
     * Update the dataset reference Id.  Overwrites the one set in the constructor.  Needed for
     * Uniprot which has a few different datasets.
     *
     * @param dataSet ID representing dataset object
     */
    public void setDataSet(String dataSet) {
        this.dataSetRefId = dataSet;
    }

    /**
     * @see BioStoreHook#setDataSets(Item, Item, Item)
     * {@inheritDoc}
     */
    public void processItem(DataConverter dataConverter, Item item) {
        setSOTerm(dataConverter, item);
        if (StringUtils.isNotEmpty(dataSetRefId)) {
            setDataSets(model, item, dataSetRefId, dataSourceRefId);
        }
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
    }

    private void setSOTerm(DataConverter dataConverter, Item item) {
        if (item.canHaveReference("sequenceOntologyTerm")
                && !item.hasReference("sequenceOntologyTerm")) {
            String soTermId = getSoTerm(dataConverter, item);
            if (!StringUtils.isEmpty(soTermId)) {
                item.setReference("sequenceOntologyTerm", soTermId);
            }
        }
    }

    /**
     * get and store a SO term based on intermine item's class.  Only will store a SO term if
     * the item is a SequenceFeature
     * @param dataConverter data converter
     * @param item item
     * @return id representing the SO term object
     */
    protected static String getSoTerm(DataConverter dataConverter, Item item) {
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
                if (ontologyRefId == null) {
                    setOntology(dataConverter);
                }
                soterm.setReference("ontology", ontologyRefId);
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

    /**
     * create and store ontology object
     * @param dataConverter data converter
     */
    private static void setOntology(DataConverter dataConverter) {
        Item item = dataConverter.createItem("Ontology");
        item.setAttribute("name", "Sequence Ontology");
        item.setAttribute("url", "http://www.sequenceontology.org");
        try {
            dataConverter.store(item);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Can't store ontology", e);
        }
        ontologyRefId = item.getIdentifier();
    }
}
