package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import org.apache.log4j.Logger;

import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Class to manage and persist user profile data such as saved bags
 * @author Mark Woodbridge
 */
public class ProfileManager
{
    private static final Logger LOG = Logger.getLogger(ProfileManager.class);
    
    protected ObjectStore os;
    protected ObjectStoreWriter osw;
    protected InterMineBagBinding bagBinding = new InterMineBagBinding();
    protected PathQueryBinding queryBinding = new PathQueryBinding();
    protected TemplateQueryBinding templateBinding = new TemplateQueryBinding();
    
    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @throws ObjectStoreException if the user profile database cannot be found
     */
    public ProfileManager(ObjectStore os) throws ObjectStoreException {
        this.os = os;
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile");
    }

    /**
     * Close this ProfileManager
     */
    public void close() {
        osw.close();
    }
    
    /**
     * Check whether a user already has a Profile
     * @param username the username
     * @return true if a profile exists
     */
    public boolean hasProfile(String username) {
        return getProfile(username) != null;
    }

    /**
     * Validate a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     * @return true if password is valid
     */
    public boolean validPassword(String username, String password) {
        return getProfile(username).getPassword().equals(password);
    }

    /**
     * Change a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     */
    public void setPassword(String username, String password) {
        UserProfile userProfile = getProfile(username);
        userProfile.setPassword(password);
        try {
            osw.store(userProfile);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @return password the password
     */
    public String getPassword(String username) {
        UserProfile userProfile = getProfile(username);
        return userProfile.getPassword();
    }

    /**
     * Get a user's Profile
     * @param username the username
     * @param password the password
     * @return the Profile, or null if one doesn't exist
     */
    public Profile getProfile(String username, String password) {
        Profile profile = null;
        if (hasProfile(username) && validPassword(username, password)) {
            UserProfile userProfile = getProfile(username);
            Map savedBags = new HashMap();
            for (Iterator i = userProfile.getSavedBags().iterator(); i.hasNext();) {
                SavedBag bag = (SavedBag) i.next();
                try {
                    savedBags.putAll(bagBinding.unmarshal(new StringReader(bag.getBag()), os));
                } catch (Exception _) {
                    // Ignore rows that don't unmarshal (they probably reference
                    // another model.
                    LOG.warn("Failed to unmarshal saved bag: " + bag.getBag());
                }
            }
            Map savedQueries = new HashMap();
            for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
                SavedQuery query = (SavedQuery) i.next();
                try {
                    savedQueries.putAll(queryBinding.unmarshal(new StringReader(query.getQuery())));
                } catch (Exception _) {
                    // Ignore rows that don't unmarshal (they probably reference
                    // another model.
                    LOG.warn("Failed to unmarshal saved query: " + query.getQuery());
                }
            }
            Map savedTemplates = new HashMap();
            for (Iterator i = userProfile.getSavedTemplateQuerys().iterator(); i.hasNext();) {
                SavedTemplateQuery template = (SavedTemplateQuery) i.next();
                try {
                    savedTemplates.putAll(
                        templateBinding.unmarshal(new StringReader(template.getTemplateQuery())));
                } catch (Exception _) {
                    // Ignore rows that don't unmarshal (they probably reference
                    // another model.
                    LOG.warn("Failed to unmarshal saved temlplate query: "
                              + template.getTemplateQuery());
                }
            }
            profile = new Profile(this, username, savedQueries, savedBags, savedTemplates);
        }
        return profile;
    }
    
    /**
     * Synchronise a user's Profile with the backing store
     * @param profile the Profile
     */
    public void saveProfile(Profile profile) {
        String username = profile.getUsername();
        if (username != null) {
            try {
                UserProfile userProfile = null;
                if (hasProfile(username)) {
                    userProfile = getProfile(username);
                    for (Iterator i = userProfile.getSavedBags().iterator(); i.hasNext();) {
                        osw.delete((InterMineObject) i.next());
                    }

                    for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
                        osw.delete((InterMineObject) i.next());
                    }
                    
                    for (Iterator i = userProfile.getSavedTemplateQuerys().iterator();
                                                                                i.hasNext();) {
                        osw.delete((InterMineObject) i.next());
                    }
                } else {
                    userProfile = new UserProfile();
                    userProfile.setUsername(username);
                }

                for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
                    InterMineBag bag = null;
                    try {
                        Map.Entry entry = (Map.Entry) i.next();
                        String bagName = (String) entry.getKey();
                        bag = (InterMineBag) entry.getValue();
                        SavedBag savedBag = new SavedBag();
                        savedBag.setBag(bagBinding.marshal(bag, bagName));
                        savedBag.setUserProfile(userProfile);
                        osw.store(savedBag);
                    } catch (Exception _) {
                        LOG.error("Failed to marshal and save bag: " + bag);
                    }
                }
                
                for (Iterator i = profile.getSavedQueries().entrySet().iterator(); i.hasNext();) {
                    PathQuery query = null;
                    try {
                        Map.Entry entry = (Map.Entry) i.next();
                        String queryName = (String) entry.getKey();
                        query = (PathQuery) entry.getValue();
                        SavedQuery savedQuery = new SavedQuery();
                        savedQuery.setQuery(queryBinding.marshal(query, queryName,
                                                                 os.getModel().getName()));
                        savedQuery.setUserProfile(userProfile);
                        osw.store(savedQuery);
                    } catch (Exception _) {
                        LOG.error("Failed to marshal and save query: " + query);
                    }
                }
                
                for (Iterator i = profile.getSavedTemplates().entrySet().iterator(); i.hasNext();) {
                    TemplateQuery template = null;
                    try {
                        Map.Entry entry = (Map.Entry) i.next();
                        String templateName = (String) entry.getKey();
                        template = (TemplateQuery) entry.getValue();
                        SavedTemplateQuery savedTemplate = new SavedTemplateQuery();
                        savedTemplate.setTemplateQuery(templateBinding.marshal(template));
                        savedTemplate.setUserProfile(userProfile);
                        osw.store(savedTemplate);
                    } catch (Exception _) {
                        LOG.error("Failed to marshal and save template: " + template);
                    }
                }
                 
                osw.store(userProfile);
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Perform a query to retrieve a user's backing UserProfile
     * @param username the username
     * @return the relevant UserProfile
     */
    protected UserProfile getProfile(String username) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        Set fieldNames = new HashSet();
        fieldNames.add("username");
        try {
            profile = (UserProfile) osw.getObjectByExample(profile, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
        return profile;
    }
}
