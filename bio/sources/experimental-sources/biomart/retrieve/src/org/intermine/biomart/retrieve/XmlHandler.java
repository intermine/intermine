package org.intermine.biomart.retrieve;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * 
 * @author "Xavier Watkins"
 */
public class XmlHandler extends DefaultHandler
{
    private List<BioMartField> fields;
    private Map<String,BioMartDataSet> datasets;
    private String currentDataSet;

    /**
     * The constructor
     */
    public XmlHandler() {
        super();
        fields = new ArrayList<BioMartField>();
        datasets = new HashMap<String, BioMartDataSet>();
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, 
     *                                                   java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String name, Attributes attributes)
                    throws SAXException {
        if ("DatasetConfig".equals(name)) {
            BioMartDataSet dataset = new BioMartDataSet();
            dataset.setName(attributes.getValue("dataset"));
            datasets.put(dataset.getName(), dataset);
            currentDataSet = dataset.getName();
        } else
            if ("Importable".equals(name)) {
                MartImportable importable = new MartImportable();
                importable.setName(attributes.getValue("name"));
                importable.setInternalName(attributes.getValue("internalName"));
                importable.setLinkName(attributes.getValue("linkName"));
                importable.setType(attributes.getValue("type"));
                importable.setFilters(Arrays.asList(StringUtils.split(attributes
                                .getValue("filters"), ",")));
                datasets.get(currentDataSet).addImportable(importable);
            } else
                if ("Exportable".equals(name)) {
                    MartExportable exportable = new MartExportable();
                    exportable.setName(attributes.getValue("name"));
                    exportable.setInternalName(attributes.getValue("internalName"));
                    exportable.setLinkName(attributes.getValue("linkName"));
                    exportable.setLinkVersion(attributes.getValue("linkVersion"));
                    exportable.setAttributes(Arrays.asList(StringUtils.split(attributes
                                    .getValue("attributes"), ",")));
                    datasets.get(currentDataSet).addExportable(exportable);
                } else
                    if ("AttributeDescription".equals(name)) {
                        BioMartField field = new BioMartField();
                        field.setField(attributes.getValue("field"));
                        field.setDisplayName(attributes.getValue("displayName"));
                        field.setInternalName(attributes.getValue("internalName"));
                        field.setTableConstraint(attributes.getValue("tableConstraint"));
                        field.setKey(attributes.getValue("key"));
                        fields.add(field);
                        datasets.get(currentDataSet).addAttribute(field);
                    }
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String name) throws SAXException {
        super.endElement(uri, localName, name);
    }

    /**
     * @return the tables
     */
    public Map<String, BioMartDataSet> getTables() {
        return datasets;
    }

    /**
     * @param datasets the tables to set
     */
    public void addTable(String name, BioMartDataSet table) {
        this.datasets.put(name, table);
    }

    /**
     * @return the datasets
     */
    public Map<String, BioMartDataSet> getDatasets() {
        return datasets;
    }

    /**
     * @param datasets the datasets to set
     */
    public void setDatasets(Map<String, BioMartDataSet> datasets) {
        this.datasets = datasets;
    }
    
//    public String getIMXML() {
//        
//        String xml;
//        return xml;
//    }

}
