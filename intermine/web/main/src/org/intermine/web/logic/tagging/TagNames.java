package org.intermine.web.logic.tagging;

/*
 * Copyright (C) 2002-2009 FlyMine
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
    public static final String IM_PUBLIC = "im:public";

    /**
     * String used to tag converter templates in the webapp.
     */
    public static final String IM_CONVERTER = "im:converter";

    /**
     * String used to tag templates that should not appear on report pages.
     */
    public static final String IM_NO_REPORT = "im:noreport";
    
    /**
     * Prefix common for all InterMine internal tags  
     */
    public static final String IM_PREFIX = "im:";

    /**
     * String used to tag favourite objects  
     */
    public static final String IM_FAVOURITE = "im:favourite";

    /**
     * String used to tag objects that should be hidden at jsp page  
     */
    public static final String IM_HIDDEN = "im:hidden";

    /**
     * String used to tag objects by some aspect  
     */
    public static final String IM_ASPECT_PREFIX = "im:aspect:";

    /**
     * String used to tag objects by Miscellaneous aspect  
     */
    public static final String IM_ASPECT_MISC = "im:aspect:Miscellaneous";

    /**
     * String used to tag objects to appear in the summary section of the report page  
     */
    public static final String IM_SUMMARY = "im:summary";

    /**
     * Tag separator. 
     */
    public static final String SEPARATOR = ":";
    
    
}
