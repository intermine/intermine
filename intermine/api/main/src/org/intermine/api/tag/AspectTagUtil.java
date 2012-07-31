package org.intermine.api.tag;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Util class for working with aspect tags.
 *
 * @author Jakub Kulaviak
 */
public final class AspectTagUtil
{
    private AspectTagUtil() {
        // don't instantiate
    }

    /**
     * Extracts aspect from tag name. For instance for aspect:Miscellaneous returns Miscellaneous
     * @param tagName tag name
     * @return if it is aspect tag then returns aspect else null
     */
    public static String getAspect(String tagName) {
        if (AspectTagUtil.isAspectTag(tagName)) {
            return tagName.substring(TagNames.IM_ASPECT_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * @param tagName tag name
     * @return true if tag is aspect tag else false
     */
    public static boolean isAspectTag(String tagName) {
        return tagName.startsWith(TagNames.IM_ASPECT_PREFIX);
    }

}
