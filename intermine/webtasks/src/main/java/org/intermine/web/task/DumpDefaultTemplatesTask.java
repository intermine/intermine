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
import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.xml.ProfileBinding;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.XmlUtil;

/**
 * Dump templates and configuration tags.
 *
 * @author Thomas Riley
 */
public class DumpDefaultTemplatesTask extends Task
{

    protected String fileName;
    private String osAlias;
    private String userProfileAlias;
    private String username;


    /**
     * Set the account name to laod template to.
     * @param user username to load templates into
     */
    public void setUsername(String user) {
        username = user;
    }

    /**
     * Set the name of the file to write to.
     * @param fileName the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Set the alias of the main object store.
     * @param osAlias the object store alias
     */
    public void setOSAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the alias of the userprofile object store.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Write templates and tags to a file.
     * {@inheritDoc}
     */
    @Override
    public void execute() {
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
            ObjectStore os = ObjectStoreFactory.getObjectStore(osAlias);
            ObjectStoreWriter userProfileOS =
                ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
            ProfileManager pm = new ProfileManager(os, userProfileOS);

            if (!pm.hasProfile(username)) {
                throw new BuildException("no such user " + username);
            }

            Profile superProfile = pm.getProfile(username, pm.getPassword(username));

            StringWriter sw = new StringWriter();
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);

            write(writer, superProfile, os);
            writer.close();

            fw.write(XmlUtil.indentXmlSimple(sw.toString()));
            fw.close();
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

    /**
     * Write template-queries and tags elements.
     * @param writer xml writer to write to
     * @param superProfile superuser profile
     * @param os objectstore
     * @throws Exception if something goes wrong
     */
    protected void write(XMLStreamWriter writer, Profile superProfile, ObjectStore os)
        throws Exception {

        log("Writing tags and templates...");
        ProfileBinding.marshal(superProfile, os, writer, false, false, true, false, true, true,
                PathQuery.USERPROFILE_VERSION, null);
        log("Done.");
    }
}
