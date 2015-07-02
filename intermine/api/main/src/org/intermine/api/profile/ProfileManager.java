package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static java.util.Collections.singleton;

import java.io.Reader;
import java.io.StringReader;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.util.TextUtil;
import org.intermine.api.xml.SavedQueryBinding;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.PermanentToken;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.Constraint;
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
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.util.CacheMap;
import org.intermine.util.PasswordHasher;
import org.intermine.util.PropertiesUtil;

/**
 * Class to manage and persist user profile data such as saved bags
 * @author Mark Woodbridge
 * @author Daniela Butano
 * @author Alex Kalderimis
 */
public class ProfileManager
{
    private static final Logger LOG = Logger.getLogger(ProfileManager.class);

    protected ObjectStore os;
    protected ObjectStoreWriter uosw;
    protected CacheMap<String, Profile> profileCache = new CacheMap<String, Profile>();
    private String superuser = null;
    /** Number determining format of queries in the database */
    protected int pathQueryFormat;

    private final Map<String, LimitedAccessToken> limitedAccessTokens
        = new HashMap<String, LimitedAccessToken>();

    private final Map<UUID, PermanentToken> permanentTokens
        = new HashMap<UUID, PermanentToken>();

    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @param userProfileOS the object store that holds user profile information
     */
    public ProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS) {
        this.os = os;
        this.uosw = userProfileOS;
        //retrieve the super user
        String superUserName = PropertiesUtil.getProperties().getProperty("superuser.account");
        UserProfile superuserProfile = new UserProfile();
        superuserProfile.setUsername(superUserName);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("username");

        try {
            superuserProfile = uosw.getObjectByExample(superuserProfile, fieldNames);
            if (superuserProfile != null) {
                superuser = superuserProfile.getUsername();
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load super user profile", e);
        }

        pathQueryFormat = loadPathQueryFormatVersion();

        permanentTokens.putAll(loadPermanentTokens());
    }

    private int loadPathQueryFormatVersion() {
        int v = 0;
        int currentVersion = PathQuery.USERPROFILE_VERSION;
        try {
            String versionString = MetadataManager.retrieve(((ObjectStoreInterMineImpl) uosw)
                .getDatabase(), MetadataManager.PROFILE_FORMAT_VERSION);
            String message = "Could not recognise userprofile format version "
                    + versionString + ", maybe you need to update InterMine";
            LOG.info("Database has userprofile version \"" + versionString + "\"");
            if (versionString != null) {
                try {
                    v = Integer.parseInt(versionString);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException(message);
                }
            }
            if ((v < 0) || (v > currentVersion)) {
                throw new IllegalStateException(message);
            }
            if (v < currentVersion) {
                // We can upgrade if there is no data that might need updating.
                Query q = new Query();
                QueryClass savedQueries = new QueryClass(SavedQuery.class);
                QueryClass templateQueries = new QueryClass(SavedTemplateQuery.class);
                q.addFrom(savedQueries);
                q.addFrom(templateQueries);
                q.addToSelect(savedQueries);
                q.addToSelect(templateQueries);
                List<?> results = uosw.execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
                if (results.isEmpty()) {
                    // We can safely upgrade the database!
                    MetadataManager.store(((ObjectStoreInterMineImpl) uosw).getDatabase(),
                            MetadataManager.PROFILE_FORMAT_VERSION,
                            "" + currentVersion);
                    v = currentVersion;
                }
            }
        } catch (ObjectStoreException e) {
            throw new IllegalStateException("Error upgrading version number in database", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Error reading version number from database", e);
        }
        return v;
    }

    private Map<UUID, PermanentToken> loadPermanentTokens() {
        Map<UUID, PermanentToken> map = new HashMap<UUID, PermanentToken>();
        try {
            // Get the current set of permanent tokens from the database.
            Query q = new Query();
            QueryClass tokens = new QueryClass(PermanentToken.class);
            q.addFrom(tokens);
            q.addToSelect(tokens);

            List<?> results = uosw.executeSingleton(q);
            Set<PermanentToken> badTokens = new HashSet<PermanentToken>();
            for (Object o: results) {
                PermanentToken token = (PermanentToken) o;
                try {
                    UUID key = UUID.fromString(token.getToken());
                    map.put(key, token);
                } catch (IllegalArgumentException e) {
                    badTokens.add(token);
                }
            }
            for (PermanentToken t: badTokens) {
                LOG.info("Removing bad token: " + t);
                uosw.delete(t);
            }
        } catch (Exception e) {
            LOG.error("Could not load permanent tokens", e);
            throw new IllegalStateException("Error loading permanent tokens", e);
        }
        return map;
    }

    /**
     * Return the ObjectStore that was passed to the constructor.
     * @return the ObjectStore from the constructor
     */
    public ObjectStore getProductionObjectStore() {
        return os;
    }

    /**
     * Return the userprofile ObjectStoreWriter that was passed to the constructor.
     * @return the userprofile  ObjectStoreWriter from the constructor
     */
    public ObjectStoreWriter getProfileObjectStoreWriter() {
        return uosw;
    }

    /**
     * Returns the version number of the data format in the database for this ProfileManager.
     *
     * @return an int
     */
    public int getVersion() {
        return pathQueryFormat;
    }

    /**
     * Close this ProfileManager
     *
     * @throws ObjectStoreException in exceptional circumstances
     */
    public void close() throws ObjectStoreException {
        uosw.close();
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
        return PasswordHasher.checkPassword(password, getUserProfile(username).getPassword());
    }

    /**
     * Change a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     */
    public synchronized void setPassword(String username, String password) {
        UserProfile userProfile = getUserProfile(username);
        userProfile.setPassword(PasswordHasher.hashPassword(password));
        try {
            uosw.store(userProfile);
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
     * Get a user's Profile using a username, password and the classKeys.
     * @param username the username
     * @param password the password
     * @param classKeys the classkeys
     * @return the Profile, or null if one doesn't exist
     */
    public synchronized Profile getProfile(String username, String password,
                        Map<String, List<FieldDescriptor>> classKeys) {
        if (hasProfile(username) && validPassword(username, password)) {
            return getProfile(username, classKeys);
        }
        return null;
    }
    /**
     * Get a user's Profile using a username and password.
     * @param username the username
     * @param password the password
     * @return the Profile, or null if one doesn't exist
     */
    public synchronized Profile getProfile(String username, String password) {
        if (hasProfile(username)) {
            if (getUserProfile(username).getLocalAccount()) {
                if (validPassword(username, password)) {
                    return getProfile(username);
                }
            } else {
                return getProfile(username);
            }
        }
        return null;
    }

    /**
     * Get a user's Profile using a username
     * @param username the username
     * @return the Profile, or null if one doesn't exist
     */
    public synchronized Profile getProfile(String username) {
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys(os.getModel());
        return getProfile(username, classKeys);
    }

    /**
     * Get a user's Profile using an ID
     * @param id userprofile ID
     * @return user's profile
     */
    public Profile getProfile(int id) {
        Map<String, List<FieldDescriptor>> classKeys = getClassKeys(os.getModel());
        UserProfile up;
        try {
            up = (UserProfile) uosw.getObjectById(id, UserProfile.class);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error retrieving profile", e);
        }
        if (up != null && profileCache.containsKey(up.getUsername())) {
            return profileCache.get(up.getUsername());
        }
        return wrapUserProfile(up, classKeys);
    }

    /**
     * Load keys that describe how objects should be uniquely identified
     */
    private Map<String, List<FieldDescriptor>> getClassKeys(Model model) {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(getClass().getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (Exception e) {
            LOG.error("Error loading class descriptions", e);
        }
        Map<String, List<FieldDescriptor>>  classKeys =
            ClassKeyHelper.readKeys(model, classKeyProps);
        return classKeys;
    }

    /**
     * Completely remove a profile and all of its associated data from the data-store.
     * Use with extreme caution.
     * @param profile The profile to remove.
     * @throws ObjectStoreException If it cannot be removed.
     */
    public void deleteProfile(Profile profile) throws ObjectStoreException {
        Integer userId = profile.getUserId();
        removeTokensForProfile(profile);
        evictFromCache(profile);
        try {
            uosw.beginTransaction();
            UserProfile userProfile = getUserProfile(userId);
            if (userProfile == null) {
                throw new ObjectStoreException("User is not in the data store.");
            }
            for (org.intermine.model.userprofile.SavedQuery sq: userProfile.getSavedQuerys()) {
                uosw.delete(sq);
            }

            for (SavedTemplateQuery st: userProfile.getSavedTemplateQuerys()) {
                uosw.delete(st);
            }

            for (SavedBag sb: userProfile.getSavedBags()) {
                uosw.delete(sb);
            }

            for (PermanentToken token: userProfile.getPermanentTokens()) {
                removePermanentToken(token);
            }

            TagManager tagManager = getTagManager();
            for (Tag tag : tagManager.getUserTags(userProfile.getUsername())) {
                tagManager.deleteTag(tag);
            }
            SharedBagManager sbm = SharedBagManager.getInstance(this);
            sbm.removeAllSharesInvolving(userId);
            sbm.removeAllInvitesBy(userId);

            uosw.delete(userProfile);
            uosw.commitTransaction();
        } catch (ObjectStoreException e) {
            if (uosw.isInTransaction()) {
                uosw.abortTransaction();
            }
            throw e;
        } finally {
            // Should not happen.
            if (uosw.isInTransaction()) {
                uosw.abortTransaction();
            }
        }
    }

    /**
     * Get a user's Profile using a username
     * @param username the username
     * @param classKeys the classkeys
     * @return the Profile, or null if one doesn't exist
     */
    public synchronized Profile getProfile(String username, Map<String,
                        List<FieldDescriptor>> classKeys) {
        if (username == null) {
            return null;
        }
        Profile profile = profileCache.get(username);
        if (profile != null) {
            return profile;
        }

        UserProfile userProfile = getUserProfile(username);

        if (userProfile == null) {
            // See if we can resolve the user by an alias.
            Integer trueId;
            try {
                // See if this is one of the unique mappings.
                for (String pref: UserPreferences.UNIQUE_KEYS) {
                    trueId = getPreferencesManager().getUserWithUniqueMapping(pref, username);
                    if (trueId != null) {
                        return getProfile(trueId);
                    }
                }
            } catch (DuplicateMappingException e) {
                LOG.error("DB in in an illegal state", e);
            } catch (SQLException e) {
                LOG.warn(e);
            }
            return null;
        }

        return wrapUserProfile(userProfile, classKeys);
    }

    private synchronized Profile wrapUserProfile(UserProfile userProfile,
            Map<String, List<FieldDescriptor>> classKeys) {
        if (userProfile == null) {
            return null;
        }
        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();
        Map<String, InvalidBag> savedInvalidBags = new HashMap<String, InvalidBag>();
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "id"));
        q.addToSelect(qc); // This loads the objects into the cache
        q.setConstraint(new ContainsConstraint(new QueryObjectReference(qc, "userProfile"),
                    ConstraintOp.CONTAINS, new ProxyReference(null, userProfile.getId(),
                        UserProfile.class)));
        try {
            // Multiple attempts to access the userprofile (create/delete bags, for instance)
            // will cause this to fail. Allow three retries.
            ConcurrentModificationException lastError = null;
            boolean succeeded = false;
            for (int attemptsRemaining = 3; attemptsRemaining >= 0; attemptsRemaining--) {
                try {
                    Results bags = uosw.execute(q, 1000, false, false, true);
                    for (Iterator<?> i = bags.iterator(); i.hasNext();) {
                        ResultsRow<?> row = (ResultsRow<?>) i.next();
                        Integer bagId = (Integer) row.get(0);
                        SavedBag savedBag = (SavedBag) row.get(1);
                        String bagName = savedBag.getName();
                        if (StringUtils.isBlank(bagName)) {
                            LOG.warn("Failed to load bag with blank name on login for user: "
                                    + userProfile.getUsername());
                        } else {
                            try {
                                InterMineBag bag = new InterMineBag(os, bagId, uosw);
                                bag.setKeyFieldNames(ClassKeyHelper.getKeyFieldNames(
                                                     classKeys, bag.getType()));
                                savedBags.put(bagName, bag);
                            } catch (UnknownBagTypeException e) {
                                LOG.warn("The bag '" + bagName + "' for user '"
                                        + userProfile.getUsername() + "'"
                                        + " with type: " + savedBag.getType()
                                        + " is not in the model. It will be saved into invalidBags"
                                        , e);
                                InvalidBag bag = new InvalidBag(savedBag, userProfile.getId(),
                                        os, uosw);
                                savedInvalidBags.put(bagName, bag);
                            }
                        }
                    }
                    succeeded = true;
                } catch (ConcurrentModificationException e) {
                    lastError = e;
                }
            }
            if (!succeeded && (lastError != null)) {
                throw lastError;
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        Map<String, org.intermine.api.profile.SavedQuery> savedQueries =
            new HashMap<String, org.intermine.api.profile.SavedQuery>();
        for (SavedQuery query : userProfile.getSavedQuerys()) {
            try {
                Reader r = new StringReader(query.getQuery());
                savedQueries = SavedQueryBinding.unmarshal(r, savedBags, pathQueryFormat);
                if (savedQueries.isEmpty()) {
                    Map<String, PathQuery> pqs = PathQueryBinding.unmarshalPathQueries(
                            new StringReader(query.getQuery()),
                            pathQueryFormat);
                    if (pqs.size() == 1) {
                        Map.Entry<String, PathQuery> entry = pqs.entrySet().iterator().next();
                        String name = (String) entry.getKey();
                        savedQueries.put(
                                name,
                                new org.intermine.api.profile.SavedQuery(name, null,
                                                                  entry.getValue()));
                    }
                }
            } catch (Exception err) {
                // Ignore rows that don't unmarshal (they probably reference
                // another model.
                LOG.warn("Failed to unmarshal saved query: " + query.getQuery());
            }
        }
        Map<String, ApiTemplate> savedTemplates = new HashMap<String, ApiTemplate>();
        for (SavedTemplateQuery template : userProfile.getSavedTemplateQuerys()) {
            try {
                StringReader sr = new StringReader(template.getTemplateQuery());
                Map<String, TemplateQuery> templateMap =
                        TemplateQueryBinding.unmarshalTemplates(sr, pathQueryFormat);
                String templateName = templateMap.keySet().iterator().next();
                TemplateQuery templateQuery = templateMap.get(templateName);
                ApiTemplate apiTemplate = new ApiTemplate(templateQuery);
                apiTemplate.setSavedTemplateQuery(template);
                savedTemplates.put(templateName, apiTemplate);
            } catch (Exception err) {
                // Ignore rows that don't unmarshal (they probably reference
                // another model.
                LOG.warn("Failed to unmarshal saved template query: "
                         + template.getTemplateQuery(), err);
            }
        }
        BagSet bags = new BagSet(savedBags, savedInvalidBags);
        Profile profile = new Profile(this, userProfile.getUsername(), userProfile.getId(),
                userProfile.getPassword(),
                savedQueries, bags, savedTemplates, userProfile.getApiKey(),
                userProfile.getLocalAccount(), userProfile.getSuperuser());
        profileCache.put(userProfile.getUsername(), profile);
        //only after saving the profile in the cache,
        //we can update the user repository with shared bags
        //if we do in the constructor we could generate loops
        profile.updateUserRepositoryWithSharedBags();
        return profile;
    }


    /**
     * Return the TagManager for adding, removing and fetching Tags assigned to templates, bags
     * and classes.
     * @return the TagManager
     */
    public TagManager getTagManager() {
        return new TagManagerFactory(this).getTagManager();
    }

    /**
     * Synchronise a user's Profile with the backing store
     * @param profile the Profile
     */
    public synchronized void saveProfile(Profile profile) {
        Integer userId = profile.getUserId();
        try {
            UserProfile userProfile = getUserProfile(userId);

            if (userProfile == null) {
                throw new RuntimeException("Cannot save this profile: The UserProfile is null");
            }

            userProfile.setApiKey(profile.getApiKey());

            syncSavedQueries(profile, userProfile);
            syncTemplates(profile, userProfile);

            uosw.store(userProfile);
            profile.setUserId(userProfile.getId());
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void syncTemplates(Profile profile, UserProfile userProfile) {
        for (Entry<String, ApiTemplate> entry: profile.getSavedTemplates().entrySet()) {
            ApiTemplate template = entry.getValue();
            SavedTemplateQuery savedTemplate = template.getSavedTemplateQuery();
            if (savedTemplate == null) {
                savedTemplate = new SavedTemplateQuery();
                savedTemplate.setUserProfile(userProfile);
            }
            String xml = TemplateQueryBinding.marshal(template, pathQueryFormat);
            if (!xml.equals(savedTemplate.getTemplateQuery())) { // Different - needs update.
                try {
                    savedTemplate.setTemplateQuery(xml);
                    uosw.store(savedTemplate);
                    template.setSavedTemplateQuery(savedTemplate);
                } catch (Exception e) {
                    LOG.error("Failed to marshal and save template: " + template, e);
                }
            }
        }
    }

    private void syncSavedQueries(Profile profile, UserProfile userProfile)
        throws ObjectStoreException {
        // Index the currently saved queries by their query XML,
        // so we know if we need to update them.
        Map<String, SavedQuery> toDelete = new HashMap<String, SavedQuery>();
        for (SavedQuery sq: userProfile.getSavedQuerys()) {
            //uosw.delete(sq);
            toDelete.put(sq.getQuery(), sq);
        }

        for (Entry<String, org.intermine.api.profile.SavedQuery> entry
                : profile.getSavedQueries().entrySet()) {
            org.intermine.api.profile.SavedQuery query = entry.getValue();
            try {
                String xml = SavedQueryBinding.marshal(query, pathQueryFormat);
                SavedQuery savedQuery = toDelete.remove(xml);
                if (savedQuery == null) { // Need to write a new one.
                    savedQuery = new SavedQuery();
                    savedQuery.setQuery(xml);
                    savedQuery.setUserProfile(userProfile);
                    uosw.store(savedQuery);
                }
            } catch (Exception e) {
                LOG.error("Failed to marshal and save query: " + query, e);
            }
        }
        for (SavedQuery delendum: toDelete.values()) {
            uosw.delete(delendum);
        }
    }

    /**
     * Create a new profile in db with username and password given in input
     * @param username the user name
     * @param password the password
     * @return new profile
     */
    public synchronized Profile createNewProfile(String username, String password) {
        return createBasicLocalProfile(username, password, null);
    }

    /**
     * Create a profile not tied to an entry in the user db. For web services users.
     * @return anon profile
     */
    public Profile createAnonymousProfile() {
        String username = null;
        Integer id = null;
        String password = null;
        String token = null;
        boolean isLocal = true;
        boolean isSuperUser = false;

        Profile p = new Profile(this, username, id, password,
                new HashMap<String, org.intermine.api.profile.SavedQuery>(),
                new HashMap<String, InterMineBag>(),
                new HashMap<String, ApiTemplate>(), token, isLocal, isSuperUser);
        return p;
    }

    /**
     * Create a new Profile with the given username, password and
     * api-key. This profile will be a local standard user.
     * @param username  The name for this user.
     * @param password The password for this user.
     * @param apiKey The API key for this user.
     * @return The profile.
     */
    public synchronized Profile createBasicLocalProfile(
            String username,
            String password,
            String apiKey) {
        if (this.hasProfile(username)) {
            throw new RuntimeException("Cannot create account: there already exists a user"
                    + " with that name");
        }

        Profile p = new Profile(
                this, username, null, password,
                Profile.NO_QUERIES, Profile.NO_BAGS, Profile.NO_TEMPLATES,
                apiKey, true, false);
        createProfile(p);
        return p;
    }

    /**
     * Create a super-user with the given username, password and API-key. The user will be
     * marked as a local super-user.
     * @param username  The name for this user.
     * @param password The password for this user.
     * @param apiKey The API key for this user.
     * @return The profile.
     */
    public synchronized Profile createSuperUser(
            String username,
            String password,
            String apiKey) {
        Profile p = new Profile(
                this, username, null, password,
                Profile.NO_QUERIES, Profile.NO_BAGS, Profile.NO_TEMPLATES,
                apiKey, true, true);
        createProfile(p);
        return p;
    }

    /**
     * Creates a profile in the userprofile database.
     *
     * @param profile a Profile object
     */
    public synchronized void createProfile(Profile profile) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername(profile.getUsername());
        userProfile.setLocalAccount(profile.isLocal());

        if (profile.isLocal() && profile.getPassword() != null) {
            userProfile.setPassword(PasswordHasher.hashPassword(profile.getPassword()));
        }
        userProfile.setSuperuser(profile.isSuperUser);

        try {
            uosw.store(userProfile);
            profile.setUserId(userProfile.getId());
            for (InterMineBag bag : profile.getSavedBags().values()) {
                bag.setProfileId(userProfile.getId());
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
        saveProfile(profile);
    }

    /**
     * Generate a new API access key for this profile and return it.
     * @param profile The profile to generate the new API key for.
     * @return A new API access key
     */
    public synchronized String generateApiKey(Profile profile) {
        String newApiKey = TextUtil.generateRandomUniqueString();
        profile.setApiKey(newApiKey);
        return newApiKey;
    }

    /**
     * Generate a single use API key and store it in memory, before returning it.
     * @param profile the user profile
     * @return the generated key
     */
    public synchronized String generateSingleUseKey(Profile profile) {
        String key = TextUtil.generateRandomUniqueString();
        LimitedAccessToken token = new SingleAccessToken(profile);
        limitedAccessTokens.put(key, token);
        return key;
    }

    /**
     * Generate a day token
     * @param profile the profile which token is valid
     * @return the token
     */
    public synchronized String generate24hrKey(Profile profile) {
        String key = TextUtil.generateRandomUniqueString();
        LimitedAccessToken token = new DayToken(profile);
        limitedAccessTokens.put(key, token);
        return key;
    }

    /**
     * Remove auth tokens for a specified users.
     *
     * @param profile users profile
     */
    public void removeTokensForProfile(Profile profile) {
        if (profile == null) {
            throw new NullPointerException("profile should not be null.");
        }
        synchronized (limitedAccessTokens) {
            Set<String> tokens = limitedAccessTokens.keySet();
            Iterator<String> itr = tokens.iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                LimitedAccessToken token = limitedAccessTokens.get(key);
                if (profile.equals(token.getProfile())) {
                    itr.remove();
                }
            }
        }
    }

    /**
     * Return whether the token given in input is suitable for using in the future.
     * @param token the token to verify
     * @return true if is suitable for using in the future.
     */
    public synchronized boolean tokenHasMoreUses(String token) {
        if (token != null) {
            if (limitedAccessTokens.containsKey(token)) {
                LimitedAccessToken lat = limitedAccessTokens.get(token);
                if (lat.isValid()) {
                    return lat.hasMoreUses();
                } else {
                    limitedAccessTokens.remove(token);
                }
            }
            try {
                UUID key = UUID.fromString(token);
                if (permanentTokens.containsKey(key)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // Suppress.
            }
        }
        return false;
    }

    /**
     * Return a permanent user access token, with ReadOnly permission.
     *
     * @param profile a users profile
     * @param message a message
     * @return A token granting read-only access to resources.
     * @throws ObjectStoreException oops
     */
    public String generateReadOnlyAccessToken(Profile profile, String message)
        throws ObjectStoreException {
        UserProfile up;
        if (profile.getUserId() == null) {
            throw new IllegalArgumentException("This profile does not have an associated "
                    + "user-profile");
        }
        up = (UserProfile) uosw.getObjectById(profile.getUserId());
        PermanentToken token = new PermanentToken();
        UUID uuid = UUID.randomUUID();
        token.setToken(uuid.toString());
        token.setLevel("RO");
        token.setUserProfile(up);
        token.setDateCreated(new Date());
        if (message != null) {
            token.setMessage(message);
        }
        uosw.store(token);
        permanentTokens.put(uuid, token);
        return token.getToken();
    }

    /**
     * Creates a profile in the userprofile database without adding bag.
     * Method used by the ProfileReadXml.
     *
     * @param profile a Profile object
     */
    public synchronized void createProfileWithoutBags(Profile profile) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername(profile.getUsername());
        if (profile.getPassword() != null) {
            userProfile.setPassword(PasswordHasher.hashPassword(profile.getPassword()));
        }
        userProfile.setSuperuser(profile.isSuperUser);
        try {
            uosw.store(userProfile);
            profile.setUserId(userProfile.getId());
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
    public synchronized UserProfile getUserProfile(String username) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("username");
        try {
            profile = uosw.getObjectByExample(profile, fieldNames);
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
    public synchronized UserProfile getUserProfile(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            return (UserProfile) uosw.getObjectById(userId, UserProfile.class);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
    }

    /**
     * Return a List of the usernames in all of the stored profiles.
     *
     * @return the usernames
     */
    public synchronized List<String> getProfileUserNames() {
        Query q = new Query();
        QueryClass qcUserProfile = new QueryClass(UserProfile.class);
        QueryField qfUserName = new QueryField(qcUserProfile, "username");
        q.addFrom(qcUserProfile);
        q.addToSelect(qfUserName);

        SingletonResults res = uosw.executeSingleton(q);

        // TODO: We copy the data here in order to avoid any future ConcurrentModificationException
        // in the SingletonResults
        List<String> userNames = new ArrayList<String>();
        for (Object userName : res) {
            userNames.add((String) userName);
        }
        return userNames;
    }

    /**
     * Return the name of the user with the given internal DB id.
     *
     * If no user with that name exists, returns null.
     * @param profileId the id of the profile.
     * @return the name of the user, or null.
     */
    public synchronized String getProfileUserName(int profileId) {
        try {
            UserProfile profile = (UserProfile) uosw.getObjectById(profileId, UserProfile.class);
            return profile.getUsername();
        } catch (ObjectStoreException e) {
            return null; // Not in DB.
        } catch (NullPointerException e) {
            return null; // profile was null (impossible!)
        }
    }

    /**
     * Return the super user name set in the properties file
     * @return the superuser name
     */
    public String getSuperuser() {
        return superuser;
    }

    /**
     * Return the superuser profile set in the properties file
     * @return the superuser profile
     */
    public Profile getSuperuserProfile() {
        return getProfile(superuser);
    }

    /**
     * @return All the profiles of users who are super-users.
     * @throws ObjectStoreException If we have trouble accessing the data-store.
     */
    public Collection<Profile> getAllSuperUsers() throws ObjectStoreException {
        Set<Profile> superUsers = new HashSet<Profile>();
        for (String name: getAllSuperNames()) {
            superUsers.add(getProfile(name));
        }
        return superUsers;
    }

    private Iterable<String> getAllSuperNames() throws ObjectStoreException {
        Set<String> names = new HashSet<String>();
        UserProfile example = new UserProfile();
        example.setSuperuser(true);
        for (UserProfile up: uosw.getObjectsByExample(example, singleton("superuser"))) {
            names.add(up.getUsername());
        }
        return names;
    }

    /**
     * Return the  super user
     * @param classKeys the classkeys
     * @return the superuser profile
     */
    public Profile getSuperuserProfile(Map<String, List<FieldDescriptor>> classKeys) {
        Profile profile = getProfile(superuser, classKeys);
        if (profile == null) {
            String msg = "Unable to retrieve superuser profile.";
            throw new UserNotFoundException(msg);
        }
        return profile;
    }

    private final Map<String, PasswordChangeToken> passwordChangeTokens
        = new HashMap<String, PasswordChangeToken>();

    /**
     * Creates a password change token assigned to the given username that will expire after a day.
     *
     * @param username the name of the user to create a password change token for
     * @return a String containing the token
     * @throws IllegalArgumentException if the username does not match a profile
     */
    public synchronized String createPasswordChangeToken(String username) {
        if (hasProfile(username)) {
            Date expiry = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            String token = TextUtil.generateRandomUniqueString();
            passwordChangeTokens.put(token, new PasswordChangeToken(username, expiry));
            return token;
        } else {
            throw new IllegalArgumentException("No such profile " + username);
        }
    }

    /**
     * Returns the username associated with the given token, if the token is valid.
     *
     * @param token the token
     * @return the username associated with the token
     * @throws IllegalArgumentException if the token is invalid
     */
    public synchronized String getUsernameForToken(String token) {
        PasswordChangeToken retval = passwordChangeTokens.get(token);
        if (retval != null) {
            if (retval.isValid()) {
                return retval.getUsername();
            } else {
                throw new IllegalArgumentException("Token has expired for username "
                        + retval.getUsername());
            }
        }
        throw new IllegalArgumentException("Token is not valid");
    }

    /**
     * Changes the password of a profile if the given token is valid.
     *
     * @param token the token
     * @param password the new password to apply to the account
     * @return the username hat has the new password
     * @throws IllegalArgumentException if the token is invalid
     */
    public synchronized String changePasswordWithToken(String token, String password) {
        PasswordChangeToken pct = passwordChangeTokens.get(token);
        if (pct != null) {
            if (pct.isValid()) {
                setPassword(pct.getUsername(), password);
                passwordChangeTokens.remove(token);
                return pct.getUsername();
            }
        }
        throw new IllegalArgumentException("Token is invalid");
    }

    /**
     * Password change token - a combination of username and expiry date.
     *
     * @author Matthew Wakeling
     */
    private static class PasswordChangeToken
    {
        private final String username;
        private final Date expiry;

        public PasswordChangeToken(String username, Date expiry) {
            this.username = username;
            this.expiry = expiry;
        }

        public String getUsername() {
            return username;
        }

        public boolean isValid() {
            return System.currentTimeMillis() < expiry.getTime();
        }
    }

    /**
     * Remove a profile from the cache
     * @param profile the profile to remove
     */
    public void evictFromCache(Profile profile) {
        profileCache.remove(profile.getUsername());
    }

    /**
     * Abstract class for API access keys.
     * @author Alex Kalderimis
     */
    private abstract static class LimitedAccessToken
    {
        private final Profile profile;

        public LimitedAccessToken(Profile profile) {
            this.profile = profile;
        }

        public Profile getProfile() {
            return profile;
        }

        public abstract ApiPermission.Level getAuthenticationLevel();
        public abstract boolean isValid();

        /**
         * The default implementation makes this equivalent to isValid.
         * @return Whether this token is suitable for using in the future.
         */
        public boolean hasMoreUses() {
            return isValid();
        }

        public void use() {
            // No op stub.
        }
    }


    /**
     * Transient API access keys for automated API access. These tokens are only valid for a
     * single use.
     * @author Alex Kalderimis
     *
     */
    private static class SingleAccessToken extends LimitedAccessToken
    {
        private final int maxUses = 1;
        private int uses = 0;

        public SingleAccessToken(Profile profile) {
            super(profile);
        }

        @Override
        public ApiPermission.Level getAuthenticationLevel() {
            return ApiPermission.Level.RO;
        }

        @Override
        public boolean isValid() {
            return uses < maxUses;
        }

        @Override
        public void use() {
            uses++;
        }
    }

    private static class DayToken extends LimitedAccessToken
    {
        private final Date createdAt;

        public DayToken(Profile profile) {
            super(profile);
            createdAt = new Date();
        }

        @Override
        public ApiPermission.Level getAuthenticationLevel() {
            return ApiPermission.Level.RW;
        }

        private Calendar getExpiry() {
            Calendar b = new GregorianCalendar();
            b.setTime(createdAt);
            b.add(Calendar.HOUR, 24);
            return b;
        }

        @Override
        public boolean isValid() {
            Calendar a = new GregorianCalendar();
            a.setTime(new Date()); // NOW

            return a.before(getExpiry());
        }

        @Override
        public boolean hasMoreUses() {
            // Say that a token is only valid for further use if it has at least another
            // hour of juice.
            Calendar a = new GregorianCalendar();
            a.setTime(new Date());
            a.add(Calendar.HOUR, 1); // AN HOUR FROM NOW

            return a.before(getExpiry());
        }
    }

    /**
     * A representation of the level of permissions granted to a user.
     */
    public static final class ApiPermission implements Principal
    {
        /**
         * The possible permission levels.
         */
        public enum Level { RO, RW };

        private final Level level;
        private final Profile profile;
        private final Set<String> roles = new HashSet<String>();

        /*
         * Only the ProfileManager has permission to grant API permissions.
         */
        private ApiPermission(Profile profile, Level level) {
            this.level = level;
            this.profile = profile;
        }

        /**
         * @return The profile associated with this level of permission.
         */
        public Profile getProfile() {
            return profile;
        }

        @Override
        public String getName() {
            return getProfile().getUsername();
        }

        /**
         * @return The level of permissions granted.
         */
        public Level getLevel() {
            return level;
        }

        /**
         * @return True if this user has been granted RW permissions.
         */
        public boolean isRW() {
            return level == Level.RW;
        }

        /**
         * @return True if this user has been granted RO permissions.
         */
        public boolean isRO() {
            return level == Level.RO;
        }

        /**
         * Add a role to this permission.
         *
         * @param role
         *            The role to add.
         */
        public void addRole(String role) {
            roles.add(role);
        }

        /**
         * True if the permission granted includes access to this role.
         *
         * @param role
         *            The role in question.
         * @return Whether or not this user has access to the given role.
         */
        public boolean isInRole(String role) {
            return roles.contains(role);
        }
    }

    /**
     * Wrap a profile in the default permission level.
     *
     * @param profile
     *            The profile to wrap.
     * @return The default permission for a particular profile.
     */
    public static ApiPermission getDefaultPermission(Profile profile) {
        return new ApiPermission(profile, ApiPermission.Level.RO);
    }

    /**
     * Grant permission to the given identity, creating a profile for this
     * identity if it is not already available.
     *
     * By this point in the process, the code calling this method is required to have
     * validated the identity claims of the issuer.
     *
     * @param issuer The client claiming this identity for a user.
     * @param identity The identity of the user.
     * @param classKeys The class keys for this service.
     *
     * @return permission to use this service.
     */
    public ApiPermission grantPermission(String issuer, String identity,
            Map<String, List<FieldDescriptor>> classKeys) {

        String username = issuer + ":" + identity;
        Profile profile = getProfile(username, classKeys);

        if (profile == null) {
            profile = createNewProfile(username, null);
        }

        if (!profile.prefers(UserPreferences.EMAIL)
                && identity.contains("@")) {
            profile.getPreferences().put(UserPreferences.EMAIL, identity);
        }

        return new ApiPermission(profile, ApiPermission.Level.RW);
    }

    /**
     * Get the level of permission granted by an access token.
     * @param token The token supposedly associated with a user.
     * @param classKeys The class keys for this user.
     * @return A permission object if authentication is successful.
     * @throws AuthenticationException if the authentication fails.
     */
    public ApiPermission getPermission(String token, Map<String, List<FieldDescriptor>> classKeys) {
        ApiPermission permission;
        if (limitedAccessTokens.containsKey(token)) {
            LimitedAccessToken t = limitedAccessTokens.get(token);
            if (!t.isValid()) {
                throw new AuthenticationException("This token (" + token + ")is invalid.");
            }
            Profile p = t.getProfile();
            t.use();
            if (!t.isValid()) {
                limitedAccessTokens.remove(token);
            }
            permission = new ApiPermission(p, t.getAuthenticationLevel());
        } else {
            try {
                UUID key = UUID.fromString(token);
                if (permanentTokens.containsKey(key)) {
                    return getPermission(permanentTokens.get(key), classKeys);
                }
            } catch (IllegalArgumentException e) {
                // Suppress, continue.
            }
            Profile p = getProfileByApiKey(token, classKeys);
            if (p == null) {
                throw new AuthenticationException(
                        "This token is not a valid access key: "
                        + token);
            } else {
                // Grant RW permission to user data
                permission = new ApiPermission(p, ApiPermission.Level.RW);
            }
        }
        return permission;
    }

    /**
     * @param token permanent user token
     * @param classKeys class keys
     * @return permission
     */
    public ApiPermission getPermission(PermanentToken token, Map<String,
            List<FieldDescriptor>> classKeys) {
        if (token.getUserProfile() == null) {
            // Remove it, as it is clearly invalid.
            removePermanentToken(token);
            throw new IllegalStateException("All permanent tokens should have users");
        }
        Profile profile = getProfile(token.getUserProfile().getUsername(), classKeys);
        if (profile == null) {
            removePermanentToken(token);
            throw new AuthenticationException("This token is not a valid access key: " + token);
        }
        ApiPermission.Level level;
        try {
            level = ApiPermission.Level.valueOf(token.getLevel());
        } catch (IllegalArgumentException e) {
            String badLevel = token.getLevel();
            removePermanentToken(token);
            throw new IllegalStateException("Token has illegal level: " + badLevel);
        }
        return new ApiPermission(profile, level);
    }

    /**
     * @param token permanent user token
     */
    public void removePermanentToken(PermanentToken token) {
        try {
            permanentTokens.remove(UUID.fromString(token.getToken()));
        } catch (Exception e) {
            // Ignore.
        }
        try {
            uosw.delete(token);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error removing permanent token", e);
        }
    }

    /**
     * Authenticate a user using username/password credentials.
     * @param username The name of the authenticating user.
     * @param password The password this user is meant to have.
     * @param classKeys Class Keys for this mine's model.
     * @return A representation of this user's permissions.
     */
    public ApiPermission getPermission(String username, String password,
            Map<String, List<FieldDescriptor>> classKeys) {
        if (StringUtils.isEmpty(username)) {
            throw new AuthenticationException("Empty user name.");
        }
        if (StringUtils.isEmpty(password)) {
            throw new AuthenticationException("Empty password.");
        }
        if (hasProfile(username)) {
            if (!validPassword(username, password)) {
                throw new AuthenticationException("Invalid password supplied: " + password);
            } else {
                Profile p = getProfile(username, classKeys);
                ApiPermission permission = new ApiPermission(p, ApiPermission.Level.RW);
                return permission;
            }
        } else {
            throw new AuthenticationException("Unknown username: " + username);
        }
    }

    private Profile getProfileByApiKey(String token, Map<String,
            List<FieldDescriptor>> classKeys) {
        UserProfile profile = new UserProfile();
        profile.setApiKey(token);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("apiKey");
        try {
            profile = uosw.getObjectByExample(profile, fieldNames);
        } catch (ObjectStoreException e) {
            return null; // Could not be found.
        }
        if (profile == null) {
            throw new AuthenticationException(
                "'" + token + "' is not a valid API access key");
        }
        return getProfile(profile.getUsername(), classKeys);
    }

    /**
     * Check if the profile, whose username is given in input, has been cached by the profile cache
     * @param username the user name
     * @return true if the profile is in the cache
     */
    public boolean isProfileCached(String username) {
        return profileCache.containsKey(username);
    }

    /**
     * Return a list of users with 'superuser' role
     * @return the user list
     */
    public List<String> getSuperUsers() {
        Query q = getSuperUserQuery();

        // Multiple concurrent attempts to access the userprofile (creating/deleting bags,
        // for instance) will cause this to fail. Allow three retries.
        ConcurrentModificationException lastError = null;
        for (int attemptsRemaining = 3; attemptsRemaining >= 0; attemptsRemaining--) {
            List<String> superusers = new ArrayList<String>();
            try {
                for (Object o: uosw.executeSingleton(q)) {
                    superusers.add(String.valueOf(o));
                }
                return superusers;
            } catch (ConcurrentModificationException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    private volatile Query superUserQuery = null;

    private Query getSuperUserQuery() {
        if (superUserQuery == null) {
            superUserQuery = new Query();
            QueryClass qc = new QueryClass(UserProfile.class);
            QueryField qfName = new QueryField(qc, "username");
            superUserQuery.addToSelect(qfName);
            superUserQuery.addFrom(qc);
            QueryField qf = new QueryField(qc, "superuser");
            Constraint c = new SimpleConstraint(qf, ConstraintOp.EQUALS, QueryValue.TRUE);
            superUserQuery.setConstraint(c);
        }
        return superUserQuery;
    }

    /**
     * Return a list of profile with 'superuser' role
     * @return the profile list
     */
    public List<Profile> getSuperUsersProfile() {
        List<Profile> superusersProfile = new ArrayList<Profile>();
        List<String> superusers = getSuperUsers();
        for (String su : superusers) {
            superusersProfile.add(getProfile(su));
        }
        return superusersProfile;
    }

    /**
     * Exception thrown when the authentication fails.
     */
    public static class AuthenticationException extends RuntimeException
    {

        /**
         * Default serial UID
         */
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         * @param message the message to display
         */
        public AuthenticationException(String message) {
            super(message);
        }

    }

    /**
     * Get the preferences for a profile.
     * @param profile The profile to retrieve preferences for.
     * @return A user-preferences map.
     */
    protected UserPreferences getPreferences(Profile profile) {
        try {
            return new UserPreferences(getPreferencesManager(), profile);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve user-preferences", e);
        }
    }

    private PreferencesManager preferencesManager = null;

    private PreferencesManager getPreferencesManager() {
        if (preferencesManager == null) {
            preferencesManager = new PreferencesManager(uosw);
        }
        return preferencesManager;
    }

}
