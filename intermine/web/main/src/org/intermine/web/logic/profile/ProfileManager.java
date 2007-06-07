package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;

import javax.servlet.ServletContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.TemplateSummary;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CacheMap;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.PathQueryBinding;
import org.intermine.web.logic.query.SavedQueryBinding;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateQueryBinding;

import net.sourceforge.iharder.Base64;

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
    private Map<String, TagChecker> tagCheckers = null;
    private HashMap<MultiKey, List<Tag>> tagCache = null;
    private final ServletContext servletContext;

    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @param userProfileOS the object store that hold user profile information
     * @param servletContext global ServletContext object
     */
    public ProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS, 
                          ServletContext servletContext) {
        this.os = os;
        this.servletContext = servletContext;
        tagCheckers = makeTagCheckers(os.getModel());
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
    public ObjectStoreWriter getUserProfileObjectStore() {
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
        } else {
            return null;
        }
    }

    /**
     * Get a user's Profile using a username
     * @param username the username
     * @param servletContext global ServletContext object
     * @return the Profile, or null if one doesn't exist
     */
    public synchronized Profile getProfile(String username) {
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
                    SavedQueryBinding.unmarshal(new StringReader(query.getQuery()), savedBags, 
                                                servletContext);
                if (queries.size() == 0) {
                    queries = 
                        PathQueryBinding.unmarshal(new StringReader(query.getQuery()), savedBags, 
                                                   servletContext);
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
                Map templateMap = templateBinding.unmarshal(sr, savedBags, servletContext);
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
        for (Iterator<TemplateQuery> iter = savedTemplates.values().iterator(); iter.hasNext(); ) {
            TemplateQuery tq = iter.next();
            String keywords = tq.getKeywords();
            if (StringUtils.isNotEmpty(keywords)) {
                String aspects[] = keywords.split(",");
                for (int i = 0; i < aspects.length; i++) {
                    String aspect = aspects[i].trim();
                    String tag = "aspect:" + aspect;
                    if (getTags(tag, tq.getName(), TagTypes.TEMPLATE, username).size() == 0) {
                        addTag(tag, tq.getName(), TagTypes.TEMPLATE, username);
                    }
                }
            }
        }
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
     * Delete a tag object from the database.
     * @param tag Tag object
     */
    public synchronized void deleteTag(Tag tag) {
        try {
            LOG.error("deleteTag() removing cache");
            tagCache = null;
            getUserProfileObjectStore().delete(tag);
        } catch (ObjectStoreException err) {
            LOG.error(err);
        }
    }

    /**
     * Get Tag by object id.
     * @param id intermine object id
     * @return Tag
     * @throws ObjectStoreException if something goes wrong
     */
    public synchronized Tag getTagById(int id) throws ObjectStoreException {
        return (Tag) getUserProfileObjectStore().getObjectById(new Integer(id), Tag.class);
    }

    /**
     * Return a List of Tags that match all the arguments.  Any null arguments will be treated as
     * wildcards.
     * @param tagName the tag name - any String
     * @param objectIdentifier an object identifier that is appropriate for the given tag type
     * (eg. "Department.name" for the "collection" type)
     * @param type the tag type (eg. "collection", "reference", "attribute", "bag")
     * @param userName the use name this tag is associated with
     * @return the matching Tags
     */
    public synchronized List<Tag> getTags(String tagName, String objectIdentifier, String type,
                        String userName) {
        Map<MultiKey, List<Tag>> cache = getTagCache();
        MultiKey key = makeKey(tagName, objectIdentifier, type, userName);

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);

        q.addFrom(qc);
        q.addToSelect(qc);

        QueryField orderByField = new QueryField(qc, "tagName");

        q.addToOrderBy(orderByField);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        if (tagName != null) {
            QueryValue qv = new QueryValue(tagName);
            QueryField qf = new QueryField(qc, "tagName");
            SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.MATCHES, qv);
            cs.addConstraint(c);
        }

        if (objectIdentifier != null) {
            QueryValue qv = new QueryValue(objectIdentifier);
            QueryField qf = new QueryField(qc, "objectIdentifier");
            SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.MATCHES, qv);
            cs.addConstraint(c);
        }

        if (type != null) {
            QueryValue qv = new QueryValue(type);
            QueryField qf = new QueryField(qc, "type");
            SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.MATCHES, qv);
            cs.addConstraint(c);
        }

        if (userName != null) {
            QueryClass userProfileQC = new QueryClass(UserProfile.class);
            q.addFrom(userProfileQC);
            QueryValue qv = new QueryValue(userName);
            QueryField qf = new QueryField(userProfileQC, "username");
            SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.MATCHES, qv);
            cs.addConstraint(c);

            QueryObjectReference qr = new QueryObjectReference(qc, "userProfile");

            ContainsConstraint cc =
                new ContainsConstraint(qr, ConstraintOp.CONTAINS, userProfileQC);
            cs.addConstraint(cc);
        }
        q.setConstraint(cs);

        ObjectStore userprofileOS = osw.getObjectStore();

        SingletonResults results = userprofileOS.executeSingleton(q);

        addToCache(cache, key, results);

        return results;
    }


    /**
     * Given a Map from name to WebSearchable, return a Map that contains only those name,
     * WebSearchable pairs where the name is tagged with all of the tags listed.
     * @param webSearchables the Map to filter
     * @param tagNames the tag names to use for filtering
     * @param tagType the tag type (from TagTypes)
     * @param userName the user name to pass to getTags()
     * @return the filtered Map
     */
    public Map<String, WebSearchable>
        filterByTags(Map<String, ? extends WebSearchable> webSearchables, 
                     List<String> tagNames, String tagType, String userName) {
        Map<String, WebSearchable> returnMap = new HashMap<String, WebSearchable>(webSearchables);

        // prime the cache
        for (String tagName: tagNames) {
            getTags(tagName, null, tagType, userName);
        }
        for (String tagName: tagNames) {
            for (Map.Entry<String, ? extends WebSearchable> entry: webSearchables.entrySet()) {
                String webSearchableName = entry.getKey();
                if (getTags(tagName, webSearchableName, tagType, userName).size() == 0) {
                    returnMap.remove(webSearchableName);
                }
            }
        }

        return returnMap;
    }
    
    private MultiKey makeKey(String tagName, String objectIdentifier, String type,
                             String userName) {
        return new MultiKey(tagName, objectIdentifier, type, userName);
    }

    private void addToCache(Map<MultiKey, List<Tag>> cache, MultiKey key, List<Tag> results) {
        LOG.error("adding to cache: " + key);
        cache.put(key, new ArrayList<Tag>(results));

        int keyNullPartCount = 0;

        for (int i = 0; i < 4; i++) {
            if (key.getKey(i) == null) {
                keyNullPartCount++;
            }
        }

        Iterator resIter = results.iterator();

        while (resIter.hasNext()) {
            Tag tag = (Tag) resIter.next();

            Object[] tagKeys = new Object[4];
            tagKeys[0] = tag.getTagName();
            tagKeys[1] = tag.getObjectIdentifier();
            tagKeys[2] = tag.getType();
            tagKeys[3] = tag.getUserProfile().getUsername();

//            if (keyNullPartCount == 2) {
//                // special case that allows the cache to be primed in a struts controller
//                // eg. calling getTags(null, null, "template", "superuser@flymine") will prime the
//                // cache so that getTags(null, "some_id", "template", "superuser@flymine") and
//                // getTags("some_tag", null, "template", "superuser@flymine") will be fast
//                for (int i = 0; i < 4; i++) {
//                    if (key.getKey(i) == null) {
//                        Object[] keysCopy = (Object[]) tagKeys.clone();
//                        keysCopy[i] = null;
//                        MultiKey keyCopy = new MultiKey(keysCopy);
//                        if (cache.containsKey(keyCopy)) {
//                            List existingList = (List) cache.get(keyCopy);
//                            if (existingList instanceof ArrayList) {
//                                existingList.add(tag);
//                            } else {
//                                ArrayList listCopy = new ArrayList(existingList);
//                                listCopy.add(tag);
//                                cache.put(keyCopy, listCopy);
//                            }
//                        } else {
//                            List newList = new ArrayList();
//                            newList.add(tag);
//                            cache.put(keyCopy, newList);
//                        }
//                    }
//                }
//
//            }
        }

    }

    private Map<MultiKey, List<Tag>> getTagCache() {
        if (tagCache == null) {
            tagCache  = new HashMap<MultiKey, List<Tag>>();
        }
        return tagCache;
    }

    /**
     * Add a new tag.  The format of objectIdentifier depends on the tag type.
     * For types "attribute", "reference" and "collection" the objectIdentifier should have the form
     * "ClassName.fieldName".
     * @param tagName the tag name - any String
     * @param objectIdentifier an object identifier that is appropriate for the given tag type
     * (eg. "Department.name" for the "collection" type)
     * @param type the tag type (eg. "collection", "reference", "attribute", "bag")
     * @param userName the name of the UserProfile to associate this tag with
     * @return the new Tag
     */
    public synchronized Tag addTag(String tagName, String objectIdentifier, String type,
                                   String userName) {
        LOG.error("addTag() deleting cache");
        tagCache = null;
        if (tagName == null) {
            throw new IllegalArgumentException("tagName cannot be null");
        }
        if (objectIdentifier == null) {
            throw new IllegalArgumentException("objectIdentifier cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (userName == null) {
            throw new IllegalArgumentException("userName cannot be null");
        }
        if (!tagCheckers.containsKey(type)) {
            throw new IllegalArgumentException("unknown tag type: " + type);
        }
        UserProfile userProfile = getUserProfile(userName);

        if (userProfile == null) {
            throw new RuntimeException("no such user " + userName);
        }

        tagCheckers.get(type).isValid(tagName, objectIdentifier, type, userProfile);
        Tag tag = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag.setTagName(tagName);
        tag.setObjectIdentifier(objectIdentifier);
        tag.setType(type);
        tag.setUserProfile(userProfile);

        try {
            osw.store(tag);
            return tag;
        } catch (ObjectStoreException e) {
            throw new RuntimeException("cannot set tag", e);
        }
    }

    /**
     * Make TagChecker objects for this ProfileManager.
     * @param model the Model
     * @return a map from tag type ("template", "reference", "attribute", etc.) to TagChecker
     */
    protected Map<String, TagChecker> makeTagCheckers(final Model model) {
        Map<String, TagChecker> newTagCheckers = new HashMap<String, TagChecker>();
        TagChecker fieldChecker = new TagChecker() {
            public void isValid(@SuppressWarnings("unused") String tagName, 
                                String objectIdentifier, String type,
                                @SuppressWarnings("unused") UserProfile userProfile) {
                int dotIndex = objectIdentifier.indexOf('.');
                if (dotIndex == -1) {
                    throw new RuntimeException("tried to tag an unknown field: "
                                               + objectIdentifier);
                }
                String className = objectIdentifier.substring(0, dotIndex);
                String fieldName = objectIdentifier.substring(dotIndex + 1);

                ClassDescriptor cd =
                    model.getClassDescriptorByName(model.getPackageName() + "." + className);
                if (cd == null) {
                    throw new RuntimeException("unknown class name \"" + className
                                               + "\" while tagging: " + objectIdentifier);
                }
                FieldDescriptor fd = cd.getFieldDescriptorByName(fieldName);
                if (fd == null) {
                    throw new RuntimeException("unknown field name \"" + fieldName
                                               + "\" in class \"" + className
                                               + "\" while tagging: " + objectIdentifier);

                }

                if (type.equals("collection") && !fd.isCollection()) {
                    throw new RuntimeException(objectIdentifier + " is not a collection");
                }
                if (type.equals("reference") && !fd.isReference()) {
                    throw new RuntimeException(objectIdentifier + " is not a reference");
                }
                if (type.equals("attribute") && !fd.isAttribute()) {
                    throw new RuntimeException(objectIdentifier + " is not a attribute");
                }
            }
        };
        newTagCheckers.put("collection", fieldChecker);
        newTagCheckers.put("reference", fieldChecker);
        newTagCheckers.put("attribute", fieldChecker);

        TagChecker templateChecker = new TagChecker() {
            public void isValid(@SuppressWarnings("unused") String tagName, 
                                @SuppressWarnings("unused") String objectIdentifier, 
                                @SuppressWarnings("unused") String type,          
                                @SuppressWarnings("unused") UserProfile userProfile) {
                // OK
            }
        };
        newTagCheckers.put("template", templateChecker);

        TagChecker bagChecker = new TagChecker() {
            public void isValid(String tagName, String objectIdentifier, String type,
                                UserProfile userProfile) {
                // OK
            }
        };
        newTagCheckers.put("bag", bagChecker);

        TagChecker classChecker = new TagChecker() {
            public void isValid(@SuppressWarnings("unused") String tagName, 
                                String objectIdentifier, 
                                @SuppressWarnings("unused") String type,
                                @SuppressWarnings("unused") UserProfile userProfile) {
                String className = objectIdentifier;
                ClassDescriptor cd = model.getClassDescriptorByName(className);
                if (cd == null) {
                    throw new RuntimeException("unknown class name \"" + className
                                               + "\" while tagging: " + objectIdentifier);
                }
            }
        };
        newTagCheckers.put("class", classChecker);
        return newTagCheckers;
    }
}
