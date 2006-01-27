package org.flymine.task;

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.model.InterMineObject;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.xml.full.ItemFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.log4j.Logger;
import org.flymine.model.genomic.Synonym;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.DataSource;

import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.HashSet;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class TSVFileReaderWithSynonymTask extends TSVFileReaderTask{

    protected ItemFactory itemFactory;

    protected String dataSourceName = null;
    private DataSource dataSource = null;


    public TSVFileReaderWithSynonymTask () {
        super();
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    }

    /**
     * Allows the dataSourceName field to be set by ANT or a constructing class.
     *
     * @param dataSourceName - the name of the source database i.e. Flybase, Uniprot etc.
     * */
    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
        this.dataSource = newDataSource();
        this.dataSource.setName(this.dataSourceName);
    }

    private static final Logger LOG = Logger.getLogger(TSVFileReaderWithSynonymTask.class);

    /**
     * Does most of the work of execute().  This method exists to help with testing.
     * @param osw the ObjectStore to read from and write to
     * @param dfc the configuration of which fields to set and which field to use as a key
     * @throws org.apache.tools.ant.BuildException if an ObjectStore method fails
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

                                    //If we have found the name column - create a synonym on it.
                                    if ("name".equalsIgnoreCase(columnFD.getName())) {

                                        Synonym syn = newSynonym((BioEntity)o, "name", rowValue);
                                        if (this.dataSource != null) {
                                            syn.setSource(dataSource);
                                        } else {
                                            LOG.warn("NO DATASOURCE SET!");
                                        }
                                        osw.store(syn);
                                    }

                                }
                            }
                        }
                    }
                }
            }

            if (this.dataSource != null) {
                osw.store(dataSource);
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

    private Synonym newSynonym(BioEntity subject, String type, String value) {

        HashSet classSet = new HashSet();
        classSet.add(Synonym.class);
        Synonym syn = (Synonym) DynamicUtil.createObject(classSet);
        syn.setSubject(subject);
        syn.setType(type);
        syn.setValue(value);

        return syn;
    }

    private DataSource newDataSource() {

        HashSet bob = new HashSet();
        bob.add(DataSource.class);
        DataSource ds = (DataSource) DynamicUtil.createObject(bob);
        return ds;
    }

}
