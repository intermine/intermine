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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.bag.ClassKeysNotFoundException;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.search.CreationEvent;
import org.intermine.api.search.DeletionEvent;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.search.UserRepository;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.api.util.NameUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.pathquery.PathQuery;

/**
 * Class to represent a user of the webapp
 *
 * The profile is responsible for informing its search repository of all web-searchable objects
 * created or deleted on its watch.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @author Daniela Butano
 */
public class Profile
{
    private static final Logger LOG = Logger.getLogger(Profile.class);
    /** Empty typed map holding no saved queries - useful when creating profiles. **/
    public static final Map<String, SavedQuery> NO_QUERIES = Collections.emptyMap();
    /** Empty typed map holding no bags - useful when creating profiles. **/
    public static final Map<String, InterMineBag> NO_BAGS = Collections.emptyMap();
    /** Empty typed map holding no templates - useful when creating profiles. **/
    public static final Map<String, ApiTemplate> NO_TEMPLATES = Collections.emptyMap();
    protected ProfileManager manager;
    protected String username;
    protected Integer userId;
    protected String password;
    protected boolean isSuperUser;
    protected Map<String, SavedQuery> savedQueries = new TreeMap<String, SavedQuery>();
    protected Map<String, InterMineBag> savedBags
        = Collections.synchronizedMap(new TreeMap<String, InterMineBag>());
    protected Map<String, ApiTemplate> savedTemplates = new TreeMap<String, ApiTemplate>();

    protected Map<String, InvalidBag> savedInvalidBags = new TreeMap<String, InvalidBag>();
    // was ListOrderedMap() - but that is the same as a LinkedHashMap.

    protected boolean savingDisabled;
    private SearchRepository searchRepository;
    private String token;
    private Map<String, String> preferences;
    @SuppressWarnings("unchecked")
    protected Map<String, SavedQuery> queryHistory = new ListOrderedMap();

    /**
     * True if this account is purely local. False if it was created
     * in reference to another authenticator, such as an OpenID provider.
     */
    private final boolean isLocal;

    /**
     * Construct a Profile
     * @param manager the manager for this profile
     * @param username the username for this profile
     * @param userId the id of this user
     * @param password the password for this profile
     * @param savedQueries the saved queries for this profile
     * @param savedBags the saved bags for this profile
     * @param savedTemplates the saved templates for this profile
     * @param token the token to use as an API key
     * @param isLocal true if the account is local
     * @param isSuperUser true if the user is a super user
     */
    public Profile(ProfileManager manager, String username, Integer userId, String password,
                   Map<String, SavedQuery> savedQueries, Map<String, InterMineBag> savedBags,
                   Map<String, ApiTemplate> savedTemplates, String token, boolean isLocal,
                   boolean isSuperUser) {
        this.manager = manager;
        this.username = username;
        this.userId = userId;
        this.password = password;
        this.isLocal = isLocal;
        this.isSuperUser = isSuperUser;
        if (savedQueries != null) {
            this.savedQueries.putAll(savedQueries);
        }
        if (savedBags != null) {
            this.savedBags.putAll(savedBags);
        }
        if (savedTemplates != null) {
            this.savedTemplates.putAll(savedTemplates);
        }
        searchRepository = new UserRepository(this);
        this.token = token;
        if (this.userId != null) {
            // preferences backed by DB.
            this.preferences = manager.getPreferences(this);
        } else {
            // preferences just stored in memory.
            this.preferences = new HashMap<String, String>();
        }
    }

    /**
     * Construct a Profile
     * @param manager the manager for this profile
     * @param username the username for this profile
     * @param userId the id of this user
     * @param password the password for this profile
     * @param savedQueries the saved queries for this profile
     * @param savedBags the saved bags for this profile
     * @param savedInvalidBags the saved bags which type doesn't match with the model
     * @param savedTemplates the saved templates for this profile
     * @param token The token to use as an API key
     * @param isLocal true if the account is local
     * @param isSuperUser the flag identifying the super user
     */
    public Profile(ProfileManager manager, String username, Integer userId, String password,
                   Map<String, SavedQuery> savedQueries, Map<String, InterMineBag> savedBags,
                   Map<String, InterMineBag> savedInvalidBags,
                   Map<String, ApiTemplate> savedTemplates, String token, boolean isLocal,
                   boolean isSuperUser) {
        this(manager, username, userId, password, savedQueries, savedBags, savedTemplates, token,
            isLocal, isSuperUser);
        for (Entry<String, InterMineBag> pair: savedInvalidBags.entrySet()) {
            this.savedInvalidBags.put(pair.getKey(), pair.getValue().invalidate());
        }
    }

    /**
     * Construct a Profile
     * @param manager the manager for this profile
     * @param username the username for this profile
     * @param userId the id of this user
     * @param password the password for this profile
     * @param savedQueries the saved queries for this profile
     * @param bagset a bagset (=valid and invalid bags) for this profile
     * @param savedTemplates the saved templates for this profile
     * @param token The token to use as an API key
     * @param isLocal true if the account is local
     * @param isSuperUser the flag identifying the super user
     */
    public Profile(ProfileManager manager, String username, Integer userId, String password,
            Map<String, SavedQuery> savedQueries, BagSet bagset,
            Map<String, ApiTemplate> savedTemplates, String token, boolean isLocal,
            boolean isSuperUser) {
        this(manager, username, userId, password, savedQueries, bagset.getBags(), savedTemplates,
                token, isLocal, isSuperUser);
        this.savedInvalidBags.putAll(bagset.getInvalidBags());
    }

    /**
     * Construct a profile without an API key
     * @param manager the manager for this profile
     * @param username the username for this profile
     * @param userId the id of this user
     * @param password the password for this profile
     * @param savedQueries the saved queries for this profile
     * @param savedBags the saved bags for this profile
     * @param savedTemplates the saved templates for this profile
     * @param isLocal true if the account is local
     * @param isSuperUser the flag identifying the super user
     */
    public Profile(ProfileManager manager, String username, Integer userId, String password,
            Map<String, SavedQuery> savedQueries, Map<String, InterMineBag> savedBags,
            Map<String, ApiTemplate> savedTemplates, boolean isLocal, boolean isSuperUser) {
        this(manager, username, userId, password, savedQueries, savedBags, savedTemplates,
                null, isLocal, isSuperUser);
    }

    /**
     * Return the ProfileManager that was passed to the constructor.
     * @return the ProfileManager
     */
    public ProfileManager getProfileManager() {
        return manager;

    }

    /**
     * Get the value of username
     * @return the value of username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get this user's preferred email address.
     * @return This user's email address.
     */
    public String getEmailAddress() {
        if (prefers(UserPreferences.EMAIL)) {
            return preferences.get(UserPreferences.EMAIL);
        }
        return getUsername();
    }

    /**
     * Return a first part of the username before the "@" sign (used in metabolicMine)
     * @author radek
     *
     * @return String
     */
    public String getName() {
        // AKA is my name for myself (not public).
        if (prefers(UserPreferences.AKA)) {
            return getPreferences().get(UserPreferences.AKA);
        }
        // ALIAS is public (and globally unique) name
        if (prefers(UserPreferences.ALIAS)) {
            return getPreferences().get(UserPreferences.ALIAS);
        }
        // Else try and work out something reasonable.
        int atPos = username.indexOf("@");
        if (atPos > 0) {
            return username.substring(0, atPos);
        } else {
            return username;
        }
    }

    /**
     * Return true if and only if the user is logged is (and the Profile will be written to the
     * userprofile).
     * @return Return true if logged in
     */
    public boolean isLoggedIn() {
        return getUsername() != null;
    }

    /**
     * Return true if and only if the user logged is superuser
     * @return Return true if superuser
     */
    public boolean isSuperuser() {
        return isSuperUser;
    }

    /**
     * Alias of isSuperUser() for jsp purposes.
     * @return The same value as isSuperUser().
     */
    public boolean getSuperuser() {
        return isSuperuser();
    }

    /**
     * Set the superuser flag and store it in userprofile database
     * @param isSuperUser if true the profile is set as superuser
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void setSuperuser(boolean isSuperUser) throws ObjectStoreException {
        ObjectStoreWriter uosw = manager.getProfileObjectStoreWriter();
        UserProfile p = (UserProfile) uosw.getObjectStore().getObjectById(userId,
            UserProfile.class);
        p.setSuperuser(isSuperUser);
        uosw.store(p);
        this.isSuperUser = isSuperUser;
    }

    /**
     * Get the value of userId
     * @return an Integer
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Set the userId
     *
     * @param userId an Integer
     */
    public void setUserId(Integer userId) {
        Integer oldId = getUserId();
        this.userId = userId;
        if (this.userId != null && !this.userId.equals(oldId)) { // Need to update the preferences
            Map<String, String> oldPrefs = preferences;
            this.preferences = manager.getPreferences(this);
            preferences.putAll(oldPrefs);
            oldPrefs.clear(); // Delete them now that they are copied over.
        }
    }

    /**
     * Get the value of password
     * @return the value of password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Disable saving until enableSaving() is called.  This is called before many templates or
     * queries need to be saved or deleted because each call to ProfileManager.saveProfile() is
     * slow.
     */
    public void disableSaving() {
        savingDisabled = true;
    }

    /**
     * Re-enable saving when saveTemplate(), deleteQuery() etc. are called.  Also calls
     * ProfileManager.saveProfile() to write this Profile to the database and rebuilds the
     * template description index.
     */
    public void enableSaving() {
        savingDisabled = false;
        if (manager != null) {
            manager.saveProfile(this);
        }
        // Why were these calls here in the first place?
        //reindex(TagTypes.TEMPLATE);
        //reindex(TagTypes.BAG);
    }

    /**
     * Get the users saved templates
     * @return saved templates
     */
    public synchronized Map<String, ApiTemplate> getSavedTemplates() {
        return Collections.unmodifiableMap(savedTemplates);
    }


    /**
     * Save a template
     * @param name the template name
     * @param template the template
     * @throws BadTemplateException if the template name is invalid.
     */
    public void saveTemplate(String name, ApiTemplate template) throws BadTemplateException {
        if (!NameUtil.isValidName(template.getName())) {
            throw new BadTemplateException("Invalid name.");
        }
        savedTemplates.put(name, template);
        if (manager != null && !savingDisabled) {
            manager.saveProfile(this);
        }
        searchRepository.receiveEvent(new CreationEvent(template));
    }

    /**
     * get a template
     * @param name the template
     * @return template
     */
    public ApiTemplate getTemplate(String name) {
        return savedTemplates.get(name);
    }

    /**
     * Delete a template and its tags, rename the template tracks adding the prefix "deleted_"
     * to the previous name. If trackerDelegate is null, the template tracks are not renamed
     * @param name the template name
     * @param trackerDelegate used to rename the template tracks.
     * @param deleteTracks true if we want to delete the tracks associated to the template.
     * Actually the tracks have never been deleted but only renamed
     */
    public void deleteTemplate(String name, TrackerDelegate trackerDelegate,
        boolean deleteTracks) {
        ApiTemplate template = savedTemplates.get(name);
        if (template == null) {
            LOG.warn("Attempt to delete non-existant template: " + name);
        } else {
            savedTemplates.remove(name);
            if (manager != null) {
                if (!savingDisabled) {
                    manager.saveProfile(this);
                }
            }

            searchRepository.receiveEvent(new DeletionEvent(template));

            TagManager tagManager = getTagManager();
            tagManager.deleteObjectTags(name, TagTypes.TEMPLATE, username);
            if (trackerDelegate != null && deleteTracks) {
                trackerDelegate.updateTemplateName(name, "deleted_" + name);
            }
        }
    }

    /**
     * Get the value of savedQueries
     * @return the value of savedQueries
     */
    public Map<String, SavedQuery> getSavedQueries() {
        return Collections.unmodifiableMap(savedQueries);
    }

    /**
     * Save or update a query.
     *
     * If a query with the given name exists in this profile, that query will be overwritten.
     *
     * @param name the query name
     * @param query the query
     */
    public void saveQuery(String name, SavedQuery query) {
        savedQueries.put(name, query);
        if (manager != null && !savingDisabled) {
            manager.saveProfile(this);
        }
    }

    /**
     * Save the map of queries as given to the user's profile, avoiding all possible name
     * collisions. This method will not overwrite any existing user data.
     *
     * @param toSave The queries to save.
     * @return successes The queries as they were saved.
     */
    public Map<String, String> saveQueries(Map<? extends String, ? extends PathQuery> toSave) {
        Map<String, String> successes = new HashMap<String, String>();
        Date now = new Date();
        for (Entry<? extends String, ? extends PathQuery> pair: toSave.entrySet()) {
            String name = pair.getKey();
            int c = 1;
            while (savedQueries.containsKey(name)) {
                name = pair.getKey() + "_"  + c++;
            }
            SavedQuery sq = new SavedQuery(name, now, pair.getValue());
            savedQueries.put(name, sq);
            successes.put(pair.getKey(), name);
        }
        try {
            if (manager != null && !savingDisabled) {
                manager.saveProfile(this);
            }
        } catch (Exception e) {
            // revert all changes.
            savedQueries.entrySet().removeAll(successes.keySet());
            try {
                manager.saveProfile(this);
            } catch (Exception e2) {
                // Ignore.
            }
            throw new RuntimeException("Could not save queries.", e);
        }
        return successes;
    }

    /**
     * Delete a query
     * @param name the query name
     */
    public void deleteQuery(String name) {
        savedQueries.remove(name);
        if (manager != null && !savingDisabled) {
            manager.saveProfile(this);
        }
    }

    /**
     * Get the session query history.
     * @return map from query name to SavedQuery
     */
    public Map<String, SavedQuery> getHistory() {
        return Collections.unmodifiableMap(queryHistory);
    }

    /**
     * Save a query to the query history.
     * @param query the SavedQuery to save to the history
     */
    public void saveHistory(SavedQuery query) {
        queryHistory.put(query.getName(), query);
    }

    /**
     * Remove an item from the query history.
     * @param name the of the SavedQuery from the history
     */
    public void deleteHistory(String name) {
        queryHistory.remove(name);
    }

    /**
     * Rename an item in the history.
     * @param oldName the name of the old item
     * @param newName the new name
     */
    public void renameHistory(String oldName, String newName) {
        SavedQuery q = queryHistory.get(oldName);
        if (q == null) {
            throw new IllegalArgumentException("No query named " + oldName);
        }
        if (StringUtils.isBlank(newName)) {
            throw new IllegalArgumentException("new name must not be blank");
        }
        if (queryHistory.containsKey(newName)) {
            throw new IllegalArgumentException("there is already a query named " + newName);
        }
        q = new SavedQuery(newName, q.getDateCreated(), q.getPathQuery());
        @SuppressWarnings("unchecked")
        // Rebuild history to preserve iteration order.
        Map<String, SavedQuery> newHistory = new ListOrderedMap();
        for (Entry<String, SavedQuery> oldEntry: queryHistory.entrySet()) {
            if (oldName.equals(oldEntry.getKey())) {
                newHistory.put(newName, q);
            } else {
                newHistory.put(oldEntry.getKey(), oldEntry.getValue());
            }
        }
        queryHistory = newHistory;
    }

    /**
     * Get the value of savedBags
     * @return the value of savedBags
     */
    public synchronized Map<String, InterMineBag> getSavedBags() {
        return Collections.unmodifiableMap(savedBags);
    }

    /**
     * @return the invalid bags for this profile.
     */
    public synchronized Map<String, InvalidBag> getInvalidBags() {
        return Collections.unmodifiableMap(this.savedInvalidBags);
    }

    /**
     * Fix this bag by changing its type
     * @param name the invalid bag name
     * @param newType The new type of this list
     * @throws UnknownBagTypeException If the new type is not in the current model.
     * @throws ObjectStoreException If there is a problem saving state to the DB.
     */
    public synchronized void fixInvalidBag(String name, String newType)
        throws UnknownBagTypeException, ObjectStoreException {
        InvalidBag invb = savedInvalidBags.get(name);
        InterMineBag imb = invb.amendTo(newType);
        savedInvalidBags.remove(name);
        saveBag(name, imb);
    }

    /**
     * Get all bags associated with this profile, both valid and invalid.
     *
     * @return a map from name to bag.
     */
    public synchronized Map<String, StorableBag> getAllBags() {
        Map<String, StorableBag> ret = new HashMap<String, StorableBag>();
        ret.putAll(savedBags);
        ret.putAll(savedInvalidBags);
        return Collections.unmodifiableMap(ret);
    }

    /**
     * Get the saved bags in a map of "status key" =&gt; map of lists
     * @return the map from status to a map containing bag name and bag
     */
    public Map<String, Map<String, InterMineBag>> getSavedBagsByStatus() {
        Map<String, Map<String, InterMineBag>> result =
            new LinkedHashMap<String, Map<String, InterMineBag>>();
        // maintain order on the JSP page
        result.put("NOT_CURRENT", new HashMap<String, InterMineBag>());
        result.put("TO_UPGRADE", new HashMap<String, InterMineBag>());
        result.put("CURRENT", new HashMap<String, InterMineBag>());
        result.put("UPGRADING", new HashMap<String, InterMineBag>());

        for (InterMineBag bag : savedBags.values()) {
            String state = bag.getState();
            // XXX: this can go pear shaped if new states are introduced
            Map<String, InterMineBag> stateMap = result.get(state);
            stateMap.put(bag.getName(), bag);
        }
        return result;
    }

    /**
     * Get the value of savedBags current
     * @return the value of savedBags
     */
    public Map<String, InterMineBag> getCurrentSavedBags() {
        Map<String, InterMineBag> clone = new HashMap<String, InterMineBag>();
        clone.putAll(savedBags);
        for (InterMineBag bag : savedBags.values()) {
            if (!bag.isCurrent()) {
                clone.remove(bag.getName());
            }
        }
        return clone;
    }

    /**
     * Stores a new bag in the profile. Note that bags are always present in the user profile
     * database, so this just adds the bag to the in-memory list of this profile.
     *
     * @param bag the InterMineBag object
     */
    public void saveBag(InterMineBag bag) {
        if (bag == null) {
            throw new IllegalArgumentException("bag may not be null");
        }
        saveBag(bag.getName(), bag);
    }

    /**
     * Stores a new bag in the profile. Note that bags are always present in the user profile
     * database, so this just adds the bag to the in-memory list of this profile.
     *
     * @param name the name of the bag
     * @param bag the InterMineBag object
     */
    public void saveBag(String name, InterMineBag bag) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("No name specified for the list to save.");
        }
        savedBags.put(name, bag);
        searchRepository.receiveEvent(new CreationEvent(bag));
    }

    /**
     * Create a bag and save it to the userprofile database.
     *
     * @param name the bag name
     * @param type the bag type
     * @param description the bag description
     * @param classKeys the classKeys used to obtain  the primary identifier field
     * @return the new bag
     * @throws UnknownBagTypeException if the bag type is wrong
     * @throws ClassKeysNotFoundException if the classKeys is empty
     * @throws ObjectStoreException if something goes wrong
     */
    public InterMineBag createBag(String name, String type, String description,
        Map<String, List<FieldDescriptor>> classKeys)
        throws UnknownBagTypeException, ClassKeysNotFoundException, ObjectStoreException {
        ObjectStore os = manager.getProductionObjectStore();
        ObjectStoreWriter uosw = manager.getProfileObjectStoreWriter();
        List<String> keyFieldNames = ClassKeyHelper.getKeyFieldNames(
                                    classKeys, type);
        InterMineBag bag = new InterMineBag(name, type, description, new Date(),
                               BagState.CURRENT, os, userId, uosw, keyFieldNames);
        saveBag(name, bag);
        return bag;
    }

    /**
     * Delete a bag from the user account, if user is logged in also deletes from the userprofile
     * database.
     * If there is no such bag associated with the account, no action is performed.
     * @param name the bag name
     * @throws ObjectStoreException if problems deleting bag
     */
    public void deleteBag(String name) throws ObjectStoreException {
        if (!savedBags.containsKey(name) && !savedInvalidBags.containsKey(name)) {
            throw new BagDoesNotExistException(name + " not found");
        }
        StorableBag bagToDelete;
        if (savedBags.containsKey(name)) {
            bagToDelete = savedBags.get(name);
            savedBags.remove(name);
        } else {
            bagToDelete = savedInvalidBags.get(name);
            savedInvalidBags.remove(name);
        }
        if (isLoggedIn()) {
            getSharedBagManager().unshareBagWithAllUsers(bagToDelete);
            bagToDelete.delete();
        } else { //refresh the search repository
            bagToDelete.delete();
        }

        TagManager tagManager = getTagManager();
        tagManager.deleteObjectTags(name, TagTypes.BAG, username);
    }

    /**
     * Update the type of bag.
     * If there is no such bag associated with the account, no action is performed.
     * @param name the bag name
     * @param newType the type to set
     * @throws UnknownBagTypeException if the bag type is wrong
     * @throws ObjectStoreException if problems storing bag
     */
    public void updateBagType(String name, String newType)
        throws UnknownBagTypeException, ObjectStoreException {
        if (!savedBags.containsKey(name) && !savedInvalidBags.containsKey(name)) {
            throw new BagDoesNotExistException(name + " not found");
        }
        if (savedBags.containsKey(name)) {
            InterMineBag bagToUpdate = savedBags.get(name);
            if (isLoggedIn()) {
                bagToUpdate.setType(newType);
            }
        } else {
            InterMineBag recovered = savedInvalidBags.get(name).amendTo(newType);
            savedInvalidBags.remove(name);
            saveBag(recovered.getName(), recovered);
        }
    }


    /**
     * Rename an existing bag, throw exceptions when bag doesn't exist of if new name already
     * exists.  Moves tags from old bag to new bag.
     * @param oldName the bag to rename
     * @param newName new name for the bag
     * @throws ObjectStoreException if problems storing
     */
    public void renameBag(String oldName, String newName) throws ObjectStoreException {
        if (!getAllBags().containsKey(oldName)) {
            throw new BagDoesNotExistException("Attempting to rename " + oldName);
        }
        if (getAllBags().containsKey(newName)) {
            throw new ProfileAlreadyExistsException("Attempting to rename a bag to a new name that"
                    + " already exists: " + newName);
        }
        if (savedBags.containsKey(oldName)) {
            InterMineBag bag = savedBags.get(oldName);
            savedBags.remove(oldName);
            bag.setName(newName);
            saveBag(newName, bag);
        } else {
            InvalidBag bag = savedInvalidBags.get(oldName);
            InvalidBag newBag = bag.rename(newName);
            savedInvalidBags.remove(oldName);
            savedInvalidBags.put(newName, newBag);
        }
        moveTagsToNewObject(oldName, newName, TagTypes.BAG);
    }

    /**
     * Update an existing template, throw exceptions when template doesn't exist.
     * Moves tags from old template to new template.
     * @param oldName the template to rename
     * @param template the new template
     * @throws ObjectStoreException if problems storing
     * @throws BadTemplateException if bad template
     */
    public void updateTemplate(String oldName, ApiTemplate template)
        throws ObjectStoreException, BadTemplateException {
        if (oldName == null) {
            throw new IllegalArgumentException("oldName may not be null");
        }
        ApiTemplate old = savedTemplates.get(oldName);
        if (old == null) {
            throw new IllegalArgumentException("Attempting to rename a template that doesn't"
                    + " exist: " + oldName);
        }

        ApiTemplate oldTemplate = savedTemplates.remove(oldName);
        try {
            saveTemplate(template.getName(), template);
        } catch (BadTemplateException bte) {
            savedTemplates.put(oldName, oldTemplate);
            throw bte;
        }
        if (!oldName.equals(template.getName())) {
            searchRepository.receiveEvent(new DeletionEvent(old));
            moveTagsToNewObject(oldName, template.getName(), TagTypes.TEMPLATE);
        }
    }

    private void moveTagsToNewObject(String oldTaggedObj, String newTaggedObj, String type) {
        TagManager tagManager = getTagManager();
        List<Tag> tags = tagManager.getTags(null, oldTaggedObj, type, username);
        for (Tag tag : tags) {
            try {
                tagManager.addTag(tag.getTagName(), newTaggedObj, type, this);
            } catch (TagManager.TagNameException e) {
                throw new IllegalStateException("Existing tag is illegal: " + tag.getTagName(), e);
            } catch (TagManager.TagNamePermissionException e) {
                throw new IllegalStateException("Object tagged with " + tag.getTagName(), e);
            }
            tagManager.deleteTag(tag);
        }
    }

    private TagManager getTagManager() {
        return new TagManagerFactory(manager).getTagManager();
    }

    private SharedBagManager getSharedBagManager() {
        return SharedBagManager.getInstance(manager);
    }

    /**
     * Return a WebSearchable Map for the given type.
     * @param type the type (from TagTypes)
     * @return the Map
     */
    public Map<String, ? extends WebSearchable> getWebSearchablesByType(String type) {
        if (type.equals(TagTypes.TEMPLATE)) {
            return savedTemplates;
        }
        if (type.equals(TagTypes.BAG)) {
            return getSavedBags();
        }
        throw new RuntimeException("unknown type: " + type);
    }

    /**
     * Get the SearchRepository for this Profile.
     * @return the SearchRepository for the user
     */
    public SearchRepository getSearchRepository() {
        return searchRepository;
    }


    /**
     * @return the user's API key token.
     */
    public String getApiKey() {
        return token;
    }

    private String dayToken = null;

    /**
     * Get a token with at least an hour of validity, and up to 24 hours.
     * @return A token for web-service use.
     */
    public String getDayToken() {
        if (dayToken == null || !manager.tokenHasMoreUses(dayToken)) {
            dayToken = getProfileManager().generate24hrKey(this);
        }
        return dayToken;
    }

    /**
     * Set the API token for this user, and save it in the backing DB.
     * @param token The API token.
     */
    public void setApiKey(String token) {
        this.token = token;
        if (manager != null && !savingDisabled) {
            manager.saveProfile(this);
        }
    }

    /**
     * Returns true if this is a local account, and not, for
     * example, an OpenID account.
     * @return Whether or not this is a local account.
     */
    public boolean isLocal() {
        return this.isLocal;
    }

    /**
     * Return a single use API key for this profile
     * @return the single use key
     */
    public String getSingleUseKey() {
        if (isLoggedIn()) {
            return manager.generateSingleUseKey(this);
        } else {
            return "";
        }
    }

    /**
     * Return the shared bags for the profile.
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getSharedBags() {
        return getSharedBagManager().getSharedBags(this);
    }

    /**
     * Update the user repository with the sharedbags
     */
    public void updateUserRepositoryWithSharedBags() {
        if (searchRepository instanceof UserRepository) {
            ((UserRepository) searchRepository).updateUserRepositoryWithSharedBags();
        }
    }

    /**
     * Get the object representing the preferences of this user.
     *
     * Changes to this user's preferences can be written directly into
     * this object.
     * @return A representation of the preferences of a user.
     */
    public Map<String, String> getPreferences() {
        return preferences;
    }

    /**
     * Determine whether a user perfers a certain thing or not.
     * @param preference The name of the preference.
     * @return Whether this preference is set by this user.
     */
    public boolean prefers(String preference) {
        return preferences.containsKey(preference);
    }

}
