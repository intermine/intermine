package org.intermine.web;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.store.Directory;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

import org.intermine.InterMineException;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.tagging.TagTypes;

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
    protected Map savedQueries = new TreeMap();
    protected Map savedBags = new TreeMap();
    protected Map savedTemplates = new TreeMap();
    //protected Map categoryTemplates;
    protected Map queryHistory = new ListOrderedMap();
    protected Directory templateIndex;
    
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
            Map savedQueries, Map savedBags, Map savedTemplates) {
        this.manager = manager;
        this.username = username;
        this.userId = userId;
        this.password = password;
        this.savedQueries.putAll(savedQueries);
        this.savedBags.putAll(savedBags);
        this.savedTemplates.putAll(savedTemplates);
        buildTemplateCategories();
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
     * Get the users saved templates
     * @return saved templates
     */
    public Map getSavedTemplates() {
        return Collections.unmodifiableMap(savedTemplates);
    }

    /**
     * Save a template
     * @param name the template name
     * @param template the template
     */
    public void saveTemplate(String name, TemplateQuery template) {
        savedTemplates.put(name, template);
        if (manager != null) {
            manager.saveProfile(this);
        }
        buildTemplateCategories();
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
            manager.saveProfile(this);
        }
        buildTemplateCategories();
    }

    /**
     * Get the value of savedQueries
     * @return the value of savedQueries
     */
    public Map getSavedQueries() {
        return Collections.unmodifiableMap(savedQueries);
    }

    /**
     * Save a query
     * @param name the query name
     * @param query the query
     */
    public void saveQuery(String name, SavedQuery query) {
        savedQueries.put(name, query);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }

    /**
     * Delete a query
     * @param name the query name
     */
    public void deleteQuery(String name) {
        savedQueries.remove(name);
        if (manager != null) {
            manager.saveProfile(this);
        }
    }
    
    /**
     * Get the session query history.
     * @return map from query name to SavedQuery
     */
    public Map getHistory() {
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
        Map newMap = new ListOrderedMap();
        Iterator iter = queryHistory.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
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
    public Map getSavedBags() {
        return Collections.unmodifiableMap(savedBags);
    }
    
    /**
     * Returns all bags of a given type
     * @param type the type
     * @param model the Model
     * @return a Map of bag name to bag
     */
    public Map getBagsOfType(String type, Model model) {
        type = model.getPackageName() + "." + type;
        Set classAndSubs = new HashSet();
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
     * Save a bag
     * @param name the bag name
     * @param bag the bag
     * @param maxNotLoggedSize the maximum bag size allowed when user not logged in, or -1 to allow
     * any bag size
     * @exception InterMineException thrown when the bag size is to high
     */
    public void saveBag(String name, InterMineBag bag, int maxNotLoggedSize)
                    throws InterMineException {
        if (maxNotLoggedSize != -1 && StringUtils.isEmpty(username)
            && bag.getSize() > maxNotLoggedSize) {
            throw new InterMineException("bag.bigNotLoggedIn");
        }
        savedBags.put(name, bag);
        if (manager != null && !StringUtils.isEmpty(username)) {
            manager.saveProfile(this);
        }
    }
    
    /**
     * Save a bag without checking the bag size
     * @param name the bag name
     * @param bag the bag
     * @exception InterMineException thrown when the bag size is to high
     */
    public void saveBag(String name, InterMineBag bag) throws InterMineException {
        saveBag(name, bag, -1);
    }

    /**
     * Delete a bag
     * @param name the bag name
     */
    public void deleteBag(String name) {
        savedBags.remove(name);
        if (manager != null  && !StringUtils.isEmpty(username)) {
            manager.saveProfile(this);
        }
    }
    
    /**
     * Create a map from category name to a list of templates contained
     * within that category.
     */
    private void buildTemplateCategories() {        
        // We also take this opportunity to index the user's template queries
        templateIndex = TemplateRepository.indexTemplates(savedTemplates, "user");
    }
    
    /**
     * Get the users's template index.
     * 
     * @return the user's template index
     */
    public Directory getUserTemplatesIndex() {
        return templateIndex;
    }
}
