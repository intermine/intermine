package org.intermine.web.logic.search;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Scope constants.
 * @author Kim Rutherford
 */
public interface Scope
{
    /**
     * Global scope for public bags, templates, etc.
     */
    public static final String GLOBAL = "global";
    
    /**
     * User scope for private bag, templates, etc.
     */
    public static final String USER = "user";
}
