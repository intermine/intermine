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
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CacheMap;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.InterMineBagBinding;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.PathQueryBinding;
import org.intermine.web.logic.query.SavedQueryBinding;
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
    protected InterMineBagBinding bagBinding = new InterMineBagBinding();
    protected TemplateQueryBinding templateBinding = new TemplateQueryBinding();
    protected CacheMap profileCache = new CacheMap();
    private Map tagCheckers = null;
    private Map tagCache = null;
    private Map classKeys;

    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @param userProfileOS the object store that hold user profile information
     * @param classKeys class key fields for model
     */
    public ProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS, Map classKeys) {
        this.os = os;
        tagCheckers = makeTagCheckers(os.getModel());
        this.osw = userProfileOS;
        this.classKeys = classKeys;
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

        
        
        Map savedBags = new HashMap();
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "name"));
        q.addToSelect(new QueryField(qc, "size"));
        q.addToSelect(new QueryField(qc, "objects"));
        q.addToSelect(new QueryField(qc, "type"));
        q.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "userProfile"),
                    ConstraintOp.CONTAINS, new ProxyReference(null, userProfile.getId(),
                        UserProfile.class)));
        Results bags;
        try {
            bags = osw.execute(q);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        for (Iterator i = bags.iterator(); i.hasNext();) {
            List row = (List) i.next();
            String bagName = (String) row.get(0);
            int bagSize = ((Integer) row.get(1)).intValue();
            String type = (String) row.get(3);
            savedBags.put(bagName, new InterMineBag(userProfile.getId(), bagName, type,
                        bagSize, osw, os));
        }
        Map savedQueries = new HashMap();
        for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
            SavedQuery query = (SavedQuery) i.next();
            try {
                Map queries = 
                    SavedQueryBinding.unmarshal(new StringReader(query.getQuery()), savedBags, 
                                                classKeys);
                if (queries.size() == 0) {
                    queries = 
                        PathQueryBinding.unmarshal(new StringReader(query.getQuery()), savedBags, 
                                                   classKeys);
                    if (queries.size() == 1) {
                        Map.Entry entry = (Map.Entry) queries.entrySet().iterator().next();
                        String name = (String) entry.getKey();
                        savedQueries.put(name, new org.intermine.web.logic.query.SavedQuery(name, null,
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
                Map templateMap = templateBinding.unmarshal(sr, savedBags, classKeys);
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
    public void convertTemplateKeywordsToTags(Map savedTemplates, String username) {
        for (Iterator iter = savedTemplates.values().iterator(); iter.hasNext(); ) {
            TemplateQuery tq = (TemplateQuery) iter.next();
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
                for (Iterator i = userProfile.getSavedBags().iterator(); i.hasNext();) {
                    SavedBag bag = (SavedBag) i.next();
                    if (!profile.getSavedBags().containsKey(bag.getName())) {
                        osw.delete(bag);
                    }
                }

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

            for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
                InterMineBag bag = null;
                try {
                    Map.Entry entry = (Map.Entry) i.next();
                    String bagName = (String) entry.getKey();
                    bag = (InterMineBag) entry.getValue();
                    if (bag.needsWrite()) {
                        SavedBag savedBag = null;
                        if (bag.getSavedBagId() != null) {
                            // TODO fix problem with missing InterMineObject table
                            // at the moment making query but should use
                            // getObjectById()
                            // savedBag= (SavedBag) osw.getObjectById(bag.getSavedBagId());
                            try {
                                Query q = new Query();
                                QueryClass qc = new QueryClass(SavedBag.class);
                                q.addFrom(qc);
                                q.addToSelect(qc);
                                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                                cs.addConstraint(new SimpleConstraint(new QueryField(qc, "id"),
                                            ConstraintOp.EQUALS, new QueryValue
                                            (bag.getSavedBagId())));
                                q.setConstraint(cs);
                                Results res = osw.execute(q);
                                savedBag = (SavedBag) ((List) res.get(0)).get(0);
                            } catch (ObjectStoreException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (savedBag == null) {
                            savedBag = new SavedBag();
                        }
                        savedBag.setBag(bagBinding.marshal(bag, bagName));
                        savedBag.setUserProfile(userProfile);
                        savedBag.setName(bagName);
                        savedBag.setSize(bag.size());
                        savedBag.setType(bag.getType());
                        osw.store(savedBag);
                        bag.setSavedBagId(savedBag.getId());
                        bag.resetToDatabase();
                    }
                } catch (Exception e) {
                    LOG.error("Failed to marshal and save bag: " + bag, e);
                }
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
                    SavedTemplateQuery savedTemplate = new SavedTemplateQuery();
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
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        profile.setUserId(userProfile.getId());
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
    public synchronized List getTags(String tagName, String objectIdentifier, String type,
                        String userName) {
        Map cache = getTagCache();
        MultiKey key = makeKey(tagName, objectIdentifier, type, userName);

        if (cache.containsKey(key)) {
            return (List) cache.get(key);
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

        SingletonResults results =
            new SingletonResults(q, userprofileOS, userprofileOS.getSequence());

        addToCache(cache, key, results);

        return results;
    }

    private MultiKey makeKey(String tagName, String objectIdentifier, String type,
                             String userName) {
        return new MultiKey(tagName, objectIdentifier, type, userName);
    }

    private void addToCache(Map cache, MultiKey key, List results) {
        LOG.error("adding to cache: " + key);
        cache.put(key, new ArrayList (results));

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

    private Map getTagCache() {
        if (tagCache == null) {
            tagCache  = new HashMap();
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
     */
    public synchronized void addTag(String tagName, String objectIdentifier, String type,
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

        ((TagChecker) tagCheckers.get(type)).isValid(tagName, objectIdentifier, type, userProfile);
        Tag tag = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag.setTagName(tagName);
        tag.setObjectIdentifier(objectIdentifier);
        tag.setType(type);
        tag.setUserProfile(userProfile);

        try {
            osw.store(tag);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("cannot set tag", e);
        }
    }

    protected Map makeTagCheckers(final Model model) {
        Map newTagCheckers = new HashMap();
        TagChecker fieldChecker = new TagChecker() {
            public void isValid(String tagName, String objectIdentifier, String type,
                                UserProfile userProfile) {
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
            public void isValid(String tagName, String objectIdentifier, String type,
                                UserProfile userProfile) {
                // OK
            }
        };
        newTagCheckers.put("template", templateChecker);

        TagChecker classChecker = new TagChecker() {
            public void isValid(String tagName, String objectIdentifier, String type,
                                UserProfile userProfile) {
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
