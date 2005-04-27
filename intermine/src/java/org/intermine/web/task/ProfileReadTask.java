package org.intermine.web.task;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.web.ProfileManager;
import org.intermine.web.ProfileManagerBinding;

/**
 * Task to read an XML file of a webapp userprofiles into a userprofile ObjectStore.
 *
 * @author Kim Rutherford
 */

public class ProfileReadTask extends Task
{
    protected String fileName;

    /**
     * Set the name of the file to read from.
     * @param fileName the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Execute the task - read the profiles.
     * @throws BuildException if there is a problem while reading from the file or writing to the
     * profiles.
     */
    public void execute() throws BuildException {
        if (fileName == null) {
            throw new BuildException("fileName parameter not set");
        }

        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        FileReader reader = null;
        
        try {
            reader = new FileReader(fileName);
        } catch (IOException e) {
            throw new BuildException("failed to open input file: " + fileName, e);
        }

        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore();
            ProfileManager pm = new ProfileManager(os);

            ProfileManagerBinding.unmarshal(reader, pm, os);
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            try {
                reader.close();
            } catch (IOException e) {
                throw new BuildException("failed to close output file: " + fileName, e);
            }
        }
    }
}
