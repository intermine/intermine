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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Scope constants.
 * @author Kim Rutherford
 * @author Alex Kalderimis
 */
public final class Scope
{
    private Scope() {
        // hidden constructor. Do not instantiate.
    }

    /**
     * Global scope for public bags, templates, etc.
     */
    public static final String GLOBAL = "global";

    /**
     * User scope for private bags, templates, etc.
     */
    public static final String USER = "user";

    /**
     * User or global scope for bags, templates, etc.
     */
    public static final String ALL = "all";

    /**
     *The possible scopes.
     */
    public static final Set<String> SCOPES =
            new HashSet<String>(Arrays.asList(GLOBAL, USER, ALL));

}
