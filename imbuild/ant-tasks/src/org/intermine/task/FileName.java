package org.intermine.task;

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
