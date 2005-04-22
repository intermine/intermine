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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.web.Profile;
import org.intermine.web.ProfileBinding;
import org.intermine.web.ProfileManager;

/**
 * Task to write an XML file of a webapp userprofile object store.
 *
 * @author Kim Rutherford
 */

public class ProfileWriteTask extends Task
{
    protected String fileName;

    /**
     * Set the name of the file to write to.
     * @param fileName the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Execute the task - write the profiles as XML.
     * @throws BuildException if there is a problem while writing to the file or reading the
     * profiles.
     */
    public void execute() throws BuildException {
        if (fileName == null) {
            throw new BuildException("fileName parameter not set");
        }

        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        FileWriter fw = null;
        
        try {
            fw = new FileWriter(fileName);
        } catch (IOException e) {
            throw new BuildException("failed to open output file: " + fileName, e);
        }

        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore();
            ProfileManager pm = new ProfileManager(os);

            XMLOutputFactory factory = XMLOutputFactory.newInstance();

            XMLStreamWriter writer = factory.createXMLStreamWriter(fw);
            writer.writeStartElement("userprofiles");

            List usernames = pm.getProfileUserNames();

            Iterator iter = usernames.iterator();

            while (iter.hasNext()) {
                Profile profile = pm.getProfile((String) iter.next());
                ProfileBinding.marshal(profile, os.getModel(), writer);
            }
            writer.writeEndElement();

        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            try {
                fw.close();
            } catch (IOException e) {
                throw new BuildException("failed to close output file: " + fileName, e);
            }
        }
    }
}
