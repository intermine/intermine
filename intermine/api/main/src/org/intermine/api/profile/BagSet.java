package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * A class containing valiud and invalid bags.
 * @author Alex Kalderimis.
 *
 */
public class BagSet
{

    private final Map<String, InterMineBag> bags;
    private final Map<String, InvalidBag> invalidBags;

    /**
     * Constructor
     * @param goodBags a map from bag name to valid bag
     * @param badBags a map from bag name to valid bag
     */
    public BagSet(Map<String, InterMineBag> goodBags,
                  Map<String, InvalidBag> badBags) {
        this.bags = goodBags;
        this.invalidBags = badBags;
    }

    /** @return the good bags **/
    public Map<String, InterMineBag> getBags() {
        return bags;
    }

    /** @return the invalid bags **/
    public Map<String, InvalidBag> getInvalidBags() {
        return invalidBags;
    }

}
