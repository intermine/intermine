package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;

import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.XmlConverter;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.metadata.Model;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * Task in invoke XML conversion
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class XmlConverterTask extends Task
{
    protected FileSet fileSet;
    protected File schema;
    protected String model;
    protected String osName;

    /**
     * Set the objectstore name
     * @param osName the model name
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Set the model name
     * @param model the model name
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set the xsd schema file
     * @param schema the file
     */
    public void setSchema(File schema) {
        this.schema = schema;
    }

    /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (schema == null) {
            throw new BuildException("schema must be specified");
        }
        if (model == null) {
            throw new BuildException("model must be specified");
        }
        if (osName == null) {
            throw new BuildException("osName must be specified");
        }

        try {
            Model m = Model.getInstanceByName(model);
            ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            ItemWriter writer = new ObjectStoreItemWriter(osw);
            XmlConverter converter = new XmlConverter(m,
                                                      new BufferedReader(new FileReader(schema)),
                                                      writer);
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                converter.process(new BufferedReader(new FileReader(new File(ds.getBasedir(),
                                                                             files[i]))));
            }
            writer.close();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
