package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

/**
 * An IdResolver for use in tests that will always resolve an id to the input id given.
 *
 * @author Richard Smith
 *
 */
public class DoNothingIdResolver extends IdResolver
{
    /**
     * Construct and empty IdResolver
     * @param clsName the class to resolve identifiers for
     */
    public DoNothingIdResolver(String clsName) {
        super(clsName);
    }

    @Override
    public Set<String> resolveId(String taxonId, String id) {
        Set<String> ids = new HashSet<String>();
        ids.add(id);
        return ids;
    }

    @Override
    public int countResolutions(String taxonId, String id) {
        return 1;
    }
}
