package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.uml.XmiParser;
import org.intermine.modelproduction.xmlschema.XmlSchemaParser;

/**
 * Ant task that calls a parser to build a InterMine model from an external source
 * and persist it as xml.
 *
 * @author Richard Smith
 */
public class ModelGenerationTask extends Task
{
    protected String namespace, pkg, modelName, type;
    protected File destDir;
    protected File source;

    /**
     * Set the name space to use in the output XML
     * @param namespace the XML name space
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Set the pacakge to use in the InterMine model
     * @param pkg the package name
     */
    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

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
     * {@inheritDoc}
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

        if (type.equals("xmi")) {
            parser = new XmiParser(modelName, pkg, namespace);
        } else if (type.equals("xmlschema")) {
            parser = new XmlSchemaParser(modelName, pkg, namespace);
        } else {
            throw new BuildException("Unrecognised value for type: " + type);
        }

        Model model = null;

        try {
            model = parser.process(new FileReader(source));
        } catch (Exception e) {
            BuildException be = new BuildException("Error parsing model: ", e);
            be.initCause(e);
            e.printStackTrace();
            throw be;
        }

        if (model == null) {
            throw new BuildException("model not generated correctly");
        }

        try {
            MetadataManager.saveModel(model, destDir);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}
