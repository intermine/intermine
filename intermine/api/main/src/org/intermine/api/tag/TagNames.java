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
 *
 * @author Kim Rutherford
 */
public interface TagNames
{
    /**
     * Tag for pubic things (bags, templates).
     */
    String IM_PUBLIC = "im:public";

    /**
     * String used to tag converter templates in the webapp.
     */
    String IM_CONVERTER = "im:converter";

    /**
     * String used to tag templates that should appear on report pages.
     */
    String IM_REPORT = "im:report";

    /**
     * Prefix common for all InterMine internal tags
     */
    String IM_PREFIX = "im:";

    /**
     * String used to tag favourite objects
     */
    String IM_FAVOURITE = "im:favourite";

    /**
     * String used to tag objects that should be hidden at jsp page
     */
    String IM_HIDDEN = "im:hidden";

    /**
     * String used to tag objects that should be hidden at jsp page
     */
    String IM_ADMIN = "im:admin";

    /**
     * String used to tag objects by some aspect
     */
    String IM_ASPECT_PREFIX = "im:aspect:";

    /**
     * String used to tag objects by Miscellaneous aspect
     */
    String IM_ASPECT_MISC = "im:aspect:Miscellaneous";

    /**
     * String used to tag objects to appear in the summary section of the report page
     */
    String IM_SUMMARY = "im:summary";

    /**
     * templates to be displayed on search results page
     */
    String IM_SEARCH_RESULTS = "im:searchresults";

    /**
     * bag with background population for specific widget 
     */
    String IM_WIDGET = "im:widget";

    /**
     * Tag separator.
     */
    String SEPARATOR = ":";
}
