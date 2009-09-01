package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;

/**
 * A BagManager provides access to all global and/or user bags and methods to fetch them by
 * type, etc.
 * @author Richard Smith
 *
 */
public class BagManager 
{
    private Profile superProfile;
    private final TagManager tagManager;
    private final Model model;
 
    /**
     * The BagManager references the super user profile to fetch global bags.
     * @param superProfile the super user profile
     * @param model the object model
     */
    public BagManager(Profile superProfile, Model model) {
        this.superProfile = superProfile;
        this.model = model;
        tagManager = new TagManagerFactory(superProfile.getProfileManager()).getTagManager();
    }
    
    /**
     * Fetch globally available bags - superuser public bags that are available to everyone.
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getGlobalBags() {
        return getBagsWithTag(superProfile, TagNames.IM_PUBLIC);
    }
    
    /**
     * Fetch bags for the given profile.
     * @param profile the user to fetch bags for
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getUserBags(Profile profile) {
        return profile.getSavedBags();
    }
    
    /**
     * Fetch all global bags and user bags combined in the same map.  If user has a bag with the
     * same name as a global bag the user's bag takes precedence.
     * @param profile the user to fetch bags for
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getUserAndGlobalBags(Profile profile) {
        // add global bags first, any user bags with same name take precedence
        Map<String, InterMineBag> allBags = new HashMap<String, InterMineBag>();
        
        allBags.putAll(getGlobalBags());
        allBags.putAll(profile.getSavedBags());
        
        return allBags;
    }
    
    /**
     * Fetch a global bag by name.
     * @param bagName the name of bag to fetch
     * @return the bag or null if not found
     */
    public InterMineBag getGlobalBag(String bagName) {
        return getGlobalBags().get(bagName);
    }

    /**
     * Fetch a user bag by name.
     * @param profile the user to fetch bags for
     * @param bagName the name of bag to fetch
     * @return the bag or null if not found
     */
    public InterMineBag getUserBag(Profile profile, String bagName) {
        return getUserBags(profile).get(bagName);
    }
    
    /**
     * Fetch a global or user bag by name.  If user has a bag with the same name as a global bag
     * the user's bag takes precedence.
     * @param profile the user to fetch bags for
     * @param bagName the name of bag to fetch
     * @return the bag or null if not found
     */
    public InterMineBag getUserOrGlobalBag(Profile profile, String bagName) {
        return getUserAndGlobalBags(profile).get(bagName);
    }
    
    /**
     * Fetch global and user bags of the specified type or a subclass of the specified type.
     * @param profile the user to fetch bags for
     * @param type an unqualified class name
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getUserOrGlobalBagsOfType(Profile profile, String type) {
        Set<String> classAndSubs = new HashSet<String>();
        classAndSubs.add(type);
        
        String qualifiedType = model.getPackageName() + "." + type;
        ClassDescriptor bagTypeCld = model.getClassDescriptorByName(qualifiedType);
        for (ClassDescriptor cld : model.getAllSubs(bagTypeCld)) {
            classAndSubs.add(cld.getUnqualifiedName());
        }
        
        Map<String, InterMineBag> bagsOfType = new HashMap<String, InterMineBag>();
        for (Map.Entry<String, InterMineBag> entry : getUserAndGlobalBags(profile).entrySet()) {
            InterMineBag bag = entry.getValue();
            if (classAndSubs.contains(bag.getType())) {
                bagsOfType.put(entry.getKey(), bag);
            }
        }
        return bagsOfType;
    }    

    
    private Map<String, InterMineBag> getBagsWithTag(Profile profile, String tag) {
        Map<String, InterMineBag> globalBags = new HashMap<String, InterMineBag>();
        
        for (Map.Entry<String, InterMineBag> entry : profile.getSavedBags().entrySet()) {
            InterMineBag bag = entry.getValue();
            List<Tag> tags = tagManager.getTags(TagNames.IM_PUBLIC, bag.getName(), TagTypes.BAG,
                    profile.getUsername());
            if (tags.size() > 0) {
                globalBags.put(entry.getKey(), entry.getValue());
            }
        }
        return globalBags;
    }
}
