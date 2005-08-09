package org.flymine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.TextFileUtil;
import org.intermine.util.TypeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.log4j.Logger;

/**
 * Read a file of tab separated values.  Use one column as the key to look up objects and use the
 * other columns to set fields in that object.
 *
 * @author Kim Rutherford
 */

public class TSVFileReaderTask extends FileReadTask
{
    private static final Logger LOG = Logger.getLogger(TSVFileReaderTask.class);

    private File configurationFile = null;

    /**
     * Set the configuration file to use.
     * @param configurationFile the configuration File
     */
    public void setConfigurationFile(File configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Query all objects of the class given by the className specified in the configurationFile that
     * have a reference to the organism given by the organismAbbreviation parameter.  Set fields in
     * the objects by using the tab separated files as input.
     * @throws BuildException if an ObjectStore method fails
     */
    public void execute() throws BuildException {
        if (getOrganismAbbreviation() == null) {
            throw new BuildException("organismAbbreviation not set");
        }

        if (getOswAlias() == null) {
            throw new BuildException("oswAlias not set");
        }

        if (configurationFile == null) {
            throw new BuildException("configurationFile not set");
        }

        Model model = getObjectStoreWriter().getModel();

        DelimitedFileConfiguration dfc;

        try {
            dfc = new DelimitedFileConfiguration(model, new FileInputStream(configurationFile));
        } catch (Exception e) {
            throw new BuildException("unable to read configuration for TSVFileReaderTask", e);
        }

        executeInternal(getObjectStoreWriter(), dfc);
    }

    /**
     * Does most of the work of execute().  This method exists to help with testing.
     * @param osw the ObjectStore to read from and write to
     * @param dfc the configuration of which fields to set and which field to use as a key
     * @throws BuildException if an ObjectStore method fails
     */
    void executeInternal(ObjectStoreWriter osw, DelimitedFileConfiguration dfc)
        throws BuildException {
        setClassName(dfc.getConfigClassDescriptor().getName());
        setKeyFieldName(dfc.keyFieldDescriptor.getName());

        File currentFile = null;

        try {
            osw.beginTransaction();
            
            Map idMap = getIdMap(osw.getObjectStore());
            
            Iterator fileSetIter = getFileSets().iterator();
            
            while (fileSetIter.hasNext()) {
                FileSet fileSet = (FileSet) fileSetIter.next();
                
                DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
                String[] files = ds.getIncludedFiles();
                for (int i = 0; i < files.length; i++) {
                    currentFile = new File(ds.getBasedir(), files[i]);
                    System.err .println("Processing file: " + currentFile.getName());
                    
                    Iterator tsvIter =
                        TextFileUtil.parseTabDelimitedReader(new FileReader(currentFile));
                    
                    while (tsvIter.hasNext()) {
                        String[] thisRow = (String[]) tsvIter.next();
                        String keyColumnValue = thisRow[dfc.getKeyColumnNumber()];
                        Integer objectId = (Integer) idMap.get(keyColumnValue);

                        if (objectId == null) {
                            LOG.warn("cannot find object for ID: " + keyColumnValue
                                     + " read from " + currentFile);
                            continue;
                        }

                        InterMineObject o = 
                            osw.getObjectStore().getObjectById(objectId);
                        
                        for (int columnIndex = 0; columnIndex < thisRow.length; columnIndex++) {
                            FieldDescriptor columnFD =
                                (FieldDescriptor) dfc.getColumnFieldDescriptors().get(columnIndex);
                            
                            if (columnFD == null) {
                                // ignore - no configuration for this column
                            } else {
                                String rowValue = thisRow[columnIndex].trim();
                                if (rowValue.length() > 0) {
                                    TypeUtil.setFieldValue(o, columnFD.getName(), rowValue);
                                    osw.store(o);
                                }
                            }
                        }
                    }
                }
            }

            osw.commitTransaction();
        } catch (NoSuchElementException e) {
            throw new BuildException("no fasta sequences in: " + currentFile, e);
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + currentFile, e);
        } catch (IOException e) {
            throw new BuildException("error while reading from: " + currentFile, e);
        } catch (ObjectStoreException e) {
            throw new BuildException("error fetching object from ObjectStore: ", e);
        }

    }
}
