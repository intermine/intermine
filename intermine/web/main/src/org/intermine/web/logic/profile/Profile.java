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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.lucene.store.Directory;

/**
 * Class to represent a user of the webapp
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class Profile
{
    protected ProfileManager manager;
    protected String username;
    protected Integer userId;
    protected String password;
    protected Map<String, SavedQuery> savedQueries = new TreeMap<String, SavedQuery>();
    protected Map<String, InterMineBag> savedBags = new TreeMap<String, InterMineBag>();
    protected Map<String, TemplateQuery> savedTemplates = new TreeMap<String, TemplateQuery>();
    //protected Map categoryTemplates;
    protected Map queryHistory = new ListOrderedMap();
    private boolean savingDisabled;
    private SearchRepository searchRepository = new SearchRepository();;

    /**
     * Construct a Profile
     * @param manager the manager for this profile
     * @param username the username for this profile
     * @param userId the id of this user
     * @param password the password for this profile
     * @param savedQueries the saved queries for this profile
     * @param savedBags the saved bags for this profile
     * @param savedTemplates the saved templates for this profile
     */
    public Profile(ProfileManager manager, String username, Integer userId, String password,
                   Map<String, SavedQuery> savedQueries, Map<String, InterMineBag> savedBags,
                   Map<String, TemplateQuery> savedTemplates) {
        this.manager = manager;
        this.username = username;
        this.userId = userId;
        this.password = password;
        this.savedQueries.putAll(savedQueries);
        this.savedBags.putAll(savedBags);
        this.savedTemplates.putAll(savedTemplates);
        reindex(TagTypes.TEMPLATE);
        reindex(TagTypes.BAG);
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
        this.userId = userId;
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
        reindex(TagTypes.TEMPLATE);
        reindex(TagTypes.BAG);
    }

    /**
     * Get the users saved templates
     * @return saved templates
     */
    public Map<String, TemplateQuery> getSavedTemplates() {
        return Collections.unmodifiableMap(savedTemplates);
    }

    /**
     * Save a template
     * @param name the template name
     * @param template the template
     */
    public void saveTemplate(String name, TemplateQuery template) {
        savedTemplates.put(name, template);
        if (manager != null && !savingDisabled) {
            manager.saveProfile(this);
            reindex(TagTypes.TEMPLATE);
        }
    }

    /**
     * Delete a template
     * @param name the template name
     */
    public void deleteTemplate(String name) {
        savedTemplates.remove(name);
        if (manager != null) {
            List favourites = manager.getTags("favourite", name, TagTypes.TEMPLATE, username);
            for (Iterator iter = favourites.iterator(); iter.hasNext();) {
                Tag tag = (Tag) iter.next();
                manager.deleteTag(tag);
            }
            if (!savingDisabled) {
                manager.saveProfile(this);
                reindex(TagTypes.TEMPLATE);
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
     * Save a query
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
        Map<String, SavedQuery> newMap = new ListOrderedMap();
        Iterator<String> iter = queryHistory.keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            SavedQuery sq = (SavedQuery) queryHistory.get(name);
            if (name.equals(oldName)) {
                sq = new SavedQuery(newName, sq.getDateCreated(), sq.getPathQuery());
            }
            newMap.put(sq.getName(), sq);
        }
        queryHistory = newMap;
    }

    /**
     * Get the value of savedBags
     * @return the value of savedBags
     */
    public Map<String, InterMineBag> getSavedBags() {
        return Collections.unmodifiableMap(savedBags);
    }

    /**
     * Stores a new bag in the profile. Note that bags are always present in the user profile
     * database, so this just adds the bag to the in-memory list of this profile.
     *
     * @param name the name of the bag
     * @param bag the InterMineBag object
     */
    public void saveBag(String name, InterMineBag bag) {
        savedBags.put(name, bag);
        reindex(TagTypes.BAG);
    }

    /**
     * Returns all bags of a given type
     * @param type the type
     * @param model the Model
     * @return a Map of bag name to bag
     */
    public Map getBagsOfType(String type, Model model) {
        type = model.getPackageName() + "." + type;
        Set<String> classAndSubs = new HashSet<String>();
        classAndSubs.add(type);
        Iterator subIter = model.getAllSubs(model.getClassDescriptorByName(type)).iterator();
        while (subIter.hasNext()) {
            classAndSubs.add(((ClassDescriptor) subIter.next()).getType().getName());
        }

        TreeMap map = new TreeMap();
        for (Iterator iter = savedBags.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            InterMineBag bag = (InterMineBag) entry.getValue();
            if (classAndSubs.contains(model.getPackageName() + "." + bag.getType())) {
                map.put(entry.getKey(), bag);
            }
        }
        return map;

    }

    /**
     * Create a bag - saves it to the user profile database too.
     *
     * @param name the bag name
     * @param type the bag type
     * @param description the bag description
     * @param os the production ObjectStore
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @throws ObjectStoreException if something goes wrong
     */
    public void createBag(String name, String type, String description, ObjectStore os,
            ObjectStoreWriter uosw) throws ObjectStoreException {
        InterMineBag bag = new InterMineBag(name, type, description, new Date(), os, userId, uosw);
        savedBags.put(name, bag);
        reindex(TagTypes.BAG);
    }

    /**
     * Delete a bag
     * @param name the bag name
     */
    public void deleteBag(String name) {
        savedBags.remove(name);
        reindex(TagTypes.BAG);
    }

    /**
     * Create a map from category name to a list of templates contained
     * within that category.
     */
    private void reindex(String type) {
        // We also take this opportunity to index the user's template queries, bags, etc.
        searchRepository.addWebSearchables(type, getWebSearchablesByType(type));
    }

    /**
     * Return a WebSearchable Map for the given type.
     * @param type the type (from TagTypes)
     * @return the Map
     */
    public Map<String, ? extends WebSearchable> getWebSearchablesByType(String type) {
        if (type.equals(TagTypes.TEMPLATE)) {
            return savedTemplates;
        } else {
            if (type.equals(TagTypes.BAG)) {
                return getSavedBags();
            } else {
                throw new RuntimeException("unknown type: " + type);
            }
        }
    }

    /**
     * Get the SearchRepository for this Profile.
     * @return
     */
    public SearchRepository getSearchRepository() {
        return searchRepository;
    }
}
