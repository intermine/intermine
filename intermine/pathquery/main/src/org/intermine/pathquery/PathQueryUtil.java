package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Utility methods for PathQuery.
 * @author Kim Rutherford
 */
public class PathQueryUtil 
{
    /**
     * Get a summary of the Problems
     * @param problems an Array of throwable
     * @return a String summary of all problems
     */
    public static String getProblemsSummary(Throwable[] problems) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < problems.length; i++) {
            sb.append(problems[i]);
        }
        return sb.toString();
    }   
}
