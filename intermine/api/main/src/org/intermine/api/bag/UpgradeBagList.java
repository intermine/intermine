package org.intermine.api.bag;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.tracker.TrackerLogger;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ObjectStoreBag;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Runnable object providing upgrading osbag_int table. 
 * @author dbutano
 *
 */
public class UpgradeBagList implements Runnable
{
    private static final Logger LOG = Logger.getLogger(UpgradeBagList.class);
    Profile profile;
    BagQueryRunner bagQueryRunner;

    public UpgradeBagList(Profile profile, BagQueryRunner bagQueryRunner) {
        this.profile = profile;
        this.bagQueryRunner = bagQueryRunner;
    }

    public void run() {
        Map<String, InterMineBag> savedBags = profile.getSavedBags();
        InterMineBag bag;
        ObjectStoreBag osb = null;
        ProfileManager pm = profile.getProfileManager();
        ObjectStoreWriter oswProduction = null;
        ObjectStoreWriter uosw = pm.getProfileObjectStoreWriter();
        try {
            oswProduction = pm.getProductionObjectStore().getNewWriter();
            for (String nameBag : savedBags.keySet()) {
                bag = savedBags.get(nameBag);
                if (!bag.isCurrent()) {
                    SavedBag savedBag = (SavedBag) uosw.getObjectById(bag.getSavedBagId(),
                                                                      SavedBag.class);
                    List<String> primaryIdentifiersList =
                        bag.getContentsASPrimaryIdentifierValues();
                    try {
                        BagQueryResult result = bagQueryRunner.searchForBag(bag.getType(),
                                                primaryIdentifiersList, "", false);
                        Map<Integer, List> matches = result.getMatches();
                        if (!matches.isEmpty()) {
                            osb = oswProduction.createObjectStoreBag();
                            for (int value : matches.keySet()) {
                                oswProduction.addToBag(osb, value);
                            }
                            savedBag.setOsbId(osb.getBagId());
                            if (result.getIssues().isEmpty()) {
                                savedBag.setCurrent(true);
                            }
                        }
                        uosw.store(savedBag);
                        bag.setOsb(osb);
                    } catch (ClassNotFoundException cnfe) {
                        LOG.warn("The type " + bag.getType() + "isn't in the model."
                                 + "Impossible upgrade the list " + bag.getTitle(), cnfe);
                    } catch (InterMineException ie) {
                        LOG.warn("Impossible upgrade the list " + bag.getTitle(), ie);
                    }
                }
            }
        } catch (ObjectStoreException ose) {
            LOG.warn("Impossible upgrade the list bags", ose);
        } finally {
            if (oswProduction != null) {
                try {
                    oswProduction.close();
                } catch (ObjectStoreException os) {
                }
            }
        }
    }
}
