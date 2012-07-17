package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.xml.SavedQueryBinding;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.util.CacheMap;
import org.intermine.util.PasswordHasher;

/**
 * Class to manage and persist user profile data such as saved bags
 * @author Mark Woodbridge
 */
public class ProfileManager
{
    private static final Logger LOG = Logger.getLogger(ProfileManager.class);

    protected ObjectStore os;
    protected ObjectStoreWriter uosw;
    protected CacheMap<String, Profile> profileCache = new CacheMap<String, Profile>();
    private String superuser = null;
    /** Number determining format of queries in the database */
    protected int version;

    private final Map<String, LimitedAccessToken> limitedAccessTokens
        = new HashMap<String, LimitedAccessToken>();
    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @param userProfileOS the object store that hold user profile information
     */
    public ProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS) {
        this.os = os;
        this.uosw = userProfileOS;
        //retrieve the super user
        UserProfile superuserProfile = new UserProfile();
        superuserProfile.setSuperuser(true);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("superuser");
        try {
            superuserProfile = (UserProfile) uosw.getObjectByExample(superuserProfile, fieldNames);
            if (superuserProfile != null) {
                superuser = superuserProfile.getUsername();
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load super user profile", e);
        }

        try {
            String versionString = MetadataManager.retrieve(((ObjectStoreInterMineImpl) uosw)
                .getDatabase(), MetadataManager.PROFILE_FORMAT_VERSION);
            LOG.info("Database has userprofile version \"" + versionString + "\"");
            if (versionString == null) {
                version = 0;
            } else {
                version = Integer.parseInt(versionString);
            }
            if ((version < 0) || (version > PathQuery.USERPROFILE_VERSION)) {
                throw new IllegalStateException("Could not recognise userprofile format version "
                        + version + ", maybe you need to update InterMine");
            }
            if (version == 0) {
                // Check to see if we can upgrade
                Query q = new Query();
                QueryClass qc = new QueryClass(SavedQuery.class);
                q.addFrom(qc);
                q.addToSelect(qc);
                List<?> results = uosw.execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
                if (results.isEmpty()) {
                    q = new Query();
                    qc = new QueryClass(SavedTemplateQuery.class);
                    q.addFrom(qc);
                    q.addToSelect(qc);
                    results = uosw.execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
                    if (results.isEmpty()) {
                        // We can safely upgrade the database
                        MetadataManager.store(((ObjectStoreInterMineImpl) uosw).getDatabase(),
                                MetadataManager.PROFILE_FORMAT_VERSION, ""
                                + PathQuery.USERPROFILE_VERSION);
                        version = PathQuery.USERPROFILE_VERSION;
                    }
                }
            }
        } catch (ObjectStoreException e) {
            throw new IllegalStateException("Error upgrading version number in database", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Error reading version number from database", e);
        }
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
        return version;
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
        return getProfile(username, new HashMap<String, List<FieldDescriptor>>());
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
            Results bags = uosw.execute(q, 1000, false, false, true);
            for (Iterator<?> i = bags.iterator(); i.hasNext();) {
                ResultsRow<?> row = (ResultsRow<?>) i.next();
                Integer bagId = (Integer) row.get(0);
                SavedBag savedBag = (SavedBag) row.get(1);
                String bagName = savedBag.getName();
                if (StringUtils.isBlank(bagName)) {
                    LOG.warn("Failed to load bag with blank name on login for user: " + username);
                } else {
                    try {
                        InterMineBag bag = new InterMineBag(os, bagId, uosw);
                        bag.setKeyFieldNames(ClassKeyHelper.getKeyFieldNames(
                                             classKeys, bag.getType()));
                        savedBags.put(bagName, bag);
                    } catch (UnknownBagTypeException e) {
                        LOG.warn("The bag '" + bagName + "' for user '" + username + "'"
                                + " with type: " + savedBag.getType()
                                + " is not in the model. It will be saved into invalidBags", e);
                        InvalidBag bag = new InvalidBag(savedBag, userProfile.getId(), os, uosw);
                        savedInvalidBags.put(bagName, bag);
                    }
                }
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        Map<String, org.intermine.api.profile.SavedQuery> savedQueries =
            new HashMap<String, org.intermine.api.profile.SavedQuery>();
        for (SavedQuery query : userProfile.getSavedQuerys()) {
            try {
                Map queries = SavedQueryBinding.unmarshal(
                            new StringReader(query.getQuery()), savedBags,
                            version);
                if (queries.size() == 0) {
                    queries = PathQueryBinding.unmarshalPathQueries(
                            new StringReader(query.getQuery()),
                            version);
                    if (queries.size() == 1) {
                        Map.Entry entry = (Map.Entry) queries.entrySet().iterator().next();
                        String name = (String) entry.getKey();
                        savedQueries.put(name,
                                         new org.intermine.api.profile.SavedQuery(name, null,
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
        Map<String, ApiTemplate> savedTemplates = new HashMap<String, ApiTemplate>();
        for (SavedTemplateQuery template : userProfile.getSavedTemplateQuerys()) {
            try {
                StringReader sr = new StringReader(template.getTemplateQuery());
                Map<String, TemplateQuery> templateMap =
                        TemplateQueryBinding.unmarshalTemplates(sr, version);
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
        profile = new Profile(this, username, userProfile.getId(), userProfile.getPassword(),
                savedQueries, bags, savedTemplates, userProfile.getApiKey(),
                userProfile.getLocalAccount(), userProfile.getSuperuser());
        profileCache.put(username, profile);
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

            if (userProfile != null) {
                userProfile.setApiKey(profile.getApiKey());
                for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
                    uosw.delete((InterMineObject) i.next());
                }

                for (Iterator i = userProfile.getSavedTemplateQuerys().iterator();
                     i.hasNext();) {
                    uosw.delete((InterMineObject) i.next());
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
                org.intermine.api.profile.SavedQuery query = null;
                try {
                    Map.Entry entry = (Map.Entry) i.next();
                    query = (org.intermine.api.profile.SavedQuery) entry.getValue();
                    SavedQuery savedQuery = new SavedQuery();
                    savedQuery.setQuery(SavedQueryBinding.marshal(query, version));
                    savedQuery.setUserProfile(userProfile);
                    uosw.store(savedQuery);
                } catch (Exception e) {
                    LOG.error("Failed to marshal and save query: " + query, e);
                }
            }

            for (Entry<String, ApiTemplate> entry: profile.getSavedTemplates().entrySet()) {
                ApiTemplate template = null;
                try {
                    template = entry.getValue();
                    SavedTemplateQuery savedTemplate = template.getSavedTemplateQuery();
                    if (savedTemplate == null) {
                        savedTemplate = new SavedTemplateQuery();
                    }
                    savedTemplate.setTemplateQuery(TemplateQueryBinding.marshal(template, version));
                    savedTemplate.setUserProfile(userProfile);
                    uosw.store(savedTemplate);
                    template.setSavedTemplateQuery(savedTemplate);
                } catch (Exception e) {
                    LOG.error("Failed to marshal and save template: " + template, e);
                }
            }

            uosw.store(userProfile);
            profile.setUserId(userProfile.getId());
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }


    public synchronized void createNewProfile(String username, String password) {
        if (this.hasProfile(username)) {
            throw new RuntimeException("Cannot create account: there already exists a user"
                    + " with that name");
        }

        // Let the arcane flaggage commence!
        Profile p = new Profile(this, username, null, password,
                new HashMap(), new HashMap(), new HashMap(),
                null, true, false);

        this.createProfile(p);
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

        if (profile.isLocal()) {
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
        String newApiKey = generateApiKey();
        profile.setApiKey(newApiKey);
        return newApiKey;
    }

    /**
     * Generate a single use API key and store it in memory, before returning it.
     * @param profile the user profile
     * @return the generated key
     */
    public synchronized String generateSingleUseKey(Profile profile) {
        String key = generateApiKey();
        LimitedAccessToken token = new SingleAccessToken(profile);
        limitedAccessTokens.put(key, token);
        return key;
    }

    public synchronized String generate24hrKey(Profile profile) {
        String key = generateApiKey();
        LimitedAccessToken token = new DayToken(profile);
        limitedAccessTokens.put(key, token);
        return key;
    }

    public synchronized boolean tokenHasMoreUses(String token) {
        if (token != null && limitedAccessTokens.containsKey(token)) {
            LimitedAccessToken lat = limitedAccessTokens.get(token);
            if (lat.isValid()) {
                return lat.hasMoreUses();
            } else {
                limitedAccessTokens.remove(token);
            }
        }
        return false;
    }

    private static String generateApiKey() {
        String timePrefix = Long.toHexString(new Date().getTime());
        String randomSuffix = RandomStringUtils.randomAlphanumeric(16);

        StringBuilder sb = new StringBuilder();

        // Interleave the random and predictable portions so that
        // the time string isn't so obvious.
        int tpl = timePrefix.length();
        int rsl = randomSuffix.length();
        for (int i = 0; i < tpl || i < rsl; i++) {
            if (i < randomSuffix.length()) {
                sb.append(randomSuffix.charAt(i));
            }
            if (i < timePrefix.length()) {
                sb.append(timePrefix.charAt(i));
            }
        }

        return sb.toString();
    }

    public String generateReadOnlyAccessToken(Profile profile) {
        return null;
    }

    /**
     * Creates a profile in the userprofile database withou adding bag.
     * Method used by the ProfielReadXml.
     *
     * @param profile a Profile object
     */
    public synchronized void createProfileWithoutBags(Profile profile) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername(profile.getUsername());
        userProfile.setPassword(PasswordHasher.hashPassword(profile.getPassword()));
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
            profile = (UserProfile) uosw.getObjectByExample(profile, fieldNames);
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
     * @return the superuser name
     */
    public String getSuperuser() {
        return superuser;
    }

    /**
     * @return the superuser profile
     */
    public Profile getSuperuserProfile() {
        return getProfile(superuser);
    }

    /**
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
            String token = generateApiKey();
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

    private static abstract class LimitedAccessToken
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
        };

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
            profile = (UserProfile) uosw.getObjectByExample(profile, fieldNames);
        } catch (ObjectStoreException e) {
            return null; // Could not be found.
        }
        if (profile == null) {
            throw new AuthenticationException(
                "'" + token + "' is not a valid API access key");
        }
        return getProfile(profile.getUsername(), classKeys);
    }

    public static class AuthenticationException extends RuntimeException
    {

        /**
         * Default serial UID
         */
        private static final long serialVersionUID = 1L;

        public AuthenticationException(String message) {
            super(message);
        }

    }
}
