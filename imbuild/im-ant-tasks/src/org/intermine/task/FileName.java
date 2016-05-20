package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
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

/**
 *
 *
 * @author Thmoas Riley
 */
public class FileName extends Task
{
    private String propName;
    private File file;

    /**
     * @param file filename
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * @throws BuildException if can't build
     */
    public void execute() throws BuildException {
        if (file == null) {
            throw new BuildException("file attribute required");
        }
        this.getProject().setProperty(propName, file.getName());
    }

    /**
     * @param propName property name
     */
    public void setProperty(String propName) {
        this.propName = propName;
    }
}
