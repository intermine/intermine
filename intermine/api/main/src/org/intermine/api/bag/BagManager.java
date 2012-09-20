package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.api.profile.TagManager.TagNameException;
import org.intermine.api.profile.TagManager.TagNamePermissionException;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.ObjectStoreBagsForObject;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;

/**
 * A BagManager provides access to all global and/or user bags and methods to fetch them by
 * type, etc.
 * @author Richard Smith
 * @author Daniela Butano
 */
public class BagManager
{
    private static final Logger LOG = Logger.getLogger(BagManager.class);
    private final Profile superProfile;
    private final TagManager tagManager;
    private final SharedBagManager sharedBagManager;
    private final Model model;
    private final ObjectStore osProduction;

    /**
     * The BagManager references the super user profile to fetch global bags.
     * @param superProfile the super user profile
     * @param model the object model
     */
    public BagManager(Profile superProfile, Model model) {
        this.superProfile = superProfile;
        if (superProfile == null) {
            String msg = "Unable to retrieve superuser profile.  Check that the superuser profile "
                + "in the MINE.properties file matches the superuser in the userprofile database.";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
        this.model = model;
        ProfileManager pm = superProfile.getProfileManager();
        this.tagManager = new TagManagerFactory(pm).getTagManager();
        this.sharedBagManager = SharedBagManager.getInstance(pm);
        this.osProduction = superProfile.getProfileManager().getProductionObjectStore();
    }

    /**
     * Fetch globally available bags - superuser public bags that are available to everyone.
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getGlobalBags() {
        return getUserBagsWithTag(superProfile, TagNames.IM_PUBLIC);
    }

    /**
     * Fetch globally available bags - superuser public bags that are available to everyone
     * with a particular tag assigned to them
     * @param tags tag the tags to filter
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getGlobalBagsWithTags(List<String> tags) {
        if (!tags.contains(TagNames.IM_PUBLIC)) {
            tags.add(TagNames.IM_PUBLIC);
        }
        return getUserBagsWithTags(superProfile, tags);
    }

    /**
     * Fetch bags from given protocol with a particular tag assigned to them.
     * @param profile the user to fetch bags from
     * @param tag the tag to filter
     * @return a map from bag name to bag
     */
    protected Map<String, InterMineBag> getUserBagsWithTag(Profile profile, String tag) {
        Map<String, InterMineBag> bagsWithTag = new HashMap<String, InterMineBag>();

        for (Map.Entry<String, InterMineBag> entry : profile.getSavedBags().entrySet()) {
            InterMineBag bag = entry.getValue();
            List<Tag> tags = tagManager.getTags(tag, bag.getName(), TagTypes.BAG,
                    profile.getUsername());
            if (tags.size() > 0) {
                bagsWithTag.put(entry.getKey(), entry.getValue());
            }
        }
        return bagsWithTag;
    }

    /**
     * Give me profile bags matching a set of tags
     * @param profile The profile these bags must belong to.
     * @param tags The tags each bag must have.
     * @return The bags of a profile with all of the required tags.
     */
    protected Map<String, InterMineBag> getUserBagsWithTags(Profile profile, List<String> tags) {
        Map<String, InterMineBag> bagsWithTags = new HashMap<String, InterMineBag>();

    outer:
        for (Map.Entry<String, InterMineBag> entry : profile.getSavedBags().entrySet()) {
            // gimme the bag
            InterMineBag bag = entry.getValue();
            // bag's tags
            List<Tag> bagTags = getTagsForBag(bag, profile);
            // do we have a winner?
        inner:
            for (String requiredTag : tags) {
                for (Tag bagTag : bagTags) {
                    if (bagTag.getTagName().equals(requiredTag)) {
                        continue inner;
                    }
                }
                continue outer;
            }
            bagsWithTags.put(entry.getKey(), entry.getValue());
        }
        return bagsWithTags;
    }

    /**
     * Add tags to a bag.
     * @param tags A list of tag names to add
     * @param bag The bag to add them to
     * @param profile The profile this bag belongs to
     * @throws TagNamePermissionException If the profile is not allowed to apply this tag.
     * @throws TagNameException If the tag name itself is illegal.
     */
    public void addTagsToBag(Collection<String> tags, InterMineBag bag, Profile profile)
        throws TagNameException, TagNamePermissionException {
        for (String tag: tags) {
            tagManager.addTag(tag, bag, profile);
        }
    }

    /**
     * Return true if the bag is public.
     * @param bag The bag in question.
     * @return True if tagged with "im:public"
     */
    public boolean isPublic(InterMineBag bag) {
        return tagManager.hasTag(TagNames.IM_PUBLIC, bag);
    }

    /**
     * Get a list of tags for a given bag.
     * @param bag The bag to get tags for.
     * @param profile The profile whose tags we are looking for.
     * @return A list of Tag objects
     */
    public List<Tag> getTagsForBag(InterMineBag bag, Profile profile) {
        List<Tag> tags = new ArrayList<Tag>(tagManager.getTags(TagNames.IM_PUBLIC, bag.getName(),
            TagTypes.BAG, null));
        tags.addAll(tagManager.getObjectTags(bag, profile));
        return tags;
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
     * Return true if there is at least one user bag for the given profile in
     * the 'not_current' state.
     * @param profile the user to fetch bags for
     * @return a map from bag name to bag
     */
    public boolean isAnyBagNotCurrent(Profile profile) {
        Map<String, InterMineBag> savedBags = profile.getSavedBags();
        for (InterMineBag bag : savedBags.values()) {
            if (bag.getState().equals(BagState.NOT_CURRENT.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if there is at least one user bag for the given profile in
     * the 'to_upgrade' state.
     * @param profile the user to fetch bags for
     * @return a map from bag name to bag
     */
    public boolean isAnyBagToUpgrade(Profile profile) {
        Map<String, InterMineBag> savedBags = profile.getSavedBags();
        for (InterMineBag bag : savedBags.values()) {
            if (bag.getState().equals(BagState.TO_UPGRADE.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetch the shared bags for the given profile.
     * @param profile the user to fetch bags for
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getSharedBags(Profile profile) {
        return sharedBagManager.getSharedBags(profile);
    }

    /**
     * Share the bag given in input with the user which userName input
     * @param bagName the bag name to share
     * @param bagOwnerUserName the owner of the bag to share
     * @param userName the user which the bag is shared with
     * @throws UserNotFoundException if the user does't exist
     * @throws UserAlreadyShareBagException if the bag is already shared by the user
     */
    public void shareBagWithUser(String bagName, String bagOwnerUserName, String userName)
        throws UserNotFoundException, UserAlreadyShareBagException {
        Profile ownerBagProfile = superProfile.getProfileManager().getProfile(bagOwnerUserName);
        InterMineBag bag = ownerBagProfile.getSavedBags().get(bagName);
        if (bag == null) {
            throw new BagDoesNotExistException("The bag " + bagName
                + " doesn't exist or doesn't belong to the user " + bagOwnerUserName);
        }
        sharedBagManager.shareBagWithUser(bag, userName);
    }
    /**
     * Unshare the bag with the user given in input
     * @param bagName the bag to un-share
     * @param bagOwnerUserName the name of the bag owner
     * @param userName the user name sharing the bag
     * @throws UserNotFoundException if the user does't exist
     * @throws BagDoesNotExistException if the bag does't exist
     */
    public void unshareBagWithUser(String bagName, String bagOwnerUserName, String userName) {
        Profile ownerBagProfile = superProfile.getProfileManager().getProfile(bagOwnerUserName);
        InterMineBag bag = ownerBagProfile.getSavedBags().get(bagName);
        if (bag == null) {
            throw new BagDoesNotExistException("The bag " + bagName
                + " doesn't exist or doesn't belong to the user " + bagOwnerUserName);
        }
        sharedBagManager.unshareBagWithUser(bag, userName);
    }

    /**
     * Return the users sharing the list given in input, not the owner
     * @param bagName the bag name the users share
     * @param bagOwnerUserName the name of the bag owner
     * @return the list of users sharing the bag
     */
    public List<String> getUsersSharingBag(String bagName, String bagOwnerUserName) {
        Profile ownerBagProfile = superProfile.getProfileManager().getProfile(bagOwnerUserName);
        InterMineBag bag = ownerBagProfile.getSavedBags().get(bagName);
        if (bag == null) {
            throw new BagDoesNotExistException("The bag " + bagName + " doesn't exist");
        }
        return sharedBagManager.getUsersSharingBag(bag);
    }

    /**
     * Fetch all global bags, user bags and shared bags combined in the same map.
     * If user has a bag with the same name as a global bag the user's bag takes precedence.
     * @param profile the user to fetch bags for
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getBags(Profile profile) {
        // add global bags first, any user bags with same name take precedence
        Map<String, InterMineBag> allBags = Collections.synchronizedSortedMap(
                new TreeMap<String, InterMineBag>());

        allBags.putAll(getGlobalBags());
        if (profile != null) {
            Map<String, InterMineBag> savedBags = profile.getSavedBags();
            allBags.putAll(savedBags);
            Map<String, InterMineBag> sharedBags = sharedBagManager.getSharedBags(profile);
            allBags.putAll(sharedBags);
        }

        return allBags;
    }

    /**
     * Get the bags this user has access to, as long as they are current.
     * @param profile the profile of the user accessing to the bags
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getCurrentBags(Profile profile) {
        Map<String, InterMineBag> ret = Collections.synchronizedSortedMap(
                new TreeMap<String, InterMineBag>(getBags(profile)));
        synchronized (ret) {
            Iterator<InterMineBag> bags = ret.values().iterator();
            while (bags.hasNext()) {
                InterMineBag bag = bags.next();
                if (!bag.isCurrent()) {
                    bags.remove();
                }
            }
        }
        return ret;
    }

    /**
     * Order a map of bags by im:order:n tag
     * @param bags unordered
     * @return an ordered Map of InterMineBags
     */
    public Map<String, InterMineBag> orderBags(Map<String, InterMineBag> bags) {
        Map<String, InterMineBag> bagsOrdered = new TreeMap<String, InterMineBag>(new ByTagOrder());
        bagsOrdered.putAll(bags);
        return bagsOrdered;
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
     * Fetch a global or user or shared bag by name. If user has a bag with the same name
     * as a global bag, the user's bag takes precedence.
     * @param profile the user to fetch bags for
     * @param bagName the name of bag to fetch
     * @return the bag or null if not found
     */
    public InterMineBag getBag(Profile profile, String bagName) {
        return getBags(profile).get(bagName);
    }

    /**
     * Fetch global and user bags of the specified type or a subclass of the specified type.
     * @param profile the user to fetch bags for
     * @param type an unqualified class name
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getBagsOfType(Profile profile, String type) {
        return getBagsOfType(profile, type, false);
    }

    /**
     * Fetch global and user bags current of the specified type or a subclass of the specified type.
     * @param profile the user to fetch bags for
     * @param type an unqualified class name
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getCurrentBagsOfType(Profile profile,
                                                          String type) {
        return getBagsOfType(profile, type, true);
    }

    /**
     * Fetch global and user bags of the specified type or a subclass of the specified type.
     * @param profile the user to fetch bags for
     * @param type an unqualified class name
     * @param onlyCurrent if true return only the current bags
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getBagsOfType(Profile profile, String type,
                                                   boolean onlyCurrent) {
        return filterBagsByType(getBags(profile), type, onlyCurrent);
    }

    /**
     * Fetch user bags current of the specified type or a subclass of the specified type.
     * @param profile the user to fetch bags for
     * @param type an unqualified class name
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getCurrentUserBagsOfType(Profile profile, String type) {
        return filterBagsByType(getUserBags(profile), type, true);
    }

    private Map<String, InterMineBag> filterBagsByType(Map<String, InterMineBag> bags,
            String type, boolean onlyCurrent) {
        Set<String> classAndSubs = new HashSet<String>();
        classAndSubs.add(type);

        ClassDescriptor bagTypeCld = model.getClassDescriptorByName(type);
        if (bagTypeCld == null) {
            throw new NullPointerException("Could not find ClassDescriptor for name " + type);
        }
        for (ClassDescriptor cld : model.getAllSubs(bagTypeCld)) {
            classAndSubs.add(cld.getUnqualifiedName());
        }

        Map<String, InterMineBag> bagsOfType = new HashMap<String, InterMineBag>();
        for (Map.Entry<String, InterMineBag> entry : bags.entrySet()) {
            InterMineBag bag = entry.getValue();
            if (classAndSubs.contains(bag.getType())) {
                if ((onlyCurrent && bag.isCurrent()) || !onlyCurrent) {
                    bagsOfType.put(entry.getKey(), bag);
                }
            }
        }
        return bagsOfType;
    }

    /**
     * Fetch global bags that contain the given id.
     * @param id the id to search bags for
     * @return bags containing the given id
     */
    public Collection<InterMineBag> getGlobalBagsContainingId(Integer id) {
        return getBagsContainingId(getGlobalBags(), id);
    }

    /**
     * Fetch user bags that contain the given id.
     * @param id the id to search bags for
     * @param profile the user to fetch bags from
     * @return bags containing the given id
     */
    public Collection<InterMineBag> getUserBagsContainingId(Profile profile, Integer id) {
        return getBagsContainingId(getUserBags(profile), id);
    }

    /**
     * Fetch bags shared by another user that contain the given id.
     * @param id the id to search bags for
     * @param profile the user to fetch bags from
     * @return bags containing the given id
     */
    public Collection<InterMineBag> getSharedBagsContainingId(Profile profile, Integer id) {
        return getBagsContainingId(getSharedBags(profile), id);
    }

    /**
     * Fetch the current user or global or shared bags that contain the given id. If user has a bag
     * with the same name as a global bag the user's bag takes precedence.
     * Only current bags are included.
     * @param id the id to search bags for
     * @param profile the user to fetch bags from
     * @return bags containing the given id
     */
    public Collection<InterMineBag> getCurrentBagsContainingId(Profile profile,
                                                                           Integer id) {
        HashSet<InterMineBag> bagsContainingId = new HashSet<InterMineBag>();
        for (InterMineBag bag: getGlobalBagsContainingId(id)) {
            if (bag.isCurrent()) {
                bagsContainingId.add(bag);
            }
        }
        for (InterMineBag bag: getUserBagsContainingId(profile, id)) {
            if (bag.isCurrent()) {
                bagsContainingId.add(bag);
            }
        }
        for (InterMineBag bag: getSharedBagsContainingId(profile, id)) {
            if (bag.isCurrent()) {
                bagsContainingId.add(bag);
            }
        }
        return bagsContainingId;
    }

    private Collection<InterMineBag> getBagsContainingId(Map<String, InterMineBag> imBags,
            Integer id) {
        Collection<ObjectStoreBag> objectStoreBags = getObjectStoreBags(imBags.values());
        Map<Integer, InterMineBag> osBagIdToInterMineBag =
            getOsBagIdToInterMineBag(imBags.values());

        // this searches bags for an object
        ObjectStoreBagsForObject osbo = new ObjectStoreBagsForObject(id, objectStoreBags);

        // run query
        Query q = new Query();
        q.addToSelect(osbo);

        Collection<InterMineBag> bagsContainingId = new HashSet<InterMineBag>();

        // this should return all bags with that object
        Results res = osProduction.executeSingleton(q);
        Iterator<Object> resIter = res.iterator();
        while (resIter.hasNext()) {
            Integer osBagId = (Integer) resIter.next();
            if (osBagIdToInterMineBag.containsKey(osBagId)) {
                bagsContainingId.add(osBagIdToInterMineBag.get(osBagId));
            }
        }

        return bagsContainingId;
    }

    private Map<Integer, InterMineBag> getOsBagIdToInterMineBag(Collection<InterMineBag> imBags) {
        Map<Integer, InterMineBag> osBagIdToInterMineBag = new HashMap<Integer, InterMineBag>();

        for (InterMineBag imBag : imBags) {
            osBagIdToInterMineBag.put(new Integer(imBag.getOsb().getBagId()), imBag);
        }
        return osBagIdToInterMineBag;
    }

    private Collection<ObjectStoreBag> getObjectStoreBags(Collection<InterMineBag> imBags) {
        Set<ObjectStoreBag> objectStoreBags = new HashSet<ObjectStoreBag>();
        for (InterMineBag imBag : imBags) {
            objectStoreBags.add(imBag.getOsb());
        }
        return objectStoreBags;
    }

    /**
     * Compare lists based on their im:order:n tag
     * @author radek
     *
     */
    public class ByTagOrder implements Comparator<String>
    {

        /**
         * For a list of tags corresponding to a bag, give us the order set in im:order:n
         * @param tags
         * @return
         */
        private Integer resolveOrderFromTagsList(List<Tag> tags) {
            for (Tag t : tags) {
                String name = t.getTagName();
                if (name.startsWith("im:order:")) {
                    return Integer.parseInt(name.replaceAll("[^0-9]", ""));
                }
            }
            return Integer.MAX_VALUE;
        }

        @Override
        public int compare(String aK, String bK) {
            // get the order from the tags for the bags for the superduper profile
            Integer aO = resolveOrderFromTagsList(tagManager.getTags(null, aK, TagTypes.BAG, null));
            Integer bO = resolveOrderFromTagsList(tagManager.getTags(null, bK, TagTypes.BAG, null));

            if (aO < bO) {
                return -1;
            } else {
                if (aO > bO) {
                    return 1;
                } else {
                    CaseInsensitiveComparator cic = new CaseInsensitiveComparator();
                    return cic.compare(aK, bK);
                }
            }
        }
    }

    /**
     * Lower-case key comparator
     * @author radek
     *
     */
    public class CaseInsensitiveComparator implements Comparator<String>
    {

        @Override
        public int compare(String aK, String bK) {
            return aK.toLowerCase().compareTo(bK.toLowerCase());
        }
    }

}
