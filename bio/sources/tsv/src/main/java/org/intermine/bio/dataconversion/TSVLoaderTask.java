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
import org.apache.tools.ant.BuildException;
import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.CollectionDescriptor;
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
import java.util.Collections;

import java.lang.reflect.Method;

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
    private String dataSourceName;
    private String dataSetTitle;
    private String licence;
    private Model model;
    private DataSource dataSource;
    private DataSet dataSet;
    //map which holds all objects by values
    private Map<String, InterMineObject> storedObjects = new HashMap();
    //map wich hold the values in a row by class
    private Map<String, String> valuesInRow;


    /**
     * Set the configuration file to use.
     * @param configurationFile the configuration File
     */
    public void setConfigurationFile(File configurationFile) {
        this.configurationFile = configurationFile;
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
        initDataSet();
        executeInternal(dfc, file);
    }

    private void initDataSet() {
        initDataSource();
        try {
            DataSet newDataSet = getDirectDataLoader().createObject(DataSet.class);
            newDataSet.setName(dataSetTitle);
            IntegrationWriter iw = getDirectDataLoader().getIntegrationWriter();
            DataSet existingDataSet = iw.getObjectByExample(newDataSet, Collections.singleton("name"));
            if (existingDataSet == null) {
                dataSet = newDataSet;
                if (licence != null) {
                    dataSet.setLicence(licence);
                }
                if (dataSource != null) {
                    dataSet.setDataSource(dataSource);
                }
                getDirectDataLoader().store(dataSet);
            } else {
                dataSet = existingDataSet;
            }
        } catch (ObjectStoreException e) {
                throw new BuildException("exception while init data set: " + dataSetTitle, e);
        }
    }

    private void initDataSource() {
        try {
            DataSource newDataSource = getDirectDataLoader().createObject(DataSource.class);
            newDataSource.setName(dataSourceName);
            IntegrationWriter iw = getDirectDataLoader().getIntegrationWriter();
            DataSource existingDataSource = iw.getObjectByExample(newDataSource, Collections.singleton("name"));
            if (existingDataSource == null) {
                dataSource = newDataSource;
                getDirectDataLoader().store(dataSource);
            } else {
                dataSource = existingDataSource;
            }
        } catch (ObjectStoreException e) {
            throw new BuildException("exception while init data source: " + dataSourceName, e);
        }
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

        while (tsvIter.hasNext()) {
            valuesInRow = new HashMap();
            String[] thisRow = (String[]) tsvIter.next();
            Set<String> classNamesSet = dfc.getClassNames();
            Iterator classNameIter = classNamesSet.iterator();

            while (classNameIter.hasNext()) {
                String className = (String) classNameIter.next();
                String fullyClassName = model.getClassDescriptorByName(className).getName();
                InterMineObject o;
                try {
                    o = getDirectDataLoader().createObject(fullyClassName);
                    if (o instanceof BioEntity) {
                        ((BioEntity) o).addDataSets(dataSet);
                    }
                } catch (ClassNotFoundException e) {
                    throw new BuildException("cannot find class while reading: " + file, e);
                } catch (ObjectStoreException e) {
                    throw new BuildException("exception while creating object of type: "
                            + className, e);
                }

                List fieldClasses = dfc.getColumnFieldClasses(className);
                StringBuilder fieldValuesStringBuilder = new StringBuilder();
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
                            fieldValuesStringBuilder.append(rowValue);
                        }
                    }
                }

                //avoid duplication
                String fieldValues = fieldValuesStringBuilder.toString();
                valuesInRow.put(className, fieldValues);
                InterMineObject storedObj = storedObjects.get(fieldValues);
                if (storedObj == null) {
                    storedObjects.put(fieldValues, o);
                    try {
                        getDirectDataLoader().store(o);
                    } catch (ObjectStoreException e) {
                        throw new BuildException("exception while storing: " + o, e);
                    }
                    setJoinFields(className, o);
                } else {
                    setJoinFields(className, storedObj);
                }
            }
        }

        try {
            getDirectDataLoader().close();
        } catch (ObjectStoreException e) {
            System.out.println("exception when closing");
            throw new IllegalArgumentException(e);
        }
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
                //e.g we do not want to set Protein.isoforms
                if (!simpleRefClassName.equals(className)) {
                    String keyInRow = valuesInRow.get(simpleRefClassName);
                    InterMineObject objectInRow = null;
                    if (keyInRow != null) {
                        objectInRow = storedObjects.get(keyInRow);
                    }
                    if (objectInRow != null) {
                        if (rd instanceof CollectionDescriptor) {
                            String methodName = "add" + StringUtils.capitalize(refName);
                            try {
                                Class<?> clazz = Class.forName("org.intermine.model.bio." + className);
                                Class<?> paramClazz = Class.forName(rd.getReferencedClassName());
                                Method m = clazz.getMethod(methodName, new Class[]{paramClazz});
                                m.invoke(imo, objectInRow);
                            } catch (Exception err) {
                                throw new BuildException(err);
                            }
                        } else {
                            imo.setFieldValue(refName, objectInRow);
                        }
                    }
                }
            }
        }
    }
}
