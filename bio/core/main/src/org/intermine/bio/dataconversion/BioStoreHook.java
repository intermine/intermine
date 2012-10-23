package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.IOException;
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
 *
 * @author Kim Rutherford
 */
public class BioStoreHook implements DataConverterStoreHook
{
    private String dataSetRefId = null;
    private String dataSourceRefId = null;
    private String ontologyRefId = null;
    private final Model model;

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     * @param dataSet the DataSet to add to items
     * @param dataSource the DataSource to add to the items
     * @param ontologyRefId id representing Ontology (SO) object
     */
    public BioStoreHook(Model model, String dataSet, String dataSource, String ontologyRefId) {
        this.model = model;
        this.dataSetRefId = dataSet;
        this.dataSourceRefId = dataSource;
        if (StringUtils.isNotEmpty(ontologyRefId)) {
            this.ontologyRefId = ontologyRefId;
        }
    }

    /**
     * Create a new DataSetStoreHook object.
     * @param model the data model
     * @param ontologyRefId id representing Ontology (SO) object
     */
    public BioStoreHook(Model model, String ontologyRefId) {
        this.model = model;
        this.dataSetRefId = null;
        this.dataSourceRefId = null;
        if (StringUtils.isNotEmpty(ontologyRefId)) {
            this.ontologyRefId = ontologyRefId;
        }
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
     * Assigns SO terms and datasets to item, if appropriate.
     *
     * @param dataConverter converter where item was created
     * @param item item to be processed
     */
    @Override
    public void processItem(DataConverter dataConverter, Item item) {
        setSOTerm(dataConverter, item, null, ontologyRefId);
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

    /**
     * For an item, set the SO term reference if one is not already set.  Use the name provided
     * else use the class name.
     *
     * @param dataConverter data converter
     * @param item item of intered
     * @param ontology id representing the Sequence Ontology object
     * @param featureType type of feature eg. sequence_feature
     */
    public static void setSOTerm(DataConverter dataConverter, Item item, String featureType,
            String ontology) {
        if (item.canHaveReference("sequenceOntologyTerm")
                && !item.hasReference("sequenceOntologyTerm")
                && StringUtils.isNotEmpty(ontology)) {
            String soTermId = getSoTerm(dataConverter, item, featureType, ontology);
            if (!StringUtils.isEmpty(soTermId)) {
                item.setReference("sequenceOntologyTerm", soTermId);
            }
        }
    }

    /**
     * Get and store a SO term based on intermine item's class or featureType (if provided).
     * Only will store a SO term if the item is a SequenceFeature.
     *
     * @param dataConverter data converter
     * @param item item
     * @param ontology id representing the SO object
     * @param featureType type of feature, eg. sequence_feature
     * @return id representing the SO term object
     */
    protected static String getSoTerm(DataConverter dataConverter, Item item, String featureType,
            String ontology) {
        String soName = null;
        try {
            if (StringUtils.isNotEmpty(featureType)) {
                soName = featureType;
            } else {
                soName = BioConverterUtil.javaNameToSO(item.getClassName());
                if (soName == null) {
                    return null;
                }
            }
            String soRefId = dataConverter.getUniqueItemId(soName);
            if (StringUtils.isEmpty(soRefId)) {
                Item soterm = dataConverter.createItem("SOTerm");
                soterm.setAttribute("name", soName);
                soterm.setReference("ontology", ontology);
                dataConverter.store(soterm);
                soRefId = soterm.getIdentifier();
                dataConverter.addUniqueItemId(soName, soRefId);
            }
            return soRefId;
        } catch (IOException e) {
            throw new RuntimeException("Error reading `soClassName.properties`", e);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error storing SOTerm", e);
        }
    }


}
