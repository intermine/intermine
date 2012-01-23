package org.intermine.api.profile;

import java.util.Map;
import java.util.Set;

public class BagSet {

    private final Map<String, InterMineBag> bags;
    private final Map<String, InvalidBag> invalidBags;

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
