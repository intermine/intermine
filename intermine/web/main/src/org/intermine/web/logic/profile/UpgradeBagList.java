package org.intermine.web.logic.profile;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
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
    private Profile profile;
    private BagQueryRunner bagQueryRunner;
    private HttpSession session;
    

    public UpgradeBagList(Profile profile, BagQueryRunner bagQueryRunner, HttpSession session) {
        this.profile = profile;
        this.bagQueryRunner = bagQueryRunner;
        this.session = session;
    }

    public void run() {
        Map<String, InterMineBag> savedBags = profile.getSavedBags();
        for (InterMineBag bag : savedBags.values()) {
            if (!bag.isCurrent()) {
                List<String> primaryIdentifiersList =
                    bag.getContentsASPrimaryIdentifierValues();
                try {
                    BagQueryResult result = bagQueryRunner.searchForBag(bag.getType(),
                                            primaryIdentifiersList, "", false);
                    Map<Integer, List> matches = result.getMatches();
                    if (!matches.isEmpty()) {
                        if (result.getIssues().isEmpty() && result.getUnresolved().isEmpty()) {
                        	bag.upgradeOsb(matches.keySet(), true);
                        } else {
                        	bag.upgradeOsb(matches.keySet());
                        	session.setAttribute("bagQueryResult_" + bag.getName(), result);
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    LOG.warn("The type " + bag.getType() + "isn't in the model."
                             + "Impossible upgrade the list " + bag.getTitle(), cnfe);
                } catch (InterMineException ie) {
                    LOG.warn("Impossible upgrade the list " + bag.getTitle(), ie);
                } catch (ObjectStoreException ose) {
                    LOG.warn("Impossible upgrade the list bags", ose);
                } 
            }
        }
    }
}
