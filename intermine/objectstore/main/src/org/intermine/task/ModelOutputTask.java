package org.intermine.task;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.codegen.JavaModelOutput;
import org.intermine.metadata.Model;

/**
 * Creates and runs a ModelOutput process to generate java or config files.
 *
 * @author Richard Smith
 */

public class ModelOutputTask extends Task
{
    protected File destDir;
    protected Model model;
    protected String type;

    /**
     * Sets the directory that output should be written to.
     * @param destDir the directory location
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Set the type of model output required.
     * @param type the type of output
     */
    public void setType(String type) {
        this.type = type.toLowerCase();
    }

    /**
     * Set the model to be used.
     * @param modelName the model to be used
     */
    public void setModel(String modelName) {
        try {
            model = Model.getInstanceByName(modelName);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (this.destDir == null) {
            throw new BuildException("destDir attribute is not set");
        }
        if (this.type == null) {
            throw new BuildException("type attribute is not set");
        }
        if (this.model == null) {
            throw new BuildException("model attribute is not set");
        }

        try {
            if ("java".equals(type)) {
                JavaModelOutput mo = new JavaModelOutput(model, destDir);
                mo.process();
            } else {
                throw new BuildException("Unrecognised value for output type: " + type);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
