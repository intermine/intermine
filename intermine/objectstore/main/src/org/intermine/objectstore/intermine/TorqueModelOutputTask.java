package org.intermine.objectstore.intermine;

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

import java.io.File;

import org.intermine.objectstore.ObjectStoreFactory;

/**
 * Creates and runs a ModelOutput process to generate java or config files.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class TorqueModelOutputTask extends Task
{
    protected File destFile;
    protected DatabaseSchema schema;

    /**
     * Sets the file to which the data should be written.
     *
     * @param destFile the file location
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Set the ObjectStore for which to generate the data.
     * 
     * @param osName the ObjectStore name to be used
     */
    public void setOsName(String osName) {
        try {
            ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) ObjectStoreFactory
                .getObjectStore(osName);
            schema = os.getSchema();
        } catch (ClassCastException e) {
            throw new BuildException("Objectstore " + osName
                    + " is not an ObjectStoreInterMineImpl", e);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        } catch (Error e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.destFile == null) {
            throw new BuildException("destFile attribute is not set");
        }
        if (this.schema == null) {
            throw new BuildException("osName attribute is not set");
        }

        TorqueModelOutput mo = new TorqueModelOutput(schema, destFile);

        mo.process();
    }
}
