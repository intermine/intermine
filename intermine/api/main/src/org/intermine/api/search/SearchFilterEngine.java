package org.intermine.api.search;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.model.userprofile.Tag;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;

/**
 * Class filtering web searchables according to the associated tags.
 * @author Jakub Kulaviak <jakub@flymine.org>
 */
public class SearchFilterEngine
{

    /**
     * Given a Map from name to WebSearchable, return a Map that contains only those name,
     * WebSearchable pairs where the name is tagged with all of the tags listed.
     * @param webSearchables the Map to filter
     * @param tagNames the tag names to use for filtering
     * @param tagType the tag type (from TagTypes)
     * @param userName the user name to pass to getTags()
     * @param <W> the type of WebSearchable
     * @param tagManager tag manager used for obtaining tags
     * @return the filtered Map
     */
    public <W extends WebSearchable> Map<String, W> filterByTags(Map<String, W> webSearchables,
            List<String> tagNames, String tagType, String userName, TagManager tagManager) {
        return filterByTags(webSearchables, tagNames, tagType, userName, tagManager, true);
    }

    /**
     * Given a Map from name to WebSearchable, return a Map that contains only those name,
     * WebSearchable pairs where the name is tagged with all of the tags listed.
     * @param webSearchables the Map to filter
     * @param tagNames the tag names to use for filtering
     * @param tagType the tag type (from TagTypes)
     * @param userName the user name to pass to getTags()
     * @param <W> the type of WebSearchable
     * @param tagManager tag manager used for obtaining tags
     * @param showHidden whether or not to filter out objects tagged with hidden tag
     * @return the filtered Map
     */
    public <W extends WebSearchable> Map<String, W> filterByTags(
            Map<String, W> webSearchables, List<String> tagNames, String tagType, String userName,
            TagManager tagManager, boolean showHidden) {

        Map<String, W> returnMap = new LinkedHashMap<String, W>(webSearchables);

        // prime the cache
        for (String tagName: tagNames) {
            tagManager.getTags(tagName, null, tagType, userName);
        }
        for (String tagName: tagNames) {
            if (StringUtils.isEmpty(tagName)) {
                continue;
            }
            for (Map.Entry<String, W> entry: webSearchables.entrySet()) {
                String webSearchableName = entry.getKey();
                List<Tag> tags = tagManager.getTags(tagName, webSearchableName, tagType, userName);
                if (tags.size() == 0) {
                    returnMap.remove(webSearchableName);
                } else if (!showHidden) {
                    tags = tagManager.getTags(TagNames.IM_HIDDEN, webSearchableName, tagType,
                            userName);
                    if (tags.size() > 0) {
                        returnMap.remove(webSearchableName);
                    }
                }
            }
        }

        return returnMap;
    }

}
