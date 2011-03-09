package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
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
 *
 */
public class BagManager
{
    private static final Logger LOG = Logger.getLogger(BagManager.class);
    private Profile superProfile;
    private final TagManager tagManager;
    private final Model model;
    private ObjectStore osProduction;

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
        this.tagManager = new TagManagerFactory(superProfile.getProfileManager()).getTagManager();
        this.osProduction = superProfile.getProfileManager().getProductionObjectStore();
    }

    /**
     * Fetch globally available bags - superuser public bags that are available to everyone.
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getGlobalBags() {
        return getBagsWithTag(superProfile, TagNames.IM_PUBLIC);
    }

    /**
     * Fetch bags from given protocol with a particular tag assigned to them.
     * @param profile the user to fetch bags from
     * @param tag the tag to filter
     * @return a map from bag name to bag
     */
    protected Map<String, InterMineBag> getBagsWithTag(Profile profile, String tag) {
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

        ClassDescriptor bagTypeCld = model.getClassDescriptorByName(type);
        if (bagTypeCld == null) {
            throw new NullPointerException("Could not find ClassDescriptor for name " + type);
        }
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
     * Fetch user or global bags that contain the given id.  If user has a bag with the same name
     * as a global bag the user's bag takes precedence.
     * @param id the id to search bags for
     * @param profile the user to fetch bags from
     * @return bags containing the given id
     */
    public Collection<InterMineBag> getUserOrGlobalBagsContainingId(Profile profile, Integer id) {
        HashSet<InterMineBag> bagsContainingId = new HashSet<InterMineBag>();
        bagsContainingId.addAll(getGlobalBagsContainingId(id));
        bagsContainingId.addAll(getUserBagsContainingId(profile, id));
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
            bagsContainingId.add(osBagIdToInterMineBag.get(osBagId));
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
}
