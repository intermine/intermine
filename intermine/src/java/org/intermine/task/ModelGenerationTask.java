/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.task;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileWriter;

import org.flymine.modelproduction.ModelParser;
import org.flymine.modelproduction.uml.XmiParser;
import org.flymine.modelproduction.acedb.AceModelParser;
import org.flymine.metadata.Model;

/**
 * Ant task that calls a parser to build a FlyMine model from an external source
 * and persist it as xml.
 *
 * @author Richard Smith
 */

public class ModelGenerationTask extends Task
{
    protected String modelName;
    protected File destDir;
    protected String type;
    protected File source;

    /**
     * Set name of the model to be created.
     * @param modelName name of the model
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Set the directory to create output xml file in.
     * @param destDir destination directory for output
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Set the type of input to be parsed.
     * @param type type of source model to be parsed
     */
    public void setType(String type) {
        this.type = type.toLowerCase();
    }

    /**
     * Set the source file to be parsed.
     * @param source the file to be parsed
     */
    public void setSource(File source) {
        this.source = source;
    }


    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.destDir == null) {
            throw new BuildException("destDir attribute is not set");
        }
        if (this.type == null) {
            throw new BuildException("type attribute is not set");
        }
        if (this.modelName == null) {
            throw new BuildException("modelName attribute is not set");
        }
        if (this.source == null) {
            throw new BuildException("source attribute is not set");
        }

        if (!source.exists()) {
            throw new BuildException("source file (" + source + ") does not exist.");
        }

        ModelParser parser = null;
        Model model = null;

        if (type.equals("xmi")) {
            parser = new XmiParser();
        } else if (type.equals("acedb")) {
            parser = new AceModelParser();
        } else {
            throw new BuildException("Unrecognised value for type: " + type);
        }


        try {
            FileInputStream fis = new FileInputStream(source);
            model = parser.process((InputStream) fis);
        } catch (Exception e) {
            throw new BuildException("Error parsing model: " + e);
        }

        if (model == null) {
            throw new BuildException("model not generated correctly");
        }

        try {
            File output = new File(destDir, modelName + "_model.xml");
            FileWriter writer = new FileWriter(output);
            writer.write(model.toString());
            writer.close();
        } catch (Exception e) {
            throw new BuildException(e);
        }

    }


}
