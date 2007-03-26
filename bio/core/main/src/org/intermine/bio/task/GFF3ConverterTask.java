package org.intermine.bio.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.bio.dataconversion.GFF3Converter;
import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.metadata.Model;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.Task;

import org.apache.log4j.Logger;


/**
 * Task to convert gff3 data
 * @author Wenyan Ji
 */
public class GFF3ConverterTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(GFF3ConverterTask.class);

    protected FileSet fileSet;
    protected String converter, targetAlias, seqClsName, orgTaxonId;
    protected String seqDataSourceName, model, handlerClassName;
    protected GFF3Parser parser;

    private String dataSourceName;
    private String dataSetTitle;

     /**
     * Set the data fileset
     * @param fs the fileset
     */
    public void addFileSet(FileSet fs) {
        this.fileSet = fs;
    }

    /**
     * Set the converter
     * @param converter the converter
     */
    public void setConverter(String converter) {
        this.converter = converter;
    }


    /**
     * Set the target ObjectStore alias
     * @param targetAlias the targetAlias
     */
    public void setTarget(String targetAlias) {
        this.targetAlias = targetAlias;
    }


     /**
     * Set the sequenceClassName
     * @param seqClsName the seqClsName;
     */
    public void setSeqClsName(String seqClsName) {
        this.seqClsName = seqClsName;
    }


    /**
     * Set the organism taxon id
     * @param orgTaxonId the organism taxon id
     */
    public void setOrgTaxonId(String orgTaxonId) {
        this.orgTaxonId = orgTaxonId;
    }

    /**
     * Set the dataSourceName
     * @param dataSourceName the dataSourceName
     */
    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    /**
     * Set the seqDataSourceName
     * @param seqDataSourceName the seqDataSourceName
     */
    public void setSeqDataSourceName(String seqDataSourceName) {
        this.seqDataSourceName = seqDataSourceName;
    }

    /**
     * Set the dataSetTitle
     * @param dataSetTitle the DataSet title
     */
    public void setDataSetTitle(String dataSetTitle) {
        this.dataSetTitle = dataSetTitle;
    }

    /**
     * Set the name of model to create data in
     * @param model name of model
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set the name of GFF3RecordHandler class to use when processing.
     * @param handlerClassName the name of the handler
     */
    public void setHandlerClassName(String handlerClassName) {
        this.handlerClassName = handlerClassName;
    }


    /**
     * @see Task#execute()
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (converter == null) {
            throw new BuildException("converter attribute not set");
        }
        if (targetAlias == null) {
            throw new BuildException("targetAlias attribute not set");
        }
        if (seqClsName == null) {
            throw new BuildException("seqClsName attribute not set");
        }
        if (orgTaxonId == null) {
            throw new BuildException("orgTaxonId attribute not set");
        }
        if (dataSourceName == null) {
            throw new BuildException("dataSourceName attribute not set");
        }
        if (seqDataSourceName == null) {
            throw new BuildException("seqDataSourceName attribute not set");
        }
        if (dataSetTitle == null) {
            throw new BuildException("dataSetTitle attribute not set");
        }
        if (model == null) {
            throw new BuildException("model attribute not set");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(targetAlias);
            writer = new ObjectStoreItemWriter(osw);
            parser = new GFF3Parser();
            Model tgtModel = Model.getInstanceByName(model);
            GFF3RecordHandler handler;
            if (handlerClassName == null) {
                handler = new GFF3RecordHandler(tgtModel);
            } else {
                Class handlerClass;
                try {
                    handlerClass = Class.forName(handlerClassName);
                } catch (ClassNotFoundException e) {
                    throw new BuildException("Class not found for " + handlerClassName, e);
                }
                Class [] types = new Class[] {Model.class};
                Object [] args = new Object[] {tgtModel};
                handler = (GFF3RecordHandler) handlerClass.getConstructor(types).newInstance(args);
            }
            GFF3Converter gff3converter =
                new GFF3Converter(writer, seqClsName, orgTaxonId, dataSourceName,
                                  dataSetTitle, seqDataSourceName,
                                  tgtModel, handler);

            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File f = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file: " + f.getName());
                gff3converter.parse(new BufferedReader(new FileReader(f)));
            }
            gff3converter.store();
            gff3converter.close();
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (writer != null) {
                    osw.close();
                }
            } catch (Exception e) {
                throw new BuildException(e);
            }

        }

    }

}
