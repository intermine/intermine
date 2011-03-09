package org.intermine.api.search;

/*
 * Copyright (C) 2002-2011 FlyMine
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
    String GLOBAL = "global";

    /**
     * User scope for private bags, templates, etc.
     */
    String USER = "user";

    /**
     * User or global scope for bags, templates, etc.
     */
    String ALL = "all";
}
