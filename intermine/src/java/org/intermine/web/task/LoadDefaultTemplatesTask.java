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
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.RequestPasswordAction;
import org.intermine.web.TemplateQuery;
import org.intermine.web.TemplateQueryBinding;

/**
 * Load template queries form an XML file into a given user profile.
 * 
 * @author Thomas Riley
 */

public class LoadDefaultTemplatesTask extends Task
{
    private static final Logger LOG = Logger.getLogger(LoadDefaultTemplatesTask.class);
    
    protected String xmlFile;
    protected String username;
    
    /**
     * Set the templates xml file.
     * @param file to xml file
     */
    public void setTemplatesXml(String file) {
        xmlFile = file;
    }
    
    /**
     * Set the account name to laod template to.
     * @param user username to load templates into
     */
    public void setUsername(String user) {
        username = user;
    }

    /**
     * Load templates from an xml file into a userprofile account.
     * 
     * @see Task#execute
     */
    public void execute() throws BuildException {
        // Needed so that STAX can find it's implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore();
            ProfileManager pm = new ProfileManager(os);
            Reader templateQueriesReader = new FileReader(xmlFile);
            Map templateQueries = new TemplateQueryBinding().unmarshal(templateQueriesReader);
            Profile profile = null;
            if (!pm.hasProfile(username)) {
                LOG.info("Creating profile for " + username);
                String password = RequestPasswordAction.generatePassword();
                profile = new Profile(pm, username, password,
                                      new HashMap(), new HashMap(), new HashMap());
                pm.saveProfile(profile);
            } else {
                LOG.warn("Profile for " + username + ", clearing template queries");
                profile = pm.getProfile(username, pm.getPassword(username));
                Map tmpls = new HashMap(profile.getSavedTemplates());
                Iterator iter = tmpls.keySet().iterator();
                while (iter.hasNext()) {
                    profile.deleteTemplate((String) iter.next());
                }
            }
            if (profile.getSavedTemplates().size() == 0) {
                Iterator iter = templateQueries.values().iterator();
                while (iter.hasNext()) {
                    TemplateQuery template = (TemplateQuery) iter.next();
                    String append = "";
                    if (!template.isValid()) {
                        append = " [invalid]";
                    }
                    log("Adding template \"" + template.getName() + "\"" + append);
                    profile.saveTemplate(template.getName(), template);
                }
            }
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
