package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryUpgrade;
import org.intermine.web.logic.session.SessionMethods;

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

    /**
     * Constructor
     *
     * @param profile The profile of the user whose lists we are upgrading.
     * @param bagQueryRunner The mechanism to search for items for the lists.
     * @param session A reference to the session to store progress information.
     */
    public UpgradeBagList(Profile profile, BagQueryRunner bagQueryRunner, HttpSession session) {
        this.profile = profile;
        this.bagQueryRunner = bagQueryRunner;
        this.session = session;
    }

    protected Map<String, Map<String, Object>> getStatus() {
        return SessionMethods.getNotCurrentSavedBagsStatus(session);
    }

    @Override
    public void run() {
        doUpgrade(getStatus());
    }

    private void doUpgrade(Map<String, Map<String, Object>> status) {
        Map<String, InterMineBag> savedBags = profile.getSavedBags();
        for (InterMineBag bag : savedBags.values()) {

            if (bag.getState().equals(BagState.NOT_CURRENT.toString())) {
                Map<String, Object> bagAttributes = new HashMap<String, Object>();

                bagAttributes.put("status", Constants.UPGRADING_BAG);

                LOG.info("Start upgrading the bag list " + bag.getName());
                status.put(bag.getName(), bagAttributes);

                BagQueryUpgrade bagQueryUpgrade = new BagQueryUpgrade(bagQueryRunner, bag);
                BagQueryResult result = bagQueryUpgrade.getBagQueryResult();
                try {
                    if (result.getUnresolved().isEmpty()
                        && (result.getIssues().isEmpty()
                            || onlyOtherIssuesAlreadyContained(result))) {
                        Map<Integer, List> matches = result.getMatches();
                        //we won't update the extra field added later
                        bag.upgradeOsb(matches.keySet(), false);
                        bagAttributes.put("status", BagState.CURRENT.toString());
                        try {
                            bagAttributes.put("size", bag.getSize());
                        } catch (ObjectStoreException e) {
                            // nothing serious happens here...
                        }
                        status.put(bag.getName(), bagAttributes);
                    } else {
                        reportResult(bag.getName(), result);
                        bag.setState(BagState.TO_UPGRADE);
                        bagAttributes.put("status", BagState.TO_UPGRADE.toString());
                        status.put(bag.getName(), bagAttributes);
                    }
                } catch (ObjectStoreException ose) {
                    LOG.warn("Could not upgrade the bags list", ose);
                }
            }
        }
    }

    protected void reportResult(String name, BagQueryResult result) {
        session.setAttribute("bagQueryResult_" + name, result);
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

            Map<String, Map<String, List>> otherMatchMap = result.getIssues()
                .get(BagQueryResult.OTHER);
            Set<Integer> matchesIds = result.getMatches().keySet();
            if (otherMatchMap != null) {
                Map<String, ArrayList<Object>> lowQualityMatches = new LinkedHashMap<String,
                ArrayList<Object>>();
                Iterator otherMatchesIter = otherMatchMap.values().iterator();
                while (otherMatchesIter.hasNext()) {
                    Map<String, ArrayList<Object>> inputToObjectsMap =
                        (Map<String, ArrayList<Object>>) otherMatchesIter.next();
                    Map<String, ArrayList<Object>> inputToObjectsMapUpdated =
                        new LinkedHashMap<String, ArrayList<Object>>();
                    for (String key : inputToObjectsMap.keySet()) {
                        ArrayList<Object> listObjects = inputToObjectsMap.get(key);
                        ArrayList<Object> listObjectsUpdated = new ArrayList<Object>();
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
