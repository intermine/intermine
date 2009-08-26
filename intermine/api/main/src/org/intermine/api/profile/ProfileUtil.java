package org.intermine.api.profile;

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.bag.InterMineBag;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.tag.TagTypes;

public class ProfileUtil {

    /**
     * @param searchRepository search repository
     * @param userBags list of user's bags
     * @return map containing all bags
     */
    public static Map<String, InterMineBag> getAllBags(Map<String, InterMineBag> userBags,
                                                       SearchRepository searchRepository) {
        Map<String, InterMineBag> searchBags = new HashMap<String, InterMineBag>();

        Map<String, InterMineBag> publicBagMap =
            (Map<String, InterMineBag>) searchRepository.getWebSearchableMap(TagTypes.BAG);

        if (publicBagMap != null) {
            searchBags.putAll(publicBagMap);
        }

        // user bags override public ones
        searchBags.putAll(userBags);
        return searchBags;
    }
}
