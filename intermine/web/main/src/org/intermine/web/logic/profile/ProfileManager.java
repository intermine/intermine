package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.iharder.Base64;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.TemplateSummary;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.SavedQueryBinding;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateQueryBinding;

/**
 * Class to manage and persist user profile data such as saved bags
 * @author Mark Woodbridge
 */
public class ProfileManager
{
    private static final Logger LOG = Logger.getLogger(ProfileManager.class);

    protected ObjectStore os;
    protected ObjectStoreWriter osw;
    protected TemplateQueryBinding templateBinding = new TemplateQueryBinding();
    protected CacheMap profileCache = new CacheMap();
    private String superuser = null;

    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @param userProfileOS the object store that hold user profile information
     */
    public ProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS) {
        this.os = os;
        superuser = PropertiesUtil.getProperties().getProperty("superuser.account");
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
     * Return the userprofile ObjectStoreWriter that was passed to the constructor.
     * @return the userprofile  ObjectStoreWriter from the constructor
     */
    public ObjectStoreWriter getProfileObjectStoreWriter() {
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
    public void setPassword(String username, String password) {
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
        }
        return null;
    }

    /**
     * Get a user's Profile using a username
     * @param username the username
     * @return the Profile, or null if one doesn't exist
     */
    public synchronized Profile getProfile(String username) {
        if (username == null) {
            return null;
        }
        Profile profile = (Profile) profileCache.get(username);
        if (profile != null) {
            return profile;
        }

        UserProfile userProfile = getUserProfile(username);

        if (userProfile == null) {
            return null;
        }

        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "id"));
        q.addToSelect(qc); // This loads the objects into the cache
        q.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "userProfile"),
                    ConstraintOp.CONTAINS, new ProxyReference(null, userProfile.getId(),
                        UserProfile.class)));
        Results bags;
        try {
            bags = osw.execute(q);
            bags.setNoOptimise();
            bags.setNoExplain();
            for (Iterator i = bags.iterator(); i.hasNext();) {
                List row = (List) i.next();
                Integer bagId = (Integer) row.get(0);
                InterMineBag bag = new InterMineBag(os, bagId, osw);
                savedBags.put(bag.getName(), bag);
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        Map<String, org.intermine.web.logic.query.SavedQuery> savedQueries =
            new HashMap<String, org.intermine.web.logic.query.SavedQuery>();
        for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
            SavedQuery query = (SavedQuery) i.next();
            try {
                Map queries =
                    SavedQueryBinding.unmarshal(new StringReader(query.getQuery()), savedBags);
                if (queries.size() == 0) {
                    queries =
                        PathQueryBinding.unmarshal(new StringReader(query.getQuery()));
                        MainHelper.checkPathQueries(queries, savedBags);
                    if (queries.size() == 1) {
                        Map.Entry entry = (Map.Entry) queries.entrySet().iterator().next();
                        String name = (String) entry.getKey();
                        savedQueries.put(name,
                                         new org.intermine.web.logic.query.SavedQuery(name, null,
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
        Map<String, TemplateQuery> savedTemplates = new HashMap<String, TemplateQuery>();
        for (Iterator i = userProfile.getSavedTemplateQuerys().iterator(); i.hasNext();) {
            SavedTemplateQuery template = (SavedTemplateQuery) i.next();
            try {
                StringReader sr = new StringReader(template.getTemplateQuery());
                Map templateMap = templateBinding.unmarshal(sr, savedBags);
                String templateName = (String) templateMap.keySet().iterator().next();
                TemplateQuery templateQuery = (TemplateQuery) templateMap.get(templateName);
                templateQuery.setSavedTemplateQuery(template);
                savedTemplates.put(templateName, templateQuery);
                Iterator summaryIter = template.getSummaries().iterator();
                if (summaryIter.hasNext()) {
                    TemplateSummary summary = (TemplateSummary) summaryIter.next();
                    templateQuery.setPossibleValues((HashMap) Base64.decodeToObject(summary
                                .getSummary()));
                }
            } catch (Exception err) {
                // Ignore rows that don't unmarshal (they probably reference
                // another model.
                LOG.warn("Failed to unmarshal saved template query: "
                         + template.getTemplateQuery(), err);
            }
        }
        convertTemplateKeywordsToTags(savedTemplates, username);
        profile = new Profile(this, username, userProfile.getId(), userProfile.getPassword(),
                savedQueries, savedBags, savedTemplates);
        profileCache.put(username, profile);
        return profile;
    }

    /**
     * Create 'aspect:xxx' tags for each keyword of each template.
     * Public so that LoadDefaultTemplates task can call in.
     * @param savedTemplates Map from template name to TemplateQuery
     * @param username username under which to store tags
     */
    public void convertTemplateKeywordsToTags(Map<String, TemplateQuery> savedTemplates,
                                              String username) {
        TagManager tagManager = getTagManager();
        for (Iterator<TemplateQuery> iter = savedTemplates.values().iterator(); iter.hasNext(); ) {
            TemplateQuery tq = iter.next();
            String keywords = tq.getKeywords();
            if (StringUtils.isNotEmpty(keywords)) {
                String aspects[] = keywords.split(",");
                for (int i = 0; i < aspects.length; i++) {
                    String aspect = aspects[i].trim();
                    String tag = TagNames.IM_ASPECT_PREFIX + aspect;
                    if (tagManager.getTags(tag, tq.getName(), TagTypes.TEMPLATE, username).size()
                            == 0) {
                        getTagManager().addTag(tag, tq.getName(), TagTypes.TEMPLATE, username);
                    }
                }
            }
        }
    }

    private TagManager getTagManager() {
        return new TagManagerFactory(this).getTagManager();
    }

    /**
     * Synchronise a user's Profile with the backing store
     * @param profile the Profile
     */
    public void saveProfile(Profile profile) {
        Integer userId = profile.getUserId();
        try {
            UserProfile userProfile = getUserProfile(userId);
            if (userProfile != null) {
                for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
                    osw.delete((InterMineObject) i.next());
                }

                for (Iterator i = userProfile.getSavedTemplateQuerys().iterator();
                     i.hasNext();) {
                    osw.delete((InterMineObject) i.next());
                }
            } else {
                // Should not happen
                throw new RuntimeException("The UserProfile is null");
//                 userProfile = new UserProfile();
//                 userProfile.setUsername(profile.getUsername());
//                 userProfile.setPassword(profile.getPassword());
//                 userProfile.setId(userId);
            }

            for (Iterator i = profile.getSavedQueries().entrySet().iterator(); i.hasNext();) {
                org.intermine.web.logic.query.SavedQuery query = null;
                try {
                    Map.Entry entry = (Map.Entry) i.next();
                    query = (org.intermine.web.logic.query.SavedQuery) entry.getValue();
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
                    template = (TemplateQuery) entry.getValue();
                    SavedTemplateQuery savedTemplate = template.getSavedTemplateQuery();
                    if (savedTemplate == null) {
                        savedTemplate = new SavedTemplateQuery();
                    }
                    savedTemplate.setTemplateQuery(templateBinding.marshal(template));
                    savedTemplate.setUserProfile(userProfile);
                    osw.store(savedTemplate);
                    template.setSavedTemplateQuery(savedTemplate);
                } catch (Exception e) {
                    LOG.error("Failed to marshal and save template: " + template, e);
                }
            }

            osw.store(userProfile);
            profile.setUserId(userProfile.getId());
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a profile in the userprofile database.
     *
     * @param profile a Profile object
     */
    public void createProfile(Profile profile) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername(profile.getUsername());
        userProfile.setPassword(profile.getPassword());
        //userProfile.setId(userId);

        try {
            osw.store(userProfile);
            profile.setUserId(userProfile.getId());
            for (InterMineBag bag : profile.getSavedBags().values()) {
                bag.setProfileId(userProfile.getId(), osw);
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        saveProfile(profile);
    }

    /**
     * Perform a query to retrieve a user's backing UserProfile
     * @param username the username
     * @return the relevant UserProfile
     */
    public UserProfile getUserProfile(String username) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("username");
        try {
            profile = (UserProfile) osw.getObjectByExample(profile, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
        return profile;
    }

    /**
     * Perform a query to retrieve a user's backing UserProfile
     *
     * @param userId the id of the user
     * @return the relevant UserProfile
     */
    public UserProfile getUserProfile(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            return (UserProfile) osw.getObjectById(userId, UserProfile.class);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
    }

    /**
     * Return a List of the usernames in all of the stored profiles.
     * @return the usernames
     */
    public List getProfileUserNames() {
        Query q = new Query();
        QueryClass qcUserProfile = new QueryClass(UserProfile.class);
        QueryField qfUserName = new QueryField(qcUserProfile, "username");
        q.addFrom(qcUserProfile);
        q.addToSelect(qfUserName);

        SingletonResults res = osw.executeSingleton(q);

        List usernames = new ArrayList();

        Iterator resIter = res.iterator();

        while (resIter.hasNext()) {
            usernames.add(resIter.next());
        }

        return usernames;
    }


    /**
     * @return the superuser
     */
    public String getSuperuser() {
        return superuser;
    }

    /**
     * @param superuser the superuser to set
     */
    public void setSuperuser(String superuser) {
        this.superuser = superuser;
    }
}
