package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Run operations on genomic model database after DataLoading
 *
 * @author Richard Smith
 */
public class PostProcessTask extends Task
{

    protected String type, alias;

    /**
     * Set the ObjectStoreWriter alias
     * @param alias name of the ObjectStoreWriter
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the type of post process operation.  Possible types:
     * @param type the type of post process operation
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (type == null) {
            throw new BuildException("type attribute is not set");
        }
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        ObjectStoreWriter osw = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(alias);
            if ("mappings".equals(type)) {
                CalculateLocations cl = new CalculateLocations(osw);
                cl.createLocations();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        } finally {
            osw.close();
        }
    }
}
