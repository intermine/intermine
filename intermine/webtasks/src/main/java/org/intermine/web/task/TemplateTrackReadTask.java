package org.intermine.web.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.tracker.xml.TemplateTrackBinding;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Task to read an templatetrack XML file of a webapp and saved into the userprofile database.
 *
 * @author dbutano
 */

public class TemplateTrackReadTask extends Task
{
    private String fileName;
    private String userProfileAlias;

    /**
     * Set the name of the file to read from.
     * @param fileName the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Set the alias of the userprofile object store.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Execute the task - read the profiles.
     * @throws BuildException if there is a problem while reading from the file or writing to the
     * profiles.
     */
    public void execute() {
        if (fileName == null) {
            throw new BuildException("fileName parameter not set");
        }

        if (userProfileAlias == null) {
            throw new BuildException("userProfileAlias parameter not set");
        }

        FileReader reader = null;

        try {
            reader = new FileReader(fileName);
        } catch (IOException e) {
            throw new BuildException("failed to open input file: " + fileName, e);
        }
        ObjectStoreWriter uosw;
        try {
            uosw = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
            TemplateTrackBinding.unmarshal(reader, uosw);
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new BuildException("failed to close input file: " + fileName, e);
            }
        }
    }
}
