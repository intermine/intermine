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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;

import org.flymine.dataconversion.GFF3Converter;
import org.flymine.io.gff3.GFF3Parser;

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
    protected String converter, target, seqClsName, orgAbbrev, infoSourceTitle, targetNameSpace;
    protected GFF3Parser parser;

     /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * Set the converter
     * @param converter the converter
     */
    public void setConverter(String converter) {
        this.converter = converter;
    }


    /**
     * Set the target
     * @param target the target
     */
    public void setTarget(String target) {
        this.target = target;
    }


     /**
     * Set the sequenceClassName
     * @param seqClsName the seqClsName;
     */
    public void setSeqClsName(String seqClsName) {
        this.seqClsName = seqClsName;
    }


    /**
     * Set the organism abbreviation (for ensembl)
     * @param orgAbbrev the organism abbreviation
     */
    public void setOrgAbbrev(String orgAbbrev) {
        this.orgAbbrev = orgAbbrev;
    }

    /**
     * Set the infoSourceTitle
     * @param infoSourceTitle the infoSourceTitle
     */
    public void setInfoSourceTitle(String infoSourceTitle) {
        this.infoSourceTitle = infoSourceTitle;
    }

    /**
     * Set the targetNameSpace
     * @param targetNameSpace the targetNameSpace
     */
    public void setTargetNameSpace(String targetNameSpace) {
        this.targetNameSpace = targetNameSpace;
    }



    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (converter == null) {
            throw new BuildException("converter attribute not set");
        }
        if (target == null) {
            throw new BuildException("target attribute not set");
        }
        if (seqClsName == null) {
            throw new BuildException("seqClsName attribute not set");
        }
        if (orgAbbrev == null) {
            throw new BuildException("orgAbbrev attribute not set");
        }
        if (infoSourceTitle == null) {
            throw new BuildException("infoSourceTitle attribute not set");
        }
        if (targetNameSpace == null) {
            throw new BuildException("targetNameSpace attribute not set");
        }


        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(target);
            writer = new ObjectStoreItemWriter(osw);
            parser = new GFF3Parser();
            GFF3Converter gff3converter = new GFF3Converter(parser, writer, seqClsName, orgAbbrev,
                          infoSourceTitle, targetNameSpace);

            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File f = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file: " + f.getName());
                gff3converter.parse(new BufferedReader(new FileReader(f)));
            }
            gff3converter.close();
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                writer.close();
                osw.close();
            } catch (Exception e) {
                throw new BuildException(e);
            }

        }

    }

}
