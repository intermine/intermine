package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;

/**
 * Retrieve the model from an objectstore and serialize it to a file
 * @author Mark Woodbridge
 */
public class GetModelFromObjectStoreTask extends Task
{
    protected String osAlias;
    protected File destFile;

    /**
     * Sets the osAlias
     * @param osAlias String used to identify objectstore
     */
    public void setOsAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the destination file for the serialized version of the model
     * @param destFile the destination file
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (osAlias == null) {
            throw new BuildException("osAlias attribute is not set");
        }
        if (destFile == null) {
            throw new BuildException("destFile attribute is not set");
        }
        BufferedWriter writer = null;
        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);
            writer = new BufferedWriter(new FileWriter(destFile));
            writer.write(os.getModel().toString());
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
