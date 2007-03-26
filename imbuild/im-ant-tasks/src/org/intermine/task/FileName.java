package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
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

    public void setFile(File file) {
        this.file = file;
    }
    
    public void execute() throws BuildException {
        if (file == null) {
            throw new BuildException("file attribute required");
        }
        this.getProject().setProperty(propName, file.getName());
    }
    
    public void setProperty(String propName) {
        this.propName = propName;
    }
}
