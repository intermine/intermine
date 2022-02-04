package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.*;
import org.apache.tools.ant.BuildException;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.DynamicUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Read a file of tab separated values.  Use one column as the key to look up objects and use the
 * other columns to set fields in that object.
 *
 * @author Daniela Butano
 * @author Kim Rutherford
 */

public class TSVLoaderTask extends FileDirectDataLoaderTask
{
    private File configurationFile;
    private String taxonId;
    private String dataSourceName;
    private String dataSetTitle;
    private String licence;
    private Model model;
    private DataSource dataSource;
    private Map<String, DataSet> dataSets = new HashMap<String, DataSet>();
    private Map<String, InterMineObject> objectsInRow;

    /**
     * Set the configuration file to use.
     * @param configurationFile the configuration File
     */
    public void setConfigurationFile(File configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Set the Taxon Id of the Organism we are loading.
     * @param taxonId the taxon id to set.
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    /**
     * Datasource for any bioentities created
     * @param dataSourceName name of datasource for items created
     */
    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    /**
     * If a value is specified this title will used when a DataSet is created.
     * @param dataSetTitle the title of the DataSets of any new features
     */
    public void setDataSetTitle(String dataSetTitle) {
        this.dataSetTitle = dataSetTitle;
    }

    /**
     * If a value is specified this title will used when a DataSet is created.
     * @param dataSetTitle the title of the DataSets of any new features
     */
    public void setLicence(String licence) {
        this.licence = licence;
    }

    /**
     * @throws BuildException if an ObjectStore method fails
     */
    @Override
    public void execute() {
        // don't configure dynamic attributes if this is a unit test!
        if (getProject() != null) {
            configureDynamicAttributes(this);
        }
        if (taxonId == null) {
            throw new BuildException("taxonId needs to be set");
        }
        if (configurationFile == null) {
            throw new BuildException("configurationFile needs to be set");
        }

        if (dataSourceName == null) {
            throw new BuildException("dataSourceName needs to be set");
        }

        if (dataSetTitle == null) {
            throw new BuildException("dataSetTitle needs to be set");
        }
        try{
            model = getDirectDataLoader().getIntegrationWriter().getModel();
        } catch (ObjectStoreException e) {
            throw new BuildException("ObjectStore problem");
        }

        // this will call processFile() for each file
        super.execute();
    }

    /**
     * Query all objects of the class given by the className specified in the configurationFile.
     * Set fields in the objects by using the tab separated files as input.
     * @param file the File to process
     * @throws BuildException if an ObjectStore method fails
     */
    public void processFile(File file) {
        DelimitedFileConfiguration dfc;
        try {
            dfc = new DelimitedFileConfiguration(model, new FileInputStream(configurationFile));
        } catch (Exception e) {
            throw new BuildException("unable to read configuration for "
                                     + this.getClass().getName(), e);
        }
        executeInternal(dfc, file);
    }

    /**
     * Does most of the work of execute().  This method exists to help with testing.
     * @param dfc the configuration of which fields to set and which field to use as a key
     * @param file The file to read from
     * @throws BuildException if an ObjectStore method fails
     */
    void executeInternal(DelimitedFileConfiguration dfc, File file) {
        Iterator tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(new FileReader(file));
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + file, e);
        }

        objectsInRow = new HashMap();
        while (tsvIter.hasNext()) {
            String[] thisRow = (String[]) tsvIter.next();
            //objectsInRow = new HashMap();
            Set<String> classNamesSet = dfc.getClassNames();
            Iterator classNameIter = classNamesSet.iterator();

            while (classNameIter.hasNext()) {
                String className = (String) classNameIter.next();
                String fullyClassName = model.getClassDescriptorByName(className).getName();
                InterMineObject o;
                try {
                    o = getDirectDataLoader().createObject(fullyClassName);
                    if (o instanceof BioEntity) {
                        ((BioEntity) o).addDataSets(getDataSet());
                    }
                } catch (ClassNotFoundException e) {
                    throw new BuildException("cannot find class while reading: " + file, e);
                } catch (ObjectStoreException e) {
                    throw new BuildException("exception while creating object of type: "
                            + className, e);
                }

                List fieldClasses = dfc.getColumnFieldClasses(className);
                Set<String> fieldNames = new HashSet<String>();
                for (int columnIndex = 0; columnIndex < thisRow.length; columnIndex++) {
                    if (dfc.getColumnFieldDescriptors(className).size() <= columnIndex) {
                        // ignore - no configuration for this column
                        continue;
                    }
                    AttributeDescriptor columnAD =
                            (AttributeDescriptor) dfc.getColumnFieldDescriptors(className).get(columnIndex);

                    if (columnAD == null) {
                        // ignore - no configuration for this column
                    } else {
                        String rowValue = thisRow[columnIndex].trim();
                        if (rowValue.length() > 0) {
                            Class fieldClass = (Class) fieldClasses.get(columnIndex);
                            Object typedObject = TypeUtil.stringToObject(fieldClass, rowValue);
                            o.setFieldValue(columnAD.getName(), typedObject);
                            fieldNames.add(columnAD.getName());
                        }
                    }
                }

                try {
                    IntegrationWriter iw = getDirectDataLoader().getIntegrationWriter();
                    //avoid duplication
                    InterMineObject existingObj = iw.getObjectByExample(o, fieldNames);
                    if (existingObj == null) {
                        objectsInRow.put(className, o);
                        getDirectDataLoader().store(o);
                        setJoinFields(className, o);
                    }
                } catch (ObjectStoreException e) {
                    throw new BuildException("exception while storing: " + o, e);
                } finally {
                    try {
                        getDirectDataLoader().close();
                    } catch (ObjectStoreException e) {
                        System.out.println("exception when closinf for " + className);
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        }
    }

    /**
     * Return the DataSet to add to each object.
     * @return the DataSet
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    public DataSet getDataSet() throws ObjectStoreException {
        if (dataSets.containsKey(dataSetTitle)) {
            return dataSets.get(dataSetTitle);
        }
        DataSet dataSet = getDirectDataLoader().createObject(DataSet.class);
        dataSet.setName(dataSetTitle);
        if (licence != null) {
            dataSet.setLicence(licence);
        }
        if (dataSourceName != null) {
            dataSet.setDataSource(getDataSource());
        }
        getDirectDataLoader().store(dataSet);
        dataSets.put(dataSetTitle, dataSet);
        return dataSet;
    }

    private DataSource getDataSource() throws ObjectStoreException {
        if (dataSource == null) {
            dataSource = getDirectDataLoader().createObject(DataSource.class);
            dataSource.setName(dataSourceName);
            getDirectDataLoader().store(dataSource);
        }
        return dataSource;
    }

    private void setJoinFields(String className, InterMineObject imo) {
        ClassDescriptor cd = model.getClassDescriptorByName(className);
        for (FieldDescriptor fd : cd.getAllFieldDescriptors()) {
            if (fd instanceof ReferenceDescriptor) {
                ReferenceDescriptor rd = (ReferenceDescriptor) fd;
                String refName = rd.getName();
                String refClassName = rd.getReferencedClassName();
                int index = refClassName.lastIndexOf(".");
                String simpleRefClassName = refClassName.substring(index + 1);
                InterMineObject objectInRow = objectsInRow.get(simpleRefClassName);
                if (objectInRow != null) {
                    imo.setFieldValue(refName, objectInRow);
                }

            }
        }
    }
}
