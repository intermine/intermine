package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.intermine.cache.InterMineCache;

import javax.servlet.ServletContext;

/**
 * Helper methods for ServletContext.
 * @author Kim Rutherford
 */
public class ServletMethods
{
    /**
     * Get the Global InterMineCache object.
     * @param context the ServletContext
     * @return the InterMineCache
     */
    public static InterMineCache getGlobalCache(ServletContext context) {
        return (InterMineCache) context.getAttribute(Constants.GLOBAL_CACHE);
    }
}
