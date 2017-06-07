package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * Interface providing the capability to check partial mappings for validity for the MappingUtil.
 *
 * @author Matthew Wakeling
 * @param <T> The element type
 */
public interface MappingUtilChecker<T>
{
    /**
     * Checks a partial mapping.
     *
     * @param map the mapping to check
     * @return true if the mapping is valid
     */
    boolean check(Map<T, T> map);
}
