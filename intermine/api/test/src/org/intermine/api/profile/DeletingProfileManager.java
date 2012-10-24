package org.intermine.api.profile;

import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedTemplateQuery;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;

/**
 * A subclass of ProfileManager that provides a public deleteProfile method for use in testing.
 * @author Richard Smith
 *
 */
public class DeletingProfileManager extends ProfileManager {

    public DeletingProfileManager(ObjectStore os, ObjectStoreWriter userProfileOS) {
        super(os, userProfileOS);
    }

    public void deleteProfile(Integer userId) {
        try {
            UserProfile userProfile = getUserProfile(userId);
            if (userProfile != null) {
                for (org.intermine.model.userprofile.SavedQuery sq : userProfile.getSavedQuerys()) {
                    uosw.delete(sq);
                }

                for (SavedTemplateQuery st : userProfile.getSavedTemplateQuerys()) {
                    uosw.delete(st);
                }

                for (SavedBag sb : userProfile.getSavedBags()) {
                    uosw.delete(sb);
                }

                TagManager tagManager = getTagManager();
                for (Tag tag : tagManager.getUserTags(userProfile.getUsername())) {
                    tagManager.deleteTag(tag);
                }
                SharedBagManager sbm = SharedBagManager.getInstance(this);
                sbm.removeAllSharesInvolving(userId);
                sbm.removeAllInvitesBy(userId);
                
                uosw.delete(userProfile);
            }
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
