package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.bag.BagQueryUpgrade;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Runnable object providing upgrading osbag_int table.
 * @author Daniela Butano
 *
 */
public class UpgradeBagList implements Runnable
{
    private static final Logger LOG = Logger.getLogger(UpgradeBagList.class);
    private Profile profile;
    private BagQueryRunner bagQueryRunner;

    /**
     * Constructor
     *
     * @param profile The profile of the user whose lists we are upgrading.
     * @param bagQueryRunner The mechanism to search for items for the lists.
     */
    public UpgradeBagList(Profile profile, BagQueryRunner bagQueryRunner) {
        this.profile = profile;
        this.bagQueryRunner = bagQueryRunner;
    }

    @Override
    public void run() {
        Map<String, InterMineBag> savedBags = profile.getSavedBags();
        for (InterMineBag bag : savedBags.values()) {
            if (isBagNeedUpgrade(bag)) {
                String bagName = bag.getName();
                BagQueryUpgrade bagQueryUpgrade = new BagQueryUpgrade(bagQueryRunner, bag);
                BagQueryResult result = bagQueryUpgrade.getBagQueryResult();
                try {
                    if (result.getUnresolvedIdentifiers().isEmpty()
                        && (result.getIssues().isEmpty()
                            || onlyOtherIssuesAlreadyContained(result))) {
                        @SuppressWarnings("rawtypes")
                        Map<Integer, List> matches = result.getMatches();
                        //we don't need to update the extra field added later
                        bag.upgradeOsb(matches.keySet(), false);
                    } else {
                        bag.setState(BagState.TO_UPGRADE);
                    }
                } catch (ObjectStoreException ose) {
                    LOG.warn("Could not upgrade the list " + bagName, ose);
                }
            }
        }
    }

    private boolean isBagNeedUpgrade(InterMineBag bag) {
        synchronized (bag) {
            if (bag.getState().equals(BagState.NOT_CURRENT.toString())) {
                try {
                    bag.setState(BagState.UPGRADING);
                } catch (ObjectStoreException ose) {
                    LOG.error("Problem to update the status to UPGRADING for list "
                        + bag.getName(), ose);
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Verify that the only issues existing have type OTHER and the ids contained already
     * exist in the list.
     * If the condition is verified the list can be upgraded automatically
     * @param result
     * @return
     */
    private boolean onlyOtherIssuesAlreadyContained(BagQueryResult result) {
        if (result.getIssues().get(BagQueryResult.DUPLICATE) == null
            && result.getIssues().get(BagQueryResult.TYPE_CONVERTED) == null
            && result.getIssues().get(BagQueryResult.WILDCARD) == null) {

            @SuppressWarnings("rawtypes")
            Map<String, Map<String, List>> otherMatchMap = result.getIssues()
                .get(BagQueryResult.OTHER);
            Set<Integer> matchesIds = result.getMatches().keySet();
            if (otherMatchMap != null) {
                @SuppressWarnings("rawtypes")
                Map<String, List> lowQualityMatches = new LinkedHashMap<String, List>();
                @SuppressWarnings("rawtypes")
                Iterator<Map<String, List>> otherMatchesIter = otherMatchMap.values().iterator();
                while (otherMatchesIter.hasNext()) {
                    @SuppressWarnings("rawtypes")
                    Map<String, List> inputToObjectsMap = otherMatchesIter.next();
                    @SuppressWarnings("rawtypes")
                    Map<String, List> inputToObjectsMapUpdated = new LinkedHashMap<String, List>();
                    for (String key : inputToObjectsMap.keySet()) {
                        @SuppressWarnings("rawtypes")
                        List listObjects = inputToObjectsMap.get(key);
                        List<Object> listObjectsUpdated = new ArrayList<Object>();
                        for (Object obj : listObjects) {
                            InterMineObject intermineObj = (InterMineObject) obj;
                            if (matchesIds.isEmpty()
                                || !matchesIds.contains(intermineObj.getId())) {
                                listObjectsUpdated.add(obj);
                            }
                        }
                        if (!listObjectsUpdated.isEmpty()) {
                            inputToObjectsMapUpdated.put(key, listObjects);
                        }
                    }
                    if (!inputToObjectsMapUpdated.isEmpty()) {
                        lowQualityMatches.putAll(inputToObjectsMapUpdated);
                    }
                }
                if (lowQualityMatches.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
