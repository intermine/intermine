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

import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.bag.IdUpgrader;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineBagBinding;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

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
    protected TemplateQueryBinding templateBinding = new TemplateQueryBinding();

    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @param userProfileOS the object store that hold user profile information
     * @param upgrader 
     * @throws ObjectStoreException if the user profile database cannot be found
     */
    public ProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS)
        throws ObjectStoreException {
        this.os = os;
        this.osw = userProfileOS;
    }

    /**
     * Return the ObjectStore that was passed to the constructor.
     * @return the ObjectStore from the constructor
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * Return the userprofile ObjectStore that was passed to the constructor.
     * @return the userprofile  ObjectStore from the constructor
     */
    public ObjectStore getUserProfileObjectStore() {
        return osw;
    }
    
    /**
     * Close this ProfileManager
     *
     * @throws ObjectStoreException in exceptional circumstances
     */
    public void close() throws ObjectStoreException {
        osw.close();
    }

    /**
     * Check whether a user already has a Profile
     * @param username the username
     * @return true if a profile exists
     */
    public boolean hasProfile(String username) {
        return getUserProfile(username) != null;
    }

    /**
     * Validate a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     * @return true if password is valid
     */
    public boolean validPassword(String username, String password) {
        return getUserProfile(username).getPassword().equals(password);
    }

    /**
     * Change a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     */
    private void setPassword(String username, String password) {
        UserProfile userProfile = getUserProfile(username);
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
        UserProfile userProfile = getUserProfile(username);
        return userProfile.getPassword();
    }

    /**
     * Get a user's Profile using a username and password.
     * @param username the username
     * @param password the password
     * @return the Profile, or null if one doesn't exist
     */
    public Profile getProfile(String username, String password) {
        if (hasProfile(username) && validPassword(username, password)) {
            return getProfile(username);
        } else {
            return null;
        }
    }

    /**
     * Get a user's Profile using a username
     * @param username the username
     * @return the Profile, or null if one doesn't exist
     */
    public Profile getProfile(String username) {
        UserProfile userProfile = getUserProfile(username);

        if (userProfile == null) {
            return null;
        }

        Map savedBags = new HashMap();
        for (Iterator i = userProfile.getSavedBags().iterator(); i.hasNext();) {
            SavedBag bag = (SavedBag) i.next();
            try {
                savedBags.putAll(InterMineBagBinding.unmarshal(
                        new StringReader(bag.getBag()), os, new IdUpgrader() {

                            public Set getNewIds(InterMineObject oldObject,
                                    ObjectStore os) {
                                throw new RuntimeException(
                                        "Shouldn't call getNewIds() in a"
                                                + " running webapp");
                            }

                        }));
            } catch (Exception err) {
                // Ignore rows that don't unmarshal (they probably reference
                // another model.
                LOG.warn("Failed to unmarshal saved bag: " + bag.getBag());
            }
        }
        Map savedQueries = new HashMap();
        for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
            SavedQuery query = (SavedQuery) i.next();
            try {
                Map queries = SavedQueryBinding.unmarshal(new StringReader(query.getQuery()));
                if (queries.size() == 0) {
                    queries = PathQueryBinding.unmarshal(new StringReader(query.getQuery()));
                    if (queries.size() == 1) {
                        Map.Entry entry = (Map.Entry) queries.entrySet().iterator().next();
                        String name = (String) entry.getKey();
                        savedQueries.put(name, new org.intermine.web.SavedQuery(name, null,
                                (PathQuery) entry.getValue()));
                    }
                } else {
                    savedQueries.putAll(queries);
                }
            } catch (Exception err) {
                // Ignore rows that don't unmarshal (they probably reference
                // another model.
                LOG.warn("Failed to unmarshal saved query: " + query.getQuery());
            }
        }
        Map savedTemplates = new HashMap();
        for (Iterator i = userProfile.getSavedTemplateQuerys().iterator(); i.hasNext();) {
            SavedTemplateQuery template = (SavedTemplateQuery) i.next();
            try {
                StringReader sr = new StringReader(template.getTemplateQuery());
                savedTemplates.putAll(templateBinding.unmarshal(sr));
            } catch (Exception err) {
                // Ignore rows that don't unmarshal (they probably reference
                // another model.
                LOG.warn("Failed to unmarshal saved template query: "
                         + template.getTemplateQuery());
            }
        }
        return new Profile(this, username, userProfile.getPassword(), savedQueries, savedBags,
                           savedTemplates);
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
                    userProfile = getUserProfile(username);
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
                    userProfile.setPassword(profile.getPassword());
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
                    } catch (Exception e) {
                        LOG.error("Failed to marshal and save bag: " + bag, e);
                    }
                }

                for (Iterator i = profile.getSavedQueries().entrySet().iterator(); i.hasNext();) {
                    org.intermine.web.SavedQuery query = null;
                    try {
                        Map.Entry entry = (Map.Entry) i.next();
                        String queryName = (String) entry.getKey();
                        query = (org.intermine.web.SavedQuery) entry.getValue();
                        SavedQuery savedQuery = new SavedQuery();
                        savedQuery.setQuery(SavedQueryBinding.marshal(query));
                        savedQuery.setUserProfile(userProfile);
                        osw.store(savedQuery);
                    } catch (Exception e) {
                        LOG.error("Failed to marshal and save query: " + query, e);
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
                    } catch (Exception e) {
                        LOG.error("Failed to marshal and save template: " + template, e);
                    }
                }

                osw.store(userProfile);
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }

            setPassword(username, profile.getPassword());
        }
    }

    /**
     * Perform a query to retrieve a user's backing UserProfile
     * @param username the username
     * @return the relevant UserProfile
     */
    protected UserProfile getUserProfile(String username) {
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

    /**
     * Return a List of the username in all of the stored profiles.
     * @return the usernames
     */
    public List getProfileUserNames() {
        Query q = new Query();
        QueryClass qcUserProfile = new QueryClass(UserProfile.class);
        QueryField qfUserName = new QueryField(qcUserProfile, "username");
        q.addFrom(qcUserProfile);
        q.addToSelect(qfUserName);

        Results res = new Results(q, osw, osw.getSequence());

        List usernames = new ArrayList();

        Iterator resIter = res.iterator();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            usernames.add(rr.get(0));
        }

        return usernames;
    }
}
