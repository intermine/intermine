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

import java.io.FileWriter;
import java.io.IOException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.tracker.xml.TemplateTrackBinding;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Task to write an XML file of template track contained in the user profile database.
 *
 * @author dbutano
 */

public class TemplateTrackWriteTask extends Task
{
    protected String fileName;
    private String userProfileAlias;

    /**
     * Set the name of the file to write to.
     * @param fileName the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Set the alias of the userprofile database.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Execute the task - write the template tracks as XML.
     * @throws BuildException if there is a problem while writing to the file or reading the
     * template tracks.
     */
    public void execute() {
        if (fileName == null) {
            throw new BuildException("fileName parameter not set");
        }

        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
        } catch (IOException e) {
            throw new BuildException("failed to open output file: " + fileName, e);
        }

        try {
            ObjectStoreWriter userProfileOS =
                ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);

            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(fw);
            TemplateTrackBinding.marshal(userProfileOS, writer);
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                throw new BuildException("failed to close output file: " + fileName, e);
            }
        }
    }
}
