package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task for creating OverlapRelation objects.
 *
 * @author Kim Rutherford
 */

public class CreateOverlapRelationsTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PostProcessTask.class);

    protected String objectStoreWriter, classesToIgnore;
    protected ObjectStoreWriter osw;

    /**
     * Sets the value of objectStoreWriter.
     * @param objectStoreWriter an objectStoreWriter alias for operations
     */
    public void setObjectStoreWriter(String objectStoreWriter) {
        this.objectStoreWriter = objectStoreWriter;
    }

    /**
     * Sets the value of classesToIgnore.
     * @param classNamesToIgnore a comma separated list of the names of those classes that should be
     * ignored when searching for overlaps.  Sub classes to these classes are ignored too
     */
    public void setClassesToIgnore(String classesToIgnore) {
        this.classesToIgnore = classesToIgnore;
    }

    private ObjectStoreWriter getObjectStoreWriter() throws Exception {
        if (objectStoreWriter == null) {
            throw new BuildException("objectStoreWriter attribute is not set");
        }
        if (osw == null) {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(objectStoreWriter);
        }
        return osw;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (objectStoreWriter == null) {
            throw new BuildException("objectStoreWriter attribute is not set");
        }
        if (classesToIgnore == null) {
            throw new BuildException("classesToIgnore attribute is not set");
        }

        try {
            CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
            LOG.info("Starting CalculateLocations.createOverlapRelations()");
            List classesToIgnoreList = new ArrayList();

            String[] classNames = StringUtil.split(classesToIgnore, ",");

            for (int i = 0; i < classNames.length; i++) {
                classesToIgnoreList.add(classNames[i].trim());
            }

            cl.createOverlapRelations(classesToIgnoreList);
        } catch (Exception e) {
            throw new BuildException("Failed to create OverlapRelations");
        }
    }
}
