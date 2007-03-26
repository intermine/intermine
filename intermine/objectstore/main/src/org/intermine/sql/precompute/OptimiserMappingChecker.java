package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.util.IdentityMap;
import org.intermine.util.MappingUtilChecker;

/**
 * Class for checking partial mappings for validity for the MappingUtil.
 *
 * @author Matthew Wakeling
 */
public class OptimiserMappingChecker implements MappingUtilChecker
{
    Set set1, set2;

    /**
     * Constructor.
     *
     * @param set1 the first set of constraints
     * @param set2 the second set of constraints
     */
    public OptimiserMappingChecker(Set set1, Set set2) {
        this.set1 = set1;
        this.set2 = set2;
    }
    
    /**
     * Checks a partial mapping.
     *
     * @param map the mapping to check - a Map from Table to Table
     * @return true if the mapping is valid
     */
    public boolean check(Map map) {
        Map reverseMap;
        if (map instanceof IdentityMap) {
            reverseMap = IdentityMap.INSTANCE;
        } else {
            reverseMap = new HashMap();
            Iterator mapIter = map.entrySet().iterator();
            while (mapIter.hasNext()) {
                Map.Entry entry = (Map.Entry) mapIter.next();
                reverseMap.put(entry.getValue(), entry.getKey());
            }
        }
        return QueryOptimiser.compareConstraints(set1, set2, new HashSet(), map, reverseMap);
    }
}
