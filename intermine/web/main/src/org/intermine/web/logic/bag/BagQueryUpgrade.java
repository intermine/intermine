package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.InterMineException;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.BagValue;

public class BagQueryUpgrade
{
    private static final Logger LOG = Logger.getLogger(BagQueryUpgrade.class);
    BagQueryRunner bagQueryRunner;
    private InterMineBag bag;

    public BagQueryUpgrade(BagQueryRunner bagQueryRunner, InterMineBag bag) {
        this.bagQueryRunner = bagQueryRunner;
        this.bag = bag;
    }

    public BagQueryResult getBagQueryResult() {
        BagQueryResult bagQueryResult = null;
        LOG.warn("ContentsOrderByExtraValue before: " + bag.getName());
        List<BagValue> bagValueList = bag.getContentsOrderByExtraValue();
        LOG.warn("ContentsOrderByExtraValue after: " + bag.getName());
        List<BagQueryResult> bagQueryResultList = new ArrayList<BagQueryResult>();
        List<String> primaryIdentifiersList = new ArrayList<String>();
        String extra;
        String prevExtra = "";
        try {
            for (BagValue bagValue : bagValueList) {
                extra = (bagValue.getExtra() != null) ? bagValue.getExtra() : "";
                if ("".equals(extra)) {
                    primaryIdentifiersList.add(bagValue.getValue());
                    prevExtra = extra;
                } else {
                    if ("".equals(prevExtra)) {
                        primaryIdentifiersList.add(bagValue.getValue());
                        prevExtra = extra;
                    } else {
                        if (prevExtra.equals(extra)) {
                            primaryIdentifiersList.add(bagValue.getValue());
                        } else {
                            bagQueryResultList.add(bagQueryRunner.searchForBag(bag.getType(), primaryIdentifiersList, prevExtra, false));
                            prevExtra = extra;
                            primaryIdentifiersList = new ArrayList<String>();
                            primaryIdentifiersList.add(bagValue.getValue());
                        }
                    }
                }
            }
            LOG.warn("after bagValueListCycle: " + bag.getName());
            bagQueryResultList.add(bagQueryRunner.searchForBag(bag.getType(), primaryIdentifiersList, prevExtra, false));
            bagQueryResult = combineBagQueryResult(bagQueryResultList);
        } catch (ClassNotFoundException cnfe) {
            LOG.warn("The type " + bag.getType() + "isn't in the model."
                     + "Impossible upgrade the bag list " + bag.getTitle(), cnfe);
        } catch (InterMineException ie) {
            LOG.warn("Impossible upgrade the bags list " + bag.getTitle(), ie);
        }
        LOG.warn("before returning bagQueryResult: " + bag.getName()); 
        return bagQueryResult;
    }

    private BagQueryResult combineBagQueryResult(List<BagQueryResult> bagQueryResultList) {
        BagQueryResult bagQueryResult = new BagQueryResult();
        for (BagQueryResult bqr : bagQueryResultList) {
            bagQueryResult.getMatches().putAll(bqr.getMatches());
            bagQueryResult.getIssues().putAll(bqr.getIssues());
            bagQueryResult.getUnresolved().putAll(bqr.getUnresolved());
        }
        return bagQueryResult;
    }
}
